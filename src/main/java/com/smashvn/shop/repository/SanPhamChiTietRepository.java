package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.SanPhamChiTiet;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {
	List<SanPhamChiTiet> findTop8ByOrderByIdDesc();
	List<SanPhamChiTiet> findBySanPham_Id(Integer sanPhamId);
}
