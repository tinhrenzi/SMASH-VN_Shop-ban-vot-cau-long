package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PhuongThucThanhToan")
@Data
public class PhuongThucThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_phuong_thuc", nullable = false, length = 100)
    private String tenPhuongThuc;
}