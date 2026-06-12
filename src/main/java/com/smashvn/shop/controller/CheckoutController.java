package com.smashvn.shop.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.GioHangService;
import com.smashvn.shop.service.UserAddressService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final GioHangService gioHangService;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final UserAddressService userAddressService;
    private final SepayConfig sepayConfig;
    private final com.smashvn.shop.repository.KhachHangRepository khachHangRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping({"/checkout", "/checkout.html"})
    public String viewCheckout(HttpSession session, Model model) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode("Bạn chưa đăng nhập. Vui lòng đăng nhập để thanh toán!", java.nio.charset.StandardCharsets.UTF_8);
        }

        // Clean up any old unpaid pending orders when loading the checkout page
        gioHangService.cleanPendingOrders(idNguoiDung);

        List<GioHangChiTiet> danhSachChiTiet = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
        if (danhSachChiTiet.isEmpty()) {
            return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng của bạn đang trống!", java.nio.charset.StandardCharsets.UTF_8);
        }

        BigDecimal tongTien = BigDecimal.ZERO;
        for (GioHangChiTiet item : danhSachChiTiet) {
            if (item.getSanPhamChiTiet() == null || item.getSanPhamChiTiet().getSanPham() == null) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng chứa sản phẩm không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }
            SanPham sp = item.getSanPhamChiTiet().getSanPham();
            int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
            String trangThai = sp.getTrangThai();

            if (item.getSoLuong() == null || item.getSoLuong() <= 0) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Số lượng sản phẩm trong giỏ hàng không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (!"dang_ban".equals(trangThai)) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' đã ngưng kinh doanh!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (tonKho <= 0) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' đã hết hàng!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (item.getSoLuong() > tonKho) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' không đủ số lượng tồn kho (Còn lại: " + tonKho + ")!", java.nio.charset.StandardCharsets.UTF_8);
            }
            tongTien = tongTien.add(item.getSanPhamChiTiet().getGiaBan().multiply(new BigDecimal(item.getSoLuong())));
        }

        List<DonViVanChuyen> listDvvc = donViVanChuyenDAO.findAll();

        com.smashvn.shop.entity.KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
        Integer idKhachHang = (khachHang != null) ? khachHang.getId() : idNguoiDung;
        List<SoDiaChi> listDiaChi = userAddressService.layDanhSachDiaChi(idKhachHang);
        boolean hasDefaultAddress = listDiaChi.stream().anyMatch(SoDiaChi::isDefaultShipping);

        Map<Integer, Map<String, Object>> addressMap = new java.util.HashMap<>();
        for (SoDiaChi dc : listDiaChi) {
            Map<String, Object> details = new java.util.HashMap<>();
            details.put("hoTen", dc.getHoNguoiNhan() + " " + dc.getTenNguoiNhan());
            details.put("sdt", dc.getSdtNguoiNhan());
            details.put("diaChiCuThe", dc.getDiaChiCuThe());
            details.put("tinhThanh", dc.getTinhThanh());
            details.put("thanhPho", dc.getThanhPho());
            details.put("quocGia", dc.getQuocGia());
            details.put("latitude", dc.getLatitude());
            details.put("longitude", dc.getLongitude());
            details.put("diaChi", dc.getDiaChiCuThe() + ", " + dc.getTinhThanh() + ", " + dc.getQuocGia());
            addressMap.put(dc.getId(), details);
        }

        String addressMapJson = "{}";
        try {
            addressMapJson = objectMapper.writeValueAsString(addressMap);
        } catch (Exception e) {
            // Ignore/fallback
        }

        model.addAttribute("danhSachCart", danhSachChiTiet);
        model.addAttribute("tongTien", tongTien);
        model.addAttribute("listDvvc", listDvvc);
        model.addAttribute("listDiaChi", listDiaChi);
        model.addAttribute("hasDefaultAddress", hasDefaultAddress);
        model.addAttribute("addressMapJson", addressMapJson);
        model.addAttribute("sepayBankAccount", sepayConfig.getBankAccount());
        model.addAttribute("sepayBankName", sepayConfig.getBankName());
        model.addAttribute("sepayMemoPrefix", sepayConfig.getMemoPrefix());

        return "checkout";
    }

    @PostMapping("/checkout/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCheckout(
            @RequestParam(value = "hoTenNhan", required = false) String hoTenNhan,
            @RequestParam(value = "sdtNhan", required = false) String sdtNhan,
            @RequestParam(value = "diaChiNhan", required = false) String diaChiNhan,
            @RequestParam(value = "idDonViVanChuyen", required = false) Integer idDonViVanChuyen,
            @RequestParam(value = "phuongThucThanhToan", required = false) String phuongThucThanhToan,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            @RequestParam(value = "ghnToDistrictId", required = false) Integer ghnToDistrictId,
            @RequestParam(value = "ghnToWardCode", required = false) String ghnToWardCode,
            @RequestParam(value = "ghnProvinceId", required = false) Integer ghnProvinceId,
            @RequestParam(value = "idDiaChiLuu", required = false) Integer idDiaChiLuu,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            response.put("trangThai", "chuadangnhap");
            response.put("message", "Vui lòng đăng nhập để thực hiện thanh toán.");
            return ResponseEntity.ok(response);
        }

        com.smashvn.shop.entity.KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
        Integer idKhachHang = (khachHang != null) ? khachHang.getId() : idNguoiDung;

        if (idDiaChiLuu != null) {
            try {
                userAddressService.layDiaChiTheoId(idDiaChiLuu, idKhachHang);
            } catch (Exception e) {
                response.put("trangThai", "loi");
                response.put("message", "Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới.");
                return ResponseEntity.ok(response);
            }
        } else {
            if (hoTenNhan == null || hoTenNhan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Họ và tên người nhận không được để trống.");
                return ResponseEntity.ok(response);
            }
            if (sdtNhan == null || sdtNhan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Số điện thoại không được để trống.");
                return ResponseEntity.ok(response);
            }
            if (diaChiNhan == null || diaChiNhan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Địa chỉ nhận hàng không được để trống.");
                return ResponseEntity.ok(response);
            }
        }

        if (idDonViVanChuyen == null) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng chọn đơn vị vận chuyển.");
            return ResponseEntity.ok(response);
        }
        if (phuongThucThanhToan == null || phuongThucThanhToan.trim().isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng chọn phương thức thanh toán.");
            return ResponseEntity.ok(response);
        }

        try {
            HoaDon hd = gioHangService.createOrder(idNguoiDung, hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyen, phuongThucThanhToan, ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId, idDiaChiLuu);
            response.put("trangThai", "ok");
            response.put("orderId", hd.getId());
            response.put("paymentMethod", hd.getPaymentMethod());
            response.put("tongTien", hd.getTongTien());
            response.put("maDonHang", hd.getMaDonHang());
            response.put("ghnToDistrictId", hd.getGhnToDistrictId());
            response.put("ghnToWardCode", hd.getGhnToWardCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("trangThai", "loi");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
