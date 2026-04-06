package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    @GetMapping("/san-pham/{id}")
    public String hienThiChiTietSanPham(@PathVariable("id") Integer id, Model model) {
        
        // 1. Tìm thông tin gốc của sản phẩm (Tên, Mô tả, Hãng)
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này!"));
        
        // 2. Tìm tất cả các phân loại/biến thể của sản phẩm đó (Giá bán, Màu, Trọng lượng)
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findBySanPham_Id(id);
        
        // Lấy đại 1 hình ảnh của biến thể đầu tiên làm ảnh đại diện to
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhSanPham();

        // 3. Đẩy dữ liệu sang giao diện
        model.addAttribute("sp", sanPham);
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        
        return "product-detail"; // Sẽ gọi file product-detail.html
    }
}
