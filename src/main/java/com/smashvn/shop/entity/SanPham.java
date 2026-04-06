package com.smashvn.shop.entity;


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

    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;
    
 // Thêm vào bên trong class SanPham
    @Column(name = "trang_thai", length = 50)
    private String trangThai = "dang_ban"; // Mặc định khi tạo mới là đang bán
    
    @Column(name = "mo_ta", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @ManyToMany
    @JoinTable(
        name = "SanPham_DotGiamGia",
        joinColumns = @JoinColumn(name = "id_san_pham"),
        inverseJoinColumns = @JoinColumn(name = "id_dot_giam_gia")
    )
    private Set<DotGiamGia> cacDotGiamGia;
}
