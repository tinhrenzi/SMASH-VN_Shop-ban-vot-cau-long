package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smashvn.shop.entity.TokenKhoiPhuc;

@Repository
public interface TokenKhoiPhucRepository extends JpaRepository<TokenKhoiPhuc, Integer> {
    TokenKhoiPhuc findByMaXacNhan(String maXacNhan);
}