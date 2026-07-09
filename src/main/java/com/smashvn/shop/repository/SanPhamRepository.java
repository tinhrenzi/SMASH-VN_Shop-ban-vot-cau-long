package com.smashvn.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smashvn.shop.entity.SanPham;
import java.math.BigDecimal;
import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    // ==========================================
    // DISCOUNT CAMPAIGN — PRODUCT SELECTION HELPERS
    // ==========================================

    /**
     * Trả về tất cả sản phẩm đang bán (trangThaiValue = true).
     * Dùng trong form tạo/sửa đợt giảm giá để tránh hiển thị sản phẩm ngừng bán.
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThaiValue = true " +
           "ORDER BY sp.id ASC")
    List<SanPham> findAllActiveProducts();

    /**
     * Tìm sản phẩm đang bán có ít nhất một biến thể đang bán với giá trong khoảng [minPrice, maxPrice].
     * Lọc cả SanPham.trangThaiValue lẫn SanPhamChiTiet.trangThaiValue.
     * maxPrice = null → không giới hạn trên.
     */
    @Query("SELECT DISTINCT sp FROM SanPham sp JOIN sp.sanPhamChiTiets spct " +
           "WHERE sp.trangThaiValue = true " +
           "AND spct.trangThaiValue = true " +
           "AND spct.giaBan >= :minPrice " +
           "AND (:maxPrice IS NULL OR spct.giaBan <= :maxPrice) " +
           "ORDER BY sp.id ASC")
    List<SanPham> findActiveByPriceRange(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Tìm sản phẩm đang bán trong danh sách ID cho trước.
     * Dùng để validate productIds từ MANUAL form chống tamper:
     * nếu kết quả.size() != ids.size() thì có sản phẩm ngừng bán hoặc ID không tồn tại.
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.id IN :ids " +
           "AND sp.trangThaiValue = true")
    List<SanPham> findActiveByIdIn(@Param("ids") List<Integer> ids);


    boolean existsByDanhMucId(Integer idDanhMuc);
    boolean existsByThuongHieuId(Integer idThuongHieu);
    java.util.List<SanPham> findByDanhMucId(Integer idDanhMuc);
    java.util.List<SanPham> findByThuongHieuId(Integer idThuongHieu);
    @Query("SELECT COUNT(sp) FROM SanPham sp WHERE sp.danhMuc.id = :categoryId AND sp.trangThaiValue = true")
    long countByDanhMucId(@Param("categoryId") Integer categoryId);

    @Query("SELECT COUNT(sp) FROM SanPham sp WHERE sp.thuongHieu.id = :brandId AND sp.trangThaiValue = true")
    long countByThuongHieuId(@Param("brandId") Integer brandId);

    /**
     * Tìm kiếm sản phẩm theo nhiều tiêu chí kết hợp, hỗ trợ phân trang.
     * Lọc theo từ khóa, danh mục, thương hiệu, khoảng giá và trọng lượng (size).
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThaiValue = true " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "       LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(sp.thuongHieu.tenThuongHieu) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(sp.danhMuc.tenDanhMuc) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR sp.danhMuc.id = :categoryId) " +
           "AND (:brandId IS NULL OR sp.thuongHieu.id = :brandId) " +
           "AND (:minPrice IS NULL AND :maxPrice IS NULL OR EXISTS (SELECT 1 FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND spct.trangThaiValue = true AND (:minPrice IS NULL OR spct.giaBan >= :minPrice) AND (:maxPrice IS NULL OR spct.giaBan <= :maxPrice))) " +
           "AND (:rating IS NULL OR :rating = 0.0 OR sp.diemTrungBinh >= :rating) " +
           "AND (:trongLuong IS NULL OR EXISTS (SELECT 1 FROM SanPhamChiTiet spct3 WHERE spct3.sanPham = sp AND spct3.trangThaiValue = true AND spct3.trongLuong IN :trongLuong)) " +
           "ORDER BY CASE WHEN (sp.trangThaiValue = true AND (SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp AND spct2.trangThaiValue = true) > 0) THEN 1 ELSE 0 END DESC, " +
           "CASE WHEN :sort = 'price_asc' THEN (SELECT MIN(spct.giaBan) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND spct.trangThaiValue = true) END ASC, " +
           "CASE WHEN :sort = 'price_desc' THEN (SELECT MIN(spct.giaBan) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND spct.trangThaiValue = true) END DESC, " +
           "sp.id DESC")
    Page<SanPham> findByFilters(
        @Param("keyword") String keyword,
        @Param("categoryId") Integer categoryId,
        @Param("brandId") Integer brandId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("rating") Double rating,
        @Param("trongLuong") java.util.List<String> trongLuong,
        @Param("sort") String sort,
        Pageable pageable
    );

    /**
     * Tìm kiếm nhanh (autocomplete) - trả về tối đa 8 sản phẩm đang bán phù hợp từ khóa.
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThaiValue = true AND " +
           "(LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(sp.thuongHieu.tenThuongHieu) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY sp.id DESC")
    java.util.List<SanPham> searchAutocomplete(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT sp) FROM SanPham sp JOIN SanPhamChiTiet spct ON spct.sanPham = sp WHERE spct.trongLuong = :trongLuong AND sp.trangThaiValue = true AND spct.trangThaiValue = true")
    long countByTrongLuong(@Param("trongLuong") String trongLuong);


    /**
     * Lấy giá thấp nhất trong toàn bộ sản phẩm (để khởi tạo slider)
     */
    @Query("SELECT MIN(spct.giaBan) FROM SanPhamChiTiet spct WHERE spct.trangThaiValue = true")
    BigDecimal findMinPrice();

    /**
     * Lấy giá cao nhất trong toàn bộ sản phẩm (để khởi tạo slider)
     */
    @Query("SELECT MAX(spct.giaBan) FROM SanPhamChiTiet spct WHERE spct.trangThaiValue = true")
    BigDecimal findMaxPrice();

    /**
     * Lấy các sản phẩm mới nhất (sắp xếp theo ID giảm dần)
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThaiValue = true " +
           "ORDER BY CASE WHEN ((SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp AND spct2.trangThaiValue = true) > 0) THEN 1 ELSE 0 END DESC, sp.id DESC")
    java.util.List<SanPham> findNewProducts(Pageable pageable);

    /**
     * Lấy các sản phẩm bán chạy nhất (sắp xếp theo tổng số lượng bán được)
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThaiValue = true " +
           "ORDER BY CASE WHEN ((SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp AND spct2.trangThaiValue = true) > 0) THEN 1 ELSE 0 END DESC, " +
           "         (SELECT COALESCE(SUM(hdct.soLuong), 0) FROM HoaDonChiTiet hdct " +
           "          WHERE hdct.sanPhamChiTiet.sanPham = sp) DESC, sp.id DESC")
    java.util.List<SanPham> findBestSellers(Pageable pageable);

    /**
     * Lấy các sản phẩm nổi bật nhất (sắp xếp theo tổng lượt mua + lượt yêu thích)
     */
    @Query("SELECT sp FROM SanPham sp " +
           "WHERE sp.trangThaiValue = true " +
           "ORDER BY CASE WHEN ((SELECT COALESCE(SUM(spct2.soLuongTon), 0) FROM SanPhamChiTiet spct2 WHERE spct2.sanPham = sp AND spct2.trangThaiValue = true) > 0) THEN 1 ELSE 0 END DESC, " +
           "         ((SELECT COALESCE(SUM(hdct.soLuong), 0) FROM HoaDonChiTiet hdct WHERE hdct.sanPhamChiTiet.sanPham = sp) + " +
           "          (SELECT COUNT(spy) FROM SanPhamYeuThich spy WHERE spy.sanPham = sp)) DESC, sp.id DESC")
    java.util.List<SanPham> findFeaturedProducts(Pageable pageable);

    @Query("SELECT sp FROM SanPham sp WHERE " +
           "sp.trangThaiValue = true AND " +
           "(LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(sp.thuongHieu.tenThuongHieu) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(sp.danhMuc.tenDanhMuc) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    java.util.List<SanPham> searchByKeyword(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(sp) FROM SanPham sp WHERE sp.trangThaiValue = true")
    long countActiveProducts();

    @Query("SELECT sp FROM SanPham sp WHERE sp.trangThaiValue = false AND (" +
           "LOWER(sp.tenSanPham) = LOWER(:query) OR " +
           "LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           ":query LIKE LOWER(CONCAT('%', sp.tenSanPham, '%')))")
    java.util.List<SanPham> findDiscontinuedProductByQuery(@Param("query") String query);

    @Query("SELECT DISTINCT sp FROM SanPham sp JOIN sp.sanPhamChiTiets spct WHERE " +
           "sp.trangThaiValue = true AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sp.danhMuc.tenDanhMuc) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sp.thuongHieu.tenThuongHieu) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "spct.trangThaiValue = true AND " +
           "(:minPrice IS NULL OR spct.giaBan >= :minPrice) AND " +
           "(:maxPrice IS NULL OR spct.giaBan <= :maxPrice)")
    java.util.List<SanPham> searchChatbotProducts(
        @Param("keyword") String keyword,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        org.springframework.data.domain.Pageable pageable
    );
}
