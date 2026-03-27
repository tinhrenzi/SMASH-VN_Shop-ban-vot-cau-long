package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.TaiKhoan;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    // Hàm này giúp kiểm tra xem email đã có ai đăng ký chưa
    boolean existsByEmail(String email);
    
    // Hàm này sẽ dùng cho chức năng Đăng nhập sau này
    TaiKhoan findByEmail(String email);
}
