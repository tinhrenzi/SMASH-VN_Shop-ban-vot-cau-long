package com.smashvn.shop.service;

import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import java.util.List;

public interface ChatbotService {
    ChatMessageDto sendMessage(ChatRequest request, Integer idTaiKhoan, String sessionId);
    List<ChatMessageDto> getConversationHistory(Long conversationId, Integer idTaiKhoan, String sessionId);
    void submitFeedback(ChatFeedbackRequest request, Integer idTaiKhoan, String sessionId);
}
