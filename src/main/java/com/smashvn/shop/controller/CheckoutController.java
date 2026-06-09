package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.GioHangService;
import com.smashvn.shop.service.UserAddressService;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final GioHangService gioHangService;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final UserAddressService userAddressService;
    private final SepayConfig sepayConfig;
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
            SanPham sp = item.getSanPhamChiTiet().getSanPham();
            int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
            String trangThai = sp.getTrangThai();

            boolean hopLe = tonKho > 0 && (trangThai == null || trangThai.equals("dang_ban"));
            if (hopLe) {
                tongTien = tongTien.add(item.getSanPhamChiTiet().getGiaBan().multiply(new BigDecimal(item.getSoLuong())));
            }
        }

        List<DonViVanChuyen> listDvvc = donViVanChuyenDAO.findAll();

        List<SoDiaChi> listDiaChi = userAddressService.layDanhSachDiaChi(idNguoiDung);
        boolean hasDefaultAddress = listDiaChi.stream().anyMatch(SoDiaChi::isDefaultShipping);

        Map<Integer, Map<String, Object>> addressMap = new java.util.HashMap<>();
        for (SoDiaChi dc : listDiaChi) {
            Map<String, Object> details = new java.util.HashMap<>();
            details.put("hoTen", dc.getHoNguoiNhan() + " " + dc.getTenNguoiNhan());
            details.put("sdt", dc.getSdtNguoiNhan());
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
            @RequestParam("hoTenNhan") String hoTenNhan,
            @RequestParam("sdtNhan") String sdtNhan,
            @RequestParam("diaChiNhan") String diaChiNhan,
            @RequestParam("idDonViVanChuyen") Integer idDonViVanChuyen,
            @RequestParam("phuongThucThanhToan") String phuongThucThanhToan,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            @RequestParam(value = "ghnToDistrictId", required = false) Integer ghnToDistrictId,
            @RequestParam(value = "ghnToWardCode", required = false) String ghnToWardCode,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            response.put("trangThai", "chuadangnhap");
            response.put("message", "Vui lòng đăng nhập để thực hiện thanh toán.");
            return ResponseEntity.ok(response);
        }

        try {
            HoaDon hd = gioHangService.createOrder(idNguoiDung, hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyen, phuongThucThanhToan, ghiChu, ghnToDistrictId, ghnToWardCode);
            response.put("trangThai", "ok");
            response.put("orderId", hd.getId());
            response.put("paymentMethod", hd.getPaymentMethod());
            response.put("tongTien", hd.getTongTien());
            response.put("maDonHang", hd.getMaDonHang());
            response.put("ghnToDistrictId", ghnToDistrictId);
            response.put("ghnToWardCode", ghnToWardCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("trangThai", "loi");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
