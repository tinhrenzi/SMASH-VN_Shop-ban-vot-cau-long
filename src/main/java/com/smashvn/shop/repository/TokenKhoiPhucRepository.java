package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.TokenKhoiPhuc;

public interface TokenKhoiPhucRepository extends JpaRepository<TokenKhoiPhuc, Integer> {

    TokenKhoiPhuc findByMaXacNhan(String maXacNhan);
}
