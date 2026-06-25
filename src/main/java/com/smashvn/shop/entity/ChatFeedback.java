package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatFeedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tin_nhan", nullable = false)
    private ChatMessage message;

    @Column(name = "diem_danh_gia", nullable = false)
    private boolean danhGia; // true for Like, false for Dislike

    @Column(name = "noi_dung", length = 500, columnDefinition = "NVARCHAR(500)")
    private String ghiChu;

    @Column(name = "ngay_tao")
    private LocalDateTime thoiGian = LocalDateTime.now();
}
