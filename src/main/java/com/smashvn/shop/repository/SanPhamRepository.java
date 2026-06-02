package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.SanPham;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    // Chỉ cần kế thừa JpaRepository là đã có sẵn hàm findAll() để lấy toàn bộ dữ liệu
}
