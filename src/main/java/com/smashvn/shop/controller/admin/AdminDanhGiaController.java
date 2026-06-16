package com.smashvn.shop.controller.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.service.product.DanhGiaService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminDanhGiaController {

    private final DanhGiaService danhGiaService;

    // Xem danh sách đánh giá của hệ thống
    @GetMapping("/admin/danh-gia")
    public String hienThiDanhSachDanhGia(Model model, HttpSession session) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        List<DanhGia> listDanhGia = danhGiaService.layTatCaDanhGia();
        model.addAttribute("listDanhGia", listDanhGia);
        return "admin/danhgia-list";
    }

    // Ẩn bình luận (văn bản)
    @PostMapping("/admin/danh-gia/an-binh-luan/{id}")
    public String anBinhLuan(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.anBinhLuan(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã ẩn nội dung bình luận thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Hiển thị lại bình luận
    @PostMapping("/admin/danh-gia/hien-binh-luan/{id}")
    public String hienBinhLuan(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.hienBinhLuan(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã phục hồi hiển thị bình luận thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Ẩn hình ảnh
    @PostMapping("/admin/danh-gia/an-hinh-anh/{id}")
    public String anHinhAnh(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.anHinhAnh(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã ẩn hình ảnh đánh giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Hiển thị lại hình ảnh
    @PostMapping("/admin/danh-gia/hien-hinh-anh/{id}")
    public String hienHinhAnh(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.hienHinhAnh(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã phục hồi hiển thị hình ảnh thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Xóa mềm đánh giá
    @PostMapping("/admin/danh-gia/xoa/{id}")
    public String xoaMemDanhGia(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.xoaMemDanhGia(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa mềm đánh giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }
}
