package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

@Service
@RequiredArgsConstructor
public class AdminSanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final NhanVienRepository nhanVienRepository;

    // --- HÀM THÊM MỚI (CHỈ LƯU SẢN PHẨM GỐC) ---
    @Transactional
    public void themSanPhamMoi(String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        SanPham sp = new SanPham();
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sp.setTrangThai("dang_ban"); 

        List<NhanVien> listNV = nhanVienRepository.findAll();
        if (!listNV.isEmpty()) sp.setNhanVien(listNV.get(0));
        
        sanPhamRepository.save(sp);
    }

    // --- HÀM CẬP NHẬT (CHỈ SỬA SẢN PHẨM GỐC) ---
    @Transactional
    public void capNhatSanPham(Integer idSanPham, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sanPhamRepository.save(sp);
    }

    // --- HÀM XÓA MỀM ---
    @Transactional
    public void xoaSanPham(Integer idSanPham) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        sp.setTrangThai("ngung_kinh_doanh");
        sanPhamRepository.save(sp);
    }
}