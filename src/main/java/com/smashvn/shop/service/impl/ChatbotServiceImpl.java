package com.smashvn.shop.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.smashvn.shop.config.ShopContactProperties;
import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatProductResponse;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.dto.chatbot.ProductSearchCriteria;
import com.smashvn.shop.dto.chatbot.ShopContactDto;
import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.chatbot.ChatIntent;
import com.smashvn.shop.repository.ChatConversationRepository;
import com.smashvn.shop.repository.ChatFeedbackRepository;
import com.smashvn.shop.repository.ChatMessageRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.ChatbotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile(
            "(?:trên|hơn|tối\\s*thiểu|ít\\s*nhất|từ)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(chục\\s*)?(triệu|tr|nghìn|ngàn|k)\\b");
    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile(
            "(?:dưới|thấp\\s*hơn|tối\\s*đa|không\\s*quá|đến|tới)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(chục\\s*)?(triệu|tr|nghìn|ngàn|k)\\b");

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final ChatbotDbHelper chatbotDbHelper;
    private final ShopContactProperties shopContactProperties;

    @Qualifier("geminiRestTemplate")
    private final RestTemplate geminiRestTemplate;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String baseUrl;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.chat.max-history-messages:5}")
    private int maxHistoryMessages;

    @Value("${gemini.chat.max-user-message-length:2000}")
    private int maxUserMessageLength;

    @Value("${gemini.chat.max-product-suggestions:3}")
    private int maxProductSuggestions;

    @Override
    public ChatMessageDto sendMessage(ChatRequest request, Integer idTaiKhoan, String sessionId) {
        // 1. Validate Input Length
        String rawMessage = request.getMessage();
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }
        if (rawMessage.length() > maxUserMessageLength) {
            throw new IllegalArgumentException("Tin nhắn quá dài (tối đa " + maxUserMessageLength + " ký tự).");
        }

        // 2. Identify and retrieve Conversation
        ChatConversation conversation = getOrCreateConversation(request.getConversationId(), idTaiKhoan, sessionId);

        // 3. Save USER Message in database
        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setVaiTro("USER");
        userMessage.setNoiDung(rawMessage.trim());
        userMessage.setTrangThai("SUCCESS");
        chatbotDbHelper.saveMessage(userMessage);

        // 4. Classify Question / Intent
        ChatIntent intent = classifyQuestion(rawMessage);
        log.info("Classified intent: {}", intent);

        // 5. Handle intent security / out of scope / medical
        if (intent == ChatIntent.SECURITY_SENSITIVE || intent == ChatIntent.OUT_OF_SCOPE) {
            ChatMessage blockedMsg = new ChatMessage();
            blockedMsg.setConversation(conversation);
            blockedMsg.setVaiTro("ASSISTANT");
            blockedMsg.setNoiDung("Xin lỗi, tôi chỉ hỗ trợ các nội dung liên quan đến sản phẩm và dịch vụ của SmashVN Shop.");
            blockedMsg.setTrangThai("BLOCKED");
            blockedMsg = chatbotDbHelper.saveMessage(blockedMsg);

            chatbotDbHelper.updateConversationTime(conversation.getId());
            return mapToDto(blockedMsg);
        }

        boolean isPureMedical = isPureMedicalQuery(rawMessage);
        if (isPureMedical) {
            // Safe Java response directly, no Gemini API call
            ChatMessage medicalMsg = new ChatMessage();
            medicalMsg.setConversation(conversation);
            medicalMsg.setVaiTro("ASSISTANT");
            medicalMsg.setNoiDung("SmashVN Shop là kênh hỗ trợ tư vấn về sản phẩm. Đối với các vấn đề liên quan đến chấn thương hoặc sức khỏe cơ xương khớp, quý khách vui lòng tham khảo ý kiến của bác sĩ hoặc chuyên gia y tế để có chẩn đoán chính xác nhất. Cửa hàng chỉ hỗ trợ tư vấn dụng cụ tập luyện phù hợp khi bạn đã hồi phục.");
            medicalMsg.setTrangThai("SUCCESS");
            medicalMsg = chatbotDbHelper.saveMessage(medicalMsg);

            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(medicalMsg);
            dto.setRequiresHumanSupport(true);
            dto.setContact(buildContactDto());
            return dto;
        }

        // 6. Retrieve and Sort/Limit Chat History in Java
        List<ChatMessage> dbMessages = chatMessageRepository.findAllByConversationId(conversation.getId());
        dbMessages.sort((m1, m2) -> {
            int dateComp = m2.getNgayTao().compareTo(m1.getNgayTao());
            if (dateComp != 0) {
                return dateComp;
            }
            return m2.getId().compareTo(m1.getId());
        });
        List<ChatMessage> history = dbMessages.stream()
                .limit(Math.min(maxHistoryMessages, 5))
                .collect(Collectors.toList());
        Collections.reverse(history);

        // 7. Query and Filter Products in Java (Database-First)
        List<SanPhamChiTiet> suggestions = queryAndFilterProducts(rawMessage).stream()
                .filter(new java.util.function.Predicate<>() {
                    private final Set<Integer> seenProductIds = new java.util.HashSet<>();

                    @Override
                    public boolean test(SanPhamChiTiet variant) {
                        return seenProductIds.add(variant.getSanPham().getId());
                    }
                })
                .limit(3)
                .toList();
        List<ChatProductResponse> suggestionDtos = suggestions.stream()
                .map(this::mapToChatProductResponse)
                .collect(Collectors.toList());

        if ((intent == ChatIntent.PRODUCT_SEARCH || intent == ChatIntent.PRODUCT_INFORMATION)
                && suggestionDtos.isEmpty()) {
            ChatMessage noResult = new ChatMessage();
            noResult.setConversation(conversation);
            noResult.setVaiTro("ASSISTANT");
            noResult.setNoiDung("Hiện tại mình chưa tìm thấy sản phẩm phù hợp với yêu cầu của bạn.");
            noResult.setTrangThai("SUCCESS");
            noResult = chatbotDbHelper.saveMessage(noResult);
            chatbotDbHelper.updateConversationTime(conversation.getId());
            ChatMessageDto dto = mapToDto(noResult);
            dto.setSuggestedProducts(Collections.emptyList());
            return dto;
        }

        // 8. Call Gemini API
        String aiResponse = null;
        String errorCode = null;
        String errorMessage = null;
        long startTime = System.currentTimeMillis();

        try {
            aiResponse = callGeminiApi(history, suggestionDtos, intent);
        } catch (HttpStatusCodeException ex) {
            log.error("Gemini API HTTP Error status: {}", ex.getStatusCode());
            errorCode = String.valueOf(ex.getStatusCode().value());
            errorMessage = ex.getResponseBodyAsString();
        } catch (ResourceAccessException ex) {
            log.error("Gemini API Connect Timeout/Network Error: {}", ex.getMessage());
            errorCode = "TIMEOUT_OR_NETWORK";
            errorMessage = ex.getMessage();
        } catch (Exception ex) {
            log.error("Gemini API Unknown Error: {}", ex.getMessage());
            errorCode = "UNKNOWN_ERROR";
            errorMessage = ex.getMessage();
        }

        long duration = System.currentTimeMillis() - startTime;

        // 9. Save ASSISTANT/FAILED Message
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setVaiTro("ASSISTANT");
        assistantMessage.setTenModel(model);
        assistantMessage.setThoiGianXuLyMs(duration);

        if (aiResponse != null) {
            // Post-validate output to ensure safety
            String validatedResponse = validateGeminiResponse(aiResponse, suggestionDtos);
            assistantMessage.setNoiDung(validatedResponse);
            assistantMessage.setTrangThai("SUCCESS");
        } else {
            assistantMessage.setNoiDung(suggestionDtos.isEmpty()
                    ? "Chatbot hiện đang bận. Bạn vui lòng thử lại sau."
                    : "Mình tìm thấy một số sản phẩm phù hợp với yêu cầu của bạn:");
            assistantMessage.setTrangThai("FAILED");
            assistantMessage.setMaLoi(errorCode);
            assistantMessage.setNoiDungLoi(errorMessage);
        }

        assistantMessage = chatbotDbHelper.saveMessage(assistantMessage);
        chatbotDbHelper.updateConversationTime(conversation.getId());

        // Map response to DTO
        ChatMessageDto dto = mapToDto(assistantMessage);
        dto.setSuggestedProducts(suggestionDtos);

        // Set human support flags if intent is ADVANCED_CONSULTATION
        if (intent == ChatIntent.ADVANCED_CONSULTATION) {
            dto.setRequiresHumanSupport(true);
            dto.setContact(buildContactDto());
        }

        return dto;
    }

    @Override
    public List<ChatMessageDto> getConversationHistory(Long conversationId, Integer idTaiKhoan, String sessionId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }

        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại."));
        verifyConversationOwnership(conversation, idTaiKhoan, sessionId);

        List<ChatMessage> messages = chatMessageRepository.findAllByConversationId(conversationId);
        messages.sort((m1, m2) -> {
            int dateComp = m2.getNgayTao().compareTo(m1.getNgayTao());
            if (dateComp != 0) {
                return dateComp;
            }
            return m2.getId().compareTo(m1.getId());
        });

        List<ChatMessage> limited = messages.stream()
                .limit(50)
                .collect(Collectors.toList());
        Collections.reverse(limited);

        return limited.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public void submitFeedback(ChatFeedbackRequest request, Integer idTaiKhoan, String sessionId) {
        ChatMessage message = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại."));

        if (!"ASSISTANT".equalsIgnoreCase(message.getVaiTro())) {
            throw new IllegalArgumentException("Chỉ được đánh giá tin nhắn phản hồi của trợ lý ảo.");
        }
        verifyConversationOwnership(message.getConversation(), idTaiKhoan, sessionId);

        Integer rating = request.getRating();
        if (rating == null || (rating != 1 && rating != -1)) {
            throw new IllegalArgumentException("Đánh giá không hợp lệ. Chỉ chấp nhận 1 hoặc -1.");
        }

        Optional<ChatFeedback> existingOpt;
        if (idTaiKhoan != null) {
            existingOpt = chatFeedbackRepository.findByMessageIdAndTaiKhoanId(message.getId(), idTaiKhoan);
        } else {
            existingOpt = chatFeedbackRepository.findByMessageIdAndSessionId(message.getId(), sessionId);
        }

        ChatFeedback feedback;
        if (existingOpt.isPresent()) {
            feedback = existingOpt.get();
            feedback.setDanhGia(rating.shortValue());
            feedback.setGhiChu(request.getNote());
            feedback.setNgayCapNhat(LocalDateTime.now());
        } else {
            feedback = new ChatFeedback();
            feedback.setMessage(message);
            if (idTaiKhoan != null) {
                feedback.setTaiKhoan(taiKhoanRepository.findById(idTaiKhoan).orElse(null));
            } else {
                feedback.setSessionId(sessionId);
            }
            feedback.setDanhGia(rating.shortValue());
            feedback.setGhiChu(request.getNote());
        }

        chatbotDbHelper.saveFeedback(feedback);
    }

    private ChatConversation getOrCreateConversation(Long conversationId, Integer idTaiKhoan, String sessionId) {
        if (conversationId != null) {
            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại."));
            verifyConversationOwnership(conversation, idTaiKhoan, sessionId);
            return conversation;
        }

        List<ChatConversation> activeConversations;
        if (idTaiKhoan != null) {
            activeConversations = chatConversationRepository.findAllByTaiKhoanIdAndTrangThai(idTaiKhoan, "ACTIVE");
        } else {
            activeConversations = chatConversationRepository.findAllBySessionIdAndTrangThai(sessionId, "ACTIVE");
        }

        if (!activeConversations.isEmpty()) {
            activeConversations.sort((c1, c2) -> {
                LocalDateTime t1 = c1.getNgayCapNhat() != null ? c1.getNgayCapNhat() : c1.getNgayTao();
                LocalDateTime t2 = c2.getNgayCapNhat() != null ? c2.getNgayCapNhat() : c2.getNgayTao();
                int dateComp = t2.compareTo(t1);
                if (dateComp != 0) {
                    return dateComp;
                }
                return c2.getId().compareTo(c1.getId());
            });
            return activeConversations.get(0);
        }

        ChatConversation newConversation = new ChatConversation();
        newConversation.setTieuDe("Hội thoại tư vấn");
        if (idTaiKhoan != null) {
            newConversation.setTaiKhoan(taiKhoanRepository.findById(idTaiKhoan).orElse(null));
        } else {
            newConversation.setSessionId(sessionId);
        }
        return chatbotDbHelper.saveConversation(newConversation);
    }

    private void verifyConversationOwnership(ChatConversation conversation, Integer idTaiKhoan, String sessionId) {
        if (idTaiKhoan != null) {
            if (conversation.getTaiKhoan() == null || !idTaiKhoan.equals(conversation.getTaiKhoan().getId())) {
                throw new IllegalArgumentException("Bạn không có quyền truy cập cuộc trò chuyện này.");
            }
        } else {
            if (conversation.getSessionId() == null || !conversation.getSessionId().equals(sessionId)) {
                throw new IllegalArgumentException("Bạn không có quyền truy cập cuộc trò chuyện này.");
            }
        }
    }

    private ChatIntent classifyQuestion(String message) {
        String msgLower = message.toLowerCase();

        // 1. Security Sensitive
        if (msgLower.contains("api key") || msgLower.contains("system prompt")
                || msgLower.contains("cơ sở dữ liệu") || msgLower.contains("database schema")
                || msgLower.contains("source code") || msgLower.contains("secret key")
                || msgLower.contains("mật khẩu admin") || msgLower.contains("dữ liệu hệ thống")) {
            return ChatIntent.SECURITY_SENSITIVE;
        }

        // 2. Out of Scope
        if (msgLower.contains("lập trình") || msgLower.contains("java code") || msgLower.contains("code java")
                || msgLower.contains("viết code") || msgLower.contains("database") || msgLower.contains("sql server")
                || msgLower.contains("chính trị") || msgLower.contains("thời tiết")
                || msgLower.contains("tin tức") || msgLower.contains("giải trí")
                || msgLower.contains("singing") || msgLower.contains("âm nhạc")
                || msgLower.contains("công thức nấu ăn") || msgLower.contains("dự báo thời tiết")) {
            return ChatIntent.OUT_OF_SCOPE;
        }

        // 3. Advanced Consultation
        if (msgLower.contains("chấn thương") || msgLower.contains("đau khớp")
                || msgLower.contains("phục hồi") || msgLower.contains("bác sĩ")
                || msgLower.contains("điều trị") || msgLower.contains("vận động viên")
                || msgLower.contains("chuyên nghiệp") || msgLower.contains("lực cổ tay")
                || msgLower.contains("thể trạng") || msgLower.contains("kỹ thuật cá nhân")
                || msgLower.contains("sức căng chính xác") || msgLower.contains("phù hợp tuyệt đối")
                || msgLower.contains("đau vai") || msgLower.contains("đau chân") || msgLower.contains("đau khớp")) {
            return ChatIntent.ADVANCED_CONSULTATION;
        }

        // 4. Store Information
        if (msgLower.contains("hotline") || msgLower.contains("địa chỉ")
                || msgLower.contains("số điện thoại") || msgLower.contains("email")
                || msgLower.contains("giờ mở cửa") || msgLower.contains("hoạt động")
                || msgLower.contains("liên hệ") || msgLower.contains("phòng trưng bày")
                || msgLower.contains("địa chỉ shop")) {
            return ChatIntent.STORE_INFORMATION;
        }

        // 5. Product search intents
        if (msgLower.contains("tìm") || msgLower.contains("mua")
                || msgLower.contains("giá") || msgLower.contains("rẻ")
                || msgLower.contains("bao nhiêu") || msgLower.contains("còn hàng")
                || msgLower.contains("sẵn hàng") || msgLower.contains("vợt")
                || msgLower.contains("giày")) {
            return ChatIntent.PRODUCT_SEARCH;
        }

        if (msgLower.contains("chi tiết") || msgLower.contains("thông số")
                || msgLower.contains("chất liệu") || msgLower.contains("cấu tạo")) {
            return ChatIntent.PRODUCT_INFORMATION;
        }

        return ChatIntent.BASIC_CONSULTATION;
    }

    private boolean isPureMedicalQuery(String message) {
        String msgLower = message.toLowerCase();
        boolean hasMedicalKeywords = msgLower.contains("chấn thương") || msgLower.contains("đau khớp")
                || msgLower.contains("phục hồi") || msgLower.contains("bác sĩ") || msgLower.contains("điều trị")
                || msgLower.contains("đau vai") || msgLower.contains("đau khớp") || msgLower.contains("đau cổ tay");

        boolean hasProductKeywords = msgLower.contains("vợt") || msgLower.contains("giày")
                || msgLower.contains("áo") || msgLower.contains("phụ kiện") || msgLower.contains("yonex")
                || msgLower.contains("lining") || msgLower.contains("victor") || msgLower.contains("sản phẩm");

        return hasMedicalKeywords && !hasProductKeywords;
    }

    private List<SanPhamChiTiet> queryAndFilterProducts(String userPrompt) {
        try {
            ProductSearchCriteria criteria = extractSearchCriteria(userPrompt);
            List<SanPhamChiTiet> results = sanPhamChiTietRepository.searchForChatbot(
                    criteria.getKeyword(),
                    criteria.getKeyword2(),
                    criteria.getKeyword3(),
                    criteria.getBrandName(),
                    criteria.getCategoryName(),
                    criteria.getMinPrice(),
                    criteria.getMaxPrice(),
                    criteria.getColor(),
                    criteria.getWeight(),
                    PageRequest.of(0, Math.min(maxProductSuggestions, 3)));
            log.debug("Chatbot database lookup returned {} product variants", results.size());
            return results;
        } catch (Exception ex) {
            log.error("Chatbot database lookup failed", ex);
            return Collections.emptyList();
        }
    }

    private ProductSearchCriteria extractSearchCriteria(String userPrompt) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        String promptLower = userPrompt.toLowerCase();

        List<String> keywords = extractProductKeywords(promptLower);
        if (!keywords.isEmpty()) {
            criteria.setKeyword(keywords.get(0));
            criteria.setKeyword2(keywords.size() > 1 ? keywords.get(1) : keywords.get(0));
            criteria.setKeyword3(keywords.size() > 2 ? keywords.get(2) : keywords.get(0));
        }

        // Brands
        if (promptLower.contains("yonex")) {
            criteria.setBrandName("Yonex");
        } else if (promptLower.contains("lining") || promptLower.contains("li-ning") || promptLower.contains("li ning")) {
            criteria.setBrandName("Lining");
        } else if (promptLower.contains("victor")) {
            criteria.setBrandName("Victor");
        }

        // Categories
        if (promptLower.contains("vợt")) {
            criteria.setCategoryName("Vợt");
        } else if (promptLower.contains("giày")) {
            criteria.setCategoryName("Giày");
        } else if (promptLower.contains("áo") || promptLower.contains("quần")) {
            criteria.setCategoryName("Trang phục");
        }

        // Weights
        if (promptLower.contains("3u")) {
            criteria.setWeight("3u");
        } else if (promptLower.contains("4u")) {
            criteria.setWeight("4u");
        } else if (promptLower.contains("5u")) {
            criteria.setWeight("5u");
        }

        criteria.setMinPrice(extractPriceBound(promptLower, MIN_PRICE_PATTERN));
        criteria.setMaxPrice(extractPriceBound(promptLower, MAX_PRICE_PATTERN));

        // Common colour filters are deliberately extracted on the backend so
        // Gemini cannot invent a colour that is not present in the database.
        for (String color : List.of("đỏ", "xanh", "đen", "trắng", "vàng", "hồng", "tím", "cam")) {
            if (promptLower.contains(color)) {
                criteria.setColor(color);
                break;
            }
        }

        return criteria;
    }

    private BigDecimal extractPriceBound(String promptLower, Pattern pattern) {
        Matcher matcher = pattern.matcher(promptLower);
        if (!matcher.find()) {
            return null;
        }

        BigDecimal value = new BigDecimal(matcher.group(1).replace(',', '.'));
        if (matcher.group(2) != null) {
            value = value.multiply(BigDecimal.TEN);
        }

        String unit = matcher.group(3);
        BigDecimal amount;
        if ("triệu".equals(unit) || "tr".equals(unit)) {
            amount = value.multiply(new BigDecimal("1000000"));
        } else {
            amount = value.multiply(new BigDecimal("1000"));
        }
        String matchedExpression = matcher.group().trim();
        if (pattern == MIN_PRICE_PATTERN
                && (matchedExpression.startsWith("trên") || matchedExpression.startsWith("hơn"))) {
            return amount.add(BigDecimal.ONE);
        }
        return amount;
    }

    private List<String> extractProductKeywords(String promptLower) {
        Set<String> ignored = Set.of("tôi", "muốn", "cần", "xin", "hãy", "giúp", "tìm", "mua", "tư", "vấn", "cho", "mình", "sản", "phẩm", "vợt", "cầu",
                "lông", "giày", "áo", "quần", "phụ", "kiện", "giá", "dưới", "trên", "triệu", "yonex",
                "lining", "li-ning", "victor", "đỏ", "xanh", "đen", "trắng", "vàng", "hồng", "tím", "cam",
                "3u", "4u", "5u", "phù", "hợp", "còn", "hàng", "bao", "nhiêu", "loại", "có", "không",
                "một", "chiếc", "cây", "nào", "được", "với", "và", "hoặc", "người", "mới", "chơi", "tốt");
        return java.util.Arrays.stream(promptLower.replaceAll("[^\\p{L}\\p{N}-]+", " ").trim().split("\\s+"))
                .filter(token -> token.length() > 1 && !ignored.contains(token) && !token.matches("\\d+(tr)?"))
                .distinct()
                .limit(3)
                .toList();
    }

    private String callGeminiApi(List<ChatMessage> history, List<ChatProductResponse> suggestionDtos, ChatIntent intent) throws Exception {
        String apiUrl = baseUrl + "/chat/completions";

        List<Map<String, String>> messagesPayload = new ArrayList<>();

        // System prompt aligned to strict guidelines
        StringBuilder systemPrompt = new StringBuilder("""
                Bạn là trợ lý tư vấn sản phẩm của SMASH.
                Quy tắc:
                - Chỉ sử dụng dữ liệu sản phẩm được hệ thống cung cấp.
                - Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu và tối đa 3 câu.
                - Không tự tạo tên, giá, tồn kho, hình ảnh hoặc đường dẫn.
                - Chỉ đề xuất tối đa 3 sản phẩm và chỉ nêu đặc điểm liên quan.
                - Không lặp lại toàn bộ thông tin sản phẩm, không tạo HTML.
                - Khi không có dữ liệu phù hợp, nói rõ là chưa tìm thấy.
                """);

        if (intent == ChatIntent.ADVANCED_CONSULTATION) {
            systemPrompt.append("Lưu ý đặc biệt: Khách hàng đang hỏi tư vấn chuyên sâu/kỹ thuật/y tế. Bạn KHÔNG được đưa ra quyết định mức căng chính xác và KHÔNG được khẳng định sản phẩm phù hợp tuyệt đối. Hãy chỉ diễn giải thông số thật từ database cơ bản và khuyên khách hàng liên hệ nhân viên hoặc đến trực tiếp cửa hàng để được hỗ trợ chuyên sâu nhất. ");
        }

        systemPrompt.append("\nDanh sách sản phẩm khớp từ hệ thống:\n");
        if (suggestionDtos.isEmpty()) {
            systemPrompt.append("(Không tìm thấy sản phẩm phù hợp trong dữ liệu của shop. Vui lòng thông báo rõ với khách và gợi ý đổi tiêu chí tìm kiếm).\n");
        } else {
            for (ChatProductResponse prod : suggestionDtos) {
                systemPrompt.append("- ID: ").append(prod.getId())
                        .append(", Tên: ").append(prod.getName())
                        .append(", Thương hiệu: ").append(prod.getBrand())
                        .append(", Giá: ").append(prod.getPrice().toPlainString()).append(" VND")
                        .append(", Mô tả: ").append(prod.getShortDescription()).append("\n");
            }
        }

        messagesPayload.add(Map.of("role", "system", "content", systemPrompt.toString()));

        for (ChatMessage msg : history) {
            String apiRole = "user";
            if ("ASSISTANT".equalsIgnoreCase(msg.getVaiTro())) {
                apiRole = "assistant";
            } else if ("SYSTEM".equalsIgnoreCase(msg.getVaiTro())) {
                apiRole = "system";
            }
            messagesPayload.add(Map.of("role", apiRole, "content", msg.getNoiDung()));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messagesPayload);
        requestBody.put("max_tokens", 220);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = geminiRestTemplate.exchange(apiUrl, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
        });

        if (response != null && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<?> choices = (List<?>) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }

        throw new RuntimeException("Cấu trúc phản hồi từ Gemini không hợp lệ.");
    }

    private String validateGeminiResponse(String responseText, List<ChatProductResponse> suggestionDtos) {
        String lowerText = responseText.toLowerCase();

        // 1. Check system leakage
        if (lowerText.contains("system prompt") || lowerText.contains("api_key")
                || lowerText.contains("api-key") || lowerText.contains("database schema")
                || lowerText.contains("bảng") || lowerText.contains("cột") || lowerText.contains("select *")) {
            return "Xin lỗi, tôi chỉ hỗ trợ các nội dung liên quan đến sản phẩm và dịch vụ của SmashVN Shop.";
        }

        // 2. Check phone/email leaks to align with dynamic configuration
        if (responseText.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")
                || responseText.matches(".*\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b.*")) {
            // Strip any raw details and redirect to UI fields
            return responseText.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "")
                    .replaceAll("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b", "")
                    .trim() + " Vui lòng xem thông tin liên hệ được đính kèm ở khung bên dưới.";
        }

        return responseText;
    }

    private ShopContactDto buildContactDto() {
        String addr = shopContactProperties.getAddress();
        String mail = shopContactProperties.getEmail();
        String ph = shopContactProperties.getPhone();

        if ((addr == null || addr.trim().isEmpty())
                && (mail == null || mail.trim().isEmpty())
                && (ph == null || ph.trim().isEmpty())) {
            return null;
        }

        ShopContactDto dto = new ShopContactDto();
        dto.setAddress(addr != null && !addr.trim().isEmpty() ? addr.trim() : null);
        dto.setEmail(mail != null && !mail.trim().isEmpty() ? mail.trim() : null);
        dto.setPhone(ph != null && !ph.trim().isEmpty() ? ph.trim() : null);
        return dto;
    }

    private ChatProductResponse mapToChatProductResponse(SanPhamChiTiet v) {
        String hinhAnh = "/images/placeholder.png";
        if (v.getHinhAnhSanPhams() != null && !v.getHinhAnhSanPhams().isEmpty()) {
            com.smashvn.shop.entity.HinhAnhSanPham mainImage = v.getHinhAnhSanPhams().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getLaAnhChinh()))
                    .findFirst()
                    .orElse(v.getHinhAnhSanPhams().get(0));
            hinhAnh = normalizeProductImageUrl(mainImage.getUrlHinhAnh());
        }
        String description = v.getSanPham().getMoTa();
        if (description != null) {
            description = description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        }
        if (description != null && description.length() > 160) {
            description = description.substring(0, 157).trim() + "...";
        }
        return ChatProductResponse.builder()
                .id(v.getSanPham().getId())
                .name(v.getSanPham().getTenSanPham())
                .brand(v.getSanPham().getThuongHieu() != null ? v.getSanPham().getThuongHieu().getTenThuongHieu() : null)
                .price(v.getGiaBan())
                .shortDescription(description)
                .imageUrl(hinhAnh)
                .productUrl("/san-pham/" + v.getSanPham().getId())
                .build();
    }

    private String normalizeProductImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "/images/placeholder.png";
        }
        String path = storedPath.trim().replace('\\', '/');
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/uploads/")) {
            return path;
        }
        if (path.startsWith("uploads/")) {
            return "/" + path;
        }
        if (path.startsWith("product/")) {
            return "/uploads/" + path;
        }
        return "/uploads/product/" + path;
    }

    private ChatMessageDto mapToDto(ChatMessage m) {
        String roleStr = m.getVaiTro();
        String senderType = "USER";
        if ("ASSISTANT".equalsIgnoreCase(roleStr)) {
            senderType = "BOT";
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = m.getNgayTao() != null ? m.getNgayTao().format(dtf) : "";

        return ChatMessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .role(roleStr)
                .senderType(senderType)
                .content(m.getNoiDung())
                .createdAt(m.getNgayTao())
                .thoiGian(formattedTime)
                .status(m.getTrangThai())
                .build();
    }
}
