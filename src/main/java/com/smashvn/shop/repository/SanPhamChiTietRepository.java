package com.smashvn.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.smashvn.shop.entity.SanPhamChiTiet;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {
    List<SanPhamChiTiet> findTop8ByOrderByIdDesc();
    List<SanPhamChiTiet> findBySanPham_Id(Integer sanPhamId);

    @Query("""
            SELECT spct
            FROM SanPhamChiTiet spct
            WHERE spct.sanPham.id = :sanPhamId
              AND (spct.trangThai IS NULL OR TRIM(spct.trangThai) = '' OR spct.trangThai = 'dang_ban')
            ORDER BY spct.id ASC
            """)
    List<SanPhamChiTiet> findActiveBySanPham_Id(@Param("sanPhamId") Integer sanPhamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.id = :id")
    Optional<SanPhamChiTiet> findByIdWithLock(@Param("id") Integer id);

    @Query("SELECT DISTINCT s.trongLuong FROM SanPhamChiTiet s WHERE s.trongLuong IS NOT NULL AND s.trongLuong != '' AND (s.trangThai IS NULL OR TRIM(s.trangThai) = '' OR s.trangThai = 'dang_ban')")
    List<String> findDistinctTrongLuong();

    @Query("""
            SELECT DISTINCT spct
            FROM SanPhamChiTiet spct
            JOIN FETCH spct.sanPham sp
            LEFT JOIN FETCH sp.danhMuc dm
            LEFT JOIN FETCH sp.thuongHieu th
            LEFT JOIN FETCH sp.cacDotGiamGia dgg
            LEFT JOIN FETCH spct.hinhAnhSanPhams imgs
            WHERE (sp.trangThai IS NULL OR TRIM(sp.trangThai) = '' OR sp.trangThai NOT IN ('ngung_kinh_doanh', 'ngung_ban'))
              AND (spct.trangThai IS NULL OR TRIM(spct.trangThai) = '' OR spct.trangThai NOT IN ('ngung_kinh_doanh', 'ngung_ban'))
              AND (:idDanhMuc IS NULL OR :idDanhMuc = -1 OR dm.id = :idDanhMuc)
              AND (:idThuongHieu IS NULL OR :idThuongHieu = -1 OR th.id = :idThuongHieu)
              AND (
                    :keyword IS NULL OR :keyword = '' OR
                    LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(spct.mauSac, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(spct.trongLuong, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(spct.mucCang, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(dm.tenDanhMuc, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(th.tenThuongHieu, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY sp.id DESC, spct.id ASC
            """)
    List<SanPhamChiTiet> searchActiveVariantsForPos(
            @Param("keyword") String keyword,
            @Param("idDanhMuc") Integer idDanhMuc,
            @Param("idThuongHieu") Integer idThuongHieu);
}
