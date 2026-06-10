package com.smashvn.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smashvn.shop.entity.SanPham;
import java.math.BigDecimal;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    boolean existsByDanhMucId(Integer idDanhMuc);
    boolean existsByThuongHieuId(Integer idThuongHieu);
    java.util.List<SanPham> findByDanhMucId(Integer idDanhMuc);
    java.util.List<SanPham> findByThuongHieuId(Integer idThuongHieu);
    long countByDanhMucId(Integer categoryId);
    long countByThuongHieuId(Integer brandId);

    /**
     * Tìm kiếm sản phẩm theo nhiều tiêu chí kết hợp, hỗ trợ phân trang.
     * Lọc theo danh mục, thương hiệu và khoảng giá (giá của biến thể đầu tiên).
     */
    @Query("SELECT DISTINCT sp FROM SanPham sp " +
           "JOIN sp.sanPhamChiTiets spct " +
           "WHERE (:categoryId IS NULL OR sp.danhMuc.id = :categoryId) " +
           "AND (:brandId IS NULL OR sp.thuongHieu.id = :brandId) " +
           "AND (:minPrice IS NULL OR spct.giaBan >= :minPrice) " +
           "AND (:maxPrice IS NULL OR spct.giaBan <= :maxPrice)")
    Page<SanPham> findByFilters(
        @Param("categoryId") Integer categoryId,
        @Param("brandId") Integer brandId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * Lấy giá thấp nhất trong toàn bộ sản phẩm (để khởi tạo slider)
     */
    @Query("SELECT MIN(spct.giaBan) FROM SanPhamChiTiet spct")
    BigDecimal findMinPrice();

    /**
     * Lấy giá cao nhất trong toàn bộ sản phẩm (để khởi tạo slider)
     */
    @Query("SELECT MAX(spct.giaBan) FROM SanPhamChiTiet spct")
    BigDecimal findMaxPrice();
}
