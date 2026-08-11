package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.EditLog;

public interface EditLogRepository extends JpaRepository<EditLog, Integer> {

    List<EditLog> findByTenBangAndIdBanGhiOrderByThoiGianAsc(String tenBang, Integer idBanGhi);

    List<EditLog> findByTenBangAndIdBanGhiInOrderByThoiGianDesc(String tenBang, List<Integer> idBanGhis);

    List<EditLog> findByTaiKhoan_Id(Integer id);
}
