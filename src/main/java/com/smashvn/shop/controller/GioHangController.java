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
import com.smashvn.shop.service.GioHangService;

@Controller
@RequestMapping("/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHangService gioHangService;

    // HÀM 1: THÊM VÀO GIỎ (Dùng cho AJAX)
    @PostMapping("/them")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xuLyThemVaoGio(
            @RequestParam("idSanPhamChiTiet") Integer idSanPhamChiTiet,
            @RequestParam("soLuong") Integer soLuong,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        if (idNguoiDung == null) {
            response.put("trangThai", "chuadangnhap");
            return ResponseEntity.ok(response);
        }

        try {
            // Service xử lý và trả về luôn dữ liệu hiển thị Modal
            Map<String, Object> data = gioHangService.themVaoGio(idNguoiDung, idSanPhamChiTiet, soLuong);
            data.put("trangThai", "ok");
            return ResponseEntity.ok(data);

        } catch (RuntimeException e) {
            response.put("trangThai", "loi");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // HÀM 2: LẤY DỮ LIỆU MINI CART (Dùng cho AJAX Header)
    @GetMapping("/api/mini-cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layDuLieuMiniCart(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        if (idNguoiDung == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("trangThai", "chuadangnhap");
            response.put("tongSoLuong", 0);
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
        if (idNguoiDung == null) return "redirect:/user/dang-nhap";

        List<GioHangChiTiet> danhSachChiTiet = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
        
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

        model.addAttribute("danhSachCart", danhSachChiTiet);
        model.addAttribute("tongTien", tongTien);
        return "cart";
    }

	 // HÀM 4 MỚI: XÓA SẢN PHẨM BẰNG AJAX (Gộp chung cho cả Cart và Mini Cart)
	    @GetMapping("/api/xoa/{id}")
	    @ResponseBody
	    public ResponseEntity<Map<String, String>> xoaSanPhamAjax(@PathVariable("id") Integer idChiTiet, HttpSession session) {
	        Map<String, String> response = new HashMap<>();
	        
	        if (session.getAttribute("idNguoiDung") == null) {
	            response.put("trangThai", "chuadangnhap");
	            return ResponseEntity.ok(response);
	        }
	        
	        try {
	            gioHangService.xoaSanPhamKhoiGio(idChiTiet);
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
    public ResponseEntity<String> capNhatSoLuong(@RequestParam("idChiTiet") Integer idChiTiet,
                                                 @RequestParam("soLuong") Integer soLuong,
                                                 HttpSession session) {
        if (session.getAttribute("idNguoiDung") == null) return ResponseEntity.status(401).body("Chưa đăng nhập");
        gioHangService.capNhatSoLuong(idChiTiet, soLuong);
        return ResponseEntity.ok("ok");
    }
}