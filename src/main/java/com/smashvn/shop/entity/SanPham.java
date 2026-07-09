package com.smashvn.shop.entity;


import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SanPham")
@Data
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_danh_muc", nullable = false)
    private DanhMuc danhMuc;

    @ManyToOne
    @JoinColumn(name = "id_thuong_hieu", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "ten_san_pham", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenSanPham;
    
    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThaiValue = true;

    public String getTrangThai() {
        return Boolean.FALSE.equals(trangThaiValue) ? "ngung_kinh_doanh" : "dang_ban";
    }

    public void setTrangThai(String status) {
        this.trangThaiValue = !"ngung_ban".equalsIgnoreCase(String.valueOf(status))
                && !"ngung_kinh_doanh".equalsIgnoreCase(String.valueOf(status))
                && !"false".equalsIgnoreCase(String.valueOf(status));
    }
    @Column(name = "mo_ta", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @ManyToMany(mappedBy = "sanPhams")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<DotGiamGia> cacDotGiamGia;
    
    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    private List<SanPhamChiTiet> sanPhamChiTiets;

    public int getTongSoLuongTon() {
        if (sanPhamChiTiets == null || sanPhamChiTiets.isEmpty()) {
            return 0;
        }
        return sanPhamChiTiets.stream()
                .filter(spct -> spct.getTrangThai() == null || spct.getTrangThai().isBlank() || "dang_ban".equals(spct.getTrangThai()))
                .mapToInt(spct -> spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                .sum();
    }

    public Integer getActiveGiamGiaPhanTram() {
        if (cacDotGiamGia == null || cacDotGiamGia.isEmpty()) {
            return 0;
        }
        return cacDotGiamGia.stream()
                .filter(dgg -> dgg.getActive() && "ACTIVE".equals(dgg.getDynamicStatus()))
                .mapToInt(DotGiamGia::getPhanTramGiam)
                .max()
                .orElse(0);
    }

    public java.math.BigDecimal getGiaSauGiam(java.math.BigDecimal giaGoc) {
        int phanTram = getActiveGiamGiaPhanTram();
        if (phanTram <= 0) {
            return giaGoc;
        }
        return giaGoc.multiply(java.math.BigDecimal.valueOf(100 - phanTram))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    public String getActiveChienDichNgayKetThuc() {
        if (cacDotGiamGia == null || cacDotGiamGia.isEmpty()) {
            return "2027/01/01 00:00:00";
        }
        return cacDotGiamGia.stream()
                .filter(dgg -> dgg.getActive() && "ACTIVE".equals(dgg.getDynamicStatus()))
                .map(DotGiamGia::getNgayKetThuc)
                .filter(java.util.Objects::nonNull)
                .max(java.time.LocalDateTime::compareTo)
                .map(ldt -> ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .orElse("2027/01/01 00:00:00");
    }

    @Column(name = "so_luot_danh_gia", nullable = false)
    private Integer soDanhGia = 0;

    @Column(name = "diem_trung_binh", nullable = false)
    private Double diemTrungBinh = 0.0;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat = LocalDateTime.now();
}
