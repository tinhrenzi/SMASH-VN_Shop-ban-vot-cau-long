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
	
	private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ThuongHieuRepository thuongHieuRepository;

    @GetMapping("/")
    public String hienThiTrangChu(Model model) {
        // Lấy danh sách sản phẩm gốc (SanPham) thay vì biến thể
        List<SanPham> danhSachSanPham = sanPhamRepository.findAll(); 
        
        List<ThuongHieu> danhSachThuongHieu = thuongHieuRepository.findAll();
        
        model.addAttribute("products", danhSachSanPham);
        model.addAttribute("brands", danhSachThuongHieu);
        
        return "index"; 
    }
}
