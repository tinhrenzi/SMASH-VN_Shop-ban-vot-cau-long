package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "SanPhamChiTiet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"sanPham", "hinhAnhSanPhams", "sanPhamChiTietThuocTinhs"})
public class SanPhamChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "so_luong_ton", nullable = false)
    @Builder.Default
    private Integer soLuongTon = 0;

    @Column(name = "gia_nhap")
    private BigDecimal giaNhap;

    @Column(name = "ngay_tao")
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    @Builder.Default
    private LocalDateTime ngayCapNhat = LocalDateTime.now();

    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private Boolean trangThaiValue = true;

    @OneToMany(mappedBy = "sanPhamChiTiet", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
    @Builder.Default
    private List<HinhAnhSanPham> hinhAnhSanPhams = new ArrayList<>();

    @OneToMany(mappedBy = "sanPhamChiTiet", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
    @Builder.Default
    private List<SanPhamChiTietThuocTinh> sanPhamChiTietThuocTinhs = new ArrayList<>();

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
            hasp.setMauSac(getMauSac());
            this.hinhAnhSanPhams.add(hasp);
        }
    }

    /**
     * Dynamic attribute getters (reads from sanPhamChiTietThuocTinhs list)
     */
    public String getGiaTriThuocTinh(String tenThuocTinh) {
        if (sanPhamChiTietThuocTinhs == null || tenThuocTinh == null) return null;
        return sanPhamChiTietThuocTinhs.stream()
                .filter(tt -> tt.getThuocTinh() != null && tenThuocTinh.equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()))
                .map(SanPhamChiTietThuocTinh::getGiaTri)
                .findFirst()
                .orElse(null);
    }

    public String getMauSac() {
        return getGiaTriThuocTinh("Màu sắc");
    }

    public String getKichThuoc() {
        return getGiaTriThuocTinh("Kích thước");
    }

    public String getTrongLuong() {
        return getGiaTriThuocTinh("Trọng lượng");
    }

    public String getSucCang() {
        return getGiaTriThuocTinh("Sức căng");
    }

    public String getChatLieu() {
        return getGiaTriThuocTinh("Chất liệu");
    }

    public String getMucCang() {
        return getSucCang();
    }

    public void setMauSac(String mauSac) {
        setGiaTriThuocTinhHelper("Màu sắc", mauSac);
    }

    public void setKichThuoc(String kichThuoc) {
        setGiaTriThuocTinhHelper("Kích thước", kichThuoc);
    }

    public void setTrongLuong(String trongLuong) {
        setGiaTriThuocTinhHelper("Trọng lượng", trongLuong);
    }

    public void setSucCang(String sucCang) {
        setGiaTriThuocTinhHelper("Sức căng", sucCang);
    }

    public void setMucCang(String mucCang) {
        setGiaTriThuocTinhHelper("Sức căng", mucCang);
    }

    private void setGiaTriThuocTinhHelper(String tenThuocTinh, String giaTri) {
        if (this.sanPhamChiTietThuocTinhs == null) {
            this.sanPhamChiTietThuocTinhs = new ArrayList<>();
        }
        SanPhamChiTietThuocTinh existing = this.sanPhamChiTietThuocTinhs.stream()
                .filter(tt -> tt.getThuocTinh() != null && tenThuocTinh.equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()))
                .findFirst()
                .orElse(null);

        if (giaTri == null || giaTri.isBlank()) {
            if (existing != null) {
                this.sanPhamChiTietThuocTinhs.remove(existing);
            }
            return;
        }

        if (existing != null) {
            existing.setGiaTri(giaTri.trim());
        } else {
            ThuocTinh tt = ThuocTinh.builder()
                    .tenThuocTinh(tenThuocTinh.trim())
                    .trangThai(true)
                    .build();
            SanPhamChiTietThuocTinh val = SanPhamChiTietThuocTinh.builder()
                    .sanPhamChiTiet(this)
                    .thuocTinh(tt)
                    .giaTri(giaTri.trim())
                    .build();
            this.sanPhamChiTietThuocTinhs.add(val);
        }
    }

    public String getPhanLoaiHienThi() {
        if (sanPhamChiTietThuocTinhs == null || sanPhamChiTietThuocTinhs.isEmpty()) {
            return "Mặc định";
        }
        return sanPhamChiTietThuocTinhs.stream()
                .filter(tt -> tt.getThuocTinh() != null && tt.getGiaTri() != null && !tt.getGiaTri().isBlank())
                .map(tt -> tt.getThuocTinh().getTenThuocTinh() + ": " + tt.getGiaTri())
                .collect(Collectors.joining(", "));
    }

    public void setTrangThai(String trangThai) {
        this.trangThaiValue = !"ngung_ban".equalsIgnoreCase(String.valueOf(trangThai))
                && !"ngung_kinh_doanh".equalsIgnoreCase(String.valueOf(trangThai))
                && !"false".equalsIgnoreCase(String.valueOf(trangThai));
    }

    public String getTrangThai() {
        return Boolean.FALSE.equals(trangThaiValue) ? "ngung_kinh_doanh" : "dang_ban";
    }

    public boolean isDangBan() {
        return !Boolean.FALSE.equals(trangThaiValue);
    }
}
