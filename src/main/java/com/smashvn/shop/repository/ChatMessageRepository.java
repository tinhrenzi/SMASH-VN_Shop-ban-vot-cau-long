package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.ChatMessage;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByThoiGianAsc(Integer conversationId);
}
