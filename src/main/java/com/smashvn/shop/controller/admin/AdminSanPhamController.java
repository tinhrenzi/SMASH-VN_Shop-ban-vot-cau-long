package com.smashvn.shop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminSanPhamService;
import com.smashvn.shop.service.admin.AdminBienTheService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/san-pham")
@RequiredArgsConstructor
public class AdminSanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final AdminSanPhamService adminSanPhamService;
    private final AdminBienTheService adminBienTheService;

    @GetMapping
    public String hienThiDanhSach(Model model) {
        model.addAttribute("danhSachSanPham", sanPhamRepository.findAll());
        return "admin/sanpham-list";
    }

    @GetMapping("/them")
    public String hienThiFormThem(Model model) {
        populateFormModel(model);
        return "admin/sanpham-add";
    }

    @PostMapping("/them")
    public String xuLyThemSanPham(
            @org.springframework.web.bind.annotation.ModelAttribute SanPhamCreateRequest requestDto,
            HttpServletRequest request,
            HttpSession session,
            Model model) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.themSanPhamVaBienThe(
                    requestDto,
                    idNguoiDung,
                    request.getRemoteAddr()
            );
            return "redirect:/admin/san-pham?thanhcong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("requestDto", requestDto);
            populateFormModel(model);
            return "admin/sanpham-add";
        }
    }

    private void populateFormModel(Model model) {
        List<com.smashvn.shop.entity.DanhMuc> activeCategories = danhMucRepository.findByTrangThaiTrue();
        model.addAttribute("listDanhMuc", activeCategories);
        model.addAttribute("listThuongHieu", thuongHieuRepository.findByTrangThaiTrue());

        java.util.Map<String, Integer> categoryIds = new java.util.HashMap<>();
        java.util.Map<Integer, String> categoryTypes = new java.util.HashMap<>();
        for (com.smashvn.shop.entity.DanhMuc dm : activeCategories) {
            com.smashvn.shop.constant.CategoryType type = com.smashvn.shop.constant.CategoryType.fromIdOrName(dm, dm.getId());
            categoryTypes.put(dm.getId(), type.name());
            if (type != com.smashvn.shop.constant.CategoryType.OTHER) {
                categoryIds.put(type.name(), dm.getId());
            }
        }
        model.addAttribute("categoryIds", categoryIds);
        model.addAttribute("categoryTypes", categoryTypes);

        model.addAttribute("listMauSac", com.smashvn.shop.constant.SanPhamAttributeConfig.DEFAULT_MAU_SAC);
        model.addAttribute("listTrongLuong", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_TRONG_LUONG_VOT);
        model.addAttribute("listKichThuocGiay", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_KICH_THUOC_GIAY);
        model.addAttribute("listKichThuocTrangPhuc", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_KICH_THUOC_TRANG_PHUC);
    }

    @GetMapping("/sua/{id}")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model) {
        SanPham sp = sanPhamRepository.findById(id).orElseThrow();
        model.addAttribute("sp", sp);

        List<com.smashvn.shop.entity.DanhMuc> activeCategories = danhMucRepository.findByTrangThaiTrue();
        if (sp.getDanhMuc() != null && Boolean.FALSE.equals(sp.getDanhMuc().getTrangThai())) {
            if (!activeCategories.contains(sp.getDanhMuc())) {
                activeCategories.add(sp.getDanhMuc());
            }
        }
        model.addAttribute("listDanhMuc", activeCategories);

        List<com.smashvn.shop.entity.ThuongHieu> activeBrands = thuongHieuRepository.findByTrangThaiTrue();
        if (sp.getThuongHieu() != null && Boolean.FALSE.equals(sp.getThuongHieu().getTrangThai())) {
            if (!activeBrands.contains(sp.getThuongHieu())) {
                activeBrands.add(sp.getThuongHieu());
            }
        }
        model.addAttribute("listThuongHieu", activeBrands);

        // Tải thêm danh sách biến thể và categoryIds cho phần quản lý biến thể tích hợp
        model.addAttribute("danhSachBienThe", adminBienTheService.layDanhSachBienThe(id));
        java.util.Map<String, Integer> categoryIds = new java.util.HashMap<>();
        for (com.smashvn.shop.entity.DanhMuc dm : danhMucRepository.findAll()) {
            com.smashvn.shop.constant.CategoryType type = com.smashvn.shop.constant.CategoryType.fromIdOrName(dm, dm.getId());
            if (type != com.smashvn.shop.constant.CategoryType.OTHER) {
                categoryIds.put(type.name(), dm.getId());
            }
        }
        model.addAttribute("categoryIds", categoryIds);

        return "admin/sanpham-edit";
    }

    @PostMapping("/sua/{id}")
    public String xuLySuaSanPham(@PathVariable("id") Integer idSanPham,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("idDanhMuc") Integer idDanhMuc,
            @RequestParam("idThuongHieu") Integer idThuongHieu,
            @RequestParam("moTa") String moTa,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.capNhatSanPham(idSanPham, tenSanPham, idDanhMuc, idThuongHieu, moTa, idNguoiDung, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin sản phẩm thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật: " + e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    @PostMapping("/xoa/{id}")
    public String xuLyXoaSanPham(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.xoaSanPham(id, idNguoiDung, request.getRemoteAddr());
            return "redirect:/admin/san-pham?xoaThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham?loiXoa";
        }
    }

    @PostMapping("/mo-ban-lai/{id}")
    public String xuLyMoBanLaiSanPham(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.moBanLaiSanPham(id, idNguoiDung, request.getRemoteAddr());
            return "redirect:/admin/san-pham?moBanLaiThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham?loiMoBanLai";
        }
    }
}
