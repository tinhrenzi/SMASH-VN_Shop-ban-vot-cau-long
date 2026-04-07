package com.smashvn.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.SoDiaChi;

@Repository
public interface SoDiaChiRepository extends JpaRepository<SoDiaChi, Integer> {
    
    // Tìm danh sách địa chỉ theo ID khách hàng
    List<SoDiaChi> findByKhachHang_Id(Integer idKhachHang);

    // Tìm danh sách địa chỉ theo ID khách hàng sắp xếp theo mặc định giao hàng trước
    @Query("SELECT sd FROM SoDiaChi sd WHERE sd.khachHang.id = :idKhachHang " +
           "ORDER BY sd.defaultShipping DESC, sd.id ASC")
    List<SoDiaChi> findByKhachHang_IdOrderByDefault(@Param("idKhachHang") Integer idKhachHang);

    // Tìm địa chỉ mặc định giao hàng của khách hàng
    Optional<SoDiaChi> findByKhachHang_IdAndDefaultShippingTrue(Integer idKhachHang);

    // Tìm địa chỉ mặc định thanh toán của khách hàng
    Optional<SoDiaChi> findByKhachHang_IdAndDefaultBillingTrue(Integer idKhachHang);

    // Tìm địa chỉ theo địa chỉ cụ thể và khách hàng
    Optional<SoDiaChi> findByKhachHang_IdAndDiaChiCuThe(Integer idKhachHang, String diaChiCuThe);

    // Kiểm tra tồn tại địa chỉ mặc định giao hàng
    boolean existsByKhachHang_IdAndDefaultShippingTrue(Integer idKhachHang);

    // Kiểm tra tồn tại địa chỉ mặc định thanh toán
    boolean existsByKhachHang_IdAndDefaultBillingTrue(Integer idKhachHang);

    // Đếm số địa chỉ giao hàng mặc định cho khách hàng
    long countByKhachHang_IdAndDefaultShippingTrue(Integer idKhachHang);

    // Đếm số địa chỉ thanh toán mặc định cho khách hàng
    long countByKhachHang_IdAndDefaultBillingTrue(Integer idKhachHang);
}
