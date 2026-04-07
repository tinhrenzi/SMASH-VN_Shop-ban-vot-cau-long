package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SoDiaChi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoDiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ho_nguoi_nhan", nullable = false, length = 50)
    private String hoNguoiNhan;

    @Column(name = "ten_nguoi_nhan", nullable = false, length = 50)
    private String tenNguoiNhan;

    @Column(name = "sdt_nguoi_nhan", nullable = false, length = 15)
    private String sdtNguoiNhan;

    @Column(name = "dia_chi_cu_the", nullable = false, length = 255)
    private String diaChiCuThe;

    @Column(name = "quoc_gia", nullable = false, length = 100)
    private String quocGia;

    @Column(name = "tinh_thanh", nullable = false, length = 100)
    private String tinhThanh;

    @Column(name = "thanh_pho", nullable = false, length = 100)
    private String thanhPho;

    @Column(name = "ma_buu_dien", nullable = false, length = 20)
    private String maBuuDien;

    @Column(name = "is_default_shipping", nullable = false)
    private boolean defaultShipping;

    @Column(name = "is_default_billing", nullable = false)
    private boolean defaultBilling;
}
