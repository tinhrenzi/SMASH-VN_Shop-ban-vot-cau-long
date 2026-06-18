package com.smashvn.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.HoaDon;

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

    @Query("SELECT hd.id, "
            + "hd.khachHang.hoKh, "
            + "hd.khachHang.tenKh, "
            + "hd.ngayTao, "
            + "hd.paymentMethod, "
            + "hd.phuongThucThanhToan.tenPhuongThuc, "
            + "hd.paymentStatus, "
            + "hd.trangThaiThanhToan, "
            + "hd.trangThaiDonHang, "
            + "hd.transactionId, "
            + "hd.appTransId, "
            + "hd.maGiaoDich, "
            + "hd.tongTien, "
            + "hd.refundStatus "
            + "FROM HoaDon hd "
            + "LEFT JOIN hd.khachHang "
            + "LEFT JOIN hd.phuongThucThanhToan "
            + "WHERE hd.ngayTao BETWEEN :start AND :end "
            + "ORDER BY hd.ngayTao DESC")
    List<Object[]> findAllOrdersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(kh.id) FROM KhachHang kh "
            + "WHERE (SELECT MIN(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang.id = kh.id "
            + "AND hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) BETWEEN :start AND :end")
    Long countNewCustomers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT hd.id, "
            + "hd.khachHang.hoKh, "
            + "hd.khachHang.tenKh, "
            + "hd.ngayTao, "
            + "hd.paymentMethod, "
            + "hd.phuongThucThanhToan.tenPhuongThuc, "
            + "hd.paymentStatus, "
            + "hd.trangThaiThanhToan, "
            + "hd.trangThaiDonHang, "
            + "hd.transactionId, "
            + "hd.appTransId, "
            + "hd.maGiaoDich, "
            + "hd.tongTien "
            + "FROM HoaDon hd "
            + "LEFT JOIN hd.khachHang "
            + "LEFT JOIN hd.phuongThucThanhToan "
            + "WHERE (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.paymentStatus) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.paymentStatus) = 'cancelled' AND hd.paidAt IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) AND hd.ngayTao BETWEEN :start AND :end "
            + "ORDER BY hd.ngayTao DESC")
    List<Object[]> findRawTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(DISTINCT hd.id) FROM HoaDon hd WHERE (LOWER(hd.paymentStatus) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.paymentStatus) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.paymentStatus) = 'cancelled' AND hd.paidAt IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) AND hd.ngayTao BETWEEN :start AND :end")
    Long countTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "DELETE FROM LichSuTrangThaiDonHang WHERE id_hoa_don = :orderId", nativeQuery = true)
    void deleteOrderStatusHistoryByOrderId(@Param("orderId") Integer orderId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT hd FROM HoaDon hd WHERE hd.id = :id")
    java.util.Optional<HoaDon> findByIdWithLock(@Param("id") Integer id);
}
