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
    @JoinColumn(name = "id_message", nullable = false)
    private ChatMessage message;

    @Column(name = "danh_gia", nullable = false)
    private boolean danhGia; // true for Like, false for Dislike

    @Column(name = "ghi_chu", length = 500, columnDefinition = "NVARCHAR(500)")
    private String ghiChu;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian = LocalDateTime.now();
}
