package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.SanPham;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    boolean existsByDanhMucId(Integer idDanhMuc);
    boolean existsByThuongHieuId(Integer idThuongHieu);
    java.util.List<SanPham> findByDanhMucId(Integer idDanhMuc);
    java.util.List<SanPham> findByThuongHieuId(Integer idThuongHieu);
}
