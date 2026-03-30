package com.smashvn.shop.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ThuongHieuRepository thuongHieuRepository;

    @GetMapping("/")
    public String hienThiTrangChu(Model model) {
        // 1. Lấy 8 sản phẩm mới nhất (hoặc bạn có thể dùng findAll() nếu muốn lấy hết)
        List<SanPhamChiTiet> danhSachSanPham = sanPhamChiTietRepository.findTop8ByOrderByIdDesc();
        
        // 2. Lấy danh sách thương hiệu
        List<ThuongHieu> danhSachThuongHieu = thuongHieuRepository.findAll();
        
        // 3. Đẩy sang Thymeleaf
        model.addAttribute("products", danhSachSanPham);
        model.addAttribute("brands", danhSachThuongHieu);
        
        return "index"; 
    }
}
