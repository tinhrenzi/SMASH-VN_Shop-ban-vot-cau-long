package com.smashvn.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.HoaDon;

public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    List<HoaDon> findByKhachHang_Id(Integer idKhachHang);

    List<HoaDon> findByKhachHang_IdOrderByIdDesc(Integer idKhachHang);

    java.util.Optional<HoaDon> findByGhnOrderCode(String ghnOrderCode);

    default java.util.Optional<HoaDon> findByMaDonHang(String maDonHang) {
        Integer id = parseIdFromMaDonHang(maDonHang);
        return id != null ? findById(id) : java.util.Optional.empty();
    }

    default java.util.Optional<HoaDon> findByMaDonHangOrNormalized(String maDonHang, String normalizedMaDonHang) {
        Integer id = parseIdFromMaDonHang(maDonHang != null ? maDonHang : normalizedMaDonHang);
        return id != null ? findById(id) : java.util.Optional.empty();
    }

    static Integer parseIdFromMaDonHang(String code) {
        if (code == null) return null;
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+$");
            java.util.regex.Matcher matcher = pattern.matcher(code.trim());
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    @Query("SELECT hd FROM HoaDon hd WHERE (hd.phuongThucThanhToan.maPhuongThuc = 'zalopay' OR hd.phuongThucThanhToan.tenPhuongThuc = 'ZaloPay') AND hd.trangThaiThanhToan = 'PENDING' AND hd.ngayTao < :cutoff")
    List<HoaDon> findPendingZaloPayOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT hd.id, "
            + "hd.khachHang.hoTenKh, "
            + "null, "
            + "hd.ngayTao, "
            + "null, "
            + "hd.phuongThucThanhToan.tenPhuongThuc, "
            + "null, "
            + "hd.trangThaiThanhToan, "
            + "hd.trangThaiDonHang, "
            + "null, "
            + "null, "
            + "null, "
            + "hd.tongTien, "
            + "null "
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
            + "hd.khachHang.hoTenKh, "
            + "null, "
            + "hd.ngayTao, "
            + "null, "
            + "hd.phuongThucThanhToan.tenPhuongThuc, "
            + "null, "
            + "hd.trangThaiThanhToan, "
            + "hd.trangThaiDonHang, "
            + "null, "
            + "null, "
            + "null, "
            + "hd.tongTien "
            + "FROM HoaDon hd "
            + "LEFT JOIN hd.khachHang "
            + "LEFT JOIN hd.phuongThucThanhToan "
            + "WHERE (LOWER(hd.trangThaiThanhToan) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.trangThaiThanhToan) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.trangThaiThanhToan) = 'cancelled' AND hd.ngayThanhToan IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) AND hd.ngayTao BETWEEN :start AND :end "
            + "ORDER BY hd.ngayTao DESC")
    List<Object[]> findRawTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(DISTINCT hd.id) FROM HoaDon hd WHERE (LOWER(hd.trangThaiThanhToan) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.trangThaiThanhToan) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.trangThaiThanhToan) = 'cancelled' AND hd.ngayThanhToan IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) AND hd.ngayTao BETWEEN :start AND :end")
    Long countTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "DELETE FROM LichSuTrangThaiDonHang WHERE id_hoa_don = :orderId", nativeQuery = true)
    void deleteOrderStatusHistoryByOrderId(@Param("orderId") Integer orderId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT hd FROM HoaDon hd WHERE hd.id = :id")
    java.util.Optional<HoaDon> findByIdWithLock(@Param("id") Integer id);
}
