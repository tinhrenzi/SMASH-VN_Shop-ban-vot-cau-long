package com.smashvn.shop.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.smashvn.shop.config.ShopContactProperties;
import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatProductResponse;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.dto.chatbot.ChatResponse;
import com.smashvn.shop.dto.chatbot.ChatbotProductSearchResponseDto;
import com.smashvn.shop.dto.chatbot.ProductSearchCriteria;
import com.smashvn.shop.dto.chatbot.ShopContactDto;
import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.SanPham;
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

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
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

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.chat.max-history-messages:5}")
    private int maxHistoryMessages;

    @Value("${gemini.chat.max-user-message-length:2000}")
    private int maxUserMessageLength;

    @Value("${gemini.chat.max-product-suggestions:5}")
    private int maxProductSuggestions;

    private String getHotline() {
        String phone = shopContactProperties.getPhone();
        if (phone == null || phone.isBlank()) {
            return "0981472035";
        }
        return phone.trim();
    }

    public static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    @Override
    @Transactional
    public ChatMessageDto sendMessage(ChatRequest request, Integer idTaiKhoan, String sessionId) {
        String rawMessage = request.getMessage();
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }
        if (rawMessage.length() > maxUserMessageLength) {
            throw new IllegalArgumentException("Tin nhắn quá dài (tối đa " + maxUserMessageLength + " ký tự).");
        }

        ChatConversation conversation = getOrCreateConversation(request.getConversationId(), idTaiKhoan, sessionId);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setVaiTro("USER");
        userMessage.setNoiDung(rawMessage.trim());
        userMessage.setTrangThai("SUCCESS");
        chatbotDbHelper.saveMessage(userMessage);

        String hotline = getHotline();
        ChatIntent intent = classifyQuestion(rawMessage);
        log.info("Classified intent: {}", intent);

        // Check vague / incomplete query first
        if (isIncompleteQuery(rawMessage)) {
            ChatMessage vagueMsg = new ChatMessage();
            vagueMsg.setConversation(conversation);
            vagueMsg.setVaiTro("ASSISTANT");
            vagueMsg.setNoiDung("Bạn muốn tìm sản phẩm theo tên hay theo khoảng giá? Ví dụ: “Tìm vợt Yonex dưới 2 triệu”.");
            vagueMsg.setTrangThai("SUCCESS");
            vagueMsg = chatbotDbHelper.saveMessage(vagueMsg);
            chatbotDbHelper.updateConversationTime(conversation.getId());
            return mapToDto(vagueMsg);
        }

        // Out of scope / security sensitive
        if (intent == ChatIntent.SECURITY_SENSITIVE || intent == ChatIntent.OUT_OF_SCOPE) {
            ChatMessage blockedMsg = new ChatMessage();
            blockedMsg.setConversation(conversation);
            blockedMsg.setVaiTro("ASSISTANT");
            blockedMsg.setNoiDung("Xin lỗi, nội dung này nằm ngoài phạm vi hỗ trợ của chatbot. Tôi chỉ có thể hỗ trợ tìm kiếm sản phẩm và cung cấp thông tin có trên website của cửa hàng. Bạn có thể liên hệ " + hotline + " để được nhân viên hỗ trợ thêm.");
            blockedMsg.setTrangThai("BLOCKED");
            blockedMsg = chatbotDbHelper.saveMessage(blockedMsg);

            chatbotDbHelper.updateConversationTime(conversation.getId());
            return mapToDto(blockedMsg);
        }

        // Advanced consultation / medical / technical play style advice
        if (intent == ChatIntent.ADVANCED_CONSULTATION || isPureMedicalQuery(rawMessage)) {
            ChatMessage medicalMsg = new ChatMessage();
            medicalMsg.setConversation(conversation);
            medicalMsg.setVaiTro("ASSISTANT");
            medicalMsg.setNoiDung("Tôi có thể hỗ trợ bạn tìm kiếm sản phẩm theo tên hoặc khoảng giá. Để được tư vấn sản phẩm phù hợp với trình độ và lối chơi, bạn vui lòng liên hệ số điện thoại " + hotline + " hoặc nhân viên chăm sóc khách hàng.");
            medicalMsg.setTrangThai("SUCCESS");
            medicalMsg = chatbotDbHelper.saveMessage(medicalMsg);

            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(medicalMsg);
            dto.setRequiresHumanSupport(true);
            dto.setContact(buildContactDto());
            return dto;
        }

        // Query product database
        ProductSearchCriteria criteria = extractSearchCriteria(rawMessage);
        ChatbotProductSearchResponseDto searchResult = executeProductSearch(criteria, 5);
        List<ChatProductResponse> suggestionDtos = searchResult.getProducts();

        if (suggestionDtos.isEmpty()) {
            ChatMessage noResult = new ChatMessage();
            noResult.setConversation(conversation);
            noResult.setVaiTro("ASSISTANT");
            noResult.setNoiDung("Tôi chưa tìm thấy sản phẩm phù hợp với yêu cầu của bạn. Bạn có thể thử nhập tên sản phẩm khác hoặc thay đổi khoảng giá.");
            noResult.setTrangThai("SUCCESS");
            noResult = chatbotDbHelper.saveMessage(noResult);
            chatbotDbHelper.updateConversationTime(conversation.getId());
            ChatMessageDto dto = mapToDto(noResult);
            dto.setSuggestedProducts(Collections.emptyList());
            return dto;
        }

        // Retrieve Chat History
        List<ChatMessage> dbMessages = chatMessageRepository.findAllByConversationId(conversation.getId());
        dbMessages.sort((m1, m2) -> {
            int dateComp = m2.getNgayTao().compareTo(m1.getNgayTao());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });
        List<ChatMessage> history = dbMessages.stream()
                .limit(Math.min(maxHistoryMessages, 5))
                .collect(Collectors.toList());
        Collections.reverse(history);

        // Call Gemini API
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

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setVaiTro("ASSISTANT");
        assistantMessage.setTenModel(model);
        assistantMessage.setThoiGianXuLyMs(duration);

        if (aiResponse != null) {
            String validatedResponse = validateGeminiResponse(aiResponse, suggestionDtos);
            assistantMessage.setNoiDung(validatedResponse);
            assistantMessage.setTrangThai("SUCCESS");
        } else {
            assistantMessage.setNoiDung("Tôi tìm thấy các sản phẩm phù hợp với yêu cầu của bạn:");
            assistantMessage.setTrangThai("FAILED");
            assistantMessage.setMaLoi(errorCode != null && errorCode.length() > 50 ? errorCode.substring(0, 50) : errorCode);
            if (errorMessage != null) {
                String safeErr = errorMessage.length() > 250 ? errorMessage.substring(0, 250) : errorMessage;
                assistantMessage.setNoiDungLoi(safeErr);
            }
        }

        assistantMessage = chatbotDbHelper.saveMessage(assistantMessage);
        chatbotDbHelper.updateConversationTime(conversation.getId());

        ChatMessageDto dto = mapToDto(assistantMessage);
        dto.setSuggestedProducts(suggestionDtos);
        return dto;
    }

    @Override
    public ChatbotProductSearchResponseDto searchProductsApi(String keyword, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, Integer limit) {
        int safeLimit = (limit == null) ? 5 : Math.min(Math.max(limit, 1), 5);

        // Swap minPrice and maxPrice if minPrice > maxPrice
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setKeyword(keyword != null ? keyword.trim() : null);
        criteria.setCategoryName(category != null ? category.trim() : null);
        criteria.setBrandName(brand != null ? brand.trim() : null);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);

        return executeProductSearch(criteria, safeLimit);
    }

    private ChatbotProductSearchResponseDto executeProductSearch(ProductSearchCriteria criteria, int limit) {
        List<SanPhamChiTiet> allActiveVariants = sanPhamChiTietRepository.findAllActiveInStock();

        String kw = criteria.getKeyword() != null ? removeAccents(criteria.getKeyword().toLowerCase()) : null;
        String kw2 = criteria.getKeyword2() != null ? removeAccents(criteria.getKeyword2().toLowerCase()) : null;
        String kw3 = criteria.getKeyword3() != null ? removeAccents(criteria.getKeyword3().toLowerCase()) : null;

        String brand = criteria.getBrandName() != null ? removeAccents(criteria.getBrandName().toLowerCase()).replace("-", "") : null;
        String cat = criteria.getCategoryName() != null ? removeAccents(criteria.getCategoryName().toLowerCase()) : null;

        BigDecimal minPrice = criteria.getMinPrice();
        BigDecimal maxPrice = criteria.getMaxPrice();

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        Map<Integer, SanPhamChiTiet> groupedByParentProduct = new LinkedHashMap<>();

        for (SanPhamChiTiet variant : allActiveVariants) {
            SanPham sp = variant.getSanPham();
            if (sp == null || !Boolean.TRUE.equals(sp.getTrangThaiValue()) || !Boolean.TRUE.equals(variant.getTrangThaiValue()) || variant.getSoLuongTon() <= 0) {
                continue;
            }

            // Calculate effective price
            BigDecimal basePrice = variant.getGiaBan();
            BigDecimal salePrice = sp.getGiaSauGiam(basePrice);
            boolean hasValidSale = salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice.compareTo(basePrice) < 0;
            BigDecimal effectivePrice = hasValidSale ? salePrice : basePrice;

            // Price filter check
            if (minPrice != null && effectivePrice.compareTo(minPrice) < 0) {
                continue;
            }
            if (maxPrice != null && effectivePrice.compareTo(maxPrice) > 0) {
                continue;
            }

            // Brand check
            if (brand != null && !brand.isEmpty()) {
                String spBrand = sp.getThuongHieu() != null ? removeAccents(sp.getThuongHieu().getTenThuongHieu().toLowerCase()).replace("-", "") : "";
                if (!spBrand.contains(brand)) {
                    continue;
                }
            }

            // Category check
            if (cat != null && !cat.isEmpty()) {
                String spCat = sp.getDanhMuc() != null ? removeAccents(sp.getDanhMuc().getTenDanhMuc().toLowerCase()) : "";
                if (!spCat.contains(cat)) {
                    continue;
                }
            }

            // Keyword check
            if (kw != null && !kw.isEmpty()) {
                String spName = removeAccents(sp.getTenSanPham().toLowerCase());
                String spDesc = sp.getMoTa() != null ? removeAccents(sp.getMoTa().toLowerCase()) : "";
                String spBrandStr = sp.getThuongHieu() != null ? removeAccents(sp.getThuongHieu().getTenThuongHieu().toLowerCase()) : "";
                String spCatStr = sp.getDanhMuc() != null ? removeAccents(sp.getDanhMuc().getTenDanhMuc().toLowerCase()) : "";

                boolean matches = spName.contains(kw) || spDesc.contains(kw) || spBrandStr.contains(kw) || spCatStr.contains(kw);
                if (!matches && kw2 != null && !kw2.isEmpty()) {
                    matches = spName.contains(kw2) || spDesc.contains(kw2);
                }
                if (!matches && kw3 != null && !kw3.isEmpty()) {
                    matches = spName.contains(kw3) || spDesc.contains(kw3);
                }

                if (!matches) {
                    continue;
                }
            }

            // Deduplicate by parent product ID
            if (!groupedByParentProduct.containsKey(sp.getId())) {
                groupedByParentProduct.put(sp.getId(), variant);
            }
        }

        long total = groupedByParentProduct.size();
        List<SanPhamChiTiet> selectedVariants = groupedByParentProduct.values().stream()
                .limit(limit)
                .toList();

        List<ChatProductResponse> productDtos = selectedVariants.stream()
                .map(this::mapToChatProductResponse)
                .collect(Collectors.toList());

        return ChatbotProductSearchResponseDto.builder()
                .success(true)
                .total(total)
                .displayed(productDtos.size())
                .products(productDtos)
                .build();
    }

    private ProductSearchCriteria extractSearchCriteria(String userPrompt) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        String promptLower = userPrompt.toLowerCase();

        List<String> keywords = extractProductKeywords(promptLower);
        if (!keywords.isEmpty()) {
            criteria.setKeyword(keywords.get(0));
            if (keywords.size() > 1) criteria.setKeyword2(keywords.get(1));
            if (keywords.size() > 2) criteria.setKeyword3(keywords.get(2));
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
        if (promptLower.contains("vợt") || promptLower.contains("vot")) {
            criteria.setCategoryName("Vợt");
        } else if (promptLower.contains("giày") || promptLower.contains("giay")) {
            criteria.setCategoryName("Giày");
        } else if (promptLower.contains("áo") || promptLower.contains("quần") || promptLower.contains("ao") || promptLower.contains("quan")) {
            criteria.setCategoryName("Trang phục");
        } else if (promptLower.contains("cầu") || promptLower.contains("cau")) {
            criteria.setCategoryName("Quả cầu lông");
        }

        // Parse prices
        BigDecimal parsedPrice = VietnamesePriceParser.parsePrice(userPrompt);

        // Range keywords check
        boolean hasRangeKeywords = promptLower.contains("dưới") || promptLower.contains("thấp hơn")
                || promptLower.contains("tối đa") || promptLower.contains("không quá")
                || promptLower.contains("trên") || promptLower.contains("hơn")
                || promptLower.contains("tối thiểu") || promptLower.contains("ít nhất")
                || promptLower.contains("từ") || promptLower.contains("đến") || promptLower.contains("tới")
                || promptLower.contains("khoảng");

        if (parsedPrice != null) {
            if (!hasRangeKeywords || promptLower.contains("khoảng")) {
                // Single price or "khoảng X" -> ±20%
                BigDecimal minP = parsedPrice.multiply(new BigDecimal("0.8")).setScale(0, RoundingMode.HALF_UP);
                BigDecimal maxP = parsedPrice.multiply(new BigDecimal("1.2")).setScale(0, RoundingMode.HALF_UP);
                criteria.setMinPrice(minP);
                criteria.setMaxPrice(maxP);
            } else {
                if (promptLower.contains("dưới") || promptLower.contains("thấp hơn") || promptLower.contains("tối đa")) {
                    criteria.setMaxPrice(parsedPrice);
                } else if (promptLower.contains("trên") || promptLower.contains("hơn") || promptLower.contains("tối thiểu")) {
                    criteria.setMinPrice(parsedPrice);
                }
            }
        }

        // Range "từ X đến Y"
        if (promptLower.contains("từ") && (promptLower.contains("đến") || promptLower.contains("tới"))) {
            Matcher rangeMatcher = Pattern.compile("(?i)từ\\s+([^đến]+?)\\s+(?:đến|tới)\\s+(.+)").matcher(userPrompt);
            if (rangeMatcher.find()) {
                BigDecimal minP = VietnamesePriceParser.parsePrice(rangeMatcher.group(1));
                BigDecimal maxP = VietnamesePriceParser.parsePrice(rangeMatcher.group(2));
                if (minP != null) criteria.setMinPrice(minP);
                if (maxP != null) criteria.setMaxPrice(maxP);
            }
        }

        if (criteria.getMinPrice() != null && criteria.getMaxPrice() != null
                && criteria.getMinPrice().compareTo(criteria.getMaxPrice()) > 0) {
            BigDecimal temp = criteria.getMinPrice();
            criteria.setMinPrice(criteria.getMaxPrice());
            criteria.setMaxPrice(temp);
        }

        return criteria;
    }

    private List<String> extractProductKeywords(String promptLower) {
        String unaccented = removeAccents(promptLower);
        Set<String> ignored = new java.util.HashSet<>(java.util.Arrays.asList(
                "toi", "muon", "can", "xin", "hay", "giup", "tim", "mua", "tu", "van", "cho", "minh", "san", "pham", "vot", "cau",
                "long", "giay", "ao", "quan", "phu", "kien", "gia", "duoi", "tren", "trieu", "yonex",
                "lining", "li-ning", "victor", "do", "xanh", "den", "trang", "vang", "hong", "cam",
                "3u", "4u", "5u", "hop", "con", "hang", "bao", "nhieu", "loai", "co", "khong",
                "mot", "chiec", "cay", "nao", "duoc", "voi", "va", "hoac", "nguoi", "moi", "choi", "tot", "khoang"));
        return java.util.Arrays.stream(unaccented.replaceAll("[^a-zA-Z0-9-]+", " ").trim().split("\\s+"))
                .filter(token -> token.length() > 1 && !ignored.contains(token) && !token.matches("\\d+(tr)?"))
                .distinct()
                .limit(3)
                .toList();
    }

    private boolean isIncompleteQuery(String message) {
        String msgLower = removeAccents(message.toLowerCase().trim());
        Set<String> vagueExact = Set.of(
                "toi muon mua vot", "muon mua vot", "tim san pham gia re",
                "tu van cho toi mot cay vot", "tu van vot", "tu van giay", "mua vot", "muon mua giay"
        );
        return vagueExact.contains(msgLower);
    }

    private ChatIntent classifyQuestion(String message) {
        String msgLower = removeAccents(message.toLowerCase());

        // 1. Security Sensitive
        if (msgLower.contains("api key") || msgLower.contains("system prompt")
                || msgLower.contains("co so du lieu") || msgLower.contains("database schema")
                || msgLower.contains("source code") || msgLower.contains("secret key")
                || msgLower.contains("mat khau admin") || msgLower.contains("du lieu he thong")) {
            return ChatIntent.SECURITY_SENSITIVE;
        }

        // 2. Out of Scope
        if (msgLower.contains("lap trinh") || msgLower.contains("java code") || msgLower.contains("code java")
                || msgLower.contains("viet code") || msgLower.contains("database") || msgLower.contains("sql server")
                || msgLower.contains("chinh tri") || msgLower.contains("thoi tiet")
                || msgLower.contains("tin tuc") || msgLower.contains("giai tri")
                || msgLower.contains("singing") || msgLower.contains("am nhac")
                || msgLower.contains("cong thuc nau an") || msgLower.contains("du bao thoi tiet")) {
            return ChatIntent.OUT_OF_SCOPE;
        }

        // 3. Advanced Consultation / Technical / Playing style
        if (msgLower.contains("chan thuong") || msgLower.contains("dau khop")
                || msgLower.contains("phuc hoi") || msgLower.contains("bac si")
                || msgLower.contains("dieu tri") || msgLower.contains("van dong vien")
                || msgLower.contains("chuyen nghiep") || msgLower.contains("luc co tay")
                || msgLower.contains("the trang") || msgLower.contains("ky thuat ca nhan")
                || msgLower.contains("suc cang chinh xac") || msgLower.contains("phu hop tuyet doi")
                || msgLower.contains("dau vai") || msgLower.contains("dau chan") || msgLower.contains("nang dau") || msgLower.contains("nhe dau")) {
            return ChatIntent.ADVANCED_CONSULTATION;
        }

        // 4. Store Information
        if (msgLower.contains("hotline") || msgLower.contains("dia chi")
                || msgLower.contains("so dien thoai") || msgLower.contains("email")
                || msgLower.contains("gio mo cua") || msgLower.contains("hoat dong")
                || msgLower.contains("lien he") || msgLower.contains("phong trung bay")
                || msgLower.contains("dia chi shop")) {
            return ChatIntent.STORE_INFORMATION;
        }

        // 5. Product search intents
        if (msgLower.contains("tim") || msgLower.contains("mua")
                || msgLower.contains("gia") || msgLower.contains("re")
                || msgLower.contains("bao nhieu") || msgLower.contains("con hang")
                || msgLower.contains("san hang") || msgLower.contains("vot")
                || msgLower.contains("giay") || msgLower.contains("k")) {
            return ChatIntent.PRODUCT_SEARCH;
        }

        if (msgLower.contains("chi tiet") || msgLower.contains("thong so")
                || msgLower.contains("chat lieu") || msgLower.contains("cau tao")) {
            return ChatIntent.PRODUCT_INFORMATION;
        }

        return ChatIntent.BASIC_CONSULTATION;
    }

    private boolean isPureMedicalQuery(String message) {
        String msgLower = removeAccents(message.toLowerCase());
        boolean hasMedicalKeywords = msgLower.contains("chan thuong") || msgLower.contains("dau khop")
                || msgLower.contains("phuc hoi") || msgLower.contains("bac si") || msgLower.contains("dieu tri")
                || msgLower.contains("dau vai") || msgLower.contains("dau co tay");

        boolean hasProductKeywords = msgLower.contains("vot") || msgLower.contains("giay")
                || msgLower.contains("ao") || msgLower.contains("phu kien") || msgLower.contains("yonex")
                || msgLower.contains("lining") || msgLower.contains("victor") || msgLower.contains("san pham");

        return hasMedicalKeywords && !hasProductKeywords;
    }

    private String callGeminiApi(List<ChatMessage> history, List<ChatProductResponse> suggestionDtos, ChatIntent intent) throws Exception {
        String apiUrl = baseUrl + "/chat/completions";

        List<Map<String, String>> messagesPayload = new ArrayList<>();

        StringBuilder systemPrompt = new StringBuilder("""
                Bạn là trợ lý tư vấn sản phẩm của SMASH.
                Quy tắc:
                - Chỉ sử dụng dữ liệu sản phẩm được hệ thống cung cấp.
                - Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu và tối đa 3 câu.
                - Không tự tạo tên, giá, tồn kho, hình ảnh hoặc đường dẫn.
                - Chỉ đề xuất tối đa 5 sản phẩm và chỉ nêu đặc điểm liên quan.
                - Khi không có dữ liệu phù hợp, thông báo rõ ràng.
                """);

        systemPrompt.append("\nDanh sách sản phẩm khớp từ hệ thống:\n");
        if (suggestionDtos.isEmpty()) {
            systemPrompt.append("(Không tìm thấy sản phẩm phù hợp. Nhắc khách thử nhập tên hoặc khoảng giá khác).\n");
        } else {
            for (ChatProductResponse prod : suggestionDtos) {
                systemPrompt.append("- ID: ").append(prod.getId())
                        .append(", Tên: ").append(prod.getName())
                        .append(", Giá: ").append(prod.getPrice().toPlainString()).append(" VND")
                        .append(prod.getSalePrice() != null ? ", Giá khuyến mãi: " + prod.getSalePrice().toPlainString() + " VND" : "")
                        .append("\n");
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
                new ParameterizedTypeReference<Map<String, Object>>() {});

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

        if (lowerText.contains("system prompt") || lowerText.contains("api_key")
                || lowerText.contains("api-key") || lowerText.contains("database schema")
                || lowerText.contains("select *")) {
            return "Xin lỗi, tôi chỉ hỗ trợ các nội dung liên quan đến sản phẩm và dịch vụ của SmashVN Shop.";
        }

        return responseText;
    }

    private ShopContactDto buildContactDto() {
        String addr = shopContactProperties.getAddress();
        String mail = shopContactProperties.getEmail();
        String ph = getHotline();

        ShopContactDto dto = new ShopContactDto();
        dto.setAddress(addr != null && !addr.trim().isEmpty() ? addr.trim() : null);
        dto.setEmail(mail != null && !mail.trim().isEmpty() ? mail.trim() : null);
        dto.setPhone(ph);
        return dto;
    }

    private ChatProductResponse mapToChatProductResponse(SanPhamChiTiet v) {
        SanPham sp = v.getSanPham();
        String hinhAnh = "/images/placeholder.png";
        if (v.getHinhAnhSanPhams() != null && !v.getHinhAnhSanPhams().isEmpty()) {
            com.smashvn.shop.entity.HinhAnhSanPham mainImage = v.getHinhAnhSanPhams().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getLaAnhChinh()))
                    .findFirst()
                    .orElse(v.getHinhAnhSanPhams().get(0));
            hinhAnh = normalizeProductImageUrl(mainImage.getUrlHinhAnh());
        }

        BigDecimal basePrice = v.getGiaBan();
        BigDecimal salePrice = sp.getGiaSauGiam(basePrice);
        boolean hasValidSale = salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice.compareTo(basePrice) < 0;

        String description = sp.getMoTa();
        if (description != null) {
            description = description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        }
        if (description != null && description.length() > 160) {
            description = description.substring(0, 157).trim() + "...";
        }

        String dynamicUrl = "/san-pham/" + sp.getId();

        return ChatProductResponse.builder()
                .id(sp.getId())
                .name(sp.getTenSanPham())
                .brand(sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : null)
                .price(basePrice)
                .salePrice(hasValidSale ? salePrice : null)
                .shortDescription(description)
                .imageUrl(hinhAnh)
                .productUrl(dynamicUrl)
                .detailUrl(dynamicUrl)
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

    @Override
    public List<ChatMessageDto> getConversationHistory(Long conversationId, Integer idTaiKhoan, String sessionId) {
        ChatConversation conversation;
        if (conversationId == null) {
            List<ChatConversation> activeConversations = idTaiKhoan != null
                    ? chatConversationRepository.findAllByTaiKhoanIdAndTrangThai(idTaiKhoan, "ACTIVE")
                    : chatConversationRepository.findAllBySessionIdAndTrangThai(sessionId, "ACTIVE");
            if (activeConversations.isEmpty()) {
                return Collections.emptyList();
            }
            conversation = activeConversations.get(0);
        } else {
            conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại."));
            verifyConversationOwnership(conversation, idTaiKhoan, sessionId);
        }

        List<ChatMessage> messages = chatMessageRepository.findAllByConversationId(conversation.getId());
        messages.sort((m1, m2) -> {
            int dateComp = m2.getNgayTao().compareTo(m1.getNgayTao());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });

        List<ChatMessage> limited = messages.stream().limit(50).collect(Collectors.toList());
        Collections.reverse(limited);

        return limited.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
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

        Optional<ChatFeedback> existingOpt = idTaiKhoan != null
                ? chatFeedbackRepository.findByMessageIdAndTaiKhoanId(message.getId(), idTaiKhoan)
                : chatFeedbackRepository.findByMessageIdAndSessionId(message.getId(), sessionId);

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

        List<ChatConversation> activeConversations = idTaiKhoan != null
                ? chatConversationRepository.findAllByTaiKhoanIdAndTrangThai(idTaiKhoan, "ACTIVE")
                : chatConversationRepository.findAllBySessionIdAndTrangThai(sessionId, "ACTIVE");

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
}
