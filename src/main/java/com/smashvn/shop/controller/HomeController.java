package com.smashvn.shop.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SanPhamRepository sanPhamRepository;
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

    @GetMapping({
        "/shop-side-version-2.html", 
        "/index.html", 
        "/product-detail.html",
        "/blog-left-sidebar.html",
        "/blog-right-sidebar.html",
        "/blog-sidebar-none.html",
        "/blog-masonry.html",
        "/blog-detail.html"
    })
    public String redirectLegacyTemplates() {
        return "redirect:/";
    }
}
