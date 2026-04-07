package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.KhachHang;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    // Tìm khách hàng dựa vào ID của bảng TaiKhoan
    KhachHang findByTaiKhoan_Id(Integer idTaiKhoan);

    // Tìm khách hàng theo họ
    List<KhachHang> findByHoKhContainingIgnoreCase(String hoKh);

    // Tìm khách hàng theo tên
    List<KhachHang> findByTenKhContainingIgnoreCase(String tenKh);

    // Tìm khách hàng theo số điện thoại
    KhachHang findBySoDienThoaiKh(String soDienThoaiKh);

    // Tìm khách hàng kết hợp cả họ và tên (LIKE trên cả hai trường)
    @Query("SELECT kh FROM KhachHang kh WHERE " +
           "CONCAT(kh.hoKh, ' ', kh.tenKh) LIKE CONCAT('%', :fullName, '%') " +
           "OR kh.hoKh LIKE CONCAT('%', :fullName, '%') " +
           "OR kh.tenKh LIKE CONCAT('%', :fullName, '%')")
    List<KhachHang> findByFullName(@Param("fullName") String fullName);

    // Tìm khách hàng đã đăng ký nhận bản tin
    List<KhachHang> findByNhanBanTinIsTrue();
}






