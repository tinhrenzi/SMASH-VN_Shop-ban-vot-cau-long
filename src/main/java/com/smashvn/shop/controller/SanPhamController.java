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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    @GetMapping("/san-pham/{id}")
    public String hienThiChiTietSanPham(@PathVariable("id") Integer id, Model model) {
        
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này!"));
        
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findBySanPham_Id(id);
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhSanPham();

        // 1. Lấy danh sách Màu Sắc không trùng lặp
        java.util.Set<String> listMauSac = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMauSac)
                .collect(java.util.stream.Collectors.toSet());
        
        // 2. Lấy danh sách Size (Trọng lượng) không trùng lặp
        java.util.Set<String> listKichThuoc = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getTrongLuong)
                .collect(java.util.stream.Collectors.toSet());

        // --- CODE FIX LỖI BẮT ĐẦU TỪ ĐÂY ---
        // 3. Tạo một list "gọn nhẹ" (Map) chỉ chứa đúng các thông tin JS cần thiết để tránh lỗi đệ quy
        List<Map<String, Object>> listBienTheJS = danhSachChiTiet.stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            map.put("mauSac", ct.getMauSac());
            map.put("trongLuong", ct.getTrongLuong());
            map.put("giaBan", ct.getGiaBan());
            map.put("soLuongTon", ct.getSoLuongTon());
            map.put("hinhAnhSanPham", ct.getHinhAnhSanPham());
            return map;
        }).collect(Collectors.toList());
        // --- KẾT THÚC CODE FIX LỖI ---

        model.addAttribute("listMauSac", listMauSac);
        model.addAttribute("listKichThuoc", listKichThuoc);
        model.addAttribute("sp", sanPham);
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        
        // Ném list đã tối ưu này sang giao diện cho JS dùng
        model.addAttribute("listBienTheJS", listBienTheJS); 
        
        return "product-detail"; 
    }
 // Trả về một đoạn HTML (fragment) thay vì trả về toàn bộ trang web
    @GetMapping("/modal/quick-look/{id}")
    public String hienThiQuickLookModal(@PathVariable("id") Integer id, Model model) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
                
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findBySanPham_Id(id);
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhSanPham();

        // THÊM 3 DÒNG NÀY ĐỂ MODAL CÓ DỮ LIỆU BIẾN THỂ
        java.util.Set<String> listMauSac = danhSachChiTiet.stream().map(SanPhamChiTiet::getMauSac).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> listKichThuoc = danhSachChiTiet.stream().map(SanPhamChiTiet::getTrongLuong).collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> listBienTheJS = danhSachChiTiet.stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            map.put("mauSac", ct.getMauSac());
            map.put("trongLuong", ct.getTrongLuong());
            map.put("giaBan", ct.getGiaBan());
            map.put("soLuongTon", ct.getSoLuongTon());
            map.put("hinhAnhSanPham", ct.getHinhAnhSanPham());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        // Đổ dữ liệu vào Model (Đổi 'sp' thành 'spQuickLook' để tránh xung đột với trang chi tiết)
        model.addAttribute("spQuickLook", sanPham); 
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        model.addAttribute("listMauSac", listMauSac);
        model.addAttribute("listKichThuoc", listKichThuoc);
        model.addAttribute("listBienTheJS", listBienTheJS);
        
        return "/layout/modals :: quick-look-fragment"; 
    }
}