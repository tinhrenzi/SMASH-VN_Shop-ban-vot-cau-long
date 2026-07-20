package com.smashvn.shop.service.impl;

import com.smashvn.shop.config.ShopContactProperties;
import com.smashvn.shop.dto.chatbot.*;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.entity.chatbot.ChatIntent;
import com.smashvn.shop.repository.ChatConversationRepository;
import com.smashvn.shop.repository.ChatMessageRepository;
import com.smashvn.shop.repository.ChatFeedbackRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.ChatbotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

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

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.chat.max-history-messages:20}")
    private int maxHistoryMessages;

    @Value("${gemini.chat.max-user-message-length:2000}")
    private int maxUserMessageLength;

    @Value("${gemini.chat.max-product-suggestions:5}")
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
        userMessage = chatbotDbHelper.saveMessage(userMessage);

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
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });
        List<ChatMessage> history = dbMessages.stream()
                .limit(maxHistoryMessages)
                .collect(Collectors.toList());
        Collections.reverse(history);

        // 7. Query and Filter Products in Java (Database-First)
        List<SanPhamChiTiet> suggestions = queryAndFilterProducts(rawMessage);
        List<ProductSuggestionDto> suggestionDtos = suggestions.stream()
                .map(this::mapToProductSuggestionDto)
                .collect(Collectors.toList());

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
            assistantMessage.setNoiDung("Xin lỗi, trợ lý ảo đang gặp sự cố kết nối. Quý khách vui lòng thử lại sau giây lát!");
            assistantMessage.setTrangThai("FAILED");
            assistantMessage.setMaLoi(errorCode);
            assistantMessage.setNoiDungLoi(errorMessage);
        }

        assistantMessage = chatbotDbHelper.saveMessage(assistantMessage);
        chatbotDbHelper.updateConversationTime(conversation.getId());

        // Map response to DTO
        ChatMessageDto dto = mapToDto(assistantMessage);
        if ("SUCCESS".equals(assistantMessage.getTrangThai())) {
            dto.setSuggestedProducts(suggestionDtos);
        } else {
            dto.setSuggestedProducts(Collections.emptyList());
        }

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
            if (dateComp != 0) return dateComp;
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
                if (dateComp != 0) return dateComp;
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
        if (msgLower.contains("api key") || msgLower.contains("system prompt") || 
            msgLower.contains("cơ sở dữ liệu") || msgLower.contains("database schema") || 
            msgLower.contains("source code") || msgLower.contains("secret key") ||
            msgLower.contains("mật khẩu admin") || msgLower.contains("dữ liệu hệ thống")) {
            return ChatIntent.SECURITY_SENSITIVE;
        }

        // 2. Out of Scope
        if (msgLower.contains("lập trình") || msgLower.contains("java code") || 
            msgLower.contains("chính trị") || msgLower.contains("thời tiết") || 
            msgLower.contains("tin tức") || msgLower.contains("giải trí") || 
            msgLower.contains("singing") || msgLower.contains("âm nhạc") ||
            msgLower.contains("công thức nấu ăn") || msgLower.contains("dự báo thời tiết")) {
            return ChatIntent.OUT_OF_SCOPE;
        }

        // 3. Advanced Consultation
        if (msgLower.contains("chấn thương") || msgLower.contains("đau khớp") || 
            msgLower.contains("phục hồi") || msgLower.contains("bác sĩ") || 
            msgLower.contains("điều trị") || msgLower.contains("vận động viên") || 
            msgLower.contains("chuyên nghiệp") || msgLower.contains("lực cổ tay") || 
            msgLower.contains("thể trạng") || msgLower.contains("kỹ thuật cá nhân") || 
            msgLower.contains("sức căng chính xác") || msgLower.contains("phù hợp tuyệt đối") || 
            msgLower.contains("đau vai") || msgLower.contains("đau chân") || msgLower.contains("đau khớp")) {
            return ChatIntent.ADVANCED_CONSULTATION;
        }

        // 4. Store Information
        if (msgLower.contains("hotline") || msgLower.contains("địa chỉ") || 
            msgLower.contains("số điện thoại") || msgLower.contains("email") || 
            msgLower.contains("giờ mở cửa") || msgLower.contains("hoạt động") || 
            msgLower.contains("liên hệ") || msgLower.contains("phòng trưng bày") ||
            msgLower.contains("địa chỉ shop")) {
            return ChatIntent.STORE_INFORMATION;
        }

        // 5. Product search intents
        if (msgLower.contains("tìm") || msgLower.contains("mua") || 
            msgLower.contains("giá") || msgLower.contains("rẻ") || 
            msgLower.contains("bao nhiêu") || msgLower.contains("còn hàng") || 
            msgLower.contains("sẵn hàng") || msgLower.contains("vợt") || 
            msgLower.contains("giày")) {
            return ChatIntent.PRODUCT_SEARCH;
        }

        if (msgLower.contains("chi tiết") || msgLower.contains("thông số") ||
            msgLower.contains("chất liệu") || msgLower.contains("cấu tạo")) {
            return ChatIntent.PRODUCT_INFORMATION;
        }

        return ChatIntent.BASIC_CONSULTATION;
    }

    private boolean isPureMedicalQuery(String message) {
        String msgLower = message.toLowerCase();
        boolean hasMedicalKeywords = msgLower.contains("chấn thương") || msgLower.contains("đau khớp") ||
                msgLower.contains("phục hồi") || msgLower.contains("bác sĩ") || msgLower.contains("điều trị") ||
                msgLower.contains("đau vai") || msgLower.contains("đau khớp") || msgLower.contains("đau cổ tay");
        
        boolean hasProductKeywords = msgLower.contains("vợt") || msgLower.contains("giày") ||
                msgLower.contains("áo") || msgLower.contains("phụ kiện") || msgLower.contains("yonex") ||
                msgLower.contains("lining") || msgLower.contains("victor") || msgLower.contains("sản phẩm");

        return hasMedicalKeywords && !hasProductKeywords;
    }

    private List<SanPhamChiTiet> queryAndFilterProducts(String userPrompt) {
        try {
            // Retrieve only active variants in stock
            List<SanPhamChiTiet> activeVariants = sanPhamChiTietRepository.findAllActiveInStock();

            // Extract search criteria
            ProductSearchCriteria criteria = extractSearchCriteria(userPrompt);

            // Filter in Java
            String promptLower = userPrompt.toLowerCase();
            return activeVariants.stream()
                    .filter(v -> {
                        if (criteria.getBrandName() != null) {
                            String brand = v.getSanPham().getThuongHieu() != null ? v.getSanPham().getThuongHieu().getTenThuongHieu().toLowerCase() : "";
                            if (!brand.contains(criteria.getBrandName().toLowerCase())) return false;
                        }
                        if (criteria.getCategoryName() != null) {
                            String cat = v.getSanPham().getDanhMuc() != null ? v.getSanPham().getDanhMuc().getTenDanhMuc().toLowerCase() : "";
                            if (!cat.contains(criteria.getCategoryName().toLowerCase())) return false;
                        }
                        if (criteria.getMaxPrice() != null) {
                            if (v.getGiaBan().compareTo(criteria.getMaxPrice()) > 0) return false;
                        }
                        if (criteria.getMinPrice() != null) {
                            if (v.getGiaBan().compareTo(criteria.getMinPrice()) < 0) return false;
                        }
                        if (criteria.getColor() != null) {
                            String color = v.getMauSac() != null ? v.getMauSac().toLowerCase() : "";
                            if (!color.contains(criteria.getColor().toLowerCase())) return false;
                        }
                        if (criteria.getWeight() != null) {
                            String weight = v.getTrongLuong() != null ? v.getTrongLuong().toLowerCase() : "";
                            if (!weight.contains(criteria.getWeight().toLowerCase())) return false;
                        }

                        // Keyword match
                        if (criteria.getKeyword() != null) {
                            String name = v.getSanPham().getTenSanPham().toLowerCase();
                            if (!name.contains(criteria.getKeyword().toLowerCase())) return false;
                        }

                        return true;
                    })
                    .limit(maxProductSuggestions)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error querying and filtering products: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private ProductSearchCriteria extractSearchCriteria(String userPrompt) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        String promptLower = userPrompt.toLowerCase();

        // Brands
        if (promptLower.contains("yonex")) criteria.setBrandName("Yonex");
        else if (promptLower.contains("lining")) criteria.setBrandName("Lining");
        else if (promptLower.contains("victor")) criteria.setBrandName("Victor");

        // Categories
        if (promptLower.contains("vợt")) criteria.setCategoryName("Vợt");
        else if (promptLower.contains("giày")) criteria.setCategoryName("Giày");
        else if (promptLower.contains("áo") || promptLower.contains("quần")) criteria.setCategoryName("Trang phục");

        // Weights
        if (promptLower.contains("3u")) criteria.setWeight("3u");
        else if (promptLower.contains("4u")) criteria.setWeight("4u");
        else if (promptLower.contains("5u")) criteria.setWeight("5u");

        // Prices
        if (promptLower.contains("dưới 1 triệu") || promptLower.contains("dưới 1tr")) {
            criteria.setMaxPrice(new BigDecimal("1000000"));
        } else if (promptLower.contains("dưới 2 triệu") || promptLower.contains("dưới 2tr")) {
            criteria.setMaxPrice(new BigDecimal("2000000"));
        }

        return criteria;
    }

    private String callGeminiApi(List<ChatMessage> history, List<ProductSuggestionDto> suggestionDtos, ChatIntent intent) throws Exception {
        String apiUrl = baseUrl + "/chat/completions";

        List<Map<String, String>> messagesPayload = new ArrayList<>();

        // System prompt aligned to strict guidelines
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("Bạn là trợ lý ảo nội bộ của SmashVN Shop. ")
                .append("Nhiệm vụ duy nhất của bạn là hỗ trợ khách hàng tìm hiểu sản phẩm đang có trong hệ thống của shop, giải thích thông tin sản phẩm, gợi ý sản phẩm ở mức cơ bản, và hỗ trợ thông tin mua hàng/liên hệ cửa hàng. ")
                .append("Bạn CHỈ được sử dụng dữ liệu sản phẩm, giá bán, tồn kho, thuộc tính được backend cung cấp trong context dưới đây. ")
                .append("Tuyệt đối KHÔNG tự bịa tên sản phẩm, giá bán, tồn kho, thông số kỹ thuật hoặc thông tin chính sách của shop. ")
                .append("Không được tự tạo card HTML hay layout cho sản phẩm (vì frontend sẽ tự hiển thị thông qua DTO gửi kèm). ")
                .append("Tuyệt đối KHÔNG viết số điện thoại, email hoặc địa chỉ cửa hàng trực tiếp vào văn bản câu trả lời (vì frontend sẽ tự động hiển thị khối liên lạc này từ DTO). ");

        if (intent == ChatIntent.ADVANCED_CONSULTATION) {
            systemPrompt.append("Lưu ý đặc biệt: Khách hàng đang hỏi tư vấn chuyên sâu/kỹ thuật/y tế. Bạn KHÔNG được đưa ra quyết định mức căng chính xác và KHÔNG được khẳng định sản phẩm phù hợp tuyệt đối. Hãy chỉ diễn giải thông số thật từ database cơ bản và khuyên khách hàng liên hệ nhân viên hoặc đến trực tiếp cửa hàng để được hỗ trợ chuyên sâu nhất. ");
        }

        systemPrompt.append("\nDanh sách sản phẩm khớp từ hệ thống:\n");
        if (suggestionDtos.isEmpty()) {
            systemPrompt.append("(Không tìm thấy sản phẩm phù hợp trong dữ liệu của shop. Vui lòng thông báo rõ với khách và gợi ý đổi tiêu chí tìm kiếm).\n");
        } else {
            for (ProductSuggestionDto prod : suggestionDtos) {
                systemPrompt.append("- ID: ").append(prod.getId())
                        .append(", Tên: ").append(prod.getTenSanPham())
                        .append(", Thương hiệu: ").append(prod.getThuongHieu())
                        .append(", Màu sắc: ").append(prod.getMauSac())
                        .append(", Trọng lượng: ").append(prod.getTrongLuong())
                        .append(", Giá: ").append(prod.getGiaBan().toPlainString()).append(" VND")
                        .append(", Tồn kho: ").append(prod.getSoLuongTon()).append("\n");
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

        int attempt = 0;
        int maxAttempts = 3;
        long delay = 1000;
        ResponseEntity<Map> response = null;

        while (attempt < maxAttempts) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                response = geminiRestTemplate.postForEntity(apiUrl, entity, Map.class);
                break;
            } catch (HttpStatusCodeException ex) {
                int status = ex.getStatusCode().value();
                if (status == 429 || status == 502 || status == 503 || status == 504) {
                    attempt++;
                    if (attempt >= maxAttempts) throw ex;
                    Thread.sleep(delay);
                    delay *= 2;
                } else {
                    throw ex;
                }
            } catch (ResourceAccessException ex) {
                attempt++;
                if (attempt >= maxAttempts) throw ex;
                Thread.sleep(delay);
                delay *= 2;
            }
        }

        if (response != null && response.getBody() != null) {
            Map body = response.getBody();
            List choices = (List) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map firstChoice = (Map) choices.get(0);
                Map message = (Map) firstChoice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }

        throw new RuntimeException("Cấu trúc phản hồi từ Gemini không hợp lệ.");
    }

    private String validateGeminiResponse(String responseText, List<ProductSuggestionDto> suggestionDtos) {
        String lowerText = responseText.toLowerCase();

        // 1. Check system leakage
        if (lowerText.contains("system prompt") || lowerText.contains("api_key") || 
            lowerText.contains("api-key") || lowerText.contains("database schema") ||
            lowerText.contains("bảng") || lowerText.contains("cột") || lowerText.contains("select *")) {
            return "Xin lỗi, tôi chỉ hỗ trợ các nội dung liên quan đến sản phẩm và dịch vụ của SmashVN Shop.";
        }

        // 2. Check phone/email leaks to align with dynamic configuration
        if (responseText.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*") || 
            responseText.matches(".*\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b.*")) {
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

        if ((addr == null || addr.trim().isEmpty()) &&
            (mail == null || mail.trim().isEmpty()) &&
            (ph == null || ph.trim().isEmpty())) {
            return null;
        }

        ShopContactDto dto = new ShopContactDto();
        dto.setAddress(addr != null && !addr.trim().isEmpty() ? addr.trim() : null);
        dto.setEmail(mail != null && !mail.trim().isEmpty() ? mail.trim() : null);
        dto.setPhone(ph != null && !ph.trim().isEmpty() ? ph.trim() : null);
        return dto;
    }

    private ProductSuggestionDto mapToProductSuggestionDto(SanPhamChiTiet v) {
        String hinhAnh = null;
        if (v.getHinhAnhSanPhams() != null && !v.getHinhAnhSanPhams().isEmpty()) {
            hinhAnh = "/uploads/" + v.getHinhAnhSanPhams().get(0).getUrlHinhAnh();
        }

        return ProductSuggestionDto.builder()
                .id(v.getId())
                .tenSanPham(v.getSanPham().getTenSanPham())
                .thuongHieu(v.getSanPham().getThuongHieu() != null ? v.getSanPham().getThuongHieu().getTenThuongHieu() : "N/A")
                .mauSac(v.getMauSac())
                .trongLuong(v.getTrongLuong())
                .giaBan(v.getGiaBan())
                .soLuongTon(v.getSoLuongTon())
                .hinhAnh(hinhAnh)
                .duongDan("/san-pham/" + v.getSanPham().getId())
                .build();
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
