package com.smashvn.shop.entity;


import java.util.List;
import java.util.Set;

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
    
 // Thêm vào bên trong class SanPham
    @Column(name = "trang_thai", length = 50)
    private String trangThai = "dang_ban"; // Mặc định khi tạo mới là đang bán
    
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
}
