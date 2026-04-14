package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

@Service
@RequiredArgsConstructor
public class SanPhamYeuThichService {

    private final SanPhamYeuThichRepository yeuThichRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    // 1. Thêm sản phẩm vào Wishlist
    @Transactional
    public String themVaoWishlist(Integer idTaiKhoan, Integer idSanPham) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) return "chuadangnhap";

        // Kiểm tra xem đã có trong Wishlist chưa
        if (yeuThichRepository.existsById_KhachHangIdAndId_SanPhamId(kh.getId(), idSanPham)) {
            return "datontai";
        }

        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // Khởi tạo và lưu khóa phức hợp
        SanPhamYeuThich spy = new SanPhamYeuThich();
        SanPhamYeuThich.SanPhamYeuThichKey key = new SanPhamYeuThich.SanPhamYeuThichKey(kh.getId(), sp.getId());
        spy.setId(key);
        spy.setKhachHang(kh);
        spy.setSanPham(sp);
        
        yeuThichRepository.save(spy);
        return "ok";
    }

    // 2. Lấy danh sách Wishlist của khách hàng
    public List<Map<String, Object>> layDanhSachWishlist(Integer idTaiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) return new ArrayList<>();

        List<SanPhamYeuThich> danhSachGoc = yeuThichRepository.findByKhachHang_Id(kh.getId());
        List<Map<String, Object>> danhSachKetQua = new ArrayList<>();

        for (SanPhamYeuThich spy : danhSachGoc) {
            SanPham sp = spy.getSanPham();
            Map<String, Object> map = new HashMap<>();
            
            map.put("idSanPham", sp.getId());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("tenDanhMuc", sp.getDanhMuc() != null ? sp.getDanhMuc().getTenDanhMuc() : "Chưa phân loại");

            // Lấy danh sách chi tiết của sản phẩm này để trích xuất Giá và Ảnh đại diện
            List<SanPhamChiTiet> listChiTiet = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
            if (!listChiTiet.isEmpty()) {
                map.put("giaBan", listChiTiet.get(0).getGiaBan());
                map.put("hinhAnh", listChiTiet.get(0).getHinhAnhSanPham());
            } else {
                map.put("giaBan", 0);
                map.put("hinhAnh", "default.jpg"); // Tránh lỗi null nếu SP chưa có chi tiết
            }

            danhSachKetQua.add(map);
        }

        return danhSachKetQua;
    }

    // 3. Xóa 1 sản phẩm khỏi Wishlist
    @Transactional
    public void xoaSanPham(Integer idTaiKhoan, Integer idSanPham) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh != null) {
            yeuThichRepository.deleteById_KhachHangIdAndId_SanPhamId(kh.getId(), idSanPham);
        }
    }

    // 4. Xóa sạch Wishlist
    @Transactional
    public void xoaTatCa(Integer idTaiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh != null) {
            yeuThichRepository.deleteByKhachHang_Id(kh.getId());
        }
    }
}