package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.TrangThaiGioHang;

@Repository	
public interface TrangThaiGioHangRepository extends JpaRepository<TrangThaiGioHang, Integer> {
}
