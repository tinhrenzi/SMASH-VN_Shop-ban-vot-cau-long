package com.smashvn.shop.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.dto.GeneralMetricsDTO;
import com.smashvn.shop.dto.ChartPointDTO;
import com.smashvn.shop.dto.OrderStatusCountDTO;

public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    List<HoaDon> findByKhachHang_Id(Integer idKhachHang);

    @Query("SELECT new com.smashvn.shop.dto.GeneralMetricsDTO(" +
           "COUNT(hd.id), " +
           "COALESCE(SUM(CASE WHEN hd.trangThaiDonHang IN ('da_giao', 'delivered') THEN 1L ELSE 0L END), 0L), " +
           "COALESCE(SUM(CASE WHEN hd.trangThaiDonHang IN ('da_huy', 'cancelled') THEN 1L ELSE 0L END), 0L), " +
           "COALESCE(SUM(CASE WHEN hd.trangThaiDonHang IN ('da_giao', 'delivered') THEN hd.tongTien ELSE 0.0 END), 0.0), " +
           "COALESCE(AVG(CASE WHEN hd.trangThaiDonHang IN ('da_giao', 'delivered') THEN hd.tongTien ELSE NULL END), 0.0), " +
           "0L" +
           ") FROM HoaDon hd WHERE hd.ngayTao BETWEEN :start AND :end")
    GeneralMetricsDTO getGeneralMetricsWithoutProductCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(HOUR(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE hd.trangThaiDonHang IN ('da_giao', 'delivered') AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY HOUR(hd.ngayTao) ORDER BY HOUR(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE hd.trangThaiDonHang IN ('da_giao', 'delivered') AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao) ORDER BY YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(YEAR(hd.ngayTao), MONTH(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE hd.trangThaiDonHang IN ('da_giao', 'delivered') AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY YEAR(hd.ngayTao), MONTH(hd.ngayTao) ORDER BY YEAR(hd.ngayTao), MONTH(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(kh.id) FROM KhachHang kh " +
           "WHERE (SELECT MIN(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang.id = kh.id AND hd.trangThaiDonHang IN ('da_giao', 'delivered')) BETWEEN :start AND :end")
    Long countNewCustomers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.OrderStatusCountDTO(hd.trangThaiDonHang, COUNT(hd.id)) " +
           "FROM HoaDon hd " +
           "WHERE hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY hd.trangThaiDonHang")
    List<OrderStatusCountDTO> getOrderStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
