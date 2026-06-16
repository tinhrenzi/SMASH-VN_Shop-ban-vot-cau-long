package com.smashvn.shop.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.DanhGia;

public interface DanhGiaDAO extends JpaRepository<DanhGia, Integer> {
    
    // Tìm các đánh giá chưa bị xóa của một sản phẩm, sắp xếp theo thời gian mới nhất (cho frontend)
    List<DanhGia> findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(Integer sanPhamId);
    
    // Tìm tất cả đánh giá sắp xếp theo thời gian mới nhất (cho admin)
    List<DanhGia> findAllByOrderByNgayDanhGiaDesc();
    
    // Tìm đánh giá cũ của khách hàng cho một sản phẩm để chỉnh sửa/ghi đè (chỉ lấy đánh giá chưa bị xóa)
    Optional<DanhGia> findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(Integer khachHangId, Integer sanPhamId);
    
    // Tìm đánh giá mới nhất của khách hàng theo tài khoản (phục vụ rate limit 30s)
    Optional<DanhGia> findTopByKhachHang_TaiKhoan_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(Integer taiKhoanId);
    
    // Đếm số lượng đánh giá của khách hàng trong khoảng thời gian nhất định (phục vụ rate limit 5 reviews/giờ)
    long countByKhachHang_TaiKhoan_IdAndDaXoaFalseAndNgayDanhGiaAfter(Integer taiKhoanId, LocalDateTime time);
}
