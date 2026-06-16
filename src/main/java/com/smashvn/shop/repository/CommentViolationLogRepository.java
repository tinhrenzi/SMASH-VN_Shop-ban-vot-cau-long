package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.CommentViolationLog;
import java.util.List;

public interface CommentViolationLogRepository extends JpaRepository<CommentViolationLog, Integer> {
    List<CommentViolationLog> findAllByOrderByNgayViPhamDesc();
}
