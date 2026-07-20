package com.smashvn.shop.controller.api;

import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.ChatbotService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatbotRestController {

    private final ChatbotService chatbotService;
    private final TaiKhoanRepository taiKhoanRepository;

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            HttpSession session) {
        Integer idTaiKhoan = resolveTaiKhoanId();
        String sessionId = resolveSessionId(session);

        List<ChatMessageDto> history = chatbotService.getConversationHistory(conversationId, idTaiKhoan, sessionId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/send")
    public ResponseEntity<List<ChatMessageDto>> sendMessage(
            @RequestBody ChatRequest request,
            HttpSession session) {
        Integer idTaiKhoan = resolveTaiKhoanId();
        String sessionId = resolveSessionId(session);

        ChatMessageDto response = chatbotService.sendMessage(request, idTaiKhoan, sessionId);
        // Wrap in list to fit frontend JS finding mechanism
        return ResponseEntity.ok(List.of(response));
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(
            @RequestBody ChatFeedbackRequest request,
            HttpSession session) {
        Integer idTaiKhoan = resolveTaiKhoanId();
        String sessionId = resolveSessionId(session);

        try {
            chatbotService.submitFeedback(request, idTaiKhoan, sessionId);
            return ResponseEntity.ok("Cảm ơn bạn đã phản hồi!");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private Integer resolveTaiKhoanId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            TaiKhoan tk = taiKhoanRepository.findByUsername(username);
            if (tk != null) {
                return tk.getId();
            }
        }
        return null;
    }

    private String resolveSessionId(HttpSession session) {
        String guestSessionId = (String) session.getAttribute("guestSessionId");
        if (guestSessionId == null) {
            guestSessionId = java.util.UUID.randomUUID().toString();
            session.setAttribute("guestSessionId", guestSessionId);
        }
        return guestSessionId;
    }
}
