package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.service.SanPhamYeuThichService;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class SanPhamYeuThichController {

    private final SanPhamYeuThichService yeuThichService;

    // 1. Hiển thị trang Wishlist
    @GetMapping
    public String hienThiWishlist(HttpSession session, Model model) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) return "redirect:/user/dang-nhap";

        model.addAttribute("listWishlist", yeuThichService.layDanhSachWishlist(idNguoiDung));
        return "wishlist"; 
    }

    // 2. API Thêm vào Wishlist (Dùng cho AJAX ở các nút trái tim)
    @PostMapping("/them")
    @ResponseBody
    public ResponseEntity<String> themVaoWishlist(@RequestParam("idSanPham") Integer idSanPham, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) return ResponseEntity.ok("chuadangnhap");

        String kq = yeuThichService.themVaoWishlist(idNguoiDung, idSanPham);
        return ResponseEntity.ok(kq);
    }

    // 3. Xóa 1 sản phẩm
    @GetMapping("/xoa/{id}")
    public String xoaSanPham(@PathVariable("id") Integer idSanPham, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung != null) {
            yeuThichService.xoaSanPham(idNguoiDung, idSanPham);
        }
        return "redirect:/wishlist";
    }

    // 4. Xóa sạch
    @GetMapping("/xoa-tat-ca")
    public String xoaTatCa(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung != null) {
            yeuThichService.xoaTatCa(idNguoiDung);
        }
        return "redirect:/wishlist";
    }
 // API Xóa sản phẩm yêu thích bằng AJAX (Không reload)
    @GetMapping("/api/xoa/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> xoaSanPhamAjax(@PathVariable("id") Integer idSanPham, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        if (idNguoiDung == null) {
            response.put("trangThai", "chuadangnhap");
            return ResponseEntity.ok(response);
        }
        
        try {
            yeuThichService.xoaSanPham(idNguoiDung, idSanPham);
            response.put("trangThai", "ok");
        } catch (Exception e) {
            response.put("trangThai", "loi");
        }
        return ResponseEntity.ok(response);
    }
}