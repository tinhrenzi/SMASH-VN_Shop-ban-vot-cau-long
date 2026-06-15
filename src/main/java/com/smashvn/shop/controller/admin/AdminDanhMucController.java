package com.smashvn.shop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.product.DanhMucService;
import com.smashvn.shop.service.product.ThuongHieuService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/danh-muc")
@RequiredArgsConstructor
public class AdminDanhMucController {

    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final SanPhamRepository sanPhamRepository;
    private final DanhMucService danhMucService;
    private final ThuongHieuService thuongHieuService;

    // ----------------------------------------------------------------
    // GET: list page
    // ----------------------------------------------------------------

    @GetMapping
    public String hienThiTrangQuanLy(Model model) {
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
        return "admin/danhmuc-list";
    }

    // ----------------------------------------------------------------
    // CATEGORY endpoints
    // ----------------------------------------------------------------

    @PostMapping("/them")
    public String themDanhMuc(
            @RequestParam(value = "tenDanhMuc", required = false) String tenDanhMuc,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            danhMucService.themDanhMuc(tenDanhMuc);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục mới thành công!");
            return "redirect:/admin/danh-muc";
        } catch (IllegalArgumentException e) {
            model.addAttribute("loiDanhMuc", e.getMessage());
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            return "admin/danhmuc-list";
        }
    }

    @PostMapping("/sua/{id}")
    public String suaDanhMuc(
            @PathVariable("id") Integer id,
            @RequestParam(value = "tenDanhMuc", required = false) String tenDanhMuc,
            RedirectAttributes redirectAttributes) {
        try {
            danhMucService.suaDanhMuc(id, tenDanhMuc);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/xoa/{id}")
    public String xoaDanhMuc(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            if (sanPhamRepository.existsByDanhMucId(id)) {
                throw new IllegalArgumentException(
                        "Không thể xóa danh mục này vì đang có sản phẩm thuộc danh mục!");
            }
            danhMucRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    // ----------------------------------------------------------------
    // BRAND (ThuongHieu) endpoints
    // ----------------------------------------------------------------

    @PostMapping("/thuong-hieu/them")
    public String themThuongHieu(
            @RequestParam(value = "tenThuongHieu", required = false) String tenThuongHieu,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            thuongHieuService.themThuongHieu(tenThuongHieu);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thương hiệu mới thành công!");
            return "redirect:/admin/danh-muc";
        } catch (IllegalArgumentException e) {
            model.addAttribute("loiThuongHieu", e.getMessage());
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            return "admin/danhmuc-list";
        }
    }

    @PostMapping("/thuong-hieu/sua/{id}")
    public String suaThuongHieu(
            @PathVariable("id") Integer id,
            @RequestParam(value = "tenThuongHieu", required = false) String tenThuongHieu,
            RedirectAttributes redirectAttributes) {
        try {
            thuongHieuService.suaThuongHieu(id, tenThuongHieu);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thương hiệu thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/thuong-hieu/xoa/{id}")
    public String xoaThuongHieu(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            if (sanPhamRepository.existsByThuongHieuId(id)) {
                throw new IllegalArgumentException(
                        "Không thể xóa thương hiệu này vì đang có sản phẩm thuộc thương hiệu!");
            }
            thuongHieuRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa thương hiệu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }
}


