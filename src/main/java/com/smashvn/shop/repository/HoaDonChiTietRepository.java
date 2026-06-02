package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.HoaDonChiTiet;

public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    List<HoaDonChiTiet> findByHoaDon_Id(Integer idHoaDon);
}
