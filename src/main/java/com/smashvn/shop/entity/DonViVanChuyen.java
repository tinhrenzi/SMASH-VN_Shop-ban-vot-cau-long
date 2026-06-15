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

    @Column(length = 255)
    private String token;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "dia_chi_kho", length = 500)
    private String diaChiKho;

    @Column(name = "phi_local")
    private java.math.BigDecimal phiLocal;

    @Column(name = "phi_nationwide")
    private java.math.BigDecimal phiNationwide;

    @Version
    private Long version;
}
