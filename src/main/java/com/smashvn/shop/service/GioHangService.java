package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private final KhachHangRepository khachHangRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TrangThaiGioHangRepository trangThaiGioHangRepository;

    public void themVaoGio(Integer idTaiKhoan, Integer idSanPhamChiTiet, Integer soLuong) {
        
        // 1. Tìm Khách Hàng từ ID Tài Khoản (Lấy từ Session đăng nhập)
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            throw new RuntimeException("Tài khoản này chưa được cập nhật thông tin Khách Hàng!");
        }

        // 2. Tìm Giỏ Hàng của Khách Hàng này. Nếu chưa có thì tạo mới.
        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setKhachHang(khachHang);
            gioHang = gioHangRepository.save(gioHang); // Lưu xuống DB để lấy ID
        }

        // 3. Kiểm tra xem Sản phẩm chi tiết này đã tồn tại trong giỏ chưa
        GioHangChiTiet chiTiet = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), idSanPhamChiTiet);

        if (chiTiet != null) {
            // Đã có trong giỏ -> Cộng dồn số lượng
            chiTiet.setSoLuong(chiTiet.getSoLuong() + soLuong);
        } else {
            // Chưa có -> Tạo mới chi tiết giỏ hàng
            chiTiet = new GioHangChiTiet();
            chiTiet.setGioHang(gioHang);
            
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            chiTiet.setSanPhamChiTiet(spct);
            
            chiTiet.setSoLuong(soLuong);
            
            // Lấy trạng thái mặc định (Giả sử ID 1 là "Chưa chọn để thanh toán")
            TrangThaiGioHang trangThai = trangThaiGioHangRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Chưa có trạng thái giỏ hàng trong DB"));
            chiTiet.setTrangThai(trangThai);
        }

        // 4. Lưu lại
        gioHangChiTietRepository.save(chiTiet);
    }
    public List<GioHangChiTiet> layDanhSachSanPhamTrongGio(Integer idTaiKhoan) {
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) return new ArrayList<>(); // Trả về list rỗng nếu chưa có thông tin KH

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) return new ArrayList<>(); // Trả về list rỗng nếu giỏ hàng trống

        return gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
    }
    public void xoaSanPhamKhoiGio(Integer idGioHangChiTiet) {
        // Xóa chi tiết giỏ hàng dựa theo ID của nó
        gioHangChiTietRepository.deleteById(idGioHangChiTiet);
    }
 // Cập nhật số lượng của một dòng sản phẩm trong giỏ
    public void capNhatSoLuong(Integer idGioHangChiTiet, Integer soLuongMoi) {
        GioHangChiTiet chiTiet = gioHangChiTietRepository.findById(idGioHangChiTiet)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));
        
        // Nếu khách nhập số lượng > 0 thì cập nhật, nếu <= 0 thì coi như xóa luôn
        if (soLuongMoi > 0) {
            chiTiet.setSoLuong(soLuongMoi);
            gioHangChiTietRepository.save(chiTiet);
        } else {
            gioHangChiTietRepository.deleteById(idGioHangChiTiet);
        }
    }
}
