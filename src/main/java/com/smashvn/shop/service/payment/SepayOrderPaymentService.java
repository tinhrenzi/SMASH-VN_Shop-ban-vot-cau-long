package com.smashvn.shop.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dto.inventory.AllocationResult;
import com.smashvn.shop.dto.inventory.AllocationStatus;
import com.smashvn.shop.dto.inventory.LotAllocation;
import com.smashvn.shop.dto.inventory.OrderItemRequest;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.inventory.InventoryLotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SepayOrderPaymentService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InventoryLotService inventoryLotService;
    private final AuditService auditService;

    /**
     * Điều phối xử lý IPN Webhook từ SePay trong 1 Transaction kín.
     * Kiểm tra Idempotency dựa trên maGiaoDich SePay.
     * Nếu mã giao dịch mới trên đơn YEU_CAU_HUY ➔ Vẫn lưu vết nhận tiền và giữ luồng chờ hoàn tiền.
     * Nếu đủ kho ➔ Phân bổ FIFO, thay HDCT tạm bằng HDCT phân bổ thực tế, chuyển DA_THANH_TOAN.
     * Nếu thiếu kho ➔ INSUFFICIENT_STOCK: Commit lưu vết nhận tiền, chuyển YEU_CAU_HUY, gán RefundStatus = PENDING, KHÔNG rollback.
     */
    @Transactional
    public boolean xuLyThanhToanSePay(Integer idHoaDon, String maGiaoDich, BigDecimal soTien, String rawPayload) {
        log.info("[SepayOrderPaymentService] Đang xử lý IPN cho Đơn hàng #{}, mã giao dịch: {}", idHoaDon, maGiaoDich);

        // Step 1: Tra cứu và khóa bản ghi HoaDon
        HoaDon order = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // Step 2: Idempotency Check dựa trên maGiaoDich SePay
        Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByTransactionId(maGiaoDich);
        if (existingTx.isPresent()) {
            log.info("[SepayOrderPaymentService] Mã giao dịch SePay '{}' đã được xử lý trước đó. Bỏ qua duplicate IPN.", maGiaoDich);
            return true;
        }

        // Step 3: Đọc danh sách HoaDonChiTiet tạm thời (provisional)
        List<HoaDonChiTiet> provisionalItems = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
        if (provisionalItems.isEmpty()) {
            log.warn("[SepayOrderPaymentService] Đơn hàng #{} không có dòng sản phẩm nào", idHoaDon);
            return false;
        }

        // Xây dựng danh sách OrderItemRequest để gọi InventoryLotService.allocateFifo
        List<OrderItemRequest> itemRequests = new ArrayList<>();
        for (HoaDonChiTiet hdct : provisionalItems) {
            itemRequests.add(OrderItemRequest.builder()
                    .sourceLineId(hdct.getId())
                    .representativeSpctId(hdct.getSanPhamChiTiet().getId())
                    .quantity(hdct.getSoLuong())
                    .build());
        }

        // Step 4: Gọi InventoryLotService.allocateFifo (đã khóa các sản phẩm cha theo ID ASC)
        AllocationResult allocationResult = inventoryLotService.allocateFifo(itemRequests);

        LocalDateTime now = LocalDateTime.now();

        if (allocationResult.status() == AllocationStatus.SUCCESS) {
            // Trường hợp 1: TỒN KHO ĐỦ ➔ Phân bổ FIFO thành công
            // Lưu vết PaymentTransaction SUCCESS
            PaymentTransaction tx = new PaymentTransaction();
            tx.setOrder(order);
            tx.setTransactionId(maGiaoDich);
            tx.setAmount(soTien);
            tx.setStatus("SUCCESS");
            tx.setGateway("SePay");
            tx.setRawPayload(rawPayload);
            tx.setCreatedAt(now);
            paymentTransactionRepository.save(tx);

            // Tạo các HoaDonChiTiet thực tế theo từng LotAllocation
            Map<Integer, HoaDonChiTiet> provisionalMap = new HashMap<>();
            for (HoaDonChiTiet pItem : provisionalItems) {
                provisionalMap.put(pItem.getId(), pItem);
            }

            for (LotAllocation alloc : allocationResult.allocations()) {
                HoaDonChiTiet sourceItem = provisionalMap.get(alloc.sourceLineId());
                if (sourceItem == null) {
                    sourceItem = provisionalItems.get(0);
                }

                HoaDonChiTiet allocatedHdct = new HoaDonChiTiet();
                allocatedHdct.setHoaDon(order);
                allocatedHdct.setSanPhamChiTiet(alloc.allocatedSpct());
                allocatedHdct.setSoLuong(alloc.quantityAllocated());
                allocatedHdct.setDonGia(sourceItem.getDonGia());
                allocatedHdct.setGiaGoc(sourceItem.getGiaGoc());
                allocatedHdct.setGiaSauGiam(sourceItem.getGiaSauGiam());
                allocatedHdct.setTenSanPhamSnapshot(sourceItem.getTenSanPhamSnapshot());
                allocatedHdct.setSkuSnapshot(sourceItem.getSkuSnapshot());
                allocatedHdct.setTenDotGiamGiaSnapshot(sourceItem.getTenDotGiamGiaSnapshot());
                allocatedHdct.setThuocTinhSnapshot(sourceItem.getThuocTinhSnapshot());
                allocatedHdct.setNgayTao(now);

                hoaDonChiTietRepository.save(allocatedHdct);
            }

            // Xóa các dòng HoaDonChiTiet tạm thời
            hoaDonChiTietRepository.deleteAll(provisionalItems);

            // Cập nhật trạng thái đơn hàng
            order.setTrangThaiThanhToan("DA_THANH_TOAN");
            order.setNgayThanhToan(now);
            order.setMaGiaoDich(maGiaoDich);
            if ("CHO_THANH_TOAN".equals(order.getTrangThaiDonHang())) {
                order.setTrangThaiDonHang("DA_THANH_TOAN");
            }
            hoaDonRepository.save(order);

            String note = String.format("Thanh toán SePay thành công cho đơn #%d, mã GD: %s, số tiền: %s", idHoaDon, maGiaoDich, soTien);
            auditService.log(null, "HoaDon", idHoaDon.longValue(), "SEPAY_PAID_SUCCESS", "", "DA_THANH_TOAN", "127.0.0.1", note, "SYSTEM");

            log.info("[SepayOrderPaymentService] Đã xử lý thanh toán thành công và trừ kho FIFO cho Đơn #{}", idHoaDon);
            return true;

        } else {
            // Trường hợp 2: TỒN KHO THIẾU ➔ INSUFFICIENT_STOCK
            // KHÔNG ném Exception, COMMIT TRANSACTION để lưu giao dịch đã nhận tiền
            PaymentTransaction tx = new PaymentTransaction();
            tx.setOrder(order);
            tx.setTransactionId(maGiaoDich);
            tx.setAmount(soTien);
            tx.setStatus("PAID_INSUFFICIENT_STOCK");
            tx.setGateway("SePay");
            tx.setRawPayload(rawPayload);
            tx.setCreatedAt(now);
            paymentTransactionRepository.save(tx);

            // Giữ nguyên HDCT tạm, KHÔNG trừ kho, chuyển trạng thái bị chặn giao hàng
            order.setTrangThaiDonHang("YEU_CAU_HUY");
            order.setTrangThaiThanhToan("CHO_HOAN_TIEN");
            order.setRefundStatus(RefundStatus.PENDING);
            order.setMaGiaoDich(maGiaoDich);
            order.setLyDoHoanTien("Đã nhận tiền chuyển khoản SePay nhưng kho không đủ tồn tự động trừ. Chuyển hoàn tiền thủ công.");
            hoaDonRepository.save(order);

            String note = String.format("Đã nhận tiền SePay (%s) cho đơn #%d nhưng thiếu tồn kho tự động. Đơn chuyển YEU_CAU_HUY chờ hoàn tiền.", maGiaoDich, idHoaDon);
            auditService.log(null, "HoaDon", idHoaDon.longValue(), "SEPAY_PAID_INSUFFICIENT_STOCK", "", "YEU_CAU_HUY", "127.0.0.1", note, "SYSTEM");


            log.warn("[SepayOrderPaymentService] Đơn #{} nhận tiền thành công nhưng thiếu tồn kho. Chuyển YEU_CAU_HUY & CHO_HOAN_TIEN. Lý do: {}", idHoaDon, allocationResult.message());
            return true; // Đã xử lý ghi nhận xong
        }
    }

    /**
     * Hoàn tiền cho đơn hàng SePay thiếu kho sau khi đã hoàn tiền thủ công cho khách:
     * Chuyển trạng thái sang DA_HUY & REFUNDED mà TUYỆT ĐỐI KHÔNG gọi hoanKhoHangLoat (vì đơn chưa từng trừ kho).
     */
    @Transactional
    public void finalizeRefundWithoutRestock(Integer idHoaDon, Integer actingUserId) {
        HoaDon order = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // Kiểm tra xem đơn có giao dịch SePay nhận tiền và đang ở YEU_CAU_HUY
        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_Id(idHoaDon);
        boolean hasInsufficientStockTx = txs.stream().anyMatch(t -> "PAID_INSUFFICIENT_STOCK".equals(t.getStatus()) || "SUCCESS".equals(t.getStatus()));

        if (!hasInsufficientStockTx && !"YEU_CAU_HUY".equals(order.getTrangThaiDonHang())) {
            log.warn("[SepayOrderPaymentService] Đơn #{} không đủ điều kiện finalizeRefundWithoutRestock", idHoaDon);
            return;
        }

        order.setTrangThaiDonHang("DA_HUY");
        order.setTrangThaiThanhToan("REFUNDED");
        order.setRefundStatus(RefundStatus.COMPLETED);
        order.setLyDoHuy("Đã hoàn tiền cho khách thành công (Đơn nhận tiền nhưng thiếu kho)");
        hoaDonRepository.save(order);

        String note = "Đã hoàn tiền thành công cho đơn SePay thiếu kho. Chuyển DA_HUY mà KHÔNG cộng tồn kho.";
        auditService.log(actingUserId, "HoaDon", idHoaDon.longValue(), "FINALIZE_REFUND_NO_RESTOCK",
                "YEU_CAU_HUY", "DA_HUY", "127.0.0.1", note, "ADMIN");


        log.info("[SepayOrderPaymentService] Đã chuyển Đơn #{} sang DA_HUY & REFUNDED mà KHÔNG cộng tồn kho.", idHoaDon);
    }
}
