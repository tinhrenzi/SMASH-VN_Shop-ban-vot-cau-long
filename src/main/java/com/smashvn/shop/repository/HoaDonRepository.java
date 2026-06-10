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

    java.util.Optional<HoaDon> findByMaDonHang(String maDonHang);

    java.util.Optional<HoaDon> findByGhnOrderCode(String ghnOrderCode);

    @Query("SELECT hd FROM HoaDon hd WHERE hd.maDonHang = :maDonHang OR REPLACE(REPLACE(hd.maDonHang, '-', ''), '_', '') = :normalizedMaDonHang")
    java.util.Optional<HoaDon> findByMaDonHangOrNormalized(
            @Param("maDonHang") String maDonHang,
            @Param("normalizedMaDonHang") String normalizedMaDonHang);

    @Query("SELECT hd FROM HoaDon hd WHERE hd.paymentMethod = 'ZaloPay' AND hd.paymentStatus = 'PENDING' AND hd.ngayTao < :cutoff")
    List<HoaDon> findPendingZaloPayOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod IN ('ZaloPay', 'zalopay', 'sepay') AND hd.ngayTao BETWEEN :start AND :end")
    Long countOnlineTransactions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod IN ('ZaloPay', 'zalopay', 'sepay') AND hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end")
    Long countOnlineSuccessful(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod IN ('ZaloPay', 'zalopay', 'sepay') AND hd.paymentStatus = 'FAILED' AND hd.ngayTao BETWEEN :start AND :end")
    Long countOnlineFailed(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE hd.paymentMethod IN ('ZaloPay', 'zalopay', 'sepay') AND hd.paymentStatus = 'PENDING' AND hd.ngayTao BETWEEN :start AND :end")
    Long countOnlinePending(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(hd.tongTien), 0.0) FROM HoaDon hd WHERE hd.paymentMethod IN ('ZaloPay', 'zalopay', 'sepay') AND hd.paymentStatus = 'PAID' AND hd.ngayTao BETWEEN :start AND :end")
    java.math.BigDecimal sumOnlineRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.GeneralMetricsDTO(" +
           "COUNT(hd.id), " +
           "COALESCE(SUM(CASE WHEN (hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN') THEN 1L ELSE 0L END), 0L), " +
           "COALESCE(SUM(CASE WHEN hd.paymentStatus = 'CANCELLED' OR hd.paymentStatus = 'FAILED' OR (hd.trangThaiDonHang = 'da_huy' AND (hd.trangThaiThanhToan = 'REFUNDED' OR hd.trangThaiThanhToan = 'HUY')) THEN 1L ELSE 0L END), 0L), " +
           "COALESCE(SUM(CASE WHEN (hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN') THEN hd.tongTien ELSE 0.0 END), 0.0), " +
           "COALESCE(AVG(CASE WHEN (hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN') THEN hd.tongTien ELSE NULL END), 0.0), " +
           "0L" +
           ") FROM HoaDon hd WHERE hd.ngayTao BETWEEN :start AND :end")
    GeneralMetricsDTO getGeneralMetricsWithoutProductCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(hd.tongTien), 0.0) FROM HoaDon hd " +
           "WHERE (hd.trangThaiDonHang IN ('da_xac_nhan', 'dang_lay_hang', 'dang_giao', 'da_giao') " +
           "OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) " +
           "AND (hd.paymentStatus IS NULL OR LOWER(hd.paymentStatus) <> 'refunded') " +
           "AND (hd.trangThaiThanhToan IS NULL OR hd.trangThaiThanhToan <> 'REFUNDED') " +
           "AND hd.ngayTao BETWEEN :start AND :end")
    java.math.BigDecimal sumExpectedRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(HOUR(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE (hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN') AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY HOUR(hd.ngayTao) ORDER BY HOUR(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE (hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN') AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao) ORDER BY YEAR(hd.ngayTao), MONTH(hd.ngayTao), DAY(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.ChartPointDTO(YEAR(hd.ngayTao), MONTH(hd.ngayTao), COALESCE(SUM(hd.tongTien), 0.0)) " +
           "FROM HoaDon hd " +
           "WHERE (hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN') AND hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY YEAR(hd.ngayTao), MONTH(hd.ngayTao) ORDER BY YEAR(hd.ngayTao), MONTH(hd.ngayTao)")
    List<ChartPointDTO> getRevenueByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(kh.id) FROM KhachHang kh " +
           "WHERE (SELECT MIN(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang.id = kh.id " +
           "AND ((hd.trangThaiDonHang = 'da_giao' OR (hd.trangThaiDonHang = 'da_huy' AND hd.trangThaiThanhToan = 'CHO_HOAN_TIEN')) AND (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN'))) BETWEEN :start AND :end")
    Long countNewCustomers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.OrderStatusCountDTO(hd.trangThaiDonHang, COUNT(hd.id)) " +
           "FROM HoaDon hd " +
           "WHERE hd.ngayTao BETWEEN :start AND :end " +
           "GROUP BY hd.trangThaiDonHang")
    List<OrderStatusCountDTO> getOrderStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT hd.id, " +
           "hd.khachHang.hoKh, " +
           "hd.khachHang.tenKh, " +
           "hd.ngayTao, " +
           "hd.paymentMethod, " +
           "hd.phuongThucThanhToan.tenPhuongThuc, " +
           "hd.paymentStatus, " +
           "hd.trangThaiThanhToan, " +
           "hd.trangThaiDonHang, " +
           "hd.transactionId, " +
           "hd.appTransId, " +
           "hd.maGiaoDich, " +
           "hd.tongTien " +
           "FROM HoaDon hd " +
           "LEFT JOIN hd.khachHang " +
           "LEFT JOIN hd.phuongThucThanhToan " +
           "WHERE (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR (LOWER(hd.paymentStatus) = 'cancelled' AND hd.paidAt IS NOT NULL)) AND hd.ngayTao BETWEEN :start AND :end " +
           "ORDER BY hd.ngayTao DESC")
    List<Object[]> findRawTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(hd.id) FROM HoaDon hd WHERE (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR (LOWER(hd.paymentStatus) = 'cancelled' AND hd.paidAt IS NOT NULL)) AND hd.ngayTao BETWEEN :start AND :end")
    Long countTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "DELETE FROM LichSuTrangThaiDonHang WHERE id_hoa_don = :orderId", nativeQuery = true)
    void deleteOrderStatusHistoryByOrderId(@Param("orderId") Integer orderId);
}
