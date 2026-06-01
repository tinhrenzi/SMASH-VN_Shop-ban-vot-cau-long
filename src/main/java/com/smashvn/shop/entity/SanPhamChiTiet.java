package com.smashvn.shop.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SanPhamChiTiet", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_san_pham", "mau_sac", "trong_luong", "muc_cang"})
})
@Data
public class SanPhamChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "mau_sac", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String mauSac;

    @Column(name = "muc_cang", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String mucCang;

    @Column(name = "trong_luong", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String trongLuong;

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "so_luong_ton", nullable = false)
    private Integer soLuongTon = 0;

    @Column(name = "hinh_anh_san_pham", length = 200)
    private String hinhAnhSanPham;
}