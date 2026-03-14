package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DonViVanChuyen")
@Data
public class DonViVanChuyen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_don_vi", length = 100)
    private String tenDonVi;

    @Column(length = 20)
    private String hotline;

    @Column(length = 100)
    private String website;
}
