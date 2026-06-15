package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TaiKhoan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaiKhoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau", nullable = false)
    private String matKhau;

    @Column(name = "vai_tro", nullable = false, length = 10)
    private String vaiTro;

    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai = "hoat_dong";

    @Column(name = "token_xac_thuc_khoa", length = 100)
    private String tokenXacThucKhoa;


    @Column(name = "la_khach_hang")
    private Boolean laKhachHang = false;

    @Column(name = "la_nhan_vien")
    private Boolean laNhanVien = false;

    @Column(name = "la_quan_ly")
    private Boolean laQuanLy = false;
}
