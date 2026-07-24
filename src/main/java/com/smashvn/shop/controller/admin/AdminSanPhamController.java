package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminSanPhamService;

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

    // Các thuộc tính thuộc phân loại vợt cầu lông
    private final List<String> listMauSacConfig = List.of("Đỏ", "Xanh dương", "Đen", "Trắng", "Vàng", "Cam");
    private final List<String> listTrongLuongConfig = List.of("3U", "4U", "5U");

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
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());

        java.util.Map<String, Integer> categoryIds = java.util.Map.of(
            "VOT", com.smashvn.shop.constant.DanhMucIds.VOT,
            "GIAY", com.smashvn.shop.constant.DanhMucIds.GIAY,
            "HOP_CAU", com.smashvn.shop.constant.DanhMucIds.HOP_CAU,
            "CUOC", com.smashvn.shop.constant.DanhMucIds.CUOC,
            "BALO", com.smashvn.shop.constant.DanhMucIds.BALO,
            "TRANG_PHUC", com.smashvn.shop.constant.DanhMucIds.TRANG_PHUC,
            "QUAN_CAN", com.smashvn.shop.constant.DanhMucIds.QUAN_CAN,
            "BANG_QUAN", com.smashvn.shop.constant.DanhMucIds.BANG_QUAN
        );
        model.addAttribute("categoryIds", categoryIds);

        model.addAttribute("listMauSac", com.smashvn.shop.constant.SanPhamAttributeConfig.DEFAULT_MAU_SAC);
        model.addAttribute("listTrongLuong", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_TRONG_LUONG_VOT);
        model.addAttribute("listKichThuocGiay", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_KICH_THUOC_GIAY);
        model.addAttribute("listKichThuocTrangPhuc", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_KICH_THUOC_TRANG_PHUC);
    }

    @GetMapping("/sua/{id}")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model) {
        SanPham sp = sanPhamRepository.findById(id).orElseThrow();
        model.addAttribute("sp", sp);
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
        return "admin/sanpham-edit";
    }

    @PostMapping("/sua/{id}")
    public String xuLySuaSanPham(@PathVariable("id") Integer idSanPham,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("idDanhMuc") Integer idDanhMuc,
            @RequestParam("idThuongHieu") Integer idThuongHieu,
            @RequestParam("moTa") String moTa,
            HttpSession session,
            HttpServletRequest request) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.capNhatSanPham(idSanPham, tenSanPham, idDanhMuc, idThuongHieu, moTa, idNguoiDung, request.getRemoteAddr());
            return "redirect:/admin/san-pham?suaThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham/sua/" + idSanPham + "?loi=LoiHeThong";
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
