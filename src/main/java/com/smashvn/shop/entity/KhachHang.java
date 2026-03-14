package com.smashvn.shop.entity;

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

    @Column(name = "ho_ten_kh", nullable = false, length = 100)
    private String hoTenKh;

    @Column(name = "so_dien_thoai_kh", nullable = false, length = 15)
    private String soDienThoaiKh;

    @Column(name = "dia_chi", nullable = false, length = 500)
    private String diaChi;
}
