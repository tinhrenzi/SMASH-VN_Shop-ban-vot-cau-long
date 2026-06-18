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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.id = :id")
    Optional<SanPhamChiTiet> findByIdWithLock(@Param("id") Integer id);

    @Query("SELECT DISTINCT s.trongLuong FROM SanPhamChiTiet s WHERE s.trongLuong IS NOT NULL AND s.trongLuong != ''")
    List<String> findDistinctTrongLuong();
}

