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
	
	@GetMapping("/")
    public String hienThiTrangChu(Model model) {
        // Lấy danh sách sản phẩm từ Database
        List<SanPhamChiTiet> danhSachSanPham = sanPhamChiTietRepository.findTop8ByOrderByIdDesc();
        
        // Truyền biến "products" sang giao diện Thymeleaf
        model.addAttribute("products", danhSachSanPham);
        
        // Trả về file index.html
        return "index"; 
    }
}
