package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DanhMuc")
@Data
public class DanhMuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_danh_muc", nullable = false)
    private String tenDanhMuc;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}