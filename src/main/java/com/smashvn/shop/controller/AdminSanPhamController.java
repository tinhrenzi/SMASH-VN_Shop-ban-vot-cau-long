package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.AdminSanPhamService;

@Controller
@RequestMapping("/admin/san-pham")
@RequiredArgsConstructor
public class AdminSanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final AdminSanPhamService adminSanPhamService;

    @GetMapping
    public String hienThiDanhSach(Model model) {
        model.addAttribute("danhSachSanPham", sanPhamRepository.findAll());
        return "admin/sanpham-list"; 
    }
    
    @GetMapping("/them")
    public String hienThiFormThem(Model model) {
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
        return "admin/sanpham-add"; 
    }

    @PostMapping("/them")
    public String xuLyThemSanPham(@RequestParam("tenSanPham") String tenSanPham,
                                  @RequestParam("idDanhMuc") Integer idDanhMuc,
                                  @RequestParam("idThuongHieu") Integer idThuongHieu,
                                  @RequestParam("moTa") String moTa) {
        try {
            adminSanPhamService.themSanPhamMoi(tenSanPham, idDanhMuc, idThuongHieu, moTa);
            return "redirect:/admin/san-pham?thanhcong"; 
        } catch (Exception e) {
            return "redirect:/admin/san-pham/them?loi=LoiHeThong";
        }
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
                                 @RequestParam("moTa") String moTa) {
        try {
            adminSanPhamService.capNhatSanPham(idSanPham, tenSanPham, idDanhMuc, idThuongHieu, moTa);
            return "redirect:/admin/san-pham?suaThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham/sua/" + idSanPham + "?loi=LoiHeThong";
        }
    }
    
    @GetMapping("/xoa/{id}")
    public String xuLyXoaSanPham(@PathVariable("id") Integer id) {
        try {
            adminSanPhamService.xoaSanPham(id);
            return "redirect:/admin/san-pham?xoaThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham?loiXoa"; 
        }
    }
}