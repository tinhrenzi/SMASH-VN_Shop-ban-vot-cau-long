package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.GioHang;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TrangThaiGioHang;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    // Tìm khách hàng dựa vào ID của bảng TaiKhoan
    KhachHang findByTaiKhoan_Id(Integer idTaiKhoan);
}






