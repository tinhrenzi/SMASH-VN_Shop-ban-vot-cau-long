package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.TaiKhoan;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

    // Hàm này giúp kiểm tra xem email đã có ai đăng ký chưa
    boolean existsByEmail(String email);

    // Hàm này sẽ dùng cho chức năng Đăng nhập sau này
    TaiKhoan findByEmail(String email);

    java.util.List<TaiKhoan> findByVaiTro(String vaiTro);

    java.util.List<TaiKhoan> findByVaiTroIn(java.util.List<String> vaiTros);
}
