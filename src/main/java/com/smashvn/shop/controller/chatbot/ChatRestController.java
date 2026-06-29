package com.smashvn.shop.controller.chatbot;

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

import com.smashvn.shop.dto.chatbot.ProductSuggestionDto;
import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.ChatMessageRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.service.chatbot.ChatService;

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
    private final ChatMessageRepository chatMessageRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    @Data
    @NoArgsConstructor
    public static class MessageResponse {

        private Long id;
        private String senderType; // USER, BOT
        private String noiDung;
        private String thoiGian;
        private List<ProductSuggestionDto> products;

        public MessageResponse(Long id, String senderType, String noiDung, String thoiGian) {
            this.id = id;
            this.senderType = senderType;
            this.noiDung = noiDung;
            this.thoiGian = thoiGian;
            this.products = Collections.emptyList();
        }

        public MessageResponse(Long id, String senderType, String noiDung, String thoiGian, List<ProductSuggestionDto> products) {
            this.id = id;
            this.senderType = senderType;
            this.noiDung = noiDung;
            this.thoiGian = thoiGian;
            this.products = products;
        }
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

    private KhachHang getOrCreateKhachHang(Integer idTaiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
            kh = new KhachHang();
            kh.setTaiKhoan(taiKhoan);
            kh.setHoKh("");
            String email = taiKhoan.getEmail();
            String name = (email != null && email.contains("@")) ? email.split("@")[0] : "Người dùng";
            kh.setTenKh(name);
            kh.setSoDienThoaiKh("");
            kh.setNhanBanTin(false);
            kh.setLaTaiKhoanNoiBo("QL".equals(taiKhoan.getVaiTro()) || "NV".equals(taiKhoan.getVaiTro()));
            kh = khachHangRepository.save(kh);
        }
        return kh;
    }

    private Integer getActiveUserId(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan).orElse(null);
        if (tk == null || tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.ACTIVE) {
            return null;
        }
        return idTaiKhoan;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(HttpSession session) {
        Integer idTaiKhoan = getActiveUserId(session);
        if (idTaiKhoan == null) {
            // Khách chưa đăng nhập: Trả về tin nhắn chào mừng mặc định của khách vãng lai
            MessageResponse welcome = new MessageResponse(
                    0L,
                    "BOT",
                    "🏸 Xin chào khách quý! Tôi là **Trợ lý ảo**.<br>Bạn vui lòng **đăng nhập** tài khoản để tôi có thể cá nhân hóa, hiển thị mã giảm giá và tra cứu đơn hàng riêng của bạn nhé! <br><br>"
                    + "Hiện tại tôi có thể hỗ trợ nhanh cho bạn:<br>"
                    + "• 🏸 *Xem các sản phẩm nổi bật* (gõ 'sản phẩm' hoặc 'vợt')<br>"
                    + "• 📞 *Thông tin cửa hàng & Hotline* (gõ 'liên hệ' hoặc 'địa chỉ')",
                    ""
            );
            return ResponseEntity.ok(Collections.singletonList(welcome));
        }

        KhachHang kh = getOrCreateKhachHang(idTaiKhoan);

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
            ChatService.BotResponseWrapper welcomeReply = chatService.generateBotResponse(conv.getId(), "chào");
            responses.add(new MessageResponse(
                    welcomeReply.getSavedMessage().getId(),
                    welcomeReply.getSavedMessage().getSenderType(),
                    welcomeReply.getSavedMessage().getNoiDung(),
                    welcomeReply.getSavedMessage().getThoiGian().format(TIME_FORMATTER),
                    welcomeReply.getProducts()
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
        String sanitizedText = org.jsoup.Jsoup.clean(userText, org.jsoup.safety.Safelist.none());
        if (sanitizedText.isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung tin nhắn trống sau khi làm sạch!");
        }
        if (sanitizedText.length() > 2000) {
            return ResponseEntity.badRequest().body("Nội dung tin nhắn không được vượt quá 2000 ký tự!");
        }

        Integer idTaiKhoan = getActiveUserId(session);

        if (idTaiKhoan == null) {
            // Khách vãng lai: Xử lý in-memory trực tiếp bằng AI nhưng không lưu database
            ChatService.BotResponseWrapper botReply = chatService.generateAIResponseForGuest(sanitizedText);
            List<MessageResponse> replies = new ArrayList<>();
            replies.add(new MessageResponse(0L, "USER", sanitizedText, ""));
            replies.add(new MessageResponse(0L, "BOT", botReply.getMessageText(), "", botReply.getProducts()));
            return ResponseEntity.ok(replies);
        }

        KhachHang kh = getOrCreateKhachHang(idTaiKhoan);

        ChatConversation conv = chatService.getOrCreateConversation(kh.getId());

        // 1. Lưu tin nhắn của User
        ChatMessage userMsg = chatService.saveUserMessage(conv.getId(), sanitizedText);
        // 2. Sinh và lưu tin nhắn phản hồi của Bot
        ChatService.BotResponseWrapper botReply = chatService.generateBotResponse(conv.getId(), sanitizedText);

        List<MessageResponse> replies = new ArrayList<>();
        replies.add(new MessageResponse(
                userMsg.getId(),
                userMsg.getSenderType(),
                userMsg.getNoiDung(),
                userMsg.getThoiGian().format(TIME_FORMATTER)
        ));
        replies.add(new MessageResponse(
                botReply.getSavedMessage().getId(),
                botReply.getSavedMessage().getSenderType(),
                botReply.getSavedMessage().getNoiDung(),
                botReply.getSavedMessage().getThoiGian().format(TIME_FORMATTER),
                botReply.getProducts()
        ));

        return ResponseEntity.ok(replies);
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackPayload payload, HttpSession session) {
        if (payload.getMessageId() == null) {
            return ResponseEntity.badRequest().body("Mã tin nhắn không được để trống!");
        }

        Integer idTaiKhoan = getActiveUserId(session);
        if (idTaiKhoan == null || payload.getMessageId() == 0L) {
            // Khách vãng lai hoặc tin nhắn ảo: Chỉ trả về OK
            return ResponseEntity.ok("Cảm ơn đóng góp của bạn!");
        }

        KhachHang currentKhachHang = getOrCreateKhachHang(idTaiKhoan);

        ChatMessage msg = chatMessageRepository.findById(payload.getMessageId()).orElse(null);
        if (msg == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy tin nhắn!");
        }

        if (msg.getConversation() == null || msg.getConversation().getKhachHang() == null
                || !msg.getConversation().getKhachHang().getId().equals(currentKhachHang.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền đánh giá tin nhắn này!");
        }

        String note = payload.getNote();
        String sanitizedNote = null;
        if (note != null) {
            String trimmed = note.trim();
            sanitizedNote = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
            if (sanitizedNote.length() > 500) {
                return ResponseEntity.badRequest().body("Ghi chú phản hồi không được vượt quá 500 ký tự!");
            }
        }

        ChatFeedback fb = chatService.saveFeedback(payload.getMessageId(), payload.isPositive(), sanitizedNote);
        return ResponseEntity.ok("Cập nhật đánh giá thành công! Cảm ơn đóng góp của bạn.");
    }
}
