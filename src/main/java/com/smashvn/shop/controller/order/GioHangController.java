package com.smashvn.shop.controller.order;

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
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.repository.SanPhamChiTietRepository;

@Controller
@RequestMapping("/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHangService gioHangService;
    private final PricingService pricingService;
    private final GuestCartService guestCartService;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    // HÀM 1: THÊM VÀO GIỎ (Dùng cho AJAX)
    @PostMapping("/them")
    @ResponseBody
    public ResponseEntity<?> xuLyThemVaoGio(
            @RequestParam(value = "idSanPhamChiTiet", required = false) Integer idSanPhamChiTiet,
            @RequestParam(value = "soLuong", required = false) Integer soLuong,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");

        if (soLuong == null) {
            return ResponseEntity.status(400).body("Số lượng sản phẩm không được để trống.");
        }
        if (idSanPhamChiTiet == null) {
            return ResponseEntity.status(400).body("Sản phẩm không hợp lệ.");
        }
        
        if (idNguoiDung == null) {
            try {
                guestCartService.addToGuestCart(session, idSanPhamChiTiet, soLuong);
                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet)
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
                Map<String, Object> data = new HashMap<>();
                data.put("trangThai", "ok");
                data.put("tenSanPham", spct.getSanPham().getTenSanPham());
                data.put("phanLoai", spct.getMauSac() + " | " + spct.getTrongLuong());
                data.put("giaBan", pricingService.calculateCurrentSellingPrice(spct));
                data.put("hinhAnh", spct.getHinhAnhSanPham());
                data.put("soLuongThem", soLuong);
                return ResponseEntity.ok(data);
            } catch (RuntimeException e) {
                return ResponseEntity.status(400).body(e.getMessage());
            }
        }

        try {
            // Service xử lý và trả về luôn dữ liệu hiển thị Modal
            Map<String, Object> data = gioHangService.themVaoGio(idNguoiDung, idSanPhamChiTiet, soLuong);
            data.put("trangThai", "ok");
            return ResponseEntity.ok(data);

        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // HÀM 2: LẤY DỮ LIỆU MINI CART (Dùng cho AJAX Header)
    @GetMapping("/api/mini-cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layDuLieuMiniCart(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        if (idNguoiDung == null) {
            Map<String, Object> response = guestCartService.layDuLieuMiniCart(session);
            return ResponseEntity.ok(response);
        }

        // Controller chỉ việc gọi 1 dòng duy nhất!
        Map<String, Object> response = gioHangService.layDuLieuMiniCart(idNguoiDung);
        return ResponseEntity.ok(response);
    }

    // HÀM 3: HIỂN THỊ TRANG GIỎ HÀNG (cart.html)
    @GetMapping
    public String hienThiGioHang(HttpSession session, Model model) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        List<GioHangChiTiet> danhSachChiTiet = new java.util.ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;

        if (idNguoiDung == null) {
            List<com.smashvn.shop.service.order.GuestCartService.GuestCartItem> guestItems = guestCartService.getGuestCartItems(session);
            for (com.smashvn.shop.service.order.GuestCartService.GuestCartItem item : guestItems) {
                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(item.getIdSanPhamChiTiet()).orElse(null);
                if (spct == null) continue;

                GioHangChiTiet detail = new GioHangChiTiet();
                detail.setId(spct.getId()); // Sử dụng ID SPCT làm ID chi tiết tạm thời
                detail.setSanPhamChiTiet(spct);
                detail.setSoLuong(item.getSoLuong());

                SanPham sp = spct.getSanPham();
                int tonKho = spct.getSoLuongTon();
                String trangThai = sp.getTrangThai();

                boolean hopLe = tonKho > 0 && (trangThai == null || trangThai.equals("dang_ban")) && item.getSoLuong() != null && item.getSoLuong() > 0;
                if (hopLe) {
                    BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(spct);
                    tongTien = tongTien.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
                }
                danhSachChiTiet.add(detail);
            }
        } else {
            danhSachChiTiet = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
            for (GioHangChiTiet item : danhSachChiTiet) {
                SanPham sp = item.getSanPhamChiTiet().getSanPham();
                int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
                String trangThai = sp.getTrangThai();

                boolean hopLe = tonKho > 0 && (trangThai == null || trangThai.equals("dang_ban")) && item.getSoLuong() != null && item.getSoLuong() > 0;
                if (hopLe) {
                    BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(item.getSanPhamChiTiet());
                    tongTien = tongTien.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
                }
            }
        }

        model.addAttribute("danhSachCart", danhSachChiTiet);
        model.addAttribute("tongTien", tongTien);
        return "cart";
    }

	 // HÀM 4 MỚI: XÓA SẢN PHẨM BẰNG AJAX (Chuyển sang POST để chống CSRF)
	    @PostMapping("/api/xoa/{id}")
	    @ResponseBody
	    public ResponseEntity<Map<String, String>> xoaSanPhamAjax(@PathVariable("id") Integer idChiTiet, HttpSession session) {
	        Map<String, String> response = new HashMap<>();
	        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
	        
	        try {
	            if (idNguoiDung == null) {
	                guestCartService.removeFromGuestCart(session, idChiTiet);
	            } else {
	                gioHangService.xoaSanPhamKhoiGio(idChiTiet, idNguoiDung);
	            }
	            response.put("trangThai", "ok");
	        } catch (Exception e) {
	            response.put("trangThai", "loi");
	            response.put("message", e.getMessage());
	        }
	        return ResponseEntity.ok(response);
	    }

    // HÀM 5: CẬP NHẬT SỐ LƯỢNG (Dùng cho AJAX trong cart.html)
    @PostMapping("/cap-nhat")
    @ResponseBody
    public ResponseEntity<String> capNhatSoLuong(@RequestParam(value = "idChiTiet", required = false) Integer idChiTiet,
                                                 @RequestParam(value = "soLuong", required = false) Integer soLuong,
                                                 HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        if (soLuong == null) {
            return ResponseEntity.status(400).body("Số lượng sản phẩm không được để trống.");
        }
        if (idChiTiet == null) {
            return ResponseEntity.status(400).body("Chi tiết giỏ hàng không hợp lệ.");
        }

        try {
            if (idNguoiDung == null) {
                guestCartService.updateGuestCartQuantity(session, idChiTiet, soLuong);
            } else {
                gioHangService.capNhatSoLuong(idChiTiet, soLuong, idNguoiDung);
            }
            return ResponseEntity.ok("ok");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}