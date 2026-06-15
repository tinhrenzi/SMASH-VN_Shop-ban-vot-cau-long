package com.smashvn.shop.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.dto.shipping.GhnOrderCreateRequestDTO;
import com.smashvn.shop.dto.shipping.GhnShipFeeRequestDTO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.SoDiaChi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.text.Normalizer;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    private final GhnConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String resolvedShopId = null;

    private static final String API_FEE = "/shiip/public-api/v2/shipping-order/fee";
    private static final String API_CREATE = "/shiip/public-api/v2/shipping-order/create";
    private static final String API_DETAIL = "/shiip/public-api/v2/shipping-order/detail";
    private static final String API_DISTRICT = "/shiip/public-api/master-data/district";
    private static final String API_WARD = "/shiip/public-api/master-data/ward";
    private static final String API_PROVINCE = "/shiip/public-api/master-data/province";

    private DonViVanChuyen getGhnCarrier() {
        try {
            return donViVanChuyenDAO.findAll().stream()
                    .filter(dv -> dv.getTenDonVi() != null && 
                            (dv.getTenDonVi().toUpperCase().contains("GIAO HÀNG NHANH") || 
                             dv.getTenDonVi().toUpperCase().contains("GHN")))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to fetch GHN carrier from DB: {}", e.getMessage());
            return null;
        }
    }

    private String getGhnToken() {
        DonViVanChuyen ghn = getGhnCarrier();
        if (ghn != null && ghn.getToken() != null && !ghn.getToken().isBlank()) {
            return ghn.getToken().trim();
        }
        return ghnConfig.getToken();
    }

    private String getGhnShopId() {
        if (resolvedShopId != null) {
            return resolvedShopId;
        }
        DonViVanChuyen ghn = getGhnCarrier();
        String configuredId = null;
        if (ghn != null && ghn.getClientId() != null && !ghn.getClientId().isBlank()) {
            configuredId = ghn.getClientId().trim();
        } else {
            configuredId = String.valueOf(ghnConfig.getShopId());
        }
        resolvedShopId = resolveShopId(configuredId, getGhnToken());
        return resolvedShopId;
    }

    private String resolveShopId(String configuredId, String token) {
        if (configuredId == null || configuredId.isBlank()) {
            return String.valueOf(ghnConfig.getShopId());
        }
        configuredId = configuredId.trim();
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shop/all";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("shops") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shops = (List<Map<String, Object>>) data.get("shops");
                    
                    // 1. If configuredId is a valid Shop ID, return it.
                    for (Map<String, Object> shop : shops) {
                        Object idObj = shop.get("_id");
                        if (idObj != null && String.valueOf(idObj).equals(configuredId)) {
                            return configuredId;
                        }
                    }
                    
                    // 2. If it is a Client ID, match the shop in district 1639 / ward 120125.
                    Integer fromDistrictId = ghnConfig.getFromDistrictId();
                    String fromWardCode = ghnConfig.getFromWardCode();
                    for (Map<String, Object> shop : shops) {
                        Object distObj = shop.get("district_id");
                        Object wardObj = shop.get("ward_code");
                        if (distObj != null && wardObj != null) {
                            if (String.valueOf(distObj).equals(String.valueOf(fromDistrictId)) && 
                                String.valueOf(wardObj).equals(fromWardCode)) {
                                return String.valueOf(shop.get("_id"));
                            }
                        }
                    }
                    
                    // 3. Fallback to first shop in list
                    if (!shops.isEmpty()) {
                        return String.valueOf(shops.get(0).get("_id"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve shop ID: {}", e.getMessage());
        }
        return configuredId;
    }

    private String findFallbackHanoiShop(String token) {
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shop/all";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("shops") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shops = (List<Map<String, Object>>) data.get("shops");
                    
                    // Look for Ba Dinh shop (Hanoi Shop ID 200610 or district 1484)
                    for (Map<String, Object> shop : shops) {
                        Object nameObj = shop.get("name");
                        Object addrObj = shop.get("address");
                        Object distObj = shop.get("district_id");
                        
                        String name = nameObj != null ? nameObj.toString().toLowerCase() : "";
                        String addr = addrObj != null ? addrObj.toString().toLowerCase() : "";
                        String dist = distObj != null ? distObj.toString() : "";
                        
                        if (name.contains("hanoi") || name.contains("hà nội") || 
                            addr.contains("hanoi") || addr.contains("hà nội") || 
                            "1484".equals(dist)) {
                            return String.valueOf(shop.get("_id"));
                        }
                    }
                    
                    if (!shops.isEmpty()) {
                        return String.valueOf(shops.get(0).get("_id"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find fallback Hanoi shop: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Tạo HttpHeaders với token GHN và shop ID (dùng cho tạo đơn và tra cứu đơn)
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", getGhnToken());
        headers.set("ShopId", getGhnShopId());
        return headers;
    }

    /**
     * Tạo HttpHeaders chỉ có token (dùng cho API tính phí và master data)
     */
    private HttpHeaders buildSimpleHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", getGhnToken());
        return headers;
    }

    /**
     * Tính phí ship từ kho đến địa chỉ người nhận
     *
     * @param toDistrictId  district_id GHN của người nhận
     * @param toWardCode    ward_code GHN của người nhận
     * @param insuranceValue giá trị bảo hiểm (tổng giá trị hàng)
     * @return phí ship (VND), null nếu lỗi
     */
    public BigDecimal calculateShipFee(Integer toDistrictId, String toWardCode, Integer insuranceValue) {
        try {
            GhnShipFeeRequestDTO req = new GhnShipFeeRequestDTO();
            req.setFromDistrictId(ghnConfig.getFromDistrictId());
            req.setFromWardCode(ghnConfig.getFromWardCode());
            req.setToDistrictId(toDistrictId);
            req.setToWardCode(toWardCode);
            req.setInsuranceValue(insuranceValue);

            String url = ghnConfig.getBaseUrl() + API_FEE;
            HttpEntity<GhnShipFeeRequestDTO> request = new HttpEntity<>(req, buildSimpleHeaders());

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});

            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("total") != null) {
                    return new BigDecimal(data.get("total").toString());
                }
            }
            log.warn("GHN calculateShipFee failed: code={}, message={}", code, response.get("message"));
        } catch (Exception e) {
            log.error("GHN calculateShipFee error: {}", e.getMessage());
        }
        // Fallback: 30,000đ
        return new BigDecimal("30000");
    }

    /**
     * Tạo đơn vận chuyển trên GHN sau khi đặt hàng thành công
     *
     * @param hoaDon HoaDon entity đã được lưu
     * @param items  Danh sách HoaDonChiTiet
     * @param toDistrictId district_id người nhận
     * @param toWardCode   ward_code người nhận
     * @return order_code GHN (mã vận đơn), null nếu lỗi
     */
    public String createShippingOrder(HoaDon hoaDon, List<HoaDonChiTiet> items,
                                      Integer toDistrictId, String toWardCode) {
        try {
            return createShippingOrderOrThrow(hoaDon, items, toDistrictId, toWardCode);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("GHN createShippingOrder HTTP error for HoaDon #{}: {} - {}", hoaDon.getId(), e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("GHN createShippingOrder error for HoaDon #{}: {}", hoaDon.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Tạo đơn vận chuyển trên GHN và ném ngoại lệ nếu có lỗi (để Admin controller hiển thị lỗi)
     */
    private Map<String, Object> getShopDetails(String shopId, String token) {
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shop/all";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("shops") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shops = (List<Map<String, Object>>) data.get("shops");
                    for (Map<String, Object> shop : shops) {
                        Object idObj = shop.get("_id");
                        if (idObj != null && String.valueOf(idObj).equals(shopId)) {
                            return shop;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch shop details from GHN: {}", e.getMessage());
        }
        return null;
    }

    public String createShippingOrderOrThrow(HoaDon hoaDon, List<HoaDonChiTiet> items,
                                             Integer toDistrictId, String toWardCode) throws Exception {
        String shopIdStr = getGhnShopId();
        String tokenStr = getGhnToken();
        try {
            return executeCreateOrderCall(shopIdStr, tokenStr, hoaDon, items, toDistrictId, toWardCode);
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("SERVER_ERR_COMMON") || e.getMessage().contains("không lấy được thông tin kho"))) {
                log.warn("GHN: Primary Shop ID {} failed with SERVER_ERR_COMMON. Attempting fallback to Hanoi Shop if available...", shopIdStr);
                String fallbackShopId = findFallbackHanoiShop(tokenStr);
                if (fallbackShopId != null && !fallbackShopId.equals(shopIdStr)) {
                    log.info("GHN: Retrying order creation with fallback Hanoi Shop ID {}", fallbackShopId);
                    try {
                        return executeCreateOrderCall(fallbackShopId, tokenStr, hoaDon, items, toDistrictId, toWardCode);
                    } catch (Exception ex) {
                        log.error("GHN: Fallback to Hanoi Shop failed: {}", ex.getMessage());
                    }
                }
            }
            throw e;
        }
    }

    private String executeCreateOrderCall(String shopId, String token, HoaDon hoaDon, List<HoaDonChiTiet> items,
                                          Integer toDistrictId, String toWardCode) throws Exception {
        GhnOrderCreateRequestDTO req = new GhnOrderCreateRequestDTO();
        
        req.setFrom_name("SmashVN Shop");
        req.setFrom_phone("0835420088");
        
        Map<String, Object> shopDetails = getShopDetails(shopId, token);
        if (shopDetails != null) {
            req.setFrom_address(shopDetails.get("address") != null ? shopDetails.get("address").toString() : ghnConfig.getFromAddress());
            req.setFrom_district_id(shopDetails.get("district_id") != null ? ((Number) shopDetails.get("district_id")).intValue() : ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(shopDetails.get("ward_code") != null ? shopDetails.get("ward_code").toString() : ghnConfig.getFromWardCode());
        } else {
            req.setFrom_address(ghnConfig.getFromAddress());
            req.setFrom_district_id(ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(ghnConfig.getFromWardCode());
        }

        req.setTo_name(hoaDon.getSdtNhan());
        req.setTo_phone(hoaDon.getSdtNhan());
        req.setTo_address(hoaDon.getDiaChiNhan());
        req.setTo_district_id(toDistrictId);
        req.setTo_ward_code(toWardCode);

        // COD amount = 0 nếu đã thanh toán online
        boolean isOnlinePaid = "PAID".equals(hoaDon.getPaymentStatus()) || "ZaloPay".equalsIgnoreCase(hoaDon.getPaymentMethod());
        req.setCod_amount(isOnlinePaid ? 0 : hoaDon.getTongTien().intValue());

        req.setNote(hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "");

        // Build items
        List<GhnOrderCreateRequestDTO.GhnItemDTO> ghnItems = new ArrayList<>();
        for (HoaDonChiTiet ct : items) {
            GhnOrderCreateRequestDTO.GhnItemDTO item = new GhnOrderCreateRequestDTO.GhnItemDTO();
            item.setName(ct.getSanPhamChiTiet().getSanPham().getTenSanPham());
            item.setCode("SP-" + ct.getSanPhamChiTiet().getId());
            item.setQuantity(ct.getSoLuong());
            item.setPrice(ct.getDonGia().intValue());
            ghnItems.add(item);
        }
        req.setItems(ghnItems);

        String url = ghnConfig.getBaseUrl() + API_CREATE;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        
        HttpEntity<GhnOrderCreateRequestDTO> request = new HttpEntity<>(req, headers);
        
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String orderCode = (String) data.get("order_code");
                    log.info("GHN: Created shipping order {} for HoaDon #{} using Shop ID {}", orderCode, hoaDon.getId(), shopId);
                    return orderCode;
                }
            }
            String msg = response.get("message") != null ? response.get("message").toString() : "Không rõ lý do";
            throw new RuntimeException("GHN từ chối tạo đơn: " + msg);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            try {
                Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
                String msg = (String) response.get("message");
                if (msg != null && !msg.isBlank()) {
                    throw new RuntimeException("GHN lỗi: " + msg);
                }
            } catch (Exception jsonEx) {
                // ignore
            }
            throw new RuntimeException("Lỗi kết nối GHN (" + e.getStatusCode() + "): " + body);
        }
    }

    /**
     * Tra cứu trạng thái vận đơn GHN
     *
     * @param orderCode mã vận đơn GHN
     * @return Map chứa status, estimated_delivery_time, logs, v.v.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> trackOrder(String orderCode) {
        try {
            Map<String, Object> body = Map.of("order_code", orderCode);
            String url = ghnConfig.getBaseUrl() + API_DETAIL;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                return (Map<String, Object>) response.get("data");
            }
            log.warn("GHN trackOrder failed: code={}, message={}", code, response.get("message"));
        } catch (Exception e) {
            log.error("GHN trackOrder error for {}: {}", orderCode, e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách Tỉnh/Thành phố
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProvinces() {
        try {
            String url = ghnConfig.getBaseUrl() + API_PROVINCE;
            HttpEntity<Void> request = new HttpEntity<>(buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                return (List<Map<String, Object>>) response.get("data");
            }
        } catch (Exception e) {
            log.error("GHN getProvinces error: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Lấy danh sách Quận/Huyện theo tỉnh
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDistricts(Integer provinceId) {
        try {
            Map<String, Object> body = Map.of("province_id", provinceId);
            String url = ghnConfig.getBaseUrl() + API_DISTRICT;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                return (List<Map<String, Object>>) response.get("data");
            }
        } catch (Exception e) {
            log.error("GHN getDistricts error: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Lấy danh sách Phường/Xã theo quận/huyện
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getWards(Integer districtId) {
        try {
            Map<String, Object> body = Map.of("district_id", districtId);
            String url = ghnConfig.getBaseUrl() + API_WARD;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                return (List<Map<String, Object>>) response.get("data");
            }
        } catch (Exception e) {
            log.error("GHN getWards error: {}", e.getMessage());
        }
        return List.of();
    }

    @Data
    @AllArgsConstructor
    public static class GhnAddressMapping {
        private Integer provinceId;
        private Integer districtId;
        private String wardCode;
    }

    public String normalizeString(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.toLowerCase();
        normalized = normalized.replace("đ", "d");
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    public GhnAddressMapping resolveGhnAddress(SoDiaChi dc) {
        if (dc == null) return null;

        // 1. Fetch provinces and match dc.getTinhThanh()
        List<Map<String, Object>> provinces = getProvinces();
        String targetProvince = normalizeString(dc.getTinhThanh());
        Integer provinceId = null;
        for (Map<String, Object> p : provinces) {
            String name = normalizeString((String) p.get("ProvinceName"));
            if (!name.isEmpty() && (name.contains(targetProvince) || targetProvince.contains(name))) {
                provinceId = (Integer) p.get("ProvinceID");
                break;
            }
        }
        if (provinceId == null) {
            log.warn("Failed to match province '{}' on GHN", dc.getTinhThanh());
            return null;
        }

        // 2. Fetch districts and match dc.getThanhPho()
        List<Map<String, Object>> districts = getDistricts(provinceId);
        String targetDistrict = normalizeString(dc.getThanhPho());
        Integer districtId = null;
        for (Map<String, Object> d : districts) {
            String name = normalizeString((String) d.get("DistrictName"));
            if (!name.isEmpty() && (name.contains(targetDistrict) || targetDistrict.contains(name))) {
                districtId = (Integer) d.get("DistrictID");
                break;
            }
        }
        if (districtId == null) {
            log.warn("Failed to match district '{}' in province ID {} on GHN", dc.getThanhPho(), provinceId);
            return null;
        }

        // 3. Fetch wards and match within dc.getDiaChiCuThe()
        List<Map<String, Object>> wards = getWards(districtId);
        String targetStreet = normalizeString(dc.getDiaChiCuThe());
        String wardCode = null;
        for (Map<String, Object> w : wards) {
            String name = normalizeString((String) w.get("WardName"));
            if (!name.isEmpty() && targetStreet.contains(name)) {
                wardCode = (String) w.get("WardCode");
                break;
            }
        }
        if (wardCode == null) {
            log.warn("Failed to match ward inside street address '{}' for district ID {} on GHN", dc.getDiaChiCuThe(), districtId);
            return null;
        }

        return new GhnAddressMapping(provinceId, districtId, wardCode);
    }
}

