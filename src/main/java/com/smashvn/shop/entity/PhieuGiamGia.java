package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PhieuGiamGia")
@Data
public class PhieuGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_phieu", nullable = false, unique = true, length = 50)
    private String maPhieu;

    @Column(name = "gia_tri", nullable = false)
    private BigDecimal giaTri;

    @Column(name = "don_vi", nullable = false, length = 10)
    private String donVi;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "so_luong_con_lai", nullable = false)
    private Integer soLuongConLai;

    @Column(name = "loai_giam_gia", nullable = false, length = 100)
    private String loaiGiamGia;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "gia_tri_don_hang_toi_thieu")
    private BigDecimal giaTriDonHangToiThieu = BigDecimal.ZERO;

    public Boolean getActive() {
        return active == null ? true : active;
    }

    public BigDecimal getGiaTriDonHangToiThieu() {
        return giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu;
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
        } else if (soLuongConLai != null && soLuongConLai <= 0) {
            return "EXPIRED"; // Or OUT_OF_STOCK, but treat it as expired for Gray badge
        } else {
            return "ACTIVE";
        }
    }
}
