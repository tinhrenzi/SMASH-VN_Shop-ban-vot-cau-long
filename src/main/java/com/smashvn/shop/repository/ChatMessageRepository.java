/**
 * Ghi chú: File này dùng để làm GFI (General File Indicator / ghi chú nội bộ).
 * Chứa repository quản lý ChatMessage.
 */
package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByThoiGianAsc(Integer conversationId);
}
