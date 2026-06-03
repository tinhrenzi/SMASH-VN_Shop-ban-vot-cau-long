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

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderViewService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;

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
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", hd.getId());
                orderMap.put("date", hd.getNgayTao().format(formatter));

                // Ánh xạ trạng thái hiển thị
                String statusText = getStatusLabel(hd.getTrangThaiDonHang());
                orderMap.put("status", statusText);
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

                // Lấy thống kê thật
                List<HoaDon> allUserOrders = hoaDonRepository.findByKhachHang_Id(idKhachHang);
                long processingCount = allUserOrders.stream().filter(o -> !"delivered".equals(o.getTrangThaiDonHang()) && !"cancelled".equals(o.getTrangThaiDonHang())).count();
                long cancelCount = allUserOrders.stream().filter(o -> "cancelled".equals(o.getTrangThaiDonHang())).count();

                modelMap.put("orderCount", allUserOrders.size());
                modelMap.put("processingCount", processingCount);
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

    private String getStatusLabel(String dbStatus) {
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
        map.put("phiVanChuyen", new BigDecimal("30000")); // Hardcode ship fee cho đơn thật nếu chưa lưu trong DB
        map.put("phuongThucThanhToan", hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "COD");
        map.put("tongTien", hd.getTongTien());

        Map<String, Object> adr = new HashMap<>();
        adr.put("hoTen", hd.getKhachHang().getHoKh() + " " + hd.getKhachHang().getTenKh());
        adr.put("diaChiDayDu", hd.getDiaChiNhan());
        adr.put("soDienThoai", hd.getSdtNhan());

        map.put("diaChiGiao", adr);
        map.put("diaChiThanhToan", adr);
        return map;
    }
}
