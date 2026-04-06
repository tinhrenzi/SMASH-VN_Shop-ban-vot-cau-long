package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.GioHangChiTiet;

@Repository
public interface GioHangChiTietRepository extends JpaRepository<GioHangChiTiet, Integer> {
    // Kiểm tra xem sản phẩm này đã có trong giỏ hàng chưa
    GioHangChiTiet findByGioHang_IdAndSanPhamChiTiet_Id(Integer idGioHang, Integer idSanPhamChiTiet);
 // Lấy danh sách sản phẩm trong giỏ hàng dựa vào ID Giỏ hàng
    List<GioHangChiTiet> findByGioHang_Id(Integer idGioHang);
}
