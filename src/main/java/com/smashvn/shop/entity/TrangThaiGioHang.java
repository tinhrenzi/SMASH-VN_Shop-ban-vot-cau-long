package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TrangThaiGioHang")
@Data
public class TrangThaiGioHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_trang_thai", length = 50)
    private String tenTrangThai;
}
