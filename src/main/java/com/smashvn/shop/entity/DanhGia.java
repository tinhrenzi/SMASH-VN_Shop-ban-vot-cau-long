package com.smashvn.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DanhGia")
@Data
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "so_sao", nullable = false)
    private Integer soSao;

    @Column(name = "binh_luan", columnDefinition = "NVARCHAR(MAX)")
    private String binhLuan;

    @Column(name = "ngay_danh_gia", nullable = false)
    private LocalDateTime ngayDanhGia = LocalDateTime.now();
}
