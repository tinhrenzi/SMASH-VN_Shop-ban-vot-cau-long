package com.smashvn.shop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.ZaloPayConfig;
import com.smashvn.shop.dto.ZaloPayResponseDTO;
import com.smashvn.shop.dto.ZaloPayCallbackDTO;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloPayServiceImpl implements ZaloPayService {

    private final ZaloPayConfig zaloPayConfig;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final AuditService auditService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public ZaloPayResponseDTO createOrder(Integer orderId) throws Exception {
        HoaDon hd = hoaDonRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));

        // State validation
        if ("PAID".equals(hd.getPaymentStatus())) {
            throw new RuntimeException("Đơn hàng này đã được thanh toán!");
        }

        // Generate app_trans_id: yymmdd_orderId_time
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String appTransId = dateStr + "_" + orderId + "_" + (System.currentTimeMillis() % 100000);

        long amount = hd.getTongTien().longValue();

        String dynamicBaseUrl = getDynamicBaseUrl();
        String redirectUrl = (dynamicBaseUrl != null) ? (dynamicBaseUrl + "/user/my-order") : zaloPayConfig.getRedirectUrl();
        String callbackUrl = (dynamicBaseUrl != null) ? (dynamicBaseUrl + "/api/payment/zalopay/callback") : zaloPayConfig.getCallbackUrl();

        Map<String, Object> embedData = new HashMap<>();
        embedData.put("redirecturl", redirectUrl);

        String embedDataStr = objectMapper.writeValueAsString(embedData);
        String itemStr = "[]"; // Empty JSON array for simplicity
        String appUser = "smashvn_customer_" + hd.getKhachHang().getId();
        long appTime = System.currentTimeMillis();

        // Sign data: app_id + "|" + app_trans_id + "|" + app_user + "|" + amount + "|" + app_time + "|" + embed_data + "|" + item
        String rawData = zaloPayConfig.getAppId() + "|" +
                appTransId + "|" +
                appUser + "|" +
                amount + "|" +
                appTime + "|" +
                embedDataStr + "|" +
                itemStr;

        String mac = hmacSha256(rawData, zaloPayConfig.getKey1());

        // Call ZaloPay API
        Map<String, Object> params = new HashMap<>();
        params.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
        params.put("app_user", appUser);
        params.put("app_trans_id", appTransId);
        params.put("app_time", appTime);
        params.put("amount", amount);
        params.put("item", itemStr);
        params.put("embed_data", embedDataStr);
        params.put("description", "Thanh toan don hang #" + orderId);
        params.put("bank_code", ""); // empty to open ZaloPay portal
        params.put("callback_url", callbackUrl);
        params.put("mac", mac);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Build URL encoded request body
        StringBuilder formBody = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (formBody.length() > 0) {
                formBody.append("&");
            }
            formBody.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        HttpEntity<String> request = new HttpEntity<>(formBody.toString(), headers);
        log.info("ZaloPay: Requesting create order for invoice #{}", orderId);

        String responseStr = restTemplate.postForObject(zaloPayConfig.getCreateOrderUrl(), request, String.class);
        Map<String, Object> responseMap = objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        Integer returnCode = (Integer) responseMap.get("return_code");
        if (returnCode == null || returnCode != 1) {
            throw new RuntimeException("Lỗi tạo giao dịch ZaloPay: " + responseMap.get("return_message"));
        }

        String paymentUrl = (String) responseMap.get("order_url");
        String qrCode = (String) responseMap.get("order_url");

        // Update HoaDon in DB
        hd.setAppTransId(appTransId);
        hd.setPaymentMethod("ZaloPay");
        hd.setPaymentStatus("PENDING");
        hoaDonRepository.save(hd);

        // Record Gateway Interaction
        auditService.log(
                hd.getKhachHang().getTaiKhoan() != null ? hd.getKhachHang().getTaiKhoan().getId() : null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                null,
                "PENDING",
                "127.0.0.1",
                "[ZALOPAY_CREATE] Created ZaloPay order. AppTransId: " + appTransId,
                "CUSTOMER"
        );

        return ZaloPayResponseDTO.builder()
                .paymentUrl(paymentUrl)
                .qrCode(qrCode)
                .appTransId(appTransId)
                .status("PENDING")
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> handleCallback(ZaloPayCallbackDTO callbackReq) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 1. Verify MAC using key2
        String expectedMac = hmacSha256(callbackReq.getData(), zaloPayConfig.getKey2());
        if (!expectedMac.equalsIgnoreCase(callbackReq.getMac())) {
            log.warn("ZaloPay Callback: MAC verification failed! Expected: {}, Received: {}", expectedMac, callbackReq.getMac());
            result.put("return_code", -1);
            result.put("return_message", "mac invalid");
            return result;
        }

        // 2. Parse data JSON string
        Map<String, Object> dataMap = objectMapper.readValue(callbackReq.getData(), new TypeReference<Map<String, Object>>() {});
        String appTransId = (String) dataMap.get("app_trans_id");
        String zpTransId = dataMap.get("zp_trans_id").toString();

        log.info("ZaloPay Callback: Processing callback success for AppTransId: {}", appTransId);

        // 3. Find order
        Optional<HoaDon> hdOpt = hoaDonRepository.findByAppTransId(appTransId);
        if (!hdOpt.isPresent()) {
            log.error("ZaloPay Callback: Order not found for AppTransId: {}", appTransId);
            result.put("return_code", -1);
            result.put("return_message", "order not found");
            return result;
        }

        HoaDon hd = hdOpt.get();

        // 4. Extract payment amount from callback payload and compare with invoice total
        Object amountObj = dataMap.get("amount");
        if (amountObj == null) {
            log.warn("[SECURITY_ALERT] ZaloPay Callback: Missing amount in payload for AppTransId: {}", appTransId);
            result.put("return_code", -1);
            result.put("return_message", "amount missing");
            return result;
        }
        BigDecimal callbackAmount = new BigDecimal(amountObj.toString());
        if (callbackAmount.compareTo(hd.getTongTien()) != 0) {
            log.warn("[SECURITY_ALERT] ZaloPay Callback: Amount mismatch! Expected: {}, Received: {} for order: {}",
                    hd.getTongTien(), callbackAmount, hd.getId());
            result.put("return_code", -1);
            result.put("return_message", "amount mismatch");
            return result;
        }

        // 5. Validate transition
        if (!canTransition(hd.getPaymentStatus(), "PAID")) {
            log.info("ZaloPay Callback: Order #{} already processed or in paid state. Current status: {}", hd.getId(), hd.getPaymentStatus());
            result.put("return_code", 1);
            result.put("return_message", "success (already processed)");
            return result;
        }

        // 6. Process success and revalidate stock with lock
        processPaymentSuccess(hd, zpTransId, callbackReq.getData());

        result.put("return_code", 1);
        result.put("return_message", "success");
        return result;
    }

    @Override
    @Transactional
    public ZaloPayResponseDTO queryTransaction(String appTransId) throws Exception {
        HoaDon hd = hoaDonRepository.findByAppTransId(appTransId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng cho mã giao dịch ZaloPay: " + appTransId));

        if ("PAID".equals(hd.getPaymentStatus())) {
            return ZaloPayResponseDTO.builder()
                    .appTransId(appTransId)
                    .status("PAID")
                    .build();
        }

        // Build MAC signature: app_id + "|" + app_trans_id + "|" + key1
        String rawData = zaloPayConfig.getAppId() + "|" + appTransId + "|" + zaloPayConfig.getKey1();
        String mac = hmacSha256(rawData, zaloPayConfig.getKey1());

        // Call ZaloPay Query API
        Map<String, Object> params = new HashMap<>();
        params.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
        params.put("app_trans_id", appTransId);
        params.put("mac", mac);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        StringBuilder formBody = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (formBody.length() > 0) {
                formBody.append("&");
            }
            formBody.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        HttpEntity<String> request = new HttpEntity<>(formBody.toString(), headers);
        log.info("ZaloPay Query: Sending request for AppTransId: {}", appTransId);

        String responseStr = restTemplate.postForObject(zaloPayConfig.getQueryUrl(), request, String.class);
        Map<String, Object> responseMap = objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        log.info("ZaloPay Query response: {}", responseStr);

        Integer returnCode = (Integer) responseMap.get("return_code");
        if (returnCode != null && returnCode == 1) {
            String zpTransId = responseMap.get("zp_trans_id").toString();
            
            // Validate transition
            if (canTransition(hd.getPaymentStatus(), "PAID")) {
                processPaymentSuccess(hd, zpTransId, responseStr);
            }
            return ZaloPayResponseDTO.builder()
                    .appTransId(appTransId)
                    .status("PAID")
                    .build();
        } else if (returnCode != null && returnCode == 2) {
            return ZaloPayResponseDTO.builder()
                    .appTransId(appTransId)
                    .status("PENDING")
                    .build();
        } else {
            // Failed / Expired
            if (canTransition(hd.getPaymentStatus(), "FAILED")) {
                hd.setPaymentStatus("FAILED");
                hd.setTrangThaiThanhToan("HUY");
                hd.setGatewayResponse(responseStr);
                hoaDonRepository.save(hd);

                auditService.log(
                        null,
                        "HoaDon",
                        Long.valueOf(hd.getId()),
                        "UPDATE",
                        "PENDING",
                        "FAILED",
                        "127.0.0.1",
                        "[ZALOPAY_QUERY_FAILED] Payment failed or cancelled via gateway query. Response: " + responseStr,
                        "SYSTEM"
                );
            }
            return ZaloPayResponseDTO.builder()
                    .appTransId(appTransId)
                    .status(hd.getPaymentStatus())
                    .build();
        }
    }

    @Override
    @Transactional
    public void cancelTransaction(String appTransId) throws Exception {
        HoaDon hd = hoaDonRepository.findByAppTransId(appTransId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + appTransId));

        if (canTransition(hd.getPaymentStatus(), "CANCELLED")) {
            hd.setPaymentStatus("CANCELLED");
            hd.setTrangThaiThanhToan("HUY");
            hoaDonRepository.save(hd);

            auditService.log(
                    hd.getKhachHang().getTaiKhoan() != null ? hd.getKhachHang().getTaiKhoan().getId() : null,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "PENDING",
                    "CANCELLED",
                    "127.0.0.1",
                    "[ZALOPAY_CANCEL] User cancelled ZaloPay payment on checkout page.",
                    "CUSTOMER"
            );
            log.info("ZaloPay: Order #{} cancelled by user request.", hd.getId());
        }
    }

    private void processPaymentSuccess(HoaDon hd, String zpTransId, String rawResponse) throws Exception {
        // Cancelled Order Protection
        if ("da_huy".equals(hd.getTrangThaiDonHang()) || "CANCELLED".equals(hd.getPaymentStatus())) {
            log.warn("[PAYMENT_RECEIVED_AFTER_CANCEL] ZaloPay: Payment received for already cancelled order #{}", hd.getId());
            
            hd.setPaymentStatus("PAID_RECEIVED_AFTER_CANCEL");
            hd.setTrangThaiThanhToan("HUY");
            hd.setTransactionId(zpTransId);
            hd.setMaGiaoDich(zpTransId);
            hd.setPaidAt(LocalDateTime.now());
            hd.setThoiGianXacNhan(LocalDateTime.now());
            hd.setNguoiXacNhanThanhToan("ZaloPay Gateway");
            hd.setGatewayResponse(rawResponse);
            hoaDonRepository.save(hd);
            
            auditService.log(
                    null,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "da_huy",
                    "da_huy",
                    "127.0.0.1",
                    "[PAYMENT_RECEIVED_AFTER_CANCEL] CRITICAL: ZaloPay payment received after order cancellation. Ref: " + zpTransId,
                    "SYSTEM"
            );
            return;
        }

        List<HoaDonChiTiet> orderItems = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());

        // 1. Revalidate stock inside transaction using PESSIMISTIC_WRITE lock
        boolean stockSufficient = true;
        for (HoaDonChiTiet orderItem : orderItems) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(orderItem.getSanPhamChiTiet().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));

            if (spct.getSoLuongTon() < orderItem.getSoLuong()) {
                stockSufficient = false;
                log.warn("ZaloPay Payment Success: Stock insufficient for variation ID: {} (Ordered: {}, Stock: {})",
                        spct.getId(), orderItem.getSoLuong(), spct.getSoLuongTon());
                break;
            }
        }

        if (stockSufficient) {
            // Deduct stock safely (never goes negative because we validated, locked, and re-read)
            for (HoaDonChiTiet orderItem : orderItems) {
                SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(orderItem.getSanPhamChiTiet().getId()).get();
                spct.setSoLuongTon(spct.getSoLuongTon() - orderItem.getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }

            // Update status to PAID
            hd.setPaymentStatus("PAID");
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            hd.setTransactionId(zpTransId);
            hd.setMaGiaoDich(zpTransId);
            hd.setPaidAt(LocalDateTime.now());
            hd.setThoiGianXacNhan(LocalDateTime.now());
            hd.setNguoiXacNhanThanhToan("ZaloPay Gateway");
            hd.setGatewayResponse(rawResponse);
            hd.setTrangThaiDonHang("cho_xac_nhan");
            hoaDonRepository.save(hd);

            // 2. Remove ONLY items belonging to this order from the customer's cart
            GioHang gioHang = gioHangRepository.findByKhachHang_Id(hd.getKhachHang().getId());
            if (gioHang != null) {
                for (HoaDonChiTiet orderItem : orderItems) {
                    GioHangChiTiet cartItem = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(
                            gioHang.getId(), orderItem.getSanPhamChiTiet().getId());
                    if (cartItem != null) {
                        gioHangChiTietRepository.delete(cartItem);
                    }
                }
            }

            auditService.log(
                    null,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "PENDING",
                    "PAID",
                    "127.0.0.1",
                    "[ZALOPAY_CALLBACK_PAID] Payment success callback handled. Order stock deducted. Cart items removed.",
                    "SYSTEM"
            );
            log.info("ZaloPay: Payment successfully applied to order #{}", hd.getId());
        } else {
            // 3. STOCK_CONFLICT: payment success but stock depleted
            log.error("[SYSTEM_ALERT] ZaloPay payment succeeded for order #{} but inventory is insufficient! Setting status to STOCK_CONFLICT.", hd.getId());
            
            hd.setPaymentStatus("PAID"); // keep payment_status = PAID
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            hd.setTransactionId(zpTransId);
            hd.setMaGiaoDich(zpTransId);
            hd.setPaidAt(LocalDateTime.now());
            hd.setThoiGianXacNhan(LocalDateTime.now());
            hd.setNguoiXacNhanThanhToan("ZaloPay Gateway");
            hd.setGatewayResponse(rawResponse);
            
            hd.setTrangThaiDonHang("STOCK_CONFLICT"); // set order_status = STOCK_CONFLICT
            hoaDonRepository.save(hd);

            // Remove items from cart as they paid successfully, resolving transaction checkout
            GioHang gioHang = gioHangRepository.findByKhachHang_Id(hd.getKhachHang().getId());
            if (gioHang != null) {
                for (HoaDonChiTiet orderItem : orderItems) {
                    GioHangChiTiet cartItem = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(
                            gioHang.getId(), orderItem.getSanPhamChiTiet().getId());
                    if (cartItem != null) {
                        gioHangChiTietRepository.delete(cartItem);
                    }
                }
            }

            // Notify administrators via system log
            auditService.log(
                    null,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "PENDING",
                    "PAID",
                    "127.0.0.1",
                    "[STOCK_CONFLICT] CRITICAL: ZaloPay paid successfully for order #" + hd.getId() + " but inventory was insufficient. Admin review needed.",
                    "SYSTEM"
            );
        }
    }

    private boolean canTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || currentStatus.isEmpty()) {
            return true;
        }
        if ("PAID".equals(currentStatus)) {
            return false; // PAID is immutable
        }
        if ("PENDING".equals(currentStatus)) {
            return true; // PENDING can transition to PAID, FAILED, CANCELLED, EXPIRED
        }
        // FAILED, CANCELLED, EXPIRED are terminal statuses
        return false;
    }

    private static String hmacSha256(String data, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return byteToHex(rawHmac);
    }

    private static String byteToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getDynamicBaseUrl() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
                String scheme = request.getHeader("X-Forwarded-Proto");
                if (scheme == null || scheme.isEmpty()) {
                    scheme = request.getScheme();
                }
                String host = request.getHeader("X-Forwarded-Host");
                if (host == null || host.isEmpty()) {
                    host = request.getHeader("Host");
                }
                if (host == null || host.isEmpty()) {
                    String serverName = request.getServerName();
                    int serverPort = request.getServerPort();
                    if (serverPort == 80 || serverPort == 443) {
                        host = serverName;
                    } else {
                        host = serverName + ":" + serverPort;
                    }
                }
                if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equalsIgnoreCase(host)) {
                    if (!host.contains(":")) {
                        host = host + ":8080";
                    }
                }
                String contextPath = request.getContextPath();
                return scheme + "://" + host + contextPath;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
