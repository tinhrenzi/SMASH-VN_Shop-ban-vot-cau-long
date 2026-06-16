package com.smashvn.shop.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DanhGia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "so_sao", nullable = false)
    private Integer soSao;

    @Column(name = "binh_luan", columnDefinition = "NVARCHAR(MAX)")
    private String binhLuan;

    @Column(name = "ngay_danh_gia", nullable = false)
    private LocalDateTime ngayDanhGia = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    // Cờ kiểm duyệt ẩn/hiện độc lập
    @Column(name = "an_binh_luan", nullable = false)
    private Boolean anBinhLuan = false;

    @Column(name = "an_hinh_anh", nullable = false)
    private Boolean anHinhAnh = false;

    // Cờ xóa mềm
    @Column(name = "da_xoa", nullable = false)
    private Boolean daXoa = false;

    @Column(name = "ngay_xoa")
    private LocalDateTime ngayXoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_xoa")
    private TaiKhoan nguoiXoa;

    // Audit kiểm duyệt bình luận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_an_binh_luan")
    private TaiKhoan nguoiAnBinhLuan;

    @Column(name = "ngay_an_binh_luan")
    private LocalDateTime ngayAnBinhLuan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_hien_binh_luan")
    private TaiKhoan nguoiHienBinhLuan;

    @Column(name = "ngay_hien_binh_luan")
    private LocalDateTime ngayHienBinhLuan;

    // Audit kiểm duyệt hình ảnh
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_an_hinh_anh")
    private TaiKhoan nguoiAnHinhAnh;

    @Column(name = "ngay_an_hinh_anh")
    private LocalDateTime ngayAnHinhAnh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_hien_hinh_anh")
    private TaiKhoan nguoiHienHinhAnh;

    @Column(name = "ngay_hien_hinh_anh")
    private LocalDateTime ngayHienHinhAnh;

    // Liên kết đa ảnh
    @OneToMany(mappedBy = "danhGia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DanhGiaAnh> danhSachAnh = new ArrayList<>();
}
