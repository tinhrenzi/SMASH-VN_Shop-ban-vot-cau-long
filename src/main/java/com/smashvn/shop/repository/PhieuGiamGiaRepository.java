package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.smashvn.shop.entity.PhieuGiamGia;
import java.util.Optional;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Integer> {
    Optional<PhieuGiamGia> findByMaPhieu(String maPhieu);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PhieuGiamGia p WHERE p.maPhieu = :maPhieu")
    Optional<PhieuGiamGia> findByMaPhieuWithLock(@Param("maPhieu") String maPhieu);

    boolean existsByMaPhieuIgnoreCase(String maPhieu);

    boolean existsByMaPhieuIgnoreCaseAndIdNot(String maPhieu, Integer id);
}
