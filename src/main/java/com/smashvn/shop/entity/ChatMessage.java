package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuoc_tro_chuyen", nullable = false)
    private ChatConversation conversation;

    @Column(name = "loai_nguoi_gui", nullable = false, length = 10)
    private String senderType; // USER, BOT

    @Column(name = "noi_dung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian = LocalDateTime.now();
}
