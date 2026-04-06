package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.SanPham;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    // Chỉ cần kế thừa JpaRepository là đã có sẵn hàm findAll() để lấy toàn bộ dữ liệu
}
