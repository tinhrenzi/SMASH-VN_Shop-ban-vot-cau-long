package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HoaDon")
@Data
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien; // Có thể null

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_thanh_toan", nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia; // Có thể null

    @ManyToOne
    @JoinColumn(name = "id_don_vi_van_chuyen", nullable = false)
    private DonViVanChuyen donViVanChuyen;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "tong_tien", nullable = false)
    private BigDecimal tongTien;

    @Column(name = "trang_thai_don_hang", nullable = false, length = 50)
    private String trangThaiDonHang = "cho_xac_nhan";

    @Column(name = "trang_thai_thanh_toan", nullable = false, length = 50)
    private String trangThaiThanhToan = "chua_thanh_toan";

    @Column(name = "dia_chi_nhan", nullable = false, length = 500)
    private String diaChiNhan;

    @Column(name = "sdt_nhan", nullable = false, length = 15)
    private String sdtNhan;
}
