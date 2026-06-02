package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "EditLog")
@Data
public class EditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ten_bang", nullable = false, length = 100)
    private String tenBang;

    @Column(name = "id_ban_ghi", nullable = false)
    private Long idBanGhi;

    @Column(name = "hanh_dong", nullable = false, length = 20)
    private String hanhDong; // 'INSERT', 'UPDATE', 'DELETE'

    @Column(name = "gia_tri_cu", columnDefinition = "NVARCHAR(MAX)")
    private String giaTriCu;

    @Column(name = "gia_tri_moi", columnDefinition = "NVARCHAR(MAX)")
    private String giaTriMoi;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();

    @Column(name = "dia_chi_ip", length = 50)
    private String diaChiIp;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "vai_tro_thuc_hien", length = 20)
    private String vaiTroThucHien;
}
