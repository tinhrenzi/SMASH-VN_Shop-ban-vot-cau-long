package com.smashvn.shop.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentMethod;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.entity.ReturnStatus;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class OrderViewService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final com.smashvn.shop.repository.EditLogRepository editLogRepository;
    private final JavaMailSender mailSender;
    private final NhanVienRepository nhanVienRepository;
    private final com.smashvn.shop.repository.ThongBaoRepository thongBaoRepository;

    @Value("${app.admin.emails}")
    private String adminEmailsConfig;

    // Helper to format dates for dash-my-order.html
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.US);

    /**
     * Lấy danh sách đơn hàng cho dash-my-order.html. Nếu khách hàng chưa có đơn
     * hàng thật nào, trả về danh sách đơn hàng giả lập (Mock Orders).
     */
    public List<Map<String, Object>> layDanhSachOrders(Integer idKhachHang) {
        List<HoaDon> realOrders = hoaDonRepository.findByKhachHang_IdOrderByIdDesc(idKhachHang);
        List<Map<String, Object>> resultList = new ArrayList<>();

        if (realOrders != null && !realOrders.isEmpty()) {
            // CÓ ĐƠN HÀNG THẬT
            for (HoaDon hd : realOrders) {
                // Skip unconfirmed orders (cho_thanh_toan or orders expired/cancelled before confirmation)
                if ("cho_thanh_toan".equalsIgnoreCase(hd.getTrangThaiDonHang())) {
                    continue;
                }
                if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang())
                        && !"paid".equalsIgnoreCase(hd.getPaymentStatus())
                        && !PaymentMethod.COD.getValue().equalsIgnoreCase(hd.getPaymentMethod())) {
                    continue;
                }
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", hd.getId());
                orderMap.put("date", hd.getNgayTao().format(formatter));

                // Ánh xạ trạng thái hiển thị
                String statusText = getFrontendStatusLabel(hd.getTrangThaiDonHang());
                orderMap.put("status", statusText);
                orderMap.put("rawStatus", hd.getTrangThaiDonHang());
                orderMap.put("total", hd.getTongTien());
                orderMap.put("paymentMethod", hd.getPaymentMethod());
                orderMap.put("maDonHang", hd.getMaDonHang());

                List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
                List<Map<String, Object>> itemMaps = new ArrayList<>();
                for (HoaDonChiTiet ct : items) {
                    Map<String, Object> itemMap = new HashMap<>();
                    SanPhamChiTiet spct = ct.getSanPhamChiTiet();

                    // Lấy ảnh hiển thị
                    String imgName = "product9.jpg"; // fallback
                    if (spct != null && spct.getHinhAnhSanPham() != null && !spct.getHinhAnhSanPham().isEmpty()) {
                        imgName = spct.getHinhAnhSanPham();
                    }

                    String title = ct.getTenSanPhamSnapshot();
                    if (title == null || title.isBlank()) {
                        title = (spct != null && spct.getSanPham() != null) ? spct.getSanPham().getTenSanPham() : "N/A";
                    }
                    String attr = (spct != null) ? spct.getMauSac() : "N/A";

                    itemMap.put("image", "../uploads/product/" + imgName); // do path ở uploads
                    itemMap.put("title", title + " [" + attr + "]");
                    itemMap.put("quantity", ct.getSoLuong());
                    itemMap.put("total", (ct.getDonGia() != null ? ct.getDonGia() : BigDecimal.ZERO).multiply(new BigDecimal(ct.getSoLuong())));
                    itemMaps.add(itemMap);
                }
                orderMap.put("items", itemMaps);
                resultList.add(orderMap);
            }
        }

        return resultList;
    }

    /**
     * Lấy chi tiết đơn hàng cho dash-manage-order.html. Trả về một Map chứa:
     * "order" (HoaDon hoặc Mock HoaDon), "orderItems" (List<HoaDonChiTiet> hoặc
     * Mock), v.v.
     */
    public Map<String, Object> layChiTietOrder(Integer idHoaDon, Integer idKhachHang) {
        Map<String, Object> modelMap = new HashMap<>();

        // 1. Tìm đơn hàng thật trong CSDL
        Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
        if (hdOpt.isPresent()) {
            HoaDon hd = hdOpt.get();
            // Đảm bảo đơn hàng này đúng của khách hàng đang đăng nhập
            if (hd.getKhachHang().getId().equals(idKhachHang)) {
                modelMap.put("order", adaptRealOrder(hd));

                List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
                // Fix path ảnh cho item trước khi ném ra giao diện
                for (HoaDonChiTiet ct : items) {
                    SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                    if (spct.getHinhAnhSanPham() != null && !spct.getHinhAnhSanPham().isEmpty()) {
                        // Thêm trick để template th:src nhận được path
                        spct.getSanPham().setMoTa(spct.getHinhAnhSanPham()); // Dùng trường moTa lưu tạm tên ảnh để template th:src dễ bind
                    } else {
                        spct.getSanPham().setMoTa("product9.jpg");
                    }
                }

                modelMap.put("orderItems", items);

                // Lấy thống kê thật (dùng giá trị DB: da_huy, da_giao)
                List<HoaDon> allUserOrders = hoaDonRepository.findByKhachHang_Id(idKhachHang);
                long cancelCount = allUserOrders.stream().filter(o -> OrderStatus.DA_HUY.getValue().equals(o.getTrangThaiDonHang())).count();
                long orderCount = allUserOrders.size() - cancelCount;

                modelMap.put("orderCount", orderCount);
                modelMap.put("cancelCount", cancelCount);
                modelMap.put("wishlistCount", 0);

                return modelMap;
            }
        }

        // Không tìm thấy -> Trả về null để Controller xử lý redirect
        return null;
    }

    private String getFrontendStatusLabel(String dbStatus) {
        if (dbStatus == null) {
            return "processing";
        }
        return switch (dbStatus.toLowerCase()) {
            case "da_giao", "delivered" ->
                "delivered";
            case "da_huy", "cancelled" ->
                "cancelled";
            default ->
                "processing";
        };
    }

    // Adapt Real HoaDon to match the dynamic properties expected in template without throwing exceptions
    private Map<String, Object> adaptRealOrder(HoaDon hd) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", hd.getId());
        map.put("ngayDat", hd.getNgayTao());
        map.put("ngayGiao", hd.getNgayTao().plusDays(3)); // Giả sử giao sau 3 ngày
        map.put("phuongThucVanChuyen", hd.getDonViVanChuyen() != null ? hd.getDonViVanChuyen().getTenDonVi() : "Standard Delivery");
        map.put("phiVanChuyen", hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO);
        map.put("phuongThucThanhToan", hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "COD");
        map.put("trangThaiThanhToan", hd.getTrangThaiThanhToan());
        map.put("tongTien", hd.getTongTien());
        map.put("trangThaiDonHang", hd.getTrangThaiDonHang());
        map.put("status", getFrontendStatusLabel(hd.getTrangThaiDonHang()));
        map.put("ghiChu", hd.getGhiChu());
        map.put("paymentMethod", hd.getPaymentMethod());
        map.put("maDonHang", hd.getMaDonHang());
        map.put("maGiaoDich", hd.getMaGiaoDich());
        map.put("transactionId", hd.getTransactionId());
        map.put("ghnOrderCode", hd.getGhnOrderCode());

        map.put("soTienGiamVoucher", hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : BigDecimal.ZERO);
        map.put("maVoucherApDung", hd.getMaVoucherApDung() != null ? hd.getMaVoucherApDung() : "");
        map.put("tenVoucherApDung", hd.getTenVoucherApDung() != null ? hd.getTenVoucherApDung() : "");
        map.put("moTaVoucherSnapshot", hd.getMoTaVoucherSnapshot() != null ? hd.getMoTaVoucherSnapshot() : "");

        Map<String, Object> adr = new HashMap<>();
        adr.put("hoTen", hd.getKhachHang().getHoKh() + " " + hd.getKhachHang().getTenKh());
        adr.put("diaChiDayDu", hd.getDiaChiNhan());
        adr.put("soDienThoai", hd.getSdtNhan());

        map.put("diaChiGiao", adr);
        map.put("diaChiThanhToan", adr);

        map.put("ngayXacNhan", hd.getThoiGianXacNhan());
        map.put("ngayThanhToan", hd.getPaidAt());

        Map<String, LocalDateTime> transitions = getStatusTransitionTimes(hd.getId());
        map.put("ngayGiaoDVVC", transitions.get("dang_giao") != null ? transitions.get("dang_giao") : (transitions.get("dang_lay_hang") != null ? transitions.get("dang_lay_hang") : null));
        map.put("ngayGiaoThanhCong", transitions.get("da_giao"));
        map.put("ngayHuy", transitions.get("da_huy"));

        return map;
    }

    @Transactional
    public boolean huyDonHang(Integer idHoaDon, Integer idKhachHang, String clientIp) {
        return huyDonHang(idHoaDon, idKhachHang, clientIp, null);
    }

    @Transactional
    public boolean huyDonHang(Integer idHoaDon, Integer idKhachHang, String clientIp, String lyDoHuy) {
        Optional<HoaDon> hdOpt = hoaDonRepository.findByIdWithLock(idHoaDon);
        if (hdOpt.isPresent()) {
            HoaDon hd = hdOpt.get();
            // Xác thực đơn hàng thuộc về đúng khách hàng đang đăng nhập
            if (!hd.getKhachHang().getId().equals(idKhachHang)) {
                return false;
            }

            // Chỉ cho phép hủy đơn ở trạng thái cho_thanh_toan, cho_xac_nhan hoặc da_xac_nhan
            String currentStatus = hd.getTrangThaiDonHang();
            if (OrderStatus.CHO_THANH_TOAN.getValue().equals(currentStatus)
                    || OrderStatus.CHO_XAC_NHAN.getValue().equals(currentStatus)
                    || OrderStatus.DA_XAC_NHAN.getValue().equals(currentStatus)) {

                // Nếu ở trạng thái cho_xac_nhan hoặc da_xac_nhan, tức là hàng đã bị trừ trong kho
                // Ta cần khôi phục lại kho cho các biến thể sản phẩm
                if (OrderStatus.CHO_XAC_NHAN.getValue().equals(currentStatus)
                        || OrderStatus.DA_XAC_NHAN.getValue().equals(currentStatus)) {
                    List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
                    for (HoaDonChiTiet item : items) {
                        SanPhamChiTiet spct = item.getSanPhamChiTiet();
                        if (spct != null) {
                            // Khóa ghi và tăng tồn kho
                            Optional<SanPhamChiTiet> lockedSpctOpt = sanPhamChiTietRepository.findByIdWithLock(spct.getId());
                            if (lockedSpctOpt.isPresent()) {
                                SanPhamChiTiet lockedSpct = lockedSpctOpt.get();
                                lockedSpct.setSoLuongTon(lockedSpct.getSoLuongTon() + item.getSoLuong());
                                sanPhamChiTietRepository.save(lockedSpct);
                            }
                        }
                    }
                }

                // Cập nhật trạng thái đơn hàng
                hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue()); // "da_huy"
                String refundLogNote = "";

                String standardizedReason = "Không cung cấp lý do";
                if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                    String trimmed = lyDoHuy.trim();
                    String sanitized = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
                    if (sanitized.length() > 500) {
                        throw new IllegalArgumentException("Lý do hủy không được vượt quá 500 ký tự.");
                    }
                    standardizedReason = sanitized;
                }

                String addition = "Lý do hủy: " + standardizedReason;
                String currentGhiChu = hd.getGhiChu();
                if (currentGhiChu == null || currentGhiChu.trim().isEmpty()) {
                    hd.setGhiChu(addition.length() > 500 ? addition.substring(0, 500) : addition);
                } else {
                    String newGhiChu = currentGhiChu + "\n" + addition;
                    hd.setGhiChu(newGhiChu.length() > 500 ? newGhiChu.substring(0, 500) : newGhiChu);
                }

                if (isOrderPaid(hd)) {
                    if (!"DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) && !"REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
                        hd.setPaymentStatus("paid");
                        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                    }
                    String pm = hd.getPaymentMethod();
                    boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                            || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));
                    if (isPrepaid || isOrderPaid(hd)) {
                        hd.setRefundStatus(RefundStatus.PENDING);
                    }
                    refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, customerId=%d",
                            hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), idKhachHang);
                    hoaDonRepository.save(hd);
                    try {
                        guiEmailYeuCauHoanTien(hd, standardizedReason);
                    } catch (Exception e) {
                        log.error("Lỗi gửi email yêu cầu hoàn tiền khi khách hàng hủy đơn: {}", e.getMessage());
                    }
                } else {
                    hd.setPaymentStatus("CANCELLED");
                    hd.setTrangThaiThanhToan("HUY");
                    hoaDonRepository.save(hd);
                }

                // Ghi nhận Audit Log
                auditService.log(
                        hd.getKhachHang().getTaiKhoan() != null ? hd.getKhachHang().getTaiKhoan().getId() : null,
                        "HoaDon",
                        Long.valueOf(hd.getId()),
                        "UPDATE",
                        currentStatus,
                        "da_huy",
                        clientIp,
                        "[CUSTOMER_CANCEL] Khách hàng tự hủy đơn hàng từ trang chi tiết." + refundLogNote,
                        "CUSTOMER"
                );

                return true;
            }
        }
        return false;
    }

    public List<String> getValidNextStatuses(String currentStatus) {
        if (currentStatus == null) {
            return List.of();
        }
        return switch (currentStatus.toLowerCase()) {
            case "cho_thanh_toan" ->
                List.of("cho_xac_nhan", "da_huy");
            case "cho_xac_nhan" ->
                List.of("da_xac_nhan", "da_huy");
            case "da_xac_nhan" ->
                List.of("dang_lay_hang", "dang_giao", "da_huy");
            case "dang_lay_hang" ->
                List.of("dang_giao", "da_huy");
            case "dang_giao" ->
                List.of("da_giao", "da_huy");
            case "stock_conflict" ->
                List.of("cho_xac_nhan", "da_huy");
            default ->
                List.of();
        };
    }

    public String getStatusLabel(String status) {
        if (status == null) {
            return "N/A";
        }
        return switch (status.toLowerCase()) {
            case "cho_thanh_toan" ->
                "Chờ thanh toán";
            case "cho_xac_nhan" ->
                "Chờ xác nhận";
            case "da_xac_nhan" ->
                "Đã xác nhận";
            case "dang_lay_hang" ->
                "Đang lấy hàng";
            case "dang_giao" ->
                "Đang giao";
            case "da_giao" ->
                "Đã giao";
            case "da_huy" ->
                "Đã hủy";
            case "stock_conflict" ->
                "Trùng kho";
            default ->
                status;
        };
    }

    @Transactional
    public void updateOrderStatusByAdmin(Integer idHoaDon, String newStatus, String expectedStatus, Integer actingTaiKhoanId, String clientIp) {
        updateOrderStatusByAdmin(idHoaDon, newStatus, expectedStatus, actingTaiKhoanId, clientIp, null);
    }

    @Transactional
    public void updateOrderStatusByAdmin(Integer idHoaDon, String newStatus, String expectedStatus, Integer actingTaiKhoanId, String clientIp, String lyDoHuy) {
        // 1. Service-Level Authorization
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái đơn hàng.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));

        String role = actingUser.getVaiTro();
        if (!"NV".equals(role) && !"QL".equals(role)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái đơn hàng.");
        }

        // 2. Load order
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String currentStatus = hd.getTrangThaiDonHang();
        String currentPaymentStatus = hd.getPaymentStatus();
        String currentTrangThaiThanhToan = hd.getTrangThaiThanhToan();

        // 3. Lost Update Protection
        if (!Objects.equals(currentStatus, expectedStatus)) {
            throw new IllegalStateException("Đơn hàng đã được người khác cập nhật. Vui lòng tải lại trang.");
        }

        // 4. Delivered and Cancelled orders are immutable
        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(currentStatus)
                || OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
            throw new IllegalArgumentException("Không thể chỉnh sửa đơn hàng đã giao hoặc đã hủy!");
        }

        // 5. If status is the same, no transition is needed
        if (Objects.equals(currentStatus, newStatus)) {
            return;
        }

        // 6. Transition Matrix Validation
        List<String> validNext = getValidNextStatuses(currentStatus);
        if (!validNext.contains(newStatus)) {
            throw new IllegalArgumentException(String.format("Không thể chuyển trạng thái từ '%s' sang '%s'.", getStatusLabel(currentStatus), getStatusLabel(newStatus)));
        }

        // 7. Inventory State tracking
        boolean oldIsDeducted = isStockDeductedState(currentStatus);
        boolean newIsDeducted = isStockDeductedState(newStatus);

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);

        if (!oldIsDeducted && newIsDeducted) {
            // Deduct stock: lock variants, validate, deduct
            for (HoaDonChiTiet item : items) {
                SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct != null) {
                    SanPhamChiTiet locked = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại."));
                    if (locked.getSoLuongTon() < item.getSoLuong()) {
                        throw new IllegalArgumentException("Sản phẩm '" + locked.getSanPham().getTenSanPham() + "' không đủ hàng tồn kho để kích hoạt đơn hàng!");
                    }
                }
            }
            for (HoaDonChiTiet item : items) {
                SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct != null) {
                    SanPhamChiTiet locked = sanPhamChiTietRepository.findByIdWithLock(spct.getId()).get();
                    locked.setSoLuongTon(locked.getSoLuongTon() - item.getSoLuong());
                    sanPhamChiTietRepository.save(locked);
                }
            }
        } else if (oldIsDeducted && !newIsDeducted) {
            boolean restoreStock = true;
            if ("dang_giao".equalsIgnoreCase(currentStatus) || "dang_lay_hang".equalsIgnoreCase(currentStatus)) {
                restoreStock = false;
                hd.setTrangThaiHoanHang(ReturnStatus.PENDING_RETURN);
            }
            if (restoreStock) {
                for (HoaDonChiTiet item : items) {
                    SanPhamChiTiet spct = item.getSanPhamChiTiet();
                    if (spct != null) {
                        SanPhamChiTiet locked = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại."));
                        locked.setSoLuongTon(locked.getSoLuongTon() + item.getSoLuong());
                        sanPhamChiTietRepository.save(locked);
                    }
                }
            }
        }

        // 8. Payment Method Logic & Cancellation Rules
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";
        String refundLogNote = "";

        if (OrderStatus.DA_XAC_NHAN.getValue().equalsIgnoreCase(newStatus)) {
            if (hd.getThoiGianXacNhan() == null) {
                hd.setThoiGianXacNhan(LocalDateTime.now());
            }
        }

        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(newStatus)) {
            // Explicit check: COD payments set paid when delivered
            if ("COD".equalsIgnoreCase(hd.getPaymentMethod())) {
                hd.setPaymentStatus(PaymentStatus.PAID.getValue());
                hd.setTrangThaiThanhToan("DA_THANH_TOAN");
                if (hd.getPaidAt() == null) {
                    hd.setPaidAt(LocalDateTime.now());
                }
                if (hd.getThoiGianXacNhan() == null) {
                    hd.setThoiGianXacNhan(LocalDateTime.now());
                }
            }
        } else if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
            String standardizedReason = "Không cung cấp lý do";
            if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                String trimmed = lyDoHuy.trim();
                String sanitized = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
                if (sanitized.length() > 500) {
                    throw new IllegalArgumentException("Lý do hủy không được vượt quá 500 ký tự.");
                }
                standardizedReason = sanitized;
            }

            String pm = hd.getPaymentMethod();
            boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                    || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));

            if (isOrderPaid(hd)) {
                if (!"DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) && !"REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
                    hd.setPaymentStatus("paid");
                    hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                }
                if (isPrepaid || isOrderPaid(hd)) {
                    hd.setRefundStatus(RefundStatus.PENDING);
                }
                refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, actingUserId=%d",
                        hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), actingTaiKhoanId);
            } else {
                hd.setPaymentStatus("CANCELLED");
                hd.setTrangThaiThanhToan("HUY");
            }

            String addition = "Lý do hủy: " + standardizedReason;
            String currentGhiChu = hd.getGhiChu();
            if (currentGhiChu == null || currentGhiChu.trim().isEmpty()) {
                hd.setGhiChu(addition.length() > 500 ? addition.substring(0, 500) : addition);
            } else {
                String newGhiChu = currentGhiChu + "\n" + addition;
                hd.setGhiChu(newGhiChu.length() > 500 ? newGhiChu.substring(0, 500) : newGhiChu);
            }
        }

        // 9. Update Order
        hd.setTrangThaiDonHang(newStatus);
        hd = hoaDonRepository.save(hd);

        // Generate notification for customer
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            try {
                String maDon = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
                String labelCu = getStatusLabel(currentStatus);
                String labelMoi = getStatusLabel(newStatus);
                String msgContent = String.format("Đơn hàng %s của bạn đã được cập nhật trạng thái từ [%s] sang [%s].",
                        maDon, labelCu, labelMoi);

                ThongBao thongBao = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe("Cập nhật trạng thái đơn hàng " + maDon)
                        .noiDung(msgContent)
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBao);
            } catch (Exception e) {
                log.error("Lỗi tạo thông báo trạng thái đơn hàng cho khách: {}", e.getMessage());
            }
        }

        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus) && "CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            String standardizedReason = "Không cung cấp lý do";
            if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                standardizedReason = lyDoHuy.trim();
            }
            try {
                guiEmailYeuCauHoanTien(hd, standardizedReason);
            } catch (Exception e) {
                log.error("Lỗi gửi email yêu cầu hoàn tiền khi admin hủy đơn: {}", e.getMessage());
            }
        }

        // 10. Audit Log Enhancement
        String giaTriCu = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                currentStatus, currentPaymentStatus, currentTrangThaiThanhToan,
                "NULL", "NULL");
        String giaTriMoi = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                newStatus, hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL",
                hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");
        String ghiChu = String.format("[ADMIN_UPDATE] Trạng thái đơn hàng được cập nhật bởi %s.%s", roleStr, refundLogNote);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                giaTriCu,
                giaTriMoi,
                clientIp,
                ghiChu,
                actingUser.getVaiTro() != null ? actingUser.getVaiTro() : roleStr
        );
    }

    @Transactional
    public void applyShippingStatus(Integer idHoaDon, String newStatus, String ghnStatus) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        String currentStatus = hd.getTrangThaiDonHang();
        String currentPaymentStatus = hd.getPaymentStatus();
        String currentTrangThaiThanhToan = hd.getTrangThaiThanhToan();

        // Ngăn chặn việc đảo ngược trạng thái đơn hàng từ các trạng thái cuối (Terminal States)
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
            log.info("[GHN_WEBHOOK] Bỏ qua cập nhật trạng thái cho đơn hàng đã HỦY #{}. Trạng thái mục tiêu: {}", idHoaDon, newStatus);
            if (!Objects.equals(hd.getGhnStatus(), ghnStatus)) {
                hd.setGhnStatus(ghnStatus);
                hoaDonRepository.save(hd);
            }
            return;
        }

        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(currentStatus)) {
            if (!OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(newStatus) && !OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
                log.info("[GHN_WEBHOOK] Bỏ qua việc đảo ngược trạng thái từ ĐÃ GIAO về active cho đơn #{}. Trạng thái mục tiêu: {}", idHoaDon, newStatus);
                if (!Objects.equals(hd.getGhnStatus(), ghnStatus)) {
                    hd.setGhnStatus(ghnStatus);
                    hoaDonRepository.save(hd);
                }
                return;
            }
        }

        // Determine target return status if transitioning to da_huy (or already da_huy)
        ReturnStatus targetReturnStatus = hd.getTrangThaiHoanHang();
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
            if ("dang_giao".equalsIgnoreCase(currentStatus) || "dang_lay_hang".equalsIgnoreCase(currentStatus) || OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
                if ("lost".equalsIgnoreCase(ghnStatus)) {
                    targetReturnStatus = ReturnStatus.LOST;
                } else if ("damage".equalsIgnoreCase(ghnStatus)) {
                    targetReturnStatus = ReturnStatus.DAMAGED;
                } else if ("cancel".equalsIgnoreCase(ghnStatus) || "return".equalsIgnoreCase(ghnStatus) || "exception".equalsIgnoreCase(ghnStatus)) {
                    targetReturnStatus = ReturnStatus.PENDING_RETURN;
                } else {
                    if (targetReturnStatus == null) {
                        targetReturnStatus = ReturnStatus.PENDING_RETURN;
                    }
                }
            }
        }

        if (Objects.equals(currentStatus, newStatus)) {
            // Duplicate webhook / same status check
            if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
                if (Objects.equals(hd.getTrangThaiHoanHang(), targetReturnStatus)) {
                    return; // Duplicate webhook, skip completely
                } else {
                    // Update return status only
                    hd.setTrangThaiHoanHang(targetReturnStatus);
                    hd.setGhnStatus(ghnStatus);
                    hd = hoaDonRepository.save(hd);

                    // Audit Log for return status update
                    String giaTriCu = String.format("status=%s, trangThaiHoanHang=%s", currentStatus, hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");
                    String giaTriMoi = String.format("status=%s, trangThaiHoanHang=%s", newStatus, targetReturnStatus != null ? targetReturnStatus.name() : "NULL");
                    String ghiChuLog = String.format("[GHN_WEBHOOK] Cập nhật trạng thái hoàn hàng từ webhook GHN. Mã vận đơn: %s, Trạng thái GHN: %s, Trạng thái hoàn hàng mới: %s", hd.getGhnOrderCode(), ghnStatus, targetReturnStatus != null ? targetReturnStatus.name() : "NULL");
                    auditService.log(
                            null,
                            "HoaDon",
                            Long.valueOf(hd.getId()),
                            "UPDATE",
                            giaTriCu,
                            giaTriMoi,
                            "127.0.0.1",
                            ghiChuLog,
                            "SYSTEM"
                    );
                }
            } else {
                // Cập nhật trạng thái GHN nếu có thay đổi mà không thay đổi trạng thái đơn hàng
                if (!Objects.equals(hd.getGhnStatus(), ghnStatus)) {
                    hd.setGhnStatus(ghnStatus);
                    hoaDonRepository.save(hd);
                }
            }
            return;
        }

        // Cập nhật trạng thái GHN
        hd.setGhnStatus(ghnStatus);

        // Inventory State tracking
        boolean oldIsDeducted = isStockDeductedState(currentStatus);
        boolean newIsDeducted = isStockDeductedState(newStatus);

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);

        boolean restoreStock = false;
        if (!oldIsDeducted && newIsDeducted) {
            // Deduct stock: lock variants, deduct
            for (HoaDonChiTiet item : items) {
                SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct != null) {
                    SanPhamChiTiet locked = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại."));
                    locked.setSoLuongTon(Math.max(0, locked.getSoLuongTon() - item.getSoLuong()));
                    sanPhamChiTietRepository.save(locked);
                }
            }
        } else if (oldIsDeducted && !newIsDeducted) {
            // If cancellation, check if already shipped
            if ("dang_giao".equalsIgnoreCase(currentStatus) || "dang_lay_hang".equalsIgnoreCase(currentStatus)) {
                // Shipped cancellation -> do NOT restore stock, set return status
                restoreStock = false;
                hd.setTrangThaiHoanHang(targetReturnStatus);
            } else {
                // Not yet shipped -> restore stock immediately
                restoreStock = true;
            }
        }

        if (restoreStock) {
            for (HoaDonChiTiet item : items) {
                SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct != null) {
                    SanPhamChiTiet locked = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại."));
                    locked.setSoLuongTon(locked.getSoLuongTon() + item.getSoLuong());
                    sanPhamChiTietRepository.save(locked);
                }
            }
        }

        // Cập nhật trạng thái thanh toán và refund status
        String pm = hd.getPaymentMethod();
        boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));

        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(newStatus)) {
            hd.setPaymentStatus(PaymentStatus.PAID.getValue());
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            if (hd.getPaidAt() == null) {
                hd.setPaidAt(LocalDateTime.now());
            }
            if (hd.getThoiGianXacNhan() == null) {
                hd.setThoiGianXacNhan(LocalDateTime.now());
            }
        } else if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
            if (isOrderPaid(hd)) {
                if (!"DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) && !"REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
                    hd.setPaymentStatus("paid");
                    hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                }
                if (isPrepaid || isOrderPaid(hd)) {
                    hd.setRefundStatus(RefundStatus.PENDING);
                }
            } else {
                hd.setPaymentStatus("CANCELLED");
                hd.setTrangThaiThanhToan("HUY");
            }
        }

        hd.setTrangThaiDonHang(newStatus);
        hd = hoaDonRepository.save(hd);

        // Generate notification for customer from GHN Webhook update
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            try {
                String maDon = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
                String labelCu = getStatusLabel(currentStatus);
                String labelMoi = getStatusLabel(newStatus);
                String msgContent = String.format("Đơn hàng %s của bạn đã được cập nhật trạng thái từ [%s] sang [%s].",
                        maDon, labelCu, labelMoi);

                ThongBao thongBao = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe("Cập nhật trạng thái giao hàng " + maDon)
                        .noiDung(msgContent)
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBao);
            } catch (Exception e) {
                log.error("Lỗi tạo thông báo trạng thái giao hàng GHN cho khách: {}", e.getMessage());
            }
        }

        // Gửi email nếu hủy đơn và trạng thái là chờ hoàn tiền
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus) && "CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            try {
                guiEmailYeuCauHoanTien(hd, "Hủy tự động qua webhook GHN do lỗi vận chuyển hoặc khách hàng từ chối nhận (Trạng thái GHN: " + ghnStatus + ")");
            } catch (Exception e) {
                // log error but do not rollback
            }
        }

        // Audit Log
        String giaTriCu = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                currentStatus, currentPaymentStatus, currentTrangThaiThanhToan,
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL",
                hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");

        String giaTriMoi = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                newStatus, hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL",
                hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");

        String ghiChuLog = String.format("[GHN_WEBHOOK] Cập nhật trạng thái tự động từ webhook GHN. Mã vận đơn: %s, Trạng thái GHN: %s", hd.getGhnOrderCode(), ghnStatus);

        auditService.log(
                null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                giaTriCu,
                giaTriMoi,
                "127.0.0.1",
                ghiChuLog,
                "SYSTEM"
        );
    }

    private boolean isStockDeductedState(String status) {
        if (status == null) {
            return false;
        }
        String lower = status.toLowerCase();
        return "cho_xac_nhan".equals(lower) || "da_xac_nhan".equals(lower)
                || "dang_lay_hang".equals(lower) || "dang_giao".equals(lower)
                || "da_giao".equals(lower);
    }

    public String getNextStatus(String currentStatus) {
        if (currentStatus == null) {
            return null;
        }
        return switch (currentStatus.toLowerCase()) {
            case "cho_thanh_toan" ->
                "cho_xac_nhan";
            case "cho_xac_nhan" ->
                "da_xac_nhan";
            case "da_xac_nhan" ->
                "dang_giao";
            case "dang_lay_hang" ->
                "dang_giao";
            case "dang_giao" ->
                "da_giao";
            case "stock_conflict" ->
                "cho_xac_nhan";
            default ->
                null;
        };
    }

    @Transactional
    public void moveOrderToNextStatus(Integer orderId, Integer actingUserId, String clientIp) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String currentStatus = hd.getTrangThaiDonHang();
        String nextStatus = getNextStatus(currentStatus);
        if (nextStatus == null) {
            throw new IllegalStateException("Đơn hàng đã ở trạng thái cuối cùng hoặc không thể tự chuyển tiếp.");
        }

        updateOrderStatusByAdmin(orderId, nextStatus, currentStatus, actingUserId, clientIp);
    }

    public Map<String, LocalDateTime> getStatusTransitionTimes(Integer idHoaDon) {
        Map<String, LocalDateTime> times = new HashMap<>();
        try {
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", idHoaDon);
            for (com.smashvn.shop.entity.EditLog log : logs) {
                String giaTriMoi = log.getGiaTriMoi();
                if (giaTriMoi != null) {
                    if (giaTriMoi.contains("status=da_xac_nhan")) {
                        times.put("da_xac_nhan", log.getThoiGian());
                    }
                    if (giaTriMoi.contains("status=dang_lay_hang")) {
                        times.put("dang_lay_hang", log.getThoiGian());
                    }
                    if (giaTriMoi.contains("status=dang_giao")) {
                        times.put("dang_giao", log.getThoiGian());
                    }
                    if (giaTriMoi.contains("status=da_giao")) {
                        times.put("da_giao", log.getThoiGian());
                    }
                    if (giaTriMoi.contains("status=da_huy")) {
                        times.put("da_huy", log.getThoiGian());
                    }
                }
            }
        } catch (Exception e) {
            // Fallback silent
        }
        return times;
    }

    private boolean isTestEnvironment() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.") || element.getClassName().startsWith("org.spockframework.")) {
                return true;
            }
        }
        return false;
    }

    private void guiEmailYeuCauHoanTien(HoaDon hd, String lyDoHuy) {
        String token = java.util.UUID.randomUUID().toString();
        // Store token in gatewayResponse
        String oldResponse = hd.getGatewayResponse() != null ? hd.getGatewayResponse() : "";
        hd.setGatewayResponse("REFUND_TOKEN:" + token + ";" + oldResponse);
        hoaDonRepository.save(hd);

        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            log.error("Không có email quản trị nào được cấu hình trong app.admin.emails!");
            return;
        }

        String maDonHang = hd.getMaDonHang() != null ? hd.getMaDonHang() : "POS#" + hd.getId();

        // Tránh spam gửi email thật khi chạy test cases
        if (isTestEnvironment() || maDonHang.startsWith("TEST-")) {
            System.out.println("[TEST] Bỏ qua gửi email xác nhận hoàn tiền cho đơn hàng test: " + maDonHang);
            return;
        }

        String appUrl = resolveAppUrl();
        String approveLink = appUrl + "/admin/don-hang/approve-refund?id=" + hd.getId() + "&token=" + token;
        String rejectLink = appUrl + "/admin/don-hang/reject-refund?id=" + hd.getId() + "&token=" + token;

        String tenKhachHang = hd.getKhachHang() != null ? (hd.getKhachHang().getHoKh() + " " + hd.getKhachHang().getTenKh()) : "Khách lẻ";
        String sdt = hd.getSdtNhan() != null ? hd.getSdtNhan() : "N/A";
        String phuongThuc = hd.getPaymentMethod() != null ? hd.getPaymentMethod() : (hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "N/A");
        String formattedTongTien = hd.getTongTien() != null ? String.format("%,.0f", hd.getTongTien()) : "0";
        String hienThiLyDo = (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) ? lyDoHuy.trim() : "Không cung cấp lý do";

        String htmlMsg = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "    <meta charset=\"utf-8\">"
                + "    <title>Yêu cầu xác nhận hoàn tiền</title>"
                + "</head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Inter', system-ui, -apple-system, sans-serif;\">"
                + "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e9ecef;\">"
                + "        <tr>"
                + "            <td style=\"padding: 32px 40px; background-color: #212529; text-align: center;\">"
                + "                <h2 style=\"margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;\">SMASH VN</h2>"
                + "                <p style=\"margin: 4px 0 0 0; color: #adb5bd; font-size: 14px;\">Yêu cầu xác nhận hoàn tiền đơn hàng</p>"
                + "            </td>"
                + "        </tr>"
                + "        <tr>"
                + "            <td style=\"padding: 40px;\">"
                + "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 16px; line-height: 1.6;\">Chào Quản lý hệ thống,</p>"
                + "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 16px; line-height: 1.6;\">Một yêu cầu hoàn tiền cho đơn hàng đã thanh toán trực tuyến vừa được tạo và đang chờ bạn phê duyệt:</p>"
                + "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f8f9fa; border-radius: 12px; padding: 20px; margin-bottom: 32px; border: 1px solid #e9ecef;\">"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px; width: 40%;\">Mã đơn hàng:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; font-family: monospace;\">" + maDonHang + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Khách hàng:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600;\">" + tenKhachHang + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Số điện thoại:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px;\">" + sdt + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Phương thức:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; color: #0d6efd;\">" + phuongThuc + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Yêu cầu hoàn tiền:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; color: #dc3545;\">" + (hd.getRefundStatus() != null ? hd.getRefundStatus().getLabel() : "Không yêu cầu") + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Trạng thái hoàn kho:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600;\">" + (hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().getLabel() : "Đã hoàn kho lập tức (Chưa xuất kho)") + "</td>"
                + "                    </tr>"
                + "                    <tr style=\"background-color: #fff5f5;\">"
                + "                        <td style=\"padding: 8px 10px; color: #dc3545; font-size: 14px; font-weight: bold;\">Lý do hủy:</td>"
                + "                        <td style=\"padding: 8px 10px; color: #dc3545; font-size: 14px; font-weight: bold;\">" + hienThiLyDo + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 8px 0 6px 0; color: #6c757d; font-size: 14px; border-top: 1px dashed #dee2e6;\">Số tiền cần hoàn:</td>"
                + "                        <td style=\"padding: 8px 0 6px 0; color: #dc3545; font-size: 16px; font-weight: 700; border-top: 1px dashed #dee2e6;\">" + formattedTongTien + " đ</td>"
                + "                    </tr>"
                + "                </table>"
                + "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 15px; line-height: 1.6; text-align: center; font-weight: 600;\">"
                + "                    Vui lòng chọn một trong các thao tác bên dưới để xử lý yêu cầu:"
                + "                </p>"
                + "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" style=\"margin: 0 auto 32px auto;\">"
                + "                    <tr>"
                + "                        <td align=\"center\" style=\"padding: 0 10px;\">"
                + "                            <a href=\"" + approveLink + "\" style=\"background-color: #198754; color: #ffffff; text-decoration: none; padding: 14px 24px; font-size: 14px; font-weight: bold; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(25, 135, 84, 0.2);\">"
                + "                                Phê duyệt hoàn tiền"
                + "                            </a>"
                + "                        </td>"
                + "                        <td align=\"center\" style=\"padding: 0 10px;\">"
                + "                            <a href=\"" + rejectLink + "\" style=\"background-color: #dc3545; color: #ffffff; text-decoration: none; padding: 14px 24px; font-size: 14px; font-weight: bold; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(220, 53, 69, 0.2);\">"
                + "                                Từ chối hoàn tiền"
                + "                            </a>"
                + "                        </td>"
                + "                    </tr>"
                + "                </table>"
                + "                <p style=\"margin: 16px 0 0 0; font-size: 13px; color: #6c757d; text-align: center;\">"
                + "                   Hoặc bạn cũng có thể duyệt/từ chối trực tiếp trên Dashboard Smash VN."
                + "                </p>"
                + "                <hr style=\"border: 0; border-top: 1px solid #e9ecef; margin: 32px 0;\">"
                + "                <p style=\"margin: 0; color: #868e96; font-size: 12px; line-height: 1.5; text-align: center;\">"
                + "                    * Lưu ý: Khi nhấp <strong>Phê duyệt hoàn tiền</strong>, số tiền này sẽ bị trừ khỏi thống kê doanh thu.<br>"
                + "                    Nếu nhấp <strong>Từ chối hoàn tiền</strong>, trạng thái thanh toán sẽ được khôi phục và doanh thu được giữ nguyên."
                + "                </p>"
                + "            </td>"
                + "        </tr>"
                + "        <tr>"
                + "            <td style=\"padding: 24px; background-color: #f8f9fa; text-align: center; border-top: 1px solid #e9ecef;\">"
                + "                <p style=\"margin: 0; color: #adb5bd; font-size: 12px;\">Hệ thống Quản trị Smash VN &copy; 2026</p>"
                + "            </td>"
                + "        </tr>"
                + "    </table>"
                + "</body>"
                + "</html>";

        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(email.trim());
                helper.setSubject("[Smash VN] Yêu cầu xác nhận hoàn tiền - Đơn hàng " + maDonHang);
                helper.setText(htmlMsg, true);
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Lỗi gửi mail yêu cầu hoàn tiền đến {}: {}", com.smashvn.shop.util.ValidationUtils.maskEmail(email), e.getMessage());
            }
        }
    }

    private String resolveAppUrl() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs
                    = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();

                // 1. Resolve scheme (taking X-Forwarded-Proto into account)
                String scheme = request.getHeader("X-Forwarded-Proto");
                if (scheme == null || scheme.isEmpty()) {
                    scheme = request.getScheme();
                }

                // 2. Resolve host (taking X-Forwarded-Host or Host header into account)
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

                // Thêm ":8080" nếu host là localhost hoặc 127.0.0.1 (không kèm cổng)
                if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equalsIgnoreCase(host)) {
                    host = host + ":8080";
                }

                String contextPath = request.getContextPath();
                return scheme + "://" + host + contextPath;
            }
        } catch (Exception e) {
            // fallback silent
        }
        return "http://localhost:8080";
    }

    @Transactional
    public void approveRefund(Integer orderId, String token, Integer actingUserId, String clientIp) {
        if (actingUserId == null) {
            throw new AccessDeniedException("Tài khoản người thực hiện không tồn tại.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingUserId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        if (!"QL".equals(actingUser.getVaiTro())) {
            throw new AccessDeniedException("Chỉ Quản lý mới có thể phê duyệt.");
        }

        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String response = hd.getGatewayResponse();
        if (token != null && !token.trim().isEmpty()) {
            if (response == null || !response.contains("REFUND_TOKEN:" + token)) {
                throw new IllegalArgumentException("Token xác nhận hoàn tiền không hợp lệ hoặc đã hết hiệu lực!");
            }
        }

        if (!"CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái chờ hoàn tiền!");
        }

        String oldState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL");

        // Update payment status to REFUNDED
        hd.setPaymentStatus("REFUNDED");
        hd.setTrangThaiThanhToan("REFUNDED");
        hd.setRefundStatus(RefundStatus.COMPLETED);
        hd.setRefundTime(LocalDateTime.now());

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingUserId);
        if (nv != null) {
            hd.setRefundConfirmedBy(nv);
        }

        // Remove token from response
        if (response != null && token != null && !token.trim().isEmpty()) {
            String newToken = response.replaceAll("REFUND_TOKEN:" + token + ";?", "");
            hd.setGatewayResponse(newToken);
        } else if (response != null && response.contains("REFUND_TOKEN:")) {
            String newToken = response.replaceAll("REFUND_TOKEN:[^;]+;?", "");
            hd.setGatewayResponse(newToken);
        }

        hoaDonRepository.save(hd);

        // Audit log
        String newState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(), hd.getRefundStatus().name());

        String actingUserRole = "QUAN_LY";
        if (actingUserId != null) {
            TaiKhoan tk = taiKhoanRepository.findById(actingUserId).orElse(null);
            if (tk != null && tk.getVaiTro() != null) {
                actingUserRole = tk.getVaiTro();
            }
        }

        auditService.log(
                actingUserId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                oldState,
                newState,
                clientIp,
                "[REFUND_APPROVED] Quản lý phê duyệt hoàn tiền thành công. Trạng thái hoàn tiền: COMPLETED. Đơn hàng hiện đã chính thức trừ khỏi thống kê doanh thu.",
                actingUserRole
        );
    }

    @Transactional
    public void rejectRefund(Integer orderId, String token, Integer actingUserId, String clientIp) {
        if (actingUserId == null) {
            throw new AccessDeniedException("Tài khoản người thực hiện không tồn tại.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingUserId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        if (!"QL".equals(actingUser.getVaiTro())) {
            throw new AccessDeniedException("Chỉ Quản lý mới có thể phê duyệt.");
        }

        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String response = hd.getGatewayResponse();
        if (token != null && !token.trim().isEmpty()) {
            if (response == null || !response.contains("REFUND_TOKEN:" + token)) {
                throw new IllegalArgumentException("Token xác nhận hoàn tiền không hợp lệ hoặc đã hết hiệu lực!");
            }
        }

        if (!"CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái chờ hoàn tiền!");
        }

        String oldState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL");

        // Revert to PAID and DA_THANH_TOAN
        hd.setPaymentStatus("paid");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setRefundStatus(null);
        hd.setRefundTime(null);
        hd.setRefundConfirmedBy(null);

        // Remove token from response
        if (response != null && token != null && !token.trim().isEmpty()) {
            String newToken = response.replaceAll("REFUND_TOKEN:" + token + ";?", "");
            hd.setGatewayResponse(newToken);
        } else if (response != null && response.contains("REFUND_TOKEN:")) {
            String newToken = response.replaceAll("REFUND_TOKEN:[^;]+;?", "");
            hd.setGatewayResponse(newToken);
        }

        hoaDonRepository.save(hd);

        // Audit log
        String newState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(), "NULL");

        String actingUserRole = "QUAN_LY";
        if (actingUserId != null) {
            TaiKhoan tk = taiKhoanRepository.findById(actingUserId).orElse(null);
            if (tk != null && tk.getVaiTro() != null) {
                actingUserRole = tk.getVaiTro();
            }
        }

        auditService.log(
                actingUserId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                oldState,
                newState,
                clientIp,
                "[REFUND_REJECTED] Quản lý từ chối yêu cầu hoàn tiền. Đơn hàng được giữ nguyên trạng thái thanh toán và thống kê doanh thu.",
                actingUserRole
        );
    }

    @Transactional
    public void updateReturnStatusByAdmin(Integer idHoaDon, String newReturnStatusStr, Integer actingTaiKhoanId, String clientIp) {
        // 1. Authorization Check
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String role = actingUser.getVaiTro();
        if (!"NV".equals(role) && !"QL".equals(role)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }

        // 2. Lock HoaDon
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturnStatus = hd.getTrangThaiHoanHang();
        ReturnStatus newReturnStatus = ReturnStatus.valueOf(newReturnStatusStr.toUpperCase());

        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 3. Validation Of Allowed Return Status Transitions
        // Only allow transition when order is cancelled (da_huy) and return status is PENDING_RETURN
        if (!OrderStatus.DA_HUY.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang()) || currentReturnStatus != ReturnStatus.PENDING_RETURN) {
            String warningMsg = String.format("[WARNING] Yêu cầu chuyển đổi trạng thái hoàn hàng không hợp lệ từ %s sang %s cho hóa đơn #%d",
                    currentReturnStatus != null ? currentReturnStatus.name() : "NULL",
                    newReturnStatus.name(),
                    hd.getId());

            auditService.log(
                    actingTaiKhoanId,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    String.format("trangThaiHoanHang=%s", currentReturnStatus != null ? currentReturnStatus.name() : "NULL"),
                    String.format("trangThaiHoanHang=%s", newReturnStatus.name()),
                    clientIp,
                    warningMsg,
                    roleStr
            );
            throw new IllegalStateException("Chỉ cho phép chuyển trạng thái hoàn hàng từ PENDING_RETURN sang RETURNED, LOST hoặc DAMAGED.");
        }

        // 4. Perform stock adjustment if new status is RETURNED
        String adjustmentDetails = "Không có điều chỉnh kho";
        if (newReturnStatus == ReturnStatus.RETURNED) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            StringBuilder detailsBuilder = new StringBuilder("Chi tiết điều chỉnh tồn kho:\n");
            for (HoaDonChiTiet item : items) {
                SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct != null) {
                    SanPhamChiTiet locked = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại."));
                    locked.setSoLuongTon(locked.getSoLuongTon() + item.getSoLuong());
                    sanPhamChiTietRepository.save(locked);

                    detailsBuilder.append(String.format("SPCT-%d : +%d\n", spct.getId(), item.getSoLuong()));
                }
            }
            adjustmentDetails = detailsBuilder.toString().trim();
        } else if (newReturnStatus == ReturnStatus.LOST || newReturnStatus == ReturnStatus.DAMAGED) {
            adjustmentDetails = "Không hoàn trả tồn kho (Hàng bị mất/hỏng)";
        }

        // 5. Update Audit Information
        hd.setTrangThaiHoanHang(newReturnStatus);
        hd.setNgayXacNhanHoanHang(LocalDateTime.now());

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv != null) {
            hd.setNhanVienXacNhan(nv);
        }

        hoaDonRepository.save(hd);

        // Generate notification for customer on return status update
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            try {
                String maDon = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
                String labelCu = currentReturnStatus != null ? currentReturnStatus.getLabel() : "Chưa có";
                String labelMoi = newReturnStatus.getLabel();
                String msgContent = String.format("Đơn hàng hoàn trả %s của bạn đã được cập nhật trạng thái từ [%s] sang [%s].",
                        maDon, labelCu, labelMoi);

                ThongBao thongBao = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe("Cập nhật trạng thái hoàn hàng " + maDon)
                        .noiDung(msgContent)
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBao);
            } catch (Exception e) {
                log.error("Lỗi tạo thông báo trạng thái hoàn hàng cho khách: {}", e.getMessage());
            }
        }

        // 6. Log Audit
        String logNote = String.format("[WAREHOUSE_RETURN_%s] Xác nhận trạng thái hoàn hàng: %s. %s",
                newReturnStatus.name(), newReturnStatus.getLabel(), adjustmentDetails);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                String.format("trangThaiHoanHang=PENDING_RETURN"),
                String.format("trangThaiHoanHang=%s", newReturnStatus.name()),
                clientIp,
                logNote,
                roleStr
        );
    }

    public record PaymentStatusInfo(String code, String label, String badgeClass) {}

    public PaymentStatusInfo getPaymentStatusInfo(String status) {
        if (status == null || status.trim().isEmpty()) {
            return new PaymentStatusInfo("UNKNOWN", "Không xác định", "bg-secondary");
        }
        String upperStatus = status.trim().toUpperCase();
        return switch (upperStatus) {
            case "PAID", "DA_THANH_TOAN" -> 
                new PaymentStatusInfo("PAID", "Đã thanh toán", "bg-success");
            case "PENDING", "CHO_THANH_TOAN" -> 
                new PaymentStatusInfo("PENDING", "Chờ thanh toán", "bg-warning text-dark");
            case "CANCELLED", "CANCELED", "HUY", "DA_HUY", "FAILED" -> 
                new PaymentStatusInfo("CANCELLED", "Đã hủy", "bg-danger");
            case "REFUNDED" -> 
                new PaymentStatusInfo("REFUNDED", "Đã hoàn tiền", "bg-danger");
            case "CHO_HOAN_TIEN", "HOAN_TIEN" -> 
                new PaymentStatusInfo("CHO_HOAN_TIEN", "Chờ hoàn tiền", "bg-warning text-dark");
            case "SAI LỆCH SỐ TIỀN", "AMOUNT_MISMATCH" -> 
                new PaymentStatusInfo("AMOUNT_MISMATCH", "Sai lệch số tiền", "bg-danger");
            case "PAID_RECEIVED_AFTER_CANCEL" -> 
                new PaymentStatusInfo("PAID_RECEIVED_AFTER_CANCEL", "Nhận thanh toán sau hủy", "bg-info");
            default -> 
                new PaymentStatusInfo("UNKNOWN", upperStatus, "bg-secondary");
        };
    }

    public boolean isOrderPaid(HoaDon hd) {
        if (hd == null) return false;
        String tt = hd.getTrangThaiThanhToan();
        String ps = hd.getPaymentStatus();
        
        if (tt != null) {
            String t = tt.trim().toUpperCase();
            if ("DA_THANH_TOAN".equals(t) || "PAID".equals(t) || "CHO_HOAN_TIEN".equals(t) || "DA_HOAN_TIEN".equals(t) || "REFUNDED".equals(t)) {
                return true;
            }
        }
        if (ps != null) {
            String p = ps.trim().toUpperCase();
            if ("DA_THANH_TOAN".equals(p) || "PAID".equals(p) || "CHO_HOAN_TIEN".equals(p) || "DA_HOAN_TIEN".equals(p) || "REFUNDED".equals(p)) {
                return true;
            }
        }
        return hd.getPaidAt() != null || hd.getNgayThanhToan() != null;
    }
}

