package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "TokenKhoiPhuc")
@Data
public class TokenKhoiPhuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ma_xac_nhan", nullable = false)
    private String maXacNhan;

    @Column(name = "loai_xac_nhan", nullable = false)
    private String loaiXacNhan; // Sẽ lưu là 'EMAIL'

    @Column(name = "thoi_gian_het_han", nullable = false)
    private LocalDateTime thoiGianHetHan;

    @Column(name = "da_su_dung", nullable = false)
    private boolean daSuDung;
}