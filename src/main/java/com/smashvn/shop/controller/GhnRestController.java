package com.smashvn.shop.controller;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.GhnService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller nội bộ cung cấp các API GHN cho frontend:
 *  - GET  /api/ghn/districts?provinceId=   → danh sách quận/huyện
 *  - GET  /api/ghn/wards?districtId=       → danh sách phường/xã
 *  - POST /api/ghn/fee                     → tính phí ship
 *  - GET  /api/ghn/track/{orderCode}       → tra cứu trạng thái vận đơn
 */
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@Slf4j
public class GhnRestController {

    private final GhnService ghnService;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    /** Lấy danh sách tỉnh/thành phố */
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        try {
            List<Map<String, Object>> provinces = ghnService.getProvinces();
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("data", provinces);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /** Lấy danh sách quận/huyện theo tỉnh */
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        try {
            List<Map<String, Object>> districts = ghnService.getDistricts(provinceId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("data", districts);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /** Lấy danh sách phường/xã theo quận/huyện */
    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        try {
            List<Map<String, Object>> wards = ghnService.getWards(districtId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("data", wards);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /** Tính phí ship */
    @PostMapping("/fee")
    public ResponseEntity<?> calculateFee(@RequestBody Map<String, Object> body) {
        try {
            Integer toDistrictId = (Integer) body.get("toDistrictId");
            String toWardCode = (String) body.get("toWardCode");
            Integer insuranceValue = body.get("insuranceValue") != null
                    ? ((Number) body.get("insuranceValue")).intValue() : 0;

            if (toDistrictId == null || toWardCode == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Thiếu toDistrictId hoặc toWardCode"));
            }

            BigDecimal fee = ghnService.calculateShipFee(toDistrictId, toWardCode, insuranceValue);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("fee", fee);
            result.put("feeFormatted", String.format("%,.0f", fee) + " đ");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GHN fee calculation error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage(),
                    "fee", 30000, "feeFormatted", "30,000 đ"));
        }
    }

    /** Tra cứu trạng thái vận đơn GHN */
    @GetMapping("/track/{orderCode}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Chưa đăng nhập"));
        }
        try {
            Map<String, Object> trackingData = ghnService.trackOrder(orderCode);
            if (trackingData == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không tìm thấy thông tin vận đơn"));
            }
            return ResponseEntity.ok(Map.of("status", "ok", "data", trackingData));
        } catch (Exception e) {
            log.error("GHN track error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /** Tra cứu vận đơn theo ID đơn hàng (dành cho user) */
    @GetMapping("/track/order/{orderId}")
    public ResponseEntity<?> trackByOrderId(@PathVariable Integer orderId, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Chưa đăng nhập"));
        }
        try {
            HoaDon hd = hoaDonRepository.findById(orderId).orElse(null);
            if (hd == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không tìm thấy đơn hàng"));
            }

            String ghnCode = hd.getGhnOrderCode();
            if (ghnCode == null || ghnCode.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "no_ghn", "message", "Đơn hàng này chưa có mã vận đơn GHN",
                        "trangThaiDonHang", hd.getTrangThaiDonHang()));
            }

            Map<String, Object> trackingData = ghnService.trackOrder(ghnCode);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("ghnOrderCode", ghnCode);
            result.put("data", trackingData);
            result.put("trangThaiDonHang", hd.getTrangThaiDonHang());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GHN trackByOrderId error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    /**
     * [ADMIN] Push thủ công đơn hàng lên GHN
     * POST /api/ghn/admin/push/{orderId}?toDistrictId=X&toWardCode=Y
     *
     * Dùng để tạo đơn vận chuyển GHN cho đơn hàng cũ (chưa có ghn_order_code)
     * hoặc khi cần retry sau lỗi.
     */
    @PostMapping("/admin/push/{orderId}")
    public ResponseEntity<?> adminPushToGhn(
            @PathVariable Integer orderId,
            @RequestParam Integer toDistrictId,
            @RequestParam String toWardCode,
            HttpSession session) {

        // Chỉ admin/nhân viên mới được dùng
        String role = (String) session.getAttribute("vaiTro");
        if (role == null || (!role.equals("QL") && !role.equals("NV"))) {
            return ResponseEntity.status(403)
                    .body(Map.of("status", "error", "message", "Không có quyền truy cập"));
        }

        try {
            HoaDon hd = hoaDonRepository.findById(orderId).orElse(null);
            if (hd == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không tìm thấy đơn hàng ID=" + orderId));
            }

            if (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().isBlank()) {
                return ResponseEntity.ok(Map.of(
                        "status", "already_exists",
                        "message", "Đơn này đã có mã GHN: " + hd.getGhnOrderCode(),
                        "ghnOrderCode", hd.getGhnOrderCode()
                ));
            }

            // Lấy chi tiết đơn hàng
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(orderId);

            if (items.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Đơn hàng không có sản phẩm"));
            }

            // Cập nhật district/ward vào entity trước khi tạo đơn
            hd.setGhnToDistrictId(toDistrictId);
            hd.setGhnToWardCode(toWardCode);

            String ghnCode = ghnService.createShippingOrderOrThrow(hd, items, toDistrictId, toWardCode);
            if (ghnCode != null) {
                hd.setGhnOrderCode(ghnCode);
                hd.setGhnStatus("ready_to_pick");
                hoaDonRepository.save(hd);
                log.info("[ADMIN] Đã push đơn #{} lên GHN thành công, mã: {}", orderId, ghnCode);
                return ResponseEntity.ok(Map.of(
                        "status", "ok",
                        "message", "Tạo đơn GHN thành công!",
                        "ghnOrderCode", ghnCode,
                        "orderId", orderId
                ));
            } else {
                return ResponseEntity.ok(Map.of("status", "error", "message", "GHN trả về mã null, kiểm tra log server"));
            }
        } catch (Exception e) {
            log.error("[ADMIN] Push GHN error orderId={}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Webhook nhận cập nhật trạng thái đơn hàng từ GHN
     * POST /api/ghn/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> ghnWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[GHN_WEBHOOK] Received payload: {}", payload);
        try {
            String orderCode = (String) payload.get("OrderCode");
            String status = (String) payload.get("Status");
            
            if (orderCode == null || status == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing OrderCode or Status"));
            }

            // Tìm đơn hàng theo mã vận đơn GHN
            HoaDon hd = hoaDonRepository.findByGhnOrderCode(orderCode).orElse(null);
            if (hd == null) {
                // Thử tìm theo ClientOrderCode nếu có
                String clientOrderCode = (String) payload.get("ClientOrderCode");
                if (clientOrderCode != null && !clientOrderCode.isBlank()) {
                    try {
                        Integer orderId = Integer.parseInt(clientOrderCode);
                        hd = hoaDonRepository.findById(orderId).orElse(null);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }

            if (hd != null) {
                String oldStatus = hd.getTrangThaiDonHang();
                String oldGhnStatus = hd.getGhnStatus();

                // Cập nhật trạng thái GHN
                hd.setGhnStatus(status);

                // Ánh xạ trạng thái GHN sang trạng thái đơn hàng nội bộ (trangThaiDonHang)
                String internalStatus = oldStatus;
                switch (status.toLowerCase()) {
                    case "ready_to_pick":
                    case "picking":
                        internalStatus = "cho_xac_nhan";
                        break;
                    case "money_collect_picking":
                    case "picked":
                    case "storing":
                    case "sorting":
                    case "transporting":
                    case "delivering":
                    case "money_collect_delivering":
                        internalStatus = "dang_giao";
                        break;
                    case "delivered":
                        internalStatus = "da_giao";
                        // Cập nhật trạng thái thanh toán là đã thanh toán nếu giao hàng thành công
                        hd.setPaymentStatus("PAID");
                        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
                        hd.setPaidAt(java.time.LocalDateTime.now());
                        break;
                    case "cancel":
                    case "exception":
                    case "lost":
                    case "damage":
                        internalStatus = "da_huy";
                        break;
                }
                hd.setTrangThaiDonHang(internalStatus);
                hoaDonRepository.save(hd);

                log.info("[GHN_WEBHOOK] Updated HoaDon #{}: oldStatus={}, oldGhnStatus={} -> newStatus={}, newGhnStatus={}",
                        hd.getId(), oldStatus, oldGhnStatus, internalStatus, status);
                return ResponseEntity.ok(Map.of("status", "ok", "message", "Update success"));
            } else {
                log.warn("[GHN_WEBHOOK] No HoaDon found for OrderCode={}", orderCode);
                return ResponseEntity.ok(Map.of("status", "not_found", "message", "No matching order found"));
            }
        } catch (Exception e) {
            log.error("[GHN_WEBHOOK] Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

