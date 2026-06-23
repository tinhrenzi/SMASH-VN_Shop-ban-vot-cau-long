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

    @Column(name = "mat_khau", nullable = true)
    private String matKhau;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_tai_khoan", nullable = false, length = 20)
    private AccountStatus trangThaiTaiKhoan = AccountStatus.ACTIVE;

    @Column(name = "so_lan_mua_thanh_cong", nullable = false)
    private Integer soLanMuaThanhCong = 0;

    @Column(name = "vai_tro", nullable = false, length = 10)
    private String vaiTro;

    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai = "hoat_dong";

    @Column(name = "token_xac_thuc_khoa", length = 100)
    private String tokenXacThucKhoa;

    @Column(name = "so_lan_nhac_nho_vi_pham", nullable = false)
    private Integer soLanNhacNhoViPham = 0;

    @Column(name = "ngay_khoa_binh_luan_den")
    private java.time.LocalDateTime ngayKhoaBinhLuanDen;

    @Column(name = "ngay_vi_pham_gan_nhat")
    private java.time.LocalDateTime ngayViPhamGanNhat;

    @Transient
    private Boolean laKhachHang = false;

    @Transient
    private Boolean laNhanVien = false;

    @Transient
    private Boolean laQuanLy = false;

    @PostLoad
    public void postLoad() {
        this.laKhachHang = "KH".equals(this.vaiTro) || "NV".equals(this.vaiTro) || "QL".equals(this.vaiTro);
        this.laNhanVien = "NV".equals(this.vaiTro) || "QL".equals(this.vaiTro);
        this.laQuanLy = "QL".equals(this.vaiTro);
    }
}
