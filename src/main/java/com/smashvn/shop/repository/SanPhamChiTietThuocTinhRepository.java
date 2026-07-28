package com.smashvn.shop.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;

public interface SanPhamChiTietThuocTinhRepository extends JpaRepository<SanPhamChiTietThuocTinh, Integer> {

    List<SanPhamChiTietThuocTinh> findBySanPhamChiTiet_Id(Integer sanPhamChiTietId);

    void deleteBySanPhamChiTiet_Id(Integer sanPhamChiTietId);

    @Query("""
            SELECT DISTINCT s.giaTri FROM SanPhamChiTietThuocTinh s
            WHERE LOWER(s.thuocTinh.tenThuocTinh) = LOWER(:tenThuocTinh)
              AND s.giaTri IS NOT NULL AND s.giaTri != ''
              AND s.sanPhamChiTiet.trangThaiValue = true
            """)
    List<String> findDistinctGiaTriByTenThuocTinh(@Param("tenThuocTinh") String tenThuocTinh);
}
