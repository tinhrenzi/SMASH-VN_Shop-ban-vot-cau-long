package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.ThuongHieu;

public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Integer> {
    // Không cần viết thêm gì, dùng sẵn findAll()
}
