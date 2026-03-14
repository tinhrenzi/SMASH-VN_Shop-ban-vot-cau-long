package com.smashvn.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DotGiamGia")
@Data
public class DotGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_chien_dich", nullable = false)
    private String tenChienDich;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "phan_tram_giam", nullable = false)
    private Integer phanTramGiam;

    @Column(name = "loai_giam_gia", nullable = false, length = 100)
    private String loaiGiamGia;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;
}
