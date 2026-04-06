package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final SanPhamRepository sanPhamRepository;

    @GetMapping("/all")
    public String hienThiDashboard(Model model) {
        // Dùng hàm findAll() có sẵn của Spring Data JPA để lấy sạch dữ liệu trong bảng
        model.addAttribute("danhSachTaiKhoan", taiKhoanRepository.findAll());
        model.addAttribute("danhSachSanPham", sanPhamRepository.findAll());
        
        return "/admin/admin-dashboard"; // Trả về file admin-dashboard.html
    }
}