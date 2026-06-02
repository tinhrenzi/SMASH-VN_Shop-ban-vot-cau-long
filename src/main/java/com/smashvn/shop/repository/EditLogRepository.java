package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.EditLog;

public interface EditLogRepository extends JpaRepository<EditLog, Long> {
}
