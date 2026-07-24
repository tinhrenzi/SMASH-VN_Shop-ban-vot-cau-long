package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "SanPhamChiTiet", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_san_pham", "mau_sac", "trong_luong", "kich_thuoc"})
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

    @Column(name = "suc_cang", length = 50, columnDefinition = "NVARCHAR(50)")
    private String sucCang;

    @Column(name = "trong_luong", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String trongLuong;

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "so_luong_ton", nullable = false)
    private Integer soLuongTon = 0;

    @Column(name = "gia_nhap")
    private BigDecimal giaNhap;

    @Column(name = "kich_thuoc", length = 50)
    private String kichThuoc;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat = LocalDateTime.now();

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThaiValue = true;

    @OneToMany(mappedBy = "sanPhamChiTiet", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
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

    public String getMucCang() {
        return sucCang;
    }

    public void setMucCang(String mucCang) {
        this.sucCang = mucCang;
    }

    public void setTrangThai(String trangThai) {
        this.trangThaiValue = !"ngung_ban".equalsIgnoreCase(String.valueOf(trangThai))
                && !"ngung_kinh_doanh".equalsIgnoreCase(String.valueOf(trangThai))
                && !"false".equalsIgnoreCase(String.valueOf(trangThai));
    }

    public String getTrangThai() {
        return Boolean.FALSE.equals(trangThaiValue) ? "ngung_kinh_doanh" : "dang_ban";
    }

    public Boolean getTrangThaiValue() {
        return trangThaiValue;
    }

    public void setTrangThaiValue(Boolean trangThaiValue) {
        this.trangThaiValue = trangThaiValue;
    }

    public boolean isDangBan() {
        return !Boolean.FALSE.equals(trangThaiValue);
    }
}
