package com.smashvn.shop.entity;

import java.math.BigDecimal;

import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SanPhamChiTiet", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_san_pham", "mau_sac", "trong_luong", "muc_cang"})
})
@Data
public class SanPhamChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "mau_sac", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String mauSac;

    @Column(name = "muc_cang", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String mucCang;

    @Column(name = "trong_luong", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String trongLuong;

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "so_luong_ton", nullable = false)
    private Integer soLuongTon = 0;

    @OneToMany(mappedBy = "sanPhamChiTiet", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<HinhAnhSanPham> hinhAnhSanPhams = new ArrayList<>();

    @Transient
    private String hinhAnhSanPham;

    public String getHinhAnhSanPham() {
        if (hinhAnhSanPhams != null && !hinhAnhSanPhams.isEmpty()) {
            return hinhAnhSanPhams.get(0).getUrlHinhAnh();
        }
        return hinhAnhSanPham;
    }

    public void setHinhAnhSanPham(String hinhAnhSanPham) {
        this.hinhAnhSanPham = hinhAnhSanPham;
        if (hinhAnhSanPham != null && !hinhAnhSanPham.isEmpty()) {
            if (this.hinhAnhSanPhams == null) {
                this.hinhAnhSanPhams = new ArrayList<>();
            } else {
                this.hinhAnhSanPhams.clear();
            }
            HinhAnhSanPham hasp = new HinhAnhSanPham();
            hasp.setSanPhamChiTiet(this);
            hasp.setUrlHinhAnh(hinhAnhSanPham);
            hasp.setMauSac(this.mauSac);
            this.hinhAnhSanPhams.add(hasp);
        }
    }
}