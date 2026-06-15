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

    @Column(name = "active")
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "SanPham_DotGiamGia",
        joinColumns = @JoinColumn(name = "id_dot_giam_gia"),
        inverseJoinColumns = @JoinColumn(name = "id_san_pham")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.Set<SanPham> sanPhams = new java.util.HashSet<>();

    public Boolean getActive() {
        return active == null ? true : active;
    }

    public String getDynamicStatus() {
        if (active != null && !active) {
            return "INACTIVE";
        }
        LocalDateTime now = LocalDateTime.now();
        if (ngayBatDau != null && now.isBefore(ngayBatDau)) {
            return "UPCOMING";
        } else if (ngayKetThuc != null && now.isAfter(ngayKetThuc)) {
            return "EXPIRED";
        } else {
            return "ACTIVE";
        }
    }
}
