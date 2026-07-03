package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "NewsletterSubscriber")
@Data
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "gioi_tinh", length = 10)
    private String gioiTinh;

    @Column(name = "ngay_dang_ky", nullable = false)
    private LocalDateTime ngayDangKy = LocalDateTime.now();

    @Column(name = "ngay_huy")
    private LocalDateTime ngayHuy;

    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai = "hoat_dong";

    @Column(name = "token_huy", nullable = false, unique = true, length = 100)
    private String tokenHuy;
}
