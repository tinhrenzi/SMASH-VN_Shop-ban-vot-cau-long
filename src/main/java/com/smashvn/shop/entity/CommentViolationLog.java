package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CommentViolationLog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentViolationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_gia_id")
    private DanhGia danhGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "noi_dung_goc", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDungGoc;

    @Column(name = "noi_dung_da_loc", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDungDaLoc;

    @Column(name = "muc_do_vi_pham", nullable = false, length = 50)
    private String mucDoViPham; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "so_lan_vi_pham", nullable = false)
    private Integer soLanViPham;

    @Column(name = "thoi_han_khoa", length = 100)
    private String thoiHanKhoa;

    @Column(name = "ngay_vi_pham", nullable = false)
    @Builder.Default
    private LocalDateTime ngayViPham = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
