package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.EditLog;
import java.util.List;

public interface EditLogRepository extends JpaRepository<EditLog, Long> {
    List<EditLog> findByTenBangAndIdBanGhiOrderByThoiGianAsc(String tenBang, Long idBanGhi);
    List<EditLog> findByTaiKhoan_Id(Integer id);
}
