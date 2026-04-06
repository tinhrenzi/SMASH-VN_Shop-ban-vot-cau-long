package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.ThuongHieu;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Integer> {
    // Không cần viết thêm gì, dùng sẵn findAll()
}