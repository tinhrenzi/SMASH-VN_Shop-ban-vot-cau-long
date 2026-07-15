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

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TaiKhoan t WHERE t.matKhau IS NOT NULL AND t.matKhau NOT LIKE '$2a$%' AND t.matKhau NOT LIKE '$2b$%' AND t.matKhau NOT LIKE '$2y$%'")
    java.util.List<TaiKhoan> findPlaintextAccounts();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TaiKhoan t WHERE t.id = :id")
    java.util.Optional<TaiKhoan> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
