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

    java.util.Optional<HoaDon> findByAppTransId(String appTransId);

    @Query("SELECT hd FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.paymentStatus = 'PENDING' AND hd.ngayTao < :cutoff")
    List<HoaDon> findPendingZaloPayOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.ngayTao BETWEEN :start AND :end")
    Long countZaloPayTransactions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end")
    Long countZaloPaySuccessful(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.paymentStatus = 'FAILED' AND hd.ngayTao BETWEEN :start AND :end")
    Long countZaloPayFailed(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.paymentStatus = 'PENDING' AND hd.ngayTao BETWEEN :start AND :end")
    Long countZaloPayPending(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(hd.tongTien), 0.0) FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end")
    java.math.BigDecimal sumZaloPayRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.GeneralMetricsDTO(" +
           "COUNT(hd.id), " +
           "COALESCE(SUM(CASE WHEN hd.paymentStatus = 'PAID' THEN 1L ELSE 0L END), 0L), " +
           "COALESCE(SUM(CASE WHEN hd.paymentStatus = 'CANCELLED' OR hd.paymentStatus = 'FAILED' OR hd.trangThaiDonHang = 'da_huy' THEN 1L ELSE 0L END), 0L), " +
           "COALESCE(SUM(CASE WHEN hd.paymentStatus = 'PAID' THEN hd.tongTien ELSE 0.0 END), 0.0), " +
           "COALESCE(AVG(CASE WHEN hd.paymentStatus = 'PAID' THEN hd.tongTien ELSE NULL END), 0.0), " +
           "0L" +
           ") FROM HoaDon hd WHERE hd.ngayTao BETWEEN :start AND :end")
    GeneralMetricsDTO getGeneralMetricsWithoutProductCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(HOUR(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY HOUR(hd.ngayTao) ORDER BY HOUR(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao) ORDER BY YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(YEAR(hd.ngayTao), MONTH(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY YEAR(hd.ngayTao), MONTH(hd.ngayTao) ORDER BY YEAR(hd.ngayTao), MONTH(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(kh.id) FROM KhachHang kh " +
           "WHERE (SELECT MIN(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang.id = kh.id AND hd.paymentStatus = 'PAID') BETWEEN :start AND :end")
    Long countNewCustomers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.OrderStatusCountDTO(hd.trangThaiDonHang, COUNT(hd.id)) " +
           "FROM HoaDon hd " +
           "WHERE hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY hd.trangThaiDonHang")
    List<OrderStatusCountDTO> getOrderStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
