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

    @Column(name = "ma_don_vi", length = 50)
    private String maDonVi;

    @Column(name = "ten_don_vi", length = 100)
    private String tenDonVi;

    @Column(name = "so_hotline", length = 20)
    private String hotline;

    @Column(name = "trang_web", length = 100)
    private String website;

    @Column(name = "ma_token", length = 255)
    private String token;

    @Column(name = "ma_client", length = 100)
    private String clientId;

    @Column(name = "dia_chi_kho", length = 500)
    private String diaChiKho;

    @Column(name = "phi_noi_dia")
    private java.math.BigDecimal phiLocal;

    @Column(name = "phi_toan_quoc")
    private java.math.BigDecimal phiNationwide;

    @Version
    @Column(name = "phien_ban")
    private Long version;
}
