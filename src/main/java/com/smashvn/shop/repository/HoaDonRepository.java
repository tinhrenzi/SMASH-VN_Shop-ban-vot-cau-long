package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.HoaDon;

public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    List<HoaDon> findByKhachHang_Id(Integer idKhachHang);
}
