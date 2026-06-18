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
     * Lọc theo danh mục, thương hiệu, khoảng giá và trọng lượng (size).
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE (:categoryId IS NULL OR sp.danhMuc.id = :categoryId) " +
           "AND (:brandId IS NULL OR sp.thuongHieu.id = :brandId) " +
           "AND (:minPrice IS NULL AND :maxPrice IS NULL OR EXISTS (SELECT 1 FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND (:minPrice IS NULL OR spct.giaBan >= :minPrice) AND (:maxPrice IS NULL OR spct.giaBan <= :maxPrice))) " +
           "AND (:rating IS NULL OR :rating = 0.0 OR sp.diemTrungBinh >= :rating) " +
           "AND (:trongLuong IS NULL OR EXISTS (SELECT 1 FROM SanPhamChiTiet spct3 WHERE spct3.sanPham = sp AND spct3.trongLuong IN :trongLuong)) " +
           "ORDER BY CASE WHEN ((sp.trangThai IS NULL OR sp.trangThai = 'dang_ban') AND (SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp) > 0) THEN 1 ELSE 0 END DESC, " +
           "CASE WHEN :sort = 'price_asc' THEN (SELECT MIN(spct.giaBan) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp) END ASC, " +
           "CASE WHEN :sort = 'price_desc' THEN (SELECT MIN(spct.giaBan) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp) END DESC, " +
           "sp.id DESC")
    Page<SanPham> findByFilters(
        @Param("categoryId") Integer categoryId,
        @Param("brandId") Integer brandId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("rating") Double rating,
        @Param("trongLuong") java.util.List<String> trongLuong,
        @Param("sort") String sort,
        Pageable pageable
    );

    @Query("SELECT COUNT(DISTINCT sp) FROM SanPham sp JOIN SanPhamChiTiet spct ON spct.sanPham = sp WHERE spct.trongLuong = :trongLuong")
    long countByTrongLuong(@Param("trongLuong") String trongLuong);


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

    /**
     * Lấy các sản phẩm mới nhất (sắp xếp theo ID giảm dần)
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThai IS NULL OR sp.trangThai = 'dang_ban' " +
           "ORDER BY CASE WHEN ((SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp) > 0) THEN 1 ELSE 0 END DESC, sp.id DESC")
    java.util.List<SanPham> findNewProducts(Pageable pageable);

    /**
     * Lấy các sản phẩm bán chạy nhất (sắp xếp theo tổng số lượng bán được)
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThai IS NULL OR sp.trangThai = 'dang_ban' " +
           "ORDER BY CASE WHEN ((SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp) > 0) THEN 1 ELSE 0 END DESC, " +
           "         (SELECT COALESCE(SUM(hdct.soLuong), 0) FROM HoaDonChiTiet hdct " +
           "          WHERE hdct.sanPhamChiTiet.sanPham = sp) DESC, sp.id DESC")
    java.util.List<SanPham> findBestSellers(Pageable pageable);

    /**
     * Lấy các sản phẩm nổi bật nhất (sắp xếp theo điểm đánh giá trung bình)
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThai IS NULL OR sp.trangThai = 'dang_ban' " +
           "ORDER BY CASE WHEN ((SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp) > 0) THEN 1 ELSE 0 END DESC, " +
           "         (SELECT COALESCE(AVG(dg.soSao), 0.0) FROM DanhGia dg " +
           "          WHERE dg.sanPham = sp) DESC, sp.id DESC")
    java.util.List<SanPham> findFeaturedProducts(Pageable pageable);
}
