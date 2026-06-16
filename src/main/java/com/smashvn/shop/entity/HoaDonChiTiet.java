package com.smashvn.shop.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HoaDonChiTiet")
@Data
public class HoaDonChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_san_pham_chi_tiet", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", nullable = false)
    private BigDecimal donGia;

    @Column(name = "gia_niem_yet")
    private BigDecimal giaNiemYet;

    @Column(name = "phan_tram_giam")
    private BigDecimal phanTramGiam;

    @Column(name = "so_tien_giam_san_pham")
    private BigDecimal soTienGiamSanPham;

    @Column(name = "ten_dot_giam_gia", length = 100)
    private String tenDotGiamGia;

    @Column(name = "id_dot_giam_gia")
    private Integer idDotGiamGia;

    @Column(name = "ten_san_pham_snapshot", length = 255)
    private String tenSanPhamSnapshot;

    @Column(name = "sku_snapshot", length = 100)
    private String skuSnapshot;

    @Column(name = "thuoc_tinh_snapshot", length = 500)
    private String thuocTinhSnapshot;

    @Column(name = "thuong_hieu_snapshot", length = 100)
    private String thuongHieuSnapshot;

    @Column(name = "danh_muc_snapshot", length = 100)
    private String danhMucSnapshot;
}
