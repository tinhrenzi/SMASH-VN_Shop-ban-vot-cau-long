package com.smashvn.shop.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DanhGiaAnh")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGiaAnh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_danh_gia", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DanhGia danhGia;

    @Column(name = "duong_dan", nullable = false, length = 255)
    private String duongDan;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
