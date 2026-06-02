package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.smashvn.shop.entity.NhanVien;

public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    @Query("SELECT nv FROM NhanVien nv WHERE nv.hoTenNv LIKE %:keyword% OR nv.taiKhoan.email LIKE %:keyword%")
    List<NhanVien> searchNhanVien(@Param("keyword") String keyword);

    NhanVien findByTaiKhoanId(Integer idTaiKhoan);

    @Query("SELECT nv FROM NhanVien nv WHERE nv.taiKhoan.trangThai = 'cho_khoa'")
    List<NhanVien> findPendingLockEmployees();
}
