package com.smashvn.shop.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.service.ChatService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final KhachHangRepository khachHangRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageResponse {

        private Long id;
        private String senderType; // USER, BOT
        private String noiDung;
        private String thoiGian;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatPayload {

        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FeedbackPayload {

        private Long messageId;
        private boolean positive;
        private String note;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            // Khách chưa đăng nhập: Trả về tin nhắn chào mừng mặc định của khách vãng lai
            MessageResponse welcome = new MessageResponse(
                    0L,
                    "BOT",
                    "🏸 Xin chào khách quý! Tôi là **Trợ lý ảo SmashVN**.<br>Bạn vui lòng **đăng nhập** tài khoản để tôi có thể cá nhân hóa, hiển thị mã giảm giá và tra cứu đơn hàng riêng của bạn nhé! <br><br>"
                    + "Hiện tại tôi có thể hỗ trợ nhanh cho bạn:<br>"
                    + "• 🏸 *Xem các sản phẩm nổi bật* (gõ 'sản phẩm' hoặc 'vợt')<br>"
                    + "• 📞 *Thông tin cửa hàng & Hotline* (gõ 'liên hệ' hoặc 'địa chỉ')",
                    ""
            );
            return ResponseEntity.ok(Collections.singletonList(welcome));
        }

        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy thông tin khách hàng liên kết!");
        }

        ChatConversation conv = chatService.getOrCreateConversation(kh.getId());
        List<ChatMessage> messages = chatService.getMessages(conv.getId());

        List<MessageResponse> responses = messages.stream().map(msg -> new MessageResponse(
                msg.getId(),
                msg.getSenderType(),
                msg.getNoiDung(),
                msg.getThoiGian().format(TIME_FORMATTER)
        )).collect(Collectors.toList());

        // Nếu lịch sử cuộc trò chuyện rỗng, tự động chào mừng
        if (responses.isEmpty()) {
            ChatMessage welcomeMsg = chatService.generateBotResponse(conv.getId(), "chào");
            responses.add(new MessageResponse(
                    welcomeMsg.getId(),
                    welcomeMsg.getSenderType(),
                    welcomeMsg.getNoiDung(),
                    welcomeMsg.getThoiGian().format(TIME_FORMATTER)
            ));
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatPayload payload, HttpSession session) {
        if (payload.getContent() == null || payload.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung tin nhắn trống!");
        }

        String userText = payload.getContent().trim();
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");

        if (idTaiKhoan == null) {
            // Khách vãng lai: Xử lý in-memory trực tiếp bằng AI nhưng không lưu database
            String botReply = chatService.generateAIResponseForGuest(userText);
            List<MessageResponse> replies = new ArrayList<>();
            replies.add(new MessageResponse(0L, "USER", userText, ""));
            replies.add(new MessageResponse(0L, "BOT", botReply, ""));
            return ResponseEntity.ok(replies);
        }

        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy thông tin khách hàng liên kết!");
        }

        ChatConversation conv = chatService.getOrCreateConversation(kh.getId());

        // 1. Lưu tin nhắn của User
        ChatMessage userMsg = chatService.saveUserMessage(conv.getId(), userText);
        // 2. Sinh và lưu tin nhắn phản hồi của Bot
        ChatMessage botMsg = chatService.generateBotResponse(conv.getId(), userText);

        List<MessageResponse> replies = new ArrayList<>();
        replies.add(new MessageResponse(
                userMsg.getId(),
                userMsg.getSenderType(),
                userMsg.getNoiDung(),
                userMsg.getThoiGian().format(TIME_FORMATTER)
        ));
        replies.add(new MessageResponse(
                botMsg.getId(),
                botMsg.getSenderType(),
                botMsg.getNoiDung(),
                botMsg.getThoiGian().format(TIME_FORMATTER)
        ));

        return ResponseEntity.ok(replies);
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackPayload payload, HttpSession session) {
        if (payload.getMessageId() == null) {
            return ResponseEntity.badRequest().body("Mã tin nhắn không được để trống!");
        }

        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null || payload.getMessageId() == 0L) {
            // Khách vãng lai hoặc tin nhắn ảo: Chỉ trả về OK
            return ResponseEntity.ok("Cảm ơn đóng góp của bạn!");
        }

        ChatFeedback fb = chatService.saveFeedback(payload.getMessageId(), payload.isPositive(), payload.getNote());
        return ResponseEntity.ok("Cập nhật đánh giá thành công! Cảm ơn đóng góp của bạn.");
    }
}
