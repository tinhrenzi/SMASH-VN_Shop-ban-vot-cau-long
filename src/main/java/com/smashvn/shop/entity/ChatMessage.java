package com.smashvn.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ChatMessage")
@Data
@NoArgsConstructor
@AllArgsConstructor

/**
 * Thực thể cho bản ghi tin nhắn trong cuộc trò chuyện. Dùng để ánh xạ dữ liệu
 * tin nhắn với bảng ChatMessage trong cơ sở dữ liệu. Bao gồm người gửi, nội
 * dung và thời gian gửi.
 */
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
