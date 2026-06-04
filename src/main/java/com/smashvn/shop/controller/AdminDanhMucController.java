package com.smashvn.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/danh-muc")
@RequiredArgsConstructor
public class AdminDanhMucController {

    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final SanPhamRepository sanPhamRepository;

    @GetMapping
    public String hienThiTrangQuanLy(Model model) {
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
        return "admin/danhmuc-list";
    }

    @PostMapping("/them")
    public String themDanhMuc(@RequestParam("tenDanhMuc") String tenDanhMuc, Model model) {
        try {
            if (tenDanhMuc == null || tenDanhMuc.trim().isEmpty()) {
                throw new RuntimeException("Tên danh mục không được để trống!");
            }
            DanhMuc dm = new DanhMuc();
            dm.setTenDanhMuc(tenDanhMuc.trim());
            danhMucRepository.save(dm);
            return "redirect:/admin/danh-muc?themDanhMucThanhCong";
        } catch (Exception e) {
            model.addAttribute("loiDanhMuc", e.getMessage());
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            return "admin/danhmuc-list";
        }
    }

    @PostMapping("/xoa/{id}")
    public String xoaDanhMuc(@PathVariable("id") Integer id) {
        try {
            if (sanPhamRepository.existsByDanhMucId(id)) {
                throw new RuntimeException("Không thể xóa danh mục này vì đang có sản phẩm thuộc danh mục!");
            }
            danhMucRepository.deleteById(id);
            return "redirect:/admin/danh-muc?xoaDanhMucThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/danh-muc?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/thuong-hieu/them")
    public String themThuongHieu(@RequestParam("tenThuongHieu") String tenThuongHieu, Model model) {
        try {
            if (tenThuongHieu == null || tenThuongHieu.trim().isEmpty()) {
                throw new RuntimeException("Tên hãng vợt không được để trống!");
            }
            ThuongHieu th = new ThuongHieu();
            th.setTenThuongHieu(tenThuongHieu.trim());
            thuongHieuRepository.save(th);
            return "redirect:/admin/danh-muc?themThuongHieuThanhCong";
        } catch (Exception e) {
            model.addAttribute("loiThuongHieu", e.getMessage());
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            return "admin/danhmuc-list";
        }
    }

    @PostMapping("/thuong-hieu/xoa/{id}")
    public String xoaThuongHieu(@PathVariable("id") Integer id) {
        try {
            if (sanPhamRepository.existsByThuongHieuId(id)) {
                throw new RuntimeException("Không thể xóa hãng này vì đang có sản phẩm thuộc hãng!");
            }
            thuongHieuRepository.deleteById(id);
            return "redirect:/admin/danh-muc?xoaThuongHieuThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/danh-muc?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
