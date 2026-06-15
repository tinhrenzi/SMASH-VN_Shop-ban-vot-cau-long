package com.smashvn.shop.service.order;
import com.smashvn.shop.service.AuditService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.ReturnStatus;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import org.springframework.security.access.AccessDeniedException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderViewService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final com.smashvn.shop.repository.EditLogRepository editLogRepository;
    private final JavaMailSender mailSender;
    private final NhanVienRepository nhanVienRepository;

    @Value("${app.admin.emails}")
    private String adminEmailsConfig;

    // Helper to format dates for dash-my-order.html
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.US);

    /**
     * Lấy danh sách đơn hàng cho dash-my-order.html. Nếu khách hàng chưa có đơn
     * hàng thật nào, trả về danh sách đơn hàng giả lập (Mock Orders).
     */
    public List<Map<String, Object>> layDanhSachOrders(Integer idKhachHang) {
        List<HoaDon> realOrders = hoaDonRepository.findByKhachHang_Id(idKhachHang);
        List<Map<String, Object>> resultList = new ArrayList<>();

        if (realOrders != null && !realOrders.isEmpty()) {
            // CÓ ĐƠN HÀNG THẬT
            for (HoaDon hd : realOrders) {
                // Skip unpaid/pending orders (cho_thanh_toan) to prevent duplicate/misleading display in Order History
                if ("cho_thanh_toan".equals(hd.getTrangThaiDonHang())) {
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

                List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
                List<Map<String, Object>> itemMaps = new ArrayList<>();
                for (HoaDonChiTiet ct : items) {
                    Map<String, Object> itemMap = new HashMap<>();
                    SanPhamChiTiet spct = ct.getSanPhamChiTiet();

                    // Lấy ảnh hiển thị
                    String imgName = "product9.jpg"; // fallback
                    if (spct.getHinhAnhSanPham() != null && !spct.getHinhAnhSanPham().isEmpty()) {
                        imgName = spct.getHinhAnhSanPham();
                    }

                    itemMap.put("image", "../uploads/product/" + imgName); // do path ở uploads
                    itemMap.put("title", spct.getSanPham().getTenSanPham() + " [" + spct.getMauSac() + "]");
                    itemMap.put("quantity", ct.getSoLuong());
                    itemMap.put("total", ct.getDonGia().multiply(new BigDecimal(ct.getSoLuong())));
                    itemMaps.add(itemMap);
                }
                orderMap.put("items", itemMaps);
                resultList.add(orderMap);
            }
        } else {
            // CHƯA CÓ ĐƠN HÀNG THẬT -> Sinh danh sách Mock Orders
            List<SanPham> activeProducts = sanPhamRepository.findAll();

            // Mock Order 1: Đang xử lý
            Map<String, Object> mock1 = new HashMap<>();
            mock1.put("id", 1001);
            mock1.put("date", LocalDateTime.now().minusHours(2).format(formatter));
            mock1.put("status", "processing");
            mock1.put("rawStatus", "cho_xac_nhan");

            List<Map<String, Object>> items1 = new ArrayList<>();
            Map<String, Object> item1_1 = new HashMap<>();

            String p1Name = "Vợt Cầu Lông Yonex Astrox 88D Pro";
            String p1Img = "product9.jpg";
            BigDecimal p1Price = new BigDecimal("4150000");

            if (!activeProducts.isEmpty()) {
                SanPham sp = activeProducts.get(0);
                p1Name = sp.getTenSanPham();
                if (sp.getSanPhamChiTiets() != null && !sp.getSanPhamChiTiets().isEmpty()) {
                    SanPhamChiTiet ct = sp.getSanPhamChiTiets().get(0);
                    p1Price = ct.getGiaBan();
                    if (ct.getHinhAnhSanPham() != null && !ct.getHinhAnhSanPham().isEmpty()) {
                        p1Img = ct.getHinhAnhSanPham();
                    }
                }
            }

            item1_1.put("image", "../uploads/product/" + p1Img);
            item1_1.put("title", p1Name + " - Cực bền");
            item1_1.put("quantity", 1);
            item1_1.put("total", p1Price);
            items1.add(item1_1);

            mock1.put("items", items1);
            mock1.put("total", p1Price.add(new BigDecimal("30000")));
            resultList.add(mock1);

            // Mock Order 2: Đã giao
            Map<String, Object> mock2 = new HashMap<>();
            mock2.put("id", 1002);
            mock2.put("date", LocalDateTime.now().minusDays(5).format(formatter));
            mock2.put("status", "delivered");
            mock2.put("rawStatus", "da_giao");

            List<Map<String, Object>> items2 = new ArrayList<>();
            Map<String, Object> item2_1 = new HashMap<>();

            String p2Name = "Vợt Cầu Lông Victor Thruster K C";
            String p2Img = "product10.jpg";
            BigDecimal p2Price = new BigDecimal("3200000");

            if (activeProducts.size() > 1) {
                SanPham sp = activeProducts.get(1);
                p2Name = sp.getTenSanPham();
                if (sp.getSanPhamChiTiets() != null && !sp.getSanPhamChiTiets().isEmpty()) {
                    SanPhamChiTiet ct = sp.getSanPhamChiTiets().get(0);
                    p2Price = ct.getGiaBan();
                    if (ct.getHinhAnhSanPham() != null && !ct.getHinhAnhSanPham().isEmpty()) {
                        p2Img = ct.getHinhAnhSanPham();
                    }
                }
            }

            item2_1.put("image", "../uploads/product/" + p2Img);
            item2_1.put("title", p2Name + " - Bản chính hãng");
            item2_1.put("quantity", 2);
            item2_1.put("total", p2Price.multiply(new BigDecimal(2)));
            items2.add(item2_1);

            mock2.put("items", items2);
            mock2.put("total", p2Price.multiply(new BigDecimal(2)).add(new BigDecimal("30000")));
            resultList.add(mock2);
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
                // Loại trừ đơn chờ thanh toán (cho_thanh_toan) và đơn đã hủy khi tính tổng đơn đã đặt
                long cancelCount = allUserOrders.stream().filter(o -> OrderStatus.DA_HUY.getValue().equals(o.getTrangThaiDonHang())).count();
                long orderCount = allUserOrders.stream().filter(o -> !"cho_thanh_toan".equals(o.getTrangThaiDonHang())).count() - cancelCount;

                modelMap.put("orderCount", orderCount);
                modelMap.put("cancelCount", cancelCount);
                modelMap.put("wishlistCount", 0);

                return modelMap;
            }
        }

        // 2. Nếu là ID mock (1001 hoặc 1002) hoặc không tìm thấy, trả về dữ liệu Mock hợp lệ
        if (idHoaDon.equals(1001) || idHoaDon.equals(1002)) {
            return generateMockOrderDetail(idHoaDon, idKhachHang);
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

    // Helper tạo Mock chi tiết cho demo UI
    private Map<String, Object> generateMockOrderDetail(Integer idHoaDon, Integer idKhachHang) {
        Map<String, Object> modelMap = new HashMap<>();

        KhachHang kh = khachHangRepository.findById(idKhachHang).orElse(null);
        if (kh == null) {
            return null;
        }

        // Tạo Mock HoaDon Object
        Map<String, Object> mockOrder = new HashMap<>();
        mockOrder.put("id", idHoaDon);
        mockOrder.put("ngayDat", idHoaDon.equals(1001) ? LocalDateTime.now().minusHours(2) : LocalDateTime.now().minusDays(5));
        mockOrder.put("ngayGiao", idHoaDon.equals(1001) ? LocalDateTime.now().plusDays(2) : LocalDateTime.now().minusDays(2));
        mockOrder.put("phuongThucVanChuyen", "Giao hàng nhanh tiết kiệm (GHTK)");
        mockOrder.put("phiVanChuyen", new BigDecimal("30000"));
        mockOrder.put("phuongThucThanhToan", "Thanh toán khi nhận hàng (COD)");
        mockOrder.put("trangThaiThanhToan", "CHO_THANH_TOAN");
        mockOrder.put("trangThaiDonHang", idHoaDon.equals(1001) ? "cho_xac_nhan" : "da_giao");
        mockOrder.put("status", idHoaDon.equals(1001) ? "processing" : "delivered");
        mockOrder.put("ngayXacNhan", mockOrder.get("ngayDat"));
        mockOrder.put("ngayThanhToan", mockOrder.get("ngayDat"));
        mockOrder.put("ngayGiaoDVVC", mockOrder.get("ngayDat"));
        mockOrder.put("ngayGiaoThanhCong", mockOrder.get("ngayGiao"));
        mockOrder.put("ghiChu", null);

        // Tạo thông tin địa chỉ giao nhận mock
        Map<String, Object> mockAddress = new HashMap<>();
        mockAddress.put("hoTen", kh.getHoKh() + " " + kh.getTenKh());
        mockAddress.put("diaChiDayDu", "Số 12, Đường Nguyễn Trãi, Quận Thanh Xuân, Hà Nội, Việt Nam");
        mockAddress.put("soDienThoai", kh.getSoDienThoaiKh().isEmpty() ? "0987654321" : kh.getSoDienThoaiKh());

        mockOrder.put("diaChiGiao", mockAddress);
        mockOrder.put("diaChiThanhToan", mockAddress);

        List<SanPham> activeProducts = sanPhamRepository.findAll();
        List<Map<String, Object>> mockItems = new ArrayList<>();
        BigDecimal totalItems; // assigned in each mock branch below

        if (idHoaDon.equals(1001)) {
            // Mock 1001 details
            Map<String, Object> itemMap = new HashMap<>();
            String pName = "Vợt Cầu Lông Yonex Astrox 88D Pro";
            String pImg = "product9.jpg";
            BigDecimal pPrice = new BigDecimal("4150000");

            if (!activeProducts.isEmpty()) {
                SanPham sp = activeProducts.get(0);
                pName = sp.getTenSanPham();
                if (sp.getSanPhamChiTiets() != null && !sp.getSanPhamChiTiets().isEmpty()) {
                    SanPhamChiTiet ct = sp.getSanPhamChiTiets().get(0);
                    pPrice = ct.getGiaBan();
                    if (ct.getHinhAnhSanPham() != null && !ct.getHinhAnhSanPham().isEmpty()) {
                        pImg = ct.getHinhAnhSanPham();
                    }
                }
            }

            // Tạo cấu trúc giống HoaDonChiTiet
            Map<String, Object> spctMap = new HashMap<>();
            spctMap.put("mauSac", "Đỏ xanh / 4U");
            spctMap.put("trongLuong", "4U");
            spctMap.put("mucCang", "26-28 lbs");
            spctMap.put("giaBan", pPrice);

            Map<String, Object> spMap = new HashMap<>();
            spMap.put("tenSanPham", pName);
            spMap.put("moTa", pImg); // Dùng làm ảnh hiển thị th:src

            spctMap.put("sanPham", spMap);

            itemMap.put("sanPhamChiTiet", spctMap);
            itemMap.put("soLuong", 1);
            itemMap.put("donGia", pPrice);
            mockItems.add(itemMap);

            totalItems = pPrice;
        } else {
            // Mock 1002 details
            Map<String, Object> itemMap = new HashMap<>();
            String pName = "Vợt Cầu Lông Victor Thruster K C";
            String pImg = "product10.jpg";
            BigDecimal pPrice = new BigDecimal("3200000");

            if (activeProducts.size() > 1) {
                SanPham sp = activeProducts.get(1);
                pName = sp.getTenSanPham();
                if (sp.getSanPhamChiTiets() != null && !sp.getSanPhamChiTiets().isEmpty()) {
                    SanPhamChiTiet ct = sp.getSanPhamChiTiets().get(0);
                    pPrice = ct.getGiaBan();
                    if (ct.getHinhAnhSanPham() != null && !ct.getHinhAnhSanPham().isEmpty()) {
                        pImg = ct.getHinhAnhSanPham();
                    }
                }
            }

            Map<String, Object> spctMap = new HashMap<>();
            spctMap.put("mauSac", "Đen Vàng / 3U");
            spctMap.put("trongLuong", "3U");
            spctMap.put("mucCang", "25-27 lbs");
            spctMap.put("giaBan", pPrice);

            Map<String, Object> spMap = new HashMap<>();
            spMap.put("tenSanPham", pName);
            spMap.put("moTa", pImg); // Dùng làm ảnh hiển thị th:src

            spctMap.put("sanPham", spMap);

            itemMap.put("sanPhamChiTiet", spctMap);
            itemMap.put("soLuong", 2);
            itemMap.put("donGia", pPrice);
            mockItems.add(itemMap);

            totalItems = pPrice.multiply(new BigDecimal(2));
        }

        mockOrder.put("tongTien", totalItems.add(new BigDecimal("30000")));

        modelMap.put("order", mockOrder);
        modelMap.put("orderItems", mockItems);

        // Thống kê mock
        modelMap.put("orderCount", 2);
        modelMap.put("cancelCount", 0);
        modelMap.put("wishlistCount", 0);

        return modelMap;
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

                if (PaymentStatus.PAID.getValue().equalsIgnoreCase(hd.getPaymentStatus())) {
                    hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                    String pm = hd.getPaymentMethod();
                    boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                            || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));
                    if (isPrepaid) {
                        hd.setRefundStatus(RefundStatus.PENDING);
                    }
                    refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, customerId=%d",
                            hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), idKhachHang);
                    hoaDonRepository.save(hd);
                    try {
                        guiEmailYeuCauHoanTien(hd, standardizedReason);
                    } catch (Exception e) {
                        System.err.println("Lỗi gửi email yêu cầu hoàn tiền khi khách hàng hủy đơn: " + e.getMessage());
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
        if (currentStatus == null) return List.of();
        return switch (currentStatus.toLowerCase()) {
            case "cho_thanh_toan" -> List.of("cho_xac_nhan", "da_huy");
            case "cho_xac_nhan" -> List.of("da_xac_nhan", "da_huy");
            case "da_xac_nhan" -> List.of("dang_lay_hang", "dang_giao", "da_huy");
            case "dang_lay_hang" -> List.of("dang_giao", "da_huy");
            case "dang_giao" -> List.of("da_giao", "da_huy");
            case "stock_conflict" -> List.of("cho_xac_nhan", "da_huy");
            default -> List.of();
        };
    }

    public String getStatusLabel(String status) {
        if (status == null) return "N/A";
        return switch (status.toLowerCase()) {
            case "cho_thanh_toan" -> "Chờ thanh toán";
            case "cho_xac_nhan" -> "Chờ xác nhận";
            case "da_xac_nhan" -> "Đã xác nhận";
            case "dang_lay_hang" -> "Đang lấy hàng";
            case "dang_giao" -> "Đang giao";
            case "da_giao" -> "Đã giao";
            case "da_huy" -> "Đã hủy";
            case "stock_conflict" -> "Trùng kho";
            default -> status;
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
        
        if (!Boolean.TRUE.equals(actingUser.getLaQuanLy()) && !Boolean.TRUE.equals(actingUser.getLaNhanVien())) {
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
        String roleStr = Boolean.TRUE.equals(actingUser.getLaQuanLy()) ? "QUAN_LY" : "NHAN_VIEN";
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

            if ("paid".equalsIgnoreCase(currentPaymentStatus) || "PAID".equalsIgnoreCase(hd.getPaymentStatus())) {
                hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                if (isPrepaid) {
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

        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus) && "CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            String standardizedReason = "Không cung cấp lý do";
            if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                standardizedReason = lyDoHuy.trim();
            }
            try {
                guiEmailYeuCauHoanTien(hd, standardizedReason);
            } catch (Exception e) {
                System.err.println("Lỗi gửi email yêu cầu hoàn tiền khi admin hủy đơn: " + e.getMessage());
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
    public void updateOrderStatusByWebhook(Integer idHoaDon, String newStatus, String ghnStatus) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        String currentStatus = hd.getTrangThaiDonHang();
        String currentPaymentStatus = hd.getPaymentStatus();
        String currentTrangThaiThanhToan = hd.getTrangThaiThanhToan();

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
            if ("paid".equalsIgnoreCase(currentPaymentStatus) || "PAID".equalsIgnoreCase(hd.getPaymentStatus())) {
                hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                if (isPrepaid) {
                    hd.setRefundStatus(RefundStatus.PENDING);
                }
            } else {
                hd.setPaymentStatus("CANCELLED");
                hd.setTrangThaiThanhToan("HUY");
            }
        }

        hd.setTrangThaiDonHang(newStatus);
        hd = hoaDonRepository.save(hd);

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
        if (status == null) return false;
        String lower = status.toLowerCase();
        return "cho_xac_nhan".equals(lower) || "da_xac_nhan".equals(lower) || "dang_lay_hang".equals(lower) || "dang_giao".equals(lower) || "da_giao".equals(lower);
    }

    public String getNextStatus(String currentStatus) {
        if (currentStatus == null) return null;
        return switch (currentStatus.toLowerCase()) {
            case "cho_thanh_toan" -> "cho_xac_nhan";
            case "cho_xac_nhan" -> "da_xac_nhan";
            case "da_xac_nhan" -> "dang_giao";
            case "dang_lay_hang" -> "dang_giao";
            case "dang_giao" -> "da_giao";
            case "stock_conflict" -> "cho_xac_nhan";
            default -> null;
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
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", Long.valueOf(idHoaDon));
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
        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            System.err.println("Không có email quản trị nào được cấu hình trong app.admin.emails!");
            return;
        }
        String token = java.util.UUID.randomUUID().toString();
        // Store token in gatewayResponse
        String oldResponse = hd.getGatewayResponse() != null ? hd.getGatewayResponse() : "";
        hd.setGatewayResponse("REFUND_TOKEN:" + token + ";" + oldResponse);
        hoaDonRepository.save(hd);

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

        String htmlMsg = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"utf-8\">" +
                "    <title>Yêu cầu xác nhận hoàn tiền</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Inter', system-ui, -apple-system, sans-serif;\">" +
                "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e9ecef;\">" +
                "        <tr>" +
                "            <td style=\"padding: 32px 40px; background-color: #212529; text-align: center;\">" +
                "                <h2 style=\"margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;\">SMASH VN</h2>" +
                "                <p style=\"margin: 4px 0 0 0; color: #adb5bd; font-size: 14px;\">Yêu cầu xác nhận hoàn tiền đơn hàng</p>" +
                "            </td>" +
                "        </tr>" +
                "        <tr>" +
                "            <td style=\"padding: 40px;\">" +
                "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 16px; line-height: 1.6;\">Chào Quản lý hệ thống,</p>" +
                "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 16px; line-height: 1.6;\">Một yêu cầu hoàn tiền cho đơn hàng đã thanh toán trực tuyến vừa được tạo và đang chờ bạn phê duyệt:</p>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f8f9fa; border-radius: 12px; padding: 20px; margin-bottom: 32px; border: 1px solid #e9ecef;\">" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px; width: 40%;\">Mã đơn hàng:</td>" +
                "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; font-family: monospace;\">" + maDonHang + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Khách hàng:</td>" +
                "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600;\">" + tenKhachHang + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Số điện thoại:</td>" +
                "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px;\">" + sdt + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Phương thức:</td>" +
                "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; color: #0d6efd;\">" + phuongThuc + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Yêu cầu hoàn tiền:</td>" +
                "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; color: #dc3545;\">" + (hd.getRefundStatus() != null ? hd.getRefundStatus().getLabel() : "Không yêu cầu") + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Trạng thái hoàn kho:</td>" +
                "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600;\">" + (hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().getLabel() : "Đã hoàn kho lập tức (Chưa xuất kho)") + "</td>" +
                "                    </tr>" +
                "                    <tr style=\"background-color: #fff5f5;\">" +
                "                        <td style=\"padding: 8px 10px; color: #dc3545; font-size: 14px; font-weight: bold;\">Lý do hủy:</td>" +
                "                        <td style=\"padding: 8px 10px; color: #dc3545; font-size: 14px; font-weight: bold;\">" + hienThiLyDo + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0 6px 0; color: #6c757d; font-size: 14px; border-top: 1px dashed #dee2e6;\">Số tiền cần hoàn:</td>" +
                "                        <td style=\"padding: 8px 0 6px 0; color: #dc3545; font-size: 16px; font-weight: 700; border-top: 1px dashed #dee2e6;\">" + formattedTongTien + " đ</td>" +
                "                    </tr>" +
                "                </table>" +
                "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 15px; line-height: 1.6; text-align: center; font-weight: 600;\">" +
                "                    Vui lòng chọn một trong các thao tác bên dưới để xử lý yêu cầu:" +
                "                </p>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" style=\"margin: 0 auto 32px auto;\">" +
                "                    <tr>" +
                "                        <td align=\"center\" style=\"padding: 0 10px;\">" +
                "                            <a href=\"" + approveLink + "\" style=\"background-color: #198754; color: #ffffff; text-decoration: none; padding: 14px 24px; font-size: 14px; font-weight: bold; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(25, 135, 84, 0.2);\">" +
                "                                Phê duyệt hoàn tiền" +
                "                            </a>" +
                "                        </td>" +
                "                        <td align=\"center\" style=\"padding: 0 10px;\">" +
                "                            <a href=\"" + rejectLink + "\" style=\"background-color: #dc3545; color: #ffffff; text-decoration: none; padding: 14px 24px; font-size: 14px; font-weight: bold; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(220, 53, 69, 0.2);\">" +
                "                                Từ chối hoàn tiền" +
                "                            </a>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "                <p style=\"margin: 16px 0 0 0; font-size: 13px; color: #6c757d; text-align: center;\">" +
                "                   Hoặc bạn cũng có thể duyệt/từ chối trực tiếp trên Dashboard Smash VN." +
                "                </p>" +
                "                <hr style=\"border: 0; border-top: 1px solid #e9ecef; margin: 32px 0;\">" +
                "                <p style=\"margin: 0; color: #868e96; font-size: 12px; line-height: 1.5; text-align: center;\">" +
                "                    * Lưu ý: Khi nhấp <strong>Phê duyệt hoàn tiền</strong>, số tiền này sẽ bị trừ khỏi thống kê doanh thu.<br>" +
                "                    Nếu nhấp <strong>Từ chối hoàn tiền</strong>, trạng thái thanh toán sẽ được khôi phục và doanh thu được giữ nguyên." +
                "                </p>" +
                "            </td>" +
                "        </tr>" +
                "        <tr>" +
                "            <td style=\"padding: 24px; background-color: #f8f9fa; text-align: center; border-top: 1px solid #e9ecef;\">" +
                "                <p style=\"margin: 0; color: #adb5bd; font-size: 12px;\">Hệ thống Quản trị Smash VN &copy; 2026</p>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";

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
                System.err.println("Lỗi gửi mail yêu cầu hoàn tiền đến " + com.smashvn.shop.util.ValidationUtils.maskEmail(email) + ": " + e.getMessage());
            }
        }
    }

    private String resolveAppUrl() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
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
        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        
        String response = hd.getGatewayResponse();
        if (response == null || !response.contains("REFUND_TOKEN:" + token)) {
            throw new IllegalArgumentException("Token xác nhận hoàn tiền không hợp lệ hoặc đã hết hiệu lực!");
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
        String newToken = response.replaceAll("REFUND_TOKEN:" + token + ";?", "");
        hd.setGatewayResponse(newToken);
        
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
        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        
        String response = hd.getGatewayResponse();
        if (response == null || !response.contains("REFUND_TOKEN:" + token)) {
            throw new IllegalArgumentException("Token xác nhận hoàn tiền không hợp lệ hoặc đã hết hiệu lực!");
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
        String newToken = response.replaceAll("REFUND_TOKEN:" + token + ";?", "");
        hd.setGatewayResponse(newToken);
        
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
        if (!Boolean.TRUE.equals(actingUser.getLaQuanLy()) && !Boolean.TRUE.equals(actingUser.getLaNhanVien())) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }

        // 2. Lock HoaDon
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturnStatus = hd.getTrangThaiHoanHang();
        ReturnStatus newReturnStatus = ReturnStatus.valueOf(newReturnStatusStr.toUpperCase());

        String roleStr = Boolean.TRUE.equals(actingUser.getLaQuanLy()) ? "QUAN_LY" : "NHAN_VIEN";

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
}
