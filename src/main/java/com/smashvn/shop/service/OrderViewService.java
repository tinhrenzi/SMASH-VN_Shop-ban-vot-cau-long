package com.smashvn.shop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
        Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
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
                if (PaymentStatus.PAID.getValue().equalsIgnoreCase(hd.getPaymentStatus())) {
                    hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                    refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, customerId=%d",
                            hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), idKhachHang);
                } else {
                    hd.setPaymentStatus("CANCELLED");
                    hd.setTrangThaiThanhToan("HUY");
                }
                hoaDonRepository.save(hd);

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
        HoaDon hd = hoaDonRepository.findById(idHoaDon)
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
            // Restock: lock variants, add back stock
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
            if ("paid".equalsIgnoreCase(currentPaymentStatus)) {
                hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, actingUserId=%d",
                        hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), actingTaiKhoanId);
            } else {
                hd.setPaymentStatus("CANCELLED");
                hd.setTrangThaiThanhToan("HUY");
            }
        }

        // 9. Update Order
        hd.setTrangThaiDonHang(newStatus);
        hd = hoaDonRepository.save(hd);

        // 10. Audit Log Enhancement
        String giaTriCu = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s", currentStatus, currentPaymentStatus, currentTrangThaiThanhToan);
        String giaTriMoi = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s", newStatus, hd.getPaymentStatus(), hd.getTrangThaiThanhToan());
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
        HoaDon hd = hoaDonRepository.findById(orderId)
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
}
