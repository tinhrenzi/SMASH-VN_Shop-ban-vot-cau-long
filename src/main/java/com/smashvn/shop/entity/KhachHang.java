package com.smashvn.shop.entity;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KhachHang")
@Data
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "id_tai_khoan", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    @Column(name = "ho_kh", nullable = false, length = 50)
    private String hoKh;

    @Column(name = "ten_kh", nullable = false, length = 50)
    private String tenKh;

    @Column(name = "so_dien_thoai_kh", nullable = false, length = 15)
    private String soDienThoaiKh;

    @Column(name = "nhan_ban_tin", nullable = false)
    private boolean nhanBanTin;

    @Column(name = "la_tai_khoan_noi_bo", columnDefinition = "bit not null default 0")
    private boolean laTaiKhoanNoiBo = false;

    @Column(name = "loai_khach_hang", length = 30)
    private String loaiKhachHang;

    @Column(name = "nguon_tao", length = 50)
    private String nguonTao;

    @Column(name = "ngay_tao")
    private java.time.LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SoDiaChi> soDiaChis;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SanPhamYeuThich> sanPhamYeuThichs;
}
