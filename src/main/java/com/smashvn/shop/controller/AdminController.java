package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.dao.PhieuGiamGiaDAO;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final HoaDonRepository hoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final DotGiamGiaDAO dotGiamGiaDAO;
    private final PhieuGiamGiaDAO phieuGiamGiaDAO;

    @GetMapping("/all")
    public String hienThiDashboard(Model model) {
        model.addAttribute("danhSachTaiKhoan", taiKhoanRepository.findAll());
        model.addAttribute("danhSachSanPham", sanPhamRepository.findAll());
        return "admin/admin-dashboard"; 
    }

    @GetMapping("/don-hang")
    public String hienThiDanhSachDonHang(Model model) {
        model.addAttribute("danhSachDonHang", hoaDonRepository.findAll());
        return "admin/donhang-list";
    }

    @GetMapping("/khach-hang")
    public String hienThiDanhSachKhachHang(Model model) {
        model.addAttribute("danhSachKhachHang", khachHangRepository.findAll());
        return "admin/khachhang-list";
    }

    @GetMapping("/khuyen-mai")
    public String hienThiDanhSachKhuyenMai(Model model) {
        model.addAttribute("danhSachDotGiamGia", dotGiamGiaDAO.findAll());
        model.addAttribute("danhSachPhieuGiamGia", phieuGiamGiaDAO.findAll());
        return "admin/khuyenmai-list";
    }
}