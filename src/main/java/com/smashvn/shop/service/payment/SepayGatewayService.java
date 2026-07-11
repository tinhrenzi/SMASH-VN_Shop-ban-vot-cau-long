package com.smashvn.shop.service.payment;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.api.GhnService;

import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.exception.InvalidPaymentException;
import com.smashvn.shop.exception.OrderNotFoundException;
import com.smashvn.shop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayGatewayService implements PaymentGatewayService {

    private final SepayConfig sepayConfig;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;
    private final GhnService ghnService;

    @Override
    @Transactional
    public Map<String, Object> handleIpn(SepayIpnRequest request, String rawPayload) throws Exception {
        SepayTransactionDto transaction = request.getTransactionData();
        String transactionId = transaction.getTransactionId();
        BigDecimal transferAmount = transaction.getTransferAmount();
        String gateway = transaction.getGateway();

        // 1. Fast duplicate check (Idempotency)
        Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByTransactionId(transactionId);
        if (existingTx.isPresent()) {
            if ("success".equalsIgnoreCase(existingTx.get().getStatus())) {
                log.info("SePay IPN: Transaction ID {} already successfully processed (duplicate check).", transactionId);
                auditService.log(null, "PaymentTransaction", null, "UPDATE", 
                        null, "PAID", "127.0.0.1", "[PAYMENT_DUPLICATE] Duplicate IPN callback ignored.", "SYSTEM");
                return createSuccessResponse("Already processed");
            } else {
                log.info("SePay IPN: Deleting previously failed transaction record {} (status: {}) to re-process.", 
                        transactionId, existingTx.get().getStatus());
                paymentTransactionRepository.delete(existingTx.get());
                paymentTransactionRepository.flush();
            }
        }

        // 2. Locate order by maDonHang
        String orderCode = extractOrderCode(transaction.getCode(), transaction.getContent());
        if (orderCode == null) {
            log.warn("SePay IPN: Could not extract order code from transaction Code: {} or Content: {}", 
                    transaction.getCode(), transaction.getContent());
            throw new OrderNotFoundException("Could not extract order code from transaction info.");
        }

        log.info("SePay IPN: Processing transaction {} for order code {}", transactionId, orderCode);

        String normalizedCode = orderCode.replace("-", "").replace("_", "").trim().toUpperCase();
        HoaDon order = hoaDonRepository.findByMaDonHangOrNormalized(orderCode, normalizedCode)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with code: " + orderCode));

        // 3. Validation
        // 3.1 Reject negative/zero transfer amounts
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Transferred amount must be positive. Received: " + transferAmount);
        }

        // 3.2 Verify transfer amount matches order total (compare by numeric value, ignoring scale/decimal notation)
        BigDecimal normalizedTransfer = transferAmount.stripTrailingZeros();
        BigDecimal normalizedOrder = order.getTongTien().stripTrailingZeros();
        if (normalizedTransfer.compareTo(normalizedOrder) != 0) {
            throw new InvalidPaymentException("Transferred amount " + transferAmount + " does not match order total " + order.getTongTien());
        }

        // 3.3 Validate that the order's paymentStatus is not already PAID
        if (isOrderAlreadyPaid(order)) {
            log.info("SePay IPN: Order {} already paid. Returning success.", orderCode);
            return createSuccessResponse("Already processed");
        }

        // 3.4 Handle POS Order (Custom flow)
        boolean isPosOrder = order.getMaDonHang() != null && order.getMaDonHang().startsWith("HDSVN");
        if (isPosOrder) {
            order.setPaymentStatus(PaymentStatus.PAID.getValue());
            order.setTrangThaiThanhToan("DA_THANH_TOAN");
            order.setTransactionId(transactionId);
            order.setMaGiaoDich(transactionId);
            order.setPaidAt(LocalDateTime.now());
            order.setThoiGianXacNhan(LocalDateTime.now());
            order.setNguoiXacNhanThanhToan("SePay Gateway (POS)");
            order.setTrangThaiDonHang(OrderStatus.DA_GIAO.getValue()); // POS -> hoan thanh luon
            
            if (sepayConfig.isDebug()) {
                order.setGatewayResponse("SePay: Successful POS payment processed. Ref: " + transactionId);
            }
            hoaDonRepository.save(order);

            // Save transaction record in db
            saveTransactionRecord(transaction, order, "success", rawPayload);

            // Log PAYMENT_CONFIRMED
            auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                    OrderStatus.CHO_THANH_TOAN.getValue(), OrderStatus.DA_GIAO.getValue(), "127.0.0.1",
                    "[PAYMENT_CONFIRMED_POS] POS payment success callback handled.", "SYSTEM");

            log.info("SePay IPN POS Success: Applied to order {}", orderCode);
            return createSuccessResponse("Processed");
        }

        // 4. Handle Cancelled Order
        if (OrderStatus.DA_HUY.getValue().equals(order.getTrangThaiDonHang())) {
            log.warn("SePay IPN: Payment received for already cancelled order: {}", orderCode);
            
            // Save transaction record
            PaymentTransaction tx = saveTransactionRecord(transaction, order, PaymentStatus.PAID_RECEIVED_AFTER_CANCEL.getValue(), rawPayload);

            // Mark payment status = PAID_RECEIVED_AFTER_CANCEL (Do not reactivate order, do not deduct stock)
            order.setPaymentStatus(PaymentStatus.PAID_RECEIVED_AFTER_CANCEL.getValue());
            order.setTrangThaiThanhToan("HUY"); // Keeps payment status synced
            hoaDonRepository.save(order);

            // Create urgent admin alert
            auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                    "da_huy", "da_huy", "127.0.0.1", 
                    "[PAYMENT_RECEIVED_AFTER_CANCEL] CRITICAL: Payment received after order cancellation. Ref: " + transactionId + ", Amt: " + transferAmount, 
                    "SYSTEM");
            
            return createSuccessResponse("Processed");
        }

        // 5. Normal Payment Processing
        List<HoaDonChiTiet> orderItems = hoaDonChiTietRepository.findByHoaDon_Id(order.getId());

        // Verify stock is sufficient
        boolean stockSufficient = true;
        for (HoaDonChiTiet item : orderItems) {
            SanPhamChiTiet spct = item.getSanPhamChiTiet();
            if (spct != null) {
                Optional<SanPhamChiTiet> lockedSpctOpt = sanPhamChiTietRepository.findByIdWithLock(spct.getId());
                if (lockedSpctOpt.isPresent()) {
                    if (lockedSpctOpt.get().getSoLuongTon() < item.getSoLuong()) {
                        stockSufficient = false;
                        break;
                    }
                } else {
                    stockSufficient = false;
                    break;
                }
            }
        }

        String targetOrderStatus = OrderStatus.CHO_XAC_NHAN.getValue();
        if (stockSufficient) {
            // Deduct stock
            for (HoaDonChiTiet item : orderItems) {
                SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct != null) {
                    SanPhamChiTiet lockedSpct = sanPhamChiTietRepository.findByIdWithLock(spct.getId()).get();
                    lockedSpct.setSoLuongTon(lockedSpct.getSoLuongTon() - item.getSoLuong());
                    sanPhamChiTietRepository.save(lockedSpct);
                }
            }
        } else {
            // Stock conflict
            targetOrderStatus = OrderStatus.STOCK_CONFLICT.getValue();
            log.warn("[STOCK_CONFLICT] SePay: Insufficient stock for order #{} on payment success. Set status to stock_conflict.", order.getId());
        }

        // Update statuses to PAID and target status
        order.setPaymentStatus(PaymentStatus.PAID.getValue());
        order.setTrangThaiThanhToan("DA_THANH_TOAN");
        order.setTransactionId(transactionId);
        order.setMaGiaoDich(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setThoiGianXacNhan(LocalDateTime.now());
        order.setNguoiXacNhanThanhToan("SePay Gateway");
        order.setTrangThaiDonHang(targetOrderStatus);
        
        if (sepayConfig.isDebug()) {
            order.setGatewayResponse("SePay: Successful payment processed. Ref: " + transactionId);
        }
        hoaDonRepository.save(order);

        // Clear items from customer's cart
        clearCustomerCart(order, orderItems);

        // Save transaction record in db (Catch race condition database exceptions)
        saveTransactionRecord(transaction, order, "success", rawPayload);

        // Log PAYMENT_CONFIRMED
        auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                OrderStatus.CHO_THANH_TOAN.getValue(), OrderStatus.CHO_XAC_NHAN.getValue(), "127.0.0.1",
                "[PAYMENT_CONFIRMED] Payment success callback handled. Cart items removed.", "SYSTEM");

        log.info("SePay IPN: Payment successfully applied to order {}", orderCode);

        // Tạo đơn GHN nếu đơn vị vận chuyển là GHN và chưa tạo mã vận đơn
        boolean isGhn = order.getDonViVanChuyen() != null 
                && (order.getDonViVanChuyen().getTenDonVi().toUpperCase().contains("GHN") 
                    || order.getDonViVanChuyen().getTenDonVi().toUpperCase().contains("GIAO HÀNG NHANH"));
        if (isGhn && (order.getGhnOrderCode() == null || order.getGhnOrderCode().isBlank())) {
            try {
                String ghnCode = ghnService.createShippingOrder(
                        order, orderItems, order.getGhnToDistrictId(), order.getGhnToWardCode());
                if (ghnCode != null) {
                    order.setGhnOrderCode(ghnCode);
                    order.setGhnStatus("ready_to_pick");
                    hoaDonRepository.save(order);
                    log.info("[GHN] Tạo đơn vận chuyển GHN sau SePay thành công: orderId={}, ghnCode={}",
                            order.getId(), ghnCode);
                }
            } catch (Exception ghnEx) {
                log.error("[GHN] Lỗi tạo đơn GHN sau SePay: orderId={}, error={}",
                        order.getId(), ghnEx.getMessage());
                // Không throw – lỗi GHN không làm hỏng thanh toán
            }
        }

        // Mask account number in logs
        String maskedAccount = maskAccountNumber(transaction.getAccountNumber());
        log.info("SePay IPN Success: TxId: {}, Order: {}, Amount: {}, Account: {}, Gateway: {}", 
                transactionId, orderCode, transferAmount, maskedAccount, gateway);

        return createSuccessResponse("Processed");
    }

    private PaymentTransaction saveTransactionRecord(SepayTransactionDto transactionDto, HoaDon order, String status, String rawPayload) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(transactionDto.getTransactionId());
        tx.setOrder(order);
        tx.setAmount(transactionDto.getTransferAmount());
        tx.setGateway(transactionDto.getGateway());
        tx.setStatus(status);
        tx.setCreatedAt(LocalDateTime.now());
        
        if (sepayConfig.isDebug()) {
            tx.setRawPayload(rawPayload);
        } else {
            tx.setRawPayload(null);
        }

        try {
            return paymentTransactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException ex) {
            log.warn("SePay IPN: Database unique constraint hit on transaction_id: {}", transactionDto.getTransactionId());
            // Rethrow a duplicate check or custom handler depending on callers, here we just allow transaction rollback or let caller return duplicate response
            throw ex;
        }
    }

    private void clearCustomerCart(HoaDon order, List<HoaDonChiTiet> orderItems) {
        try {
            GioHang gioHang = gioHangRepository.findByKhachHang_Id(order.getKhachHang().getId());
            if (gioHang != null) {
                for (HoaDonChiTiet orderItem : orderItems) {
                    GioHangChiTiet cartItem = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(
                            gioHang.getId(), orderItem.getSanPhamChiTiet().getId());
                    if (cartItem != null) {
                        gioHangChiTietRepository.delete(cartItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("SePay IPN: Error clearing cart for order {}: ", order.getMaDonHang(), e);
        }
    }

    private String extractOrderCode(String code, String content) {
        String parsedCode = parseCode(code);
        String parsedContent = parseCode(content);

        if (parsedCode != null && parsedContent != null) {
            // Prefer the longer match, which represents the full order code including date and UUID suffix
            return parsedCode.length() >= parsedContent.length() ? parsedCode : parsedContent;
        }
        return parsedCode != null ? parsedCode : parsedContent;
    }

    private String parseCode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        // Match HDSVN (POS orders), DHSVN or DH followed by YYYYMMDDHHMMSS date and 6 hex chars
        Pattern pattern = Pattern.compile("(HDSVN|DHSVN|DH)[-_\\s]*\\d+[-_\\s]*[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).replaceAll("\\s+", "").toUpperCase();
        }
        
        // General fallback to matches beginning with HDSVN, DHSVN or DH and digits/letters (at least 8 chars to avoid collision)
        Pattern generalPattern = Pattern.compile("(HDSVN|DHSVN|DH)[-_\\s]*[A-Z0-9]{8,}", Pattern.CASE_INSENSITIVE);
        Matcher generalMatcher = generalPattern.matcher(text);
        if (generalMatcher.find()) {
            return generalMatcher.group(0).replaceAll("\\s+", "").toUpperCase();
        }
        
        return null;
    }

    private boolean isOrderAlreadyPaid(HoaDon order) {
        return order != null
                && "DA_THANH_TOAN".equalsIgnoreCase(order.getTrangThaiThanhToan());
    }

    private String maskAccountNumber(String acc) {
        if (acc == null || acc.length() < 4) {
            return "****";
        }
        return "*".repeat(acc.length() - 4) + acc.substring(acc.length() - 4);
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        return resp;
    }
}
