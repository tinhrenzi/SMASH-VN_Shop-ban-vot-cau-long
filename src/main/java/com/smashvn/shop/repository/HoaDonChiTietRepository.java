package com.smashvn.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.dto.product.TopProductDTO;
import com.smashvn.shop.entity.HoaDonChiTiet;

public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    List<HoaDonChiTiet> findByHoaDon_Id(Integer idHoaDon);

    @Query("SELECT COALESCE(SUM(hdct.soLuong), 0L) FROM HoaDonChiTiet hdct "
            + "WHERE hdct.hoaDon.trangThaiDonHang IN ('da_giao', 'delivered') AND hdct.hoaDon.ngayTao BETWEEN :start AND :end")
    Long getTotalProductsSold(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.product.TopProductDTO("
            + "hdct.sanPhamChiTiet.sanPham.id, "
            + "hdct.sanPhamChiTiet.sanPham.tenSanPham, "
            + "COALESCE((SELECT MIN(ha.urlHinhAnh) FROM HinhAnhSanPham ha WHERE ha.sanPhamChiTiet.id = MIN(hdct.sanPhamChiTiet.id)), ''), "
            + "hdct.sanPhamChiTiet.sanPham.danhMuc.tenDanhMuc, "
            + "COALESCE(SUM(hdct.soLuong), 0L), "
            + "COALESCE(SUM(hdct.soLuong * hdct.donGia), 0.0)"
            + ") "
            + "FROM HoaDonChiTiet hdct "
            + "WHERE hdct.hoaDon.trangThaiDonHang IN ('da_giao', 'delivered') "
            + "AND hdct.hoaDon.ngayTao BETWEEN :startDate AND :endDate "
            + "GROUP BY hdct.sanPhamChiTiet.sanPham.id, hdct.sanPhamChiTiet.sanPham.tenSanPham, hdct.sanPhamChiTiet.sanPham.danhMuc.tenDanhMuc "
            + "ORDER BY SUM(hdct.soLuong) DESC")
    List<TopProductDTO> findBestSellingProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(hdct) > 0 FROM HoaDonChiTiet hdct "
            + "WHERE hdct.hoaDon.khachHang.taiKhoan.id = :taiKhoanId "
            + "AND hdct.sanPhamChiTiet.sanPham.id = :sanPhamId "
            + "AND LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh')")
    boolean hasPurchasedProduct(@Param("taiKhoanId") Integer taiKhoanId, @Param("sanPhamId") Integer sanPhamId);

    boolean existsBySanPhamChiTiet_Id(Integer idBienThe);

    @Query("SELECT DISTINCT hdct.sanPhamChiTiet.id FROM HoaDonChiTiet hdct "
            + "WHERE hdct.sanPhamChiTiet.sanPham.id = :sanPhamId")
    List<Integer> findOrderedVariantIdsBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
}
