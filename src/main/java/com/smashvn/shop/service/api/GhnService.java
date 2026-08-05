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
import com.smashvn.shop.repository.SoDiaChiRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    private final GhnConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final SoDiaChiRepository soDiaChiRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String resolvedShopId = null;

    private static final String API_FEE = "/shiip/public-api/v2/shipping-order/fee";
    private static final String API_CREATE = "/shiip/public-api/v2/shipping-order/create";
    private static final String API_AVAILABLE_SERVICES = "/shiip/public-api/v2/shipping-order/available-services";
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
            GhnServiceInfo serviceInfo = resolveAvailableService(getGhnShopId(), ghnConfig.getFromDistrictId(), toDistrictId);
            GhnShipFeeRequestDTO req = new GhnShipFeeRequestDTO();
            req.setFromDistrictId(ghnConfig.getFromDistrictId());
            req.setFromWardCode(ghnConfig.getFromWardCode());
            req.setToDistrictId(toDistrictId);
            req.setToWardCode(toWardCode);
            req.setInsuranceValue(insuranceValue);
            req.setServiceId(serviceInfo.getServiceId());
            req.setServiceTypeId(serviceInfo.getServiceTypeId());

            String url = ghnConfig.getBaseUrl() + API_FEE;
            HttpEntity<GhnShipFeeRequestDTO> request = new HttpEntity<>(req, buildHeaders());

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

    @SuppressWarnings("unchecked")
    private GhnServiceInfo resolveAvailableService(String shopId, Integer fromDistrictId, Integer toDistrictId) {
        if (fromDistrictId == null || toDistrictId == null) {
            throw new IllegalArgumentException("Thiếu quận/huyện gửi hoặc nhận để kiểm tra dịch vụ GHN.");
        }

        try {
            Map<String, Object> body = Map.of(
                    "shop_id", Integer.valueOf(shopId),
                    "from_district", fromDistrictId,
                    "to_district", toDistrictId
            );
            String url = ghnConfig.getBaseUrl() + API_AVAILABLE_SERVICES;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = response.get("code") instanceof Number ? ((Number) response.get("code")).intValue() : null;
            if (code != null && code == 200) {
                List<Map<String, Object>> services = (List<Map<String, Object>>) response.get("data");
                if (services != null && !services.isEmpty()) {
                    Map<String, Object> selected = services.stream()
                            .filter(s -> intValue(s.get("service_type_id")) != null && intValue(s.get("service_type_id")) == 2)
                            .findFirst()
                            .orElse(services.get(0));
                    Integer serviceId = intValue(selected.get("service_id"));
                    Integer serviceTypeId = intValue(selected.get("service_type_id"));
                    if (serviceId != null || serviceTypeId != null) {
                        log.debug("GHN service selected for route {} -> {}: service_id={}, service_type_id={}",
                                fromDistrictId, toDistrictId, serviceId, serviceTypeId);
                        return new GhnServiceInfo(serviceId, serviceTypeId != null ? serviceTypeId : 2);
                    }
                }
            }

            String msg = response.get("message") != null ? response.get("message").toString() : "Không có dịch vụ khả dụng";
            throw new IllegalArgumentException("GHN chưa hỗ trợ tuyến giao hàng này: " + msg);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String msg = e.getResponseBodyAsString();
            try {
                Map<String, Object> response = objectMapper.readValue(msg, new TypeReference<>() {});
                if (response.get("message") != null) {
                    msg = response.get("message").toString();
                }
            } catch (Exception ignored) {
                // Keep raw response body.
            }
            throw new IllegalArgumentException("GHN chưa hỗ trợ tuyến giao hàng này: " + msg);
        } catch (Exception e) {
            log.warn("GHN available-services lookup failed for shop {}, route {} -> {}: {}", shopId, fromDistrictId, toDistrictId, e.getMessage());
            return new GhnServiceInfo(null, 2);
        }
    }

    private Integer intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
        if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
            if (hoaDon.getDiaChi() != null && hoaDon.getDiaChi().getDistrictId() != null && hoaDon.getDiaChi().getWardCode() != null) {
                toDistrictId = hoaDon.getDiaChi().getDistrictId();
                toWardCode = hoaDon.getDiaChi().getWardCode();
            } else if (hoaDon.getKhachHang() != null) {
                try {
                    List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(hoaDon.getKhachHang().getId());
                    if (addresses != null) {
                        for (SoDiaChi sdc : addresses) {
                            if (sdc.getSdtNguoiNhan() != null 
                                    && sdc.getSdtNguoiNhan().trim().equalsIgnoreCase(hoaDon.getSdtNhan().trim())
                                    && sdc.getGhnDistrictId() != null 
                                    && sdc.getGhnWardCode() != null) {
                                toDistrictId = sdc.getGhnDistrictId();
                                toWardCode = sdc.getGhnWardCode();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve district/ward from SoDiaChi for GHN order: {}", e.getMessage());
                }
            }
        }
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

        String receiverName = null;
        if (hoaDon.getKhachHang() != null) {
            try {
                List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(hoaDon.getKhachHang().getId());
                if (addresses != null) {
                    for (SoDiaChi sdc : addresses) {
                        if (sdc.getSdtNguoiNhan() != null && sdc.getSdtNguoiNhan().trim().equals(hoaDon.getSdtNhan().trim())) {
                            receiverName = (sdc.getHoNguoiNhan() + " " + sdc.getTenNguoiNhan()).trim();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to lookup receiver name from SoDiaChi: {}", e.getMessage());
            }
            if (receiverName == null || receiverName.isEmpty()) {
                receiverName = (hoaDon.getKhachHang().getHoKh() + " " + hoaDon.getKhachHang().getTenKh()).trim();
            }
        }
        if (receiverName == null || receiverName.isEmpty()) {
            receiverName = "Khách hàng";
        }

        req.setTo_name(receiverName);
        req.setTo_phone(hoaDon.getSdtNhan());
        req.setTo_address(hoaDon.getDiaChiNhan());
        req.setTo_district_id(toDistrictId);
        req.setTo_ward_code(toWardCode);
        GhnServiceInfo serviceInfo = resolveAvailableService(shopId, req.getFrom_district_id(), toDistrictId);
        req.setService_id(serviceInfo.getServiceId());
        req.setService_type_id(serviceInfo.getServiceTypeId());

        // COD amount = 0 nếu đã thanh toán online
        boolean isOnlinePaid = "PAID".equals(hoaDon.getPaymentStatus()) || "ZaloPay".equalsIgnoreCase(hoaDon.getPaymentMethod());
        req.setCod_amount(isOnlinePaid ? 0 : hoaDon.getTongTien().intValue());
        req.setInsurance_value(hoaDon.getTongTien().intValue());

        req.setNote(hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "");

        // Tính toán tổng số lượng và thiết lập kích thước/trọng lượng động cho gói hàng
        int totalQty = items.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum();
        req.setWeight(totalQty * 500); // 500g mỗi vợt (kèm bao và hộp đóng gói)
        req.setHeight(Math.min(150, 10 + (totalQty - 1) * 2)); // Tăng chiều cao hộp khi gửi nhiều cây vợt

        // Build items
        List<GhnOrderCreateRequestDTO.GhnItemDTO> ghnItems = new ArrayList<>();
        for (HoaDonChiTiet ct : items) {
            GhnOrderCreateRequestDTO.GhnItemDTO item = new GhnOrderCreateRequestDTO.GhnItemDTO();
            String classification = ct.getSanPhamChiTiet().getPhanLoaiHienThi();
            String detailName = ct.getSanPhamChiTiet().getSanPham().getTenSanPham() + (!classification.isEmpty() ? " [" + classification + "]" : "");
            item.setName(detailName);
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
                    try {
                        jdbcTemplate.update(
                            "MERGE INTO TichHopVanChuyen WITH (HOLDLOCK) AS target " +
                            "USING (SELECT ? AS id_hoa_don, ? AS ma_van_don, ? AS trang_thai) AS source " +
                            "ON target.id_hoa_don = source.id_hoa_don " +
                            "WHEN MATCHED THEN UPDATE SET ma_van_don = COALESCE(source.ma_van_don, target.ma_van_don), " +
                            "                             ma_don_hang_ngoai = COALESCE(source.ma_van_don, target.ma_don_hang_ngoai), " +
                            "                             trang_thai = COALESCE(source.trang_thai, target.trang_thai) " +
                            "WHEN NOT MATCHED THEN INSERT (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) " +
                            "VALUES (source.id_hoa_don, 'GHN', source.ma_van_don, source.ma_van_don, source.trang_thai, GETDATE());",
                            hoaDon.getId(), orderCode, "ready_to_pick"
                        );
                        log.info("GHN: Successfully saved shipping mapping in TichHopVanChuyen for HoaDon #{}", hoaDon.getId());
                    } catch (Exception dbEx) {
                        log.error("GHN: Failed to save shipping mapping in TichHopVanChuyen for HoaDon #{}: {}", hoaDon.getId(), dbEx.getMessage(), dbEx);
                    }
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
     * Tạo vận đơn GHN chiều trả (từ Khách hàng về lại Shop) khi Admin duyệt yêu cầu trả hàng
     */
    @SuppressWarnings("unchecked")
    public String createReturnShippingOrder(HoaDon hoaDon, List<HoaDonChiTiet> items) {
        try {
            Integer toDistrictId = ghnConfig.getFromDistrictId();
            String toWardCode = ghnConfig.getFromWardCode();
            Integer fromDistrictId = hoaDon.getDiaChi() != null ? hoaDon.getDiaChi().getDistrictId() : ghnConfig.getFromDistrictId();
            String fromWardCode = hoaDon.getDiaChi() != null ? hoaDon.getDiaChi().getWardCode() : ghnConfig.getFromWardCode();
            
            String shopIdStr = getGhnShopId();
            String tokenStr = getGhnToken();
            
            GhnOrderCreateRequestDTO req = new GhnOrderCreateRequestDTO();
            String senderName = (hoaDon.getKhachHang() != null ? (hoaDon.getKhachHang().getHoKh() + " " + hoaDon.getKhachHang().getTenKh()) : "Khách hàng").trim();
            req.setFrom_name(senderName);
            req.setFrom_phone(hoaDon.getSdtNhan() != null ? hoaDon.getSdtNhan() : "0900000000");
            req.setFrom_address(hoaDon.getDiaChiNhan() != null ? hoaDon.getDiaChiNhan() : "Địa chỉ khách hàng");
            req.setFrom_district_id(fromDistrictId != null ? fromDistrictId : ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(fromWardCode != null ? fromWardCode : ghnConfig.getFromWardCode());
            
            req.setTo_name("SmashVN Shop (Kho Nhận Hàng Trả)");
            req.setTo_phone("0835420088");
            req.setTo_address(ghnConfig.getFromAddress());
            req.setTo_district_id(toDistrictId);
            req.setTo_ward_code(toWardCode);
            
            req.setCod_amount(0);
            req.setInsurance_value(hoaDon.getTongTien() != null ? hoaDon.getTongTien().intValue() : 0);
            req.setNote("ĐƠN HÀNG THU HỒI TRẢ VỀ SHOP - " + (hoaDon.getLyDoHoanTien() != null ? hoaDon.getLyDoHoanTien() : ""));
            
            int totalQty = items != null ? items.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum() : 1;
            req.setWeight(totalQty * 500);
            
            List<GhnOrderCreateRequestDTO.GhnItemDTO> ghnItems = new ArrayList<>();
            if (items != null) {
                for (HoaDonChiTiet ct : items) {
                    GhnOrderCreateRequestDTO.GhnItemDTO item = new GhnOrderCreateRequestDTO.GhnItemDTO();
                    item.setName(ct.getSanPhamChiTiet() != null ? ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : "Sản phẩm trả");
                    item.setQuantity(ct.getSoLuong());
                    item.setPrice(ct.getDonGia().intValue());
                    ghnItems.add(item);
                }
            }
            req.setItems(ghnItems);
            
            String url = ghnConfig.getBaseUrl() + API_CREATE;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", tokenStr);
            headers.set("ShopId", shopIdStr);
            
            HttpEntity<GhnOrderCreateRequestDTO> request = new HttpEntity<>(req, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("order_code") != null) {
                    String orderCode = (String) data.get("order_code");
                    log.info("GHN: Created return order {} for HoaDon #{}", orderCode, hoaDon.getId());
                    return orderCode;
                }
            }
        } catch (Exception e) {
            log.warn("GHN createReturnShippingOrder API call failed or simulated: {}", e.getMessage());
        }
        return "GHNRET" + hoaDon.getId() + String.format("%04d", (int)(Math.random() * 10000));
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
    private static class GhnServiceInfo {
        private Integer serviceId;
        private Integer serviceTypeId;
    }

    @Data
    @AllArgsConstructor
    public static class GhnAddressMapping {
        private Integer provinceId;
        private Integer districtId;
        private String wardCode;
    }

    public String normalizeString(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT);

        normalized = normalized.replace("đ", "d"); // compatibility for input without accents

        normalized = normalized
                .replaceAll("\\btp\\.?\\b", " thanh pho ")
                .replaceAll("\\bq\\.?\\b", " quan ")
                .replaceAll("\\bp\\.?\\b", " phuong ")
                .replaceAll("\\bh\\.?\\b", " huyen ")
                .replaceAll("\\btx\\.?\\b", " thi xa ");

        return normalized.replaceAll("\\s+", " ").trim();
    }

    public GhnAddressMapping resolveGhnAddress(SoDiaChi dc) {
        if (dc == null) return null;

        // Bypass fuzzy matching only when all GHN fields are fully present and valid
        if (dc.getProvinceId() != null && dc.getDistrictId() != null && 
            dc.getWardCode() != null && !dc.getWardCode().trim().isEmpty()) {
            log.info("Using pre-saved GHN address mapping for address ID {}: provinceId={}, districtId={}, wardCode={}",
                     dc.getId(), dc.getProvinceId(), dc.getDistrictId(), dc.getWardCode());
            return new GhnAddressMapping(dc.getProvinceId(), dc.getDistrictId(), dc.getWardCode().trim());
        }

        String userProvince = dc.getTinhThanh();
        String userDistrict = dc.getDiaChiCuThe();
        String userWard = dc.getDiaChiCuThe();

        // 1. Fetch provinces and match dc.getTinhThanh() or dc.getDiaChiCuThe()
        List<Map<String, Object>> provinces = getProvinces();
        String targetProvince = normalizeString(userProvince);
        Integer provinceId = null;
        Map<String, Object> matchedProvince = null;

        // Pass 1: Try to match province using dc.getTinhThanh()
        for (Map<String, Object> p : provinces) {
            String name = normalizeString((String) p.get("ProvinceName"));
            if (!name.isEmpty() && (name.contains(targetProvince) || targetProvince.contains(name))) {
                provinceId = (Integer) p.get("ProvinceID");
                matchedProvince = p;
                break;
            }
        }

        // Pass 2: Fallback to match province using dc.getDiaChiCuThe()
        if (provinceId == null) {
            String streetAddr = normalizeString(dc.getDiaChiCuThe());
            for (Map<String, Object> p : provinces) {
                String name = normalizeString((String) p.get("ProvinceName"));
                String cleanProvName = name.replaceAll("^(tinh|thanh pho|tp)\\s+", "").trim();
                if (!cleanProvName.isEmpty() && streetAddr.contains(cleanProvName)) {
                    provinceId = (Integer) p.get("ProvinceID");
                    matchedProvince = p;
                    break;
                }
            }
        }

        if (provinceId == null) {
            log.warn("Unable to resolve GHN province from address: {}", dc.getDiaChiCuThe());
            return null;
        }

        log.debug("GHN Province matched: {} -> {}",
                userProvince,
                (String) matchedProvince.get("ProvinceName"));

        String cleanProvinceName = normalizeString((String) matchedProvince.get("ProvinceName"))
                .replaceAll("^(tinh|thanh pho|tp)\\s+", "").trim();

        // 2. Fetch districts and match dc.getDiaChiCuThe() first, then fallback to dc.getThanhPho()
        List<Map<String, Object>> districts = getDistricts(provinceId);
        String streetAddress = normalizeString(dc.getDiaChiCuThe());
        String fallbackDistrict = normalizeString(dc.getThanhPho());
        Integer districtId = null;
        Map<String, Object> matchedDistrict = null;

        Map<Map<String, Object>, String> districtCleanNames = new java.util.HashMap<>();
        Map<Map<String, Object>, String> districtNormNames = new java.util.HashMap<>();
        for (Map<String, Object> d : districts) {
            String rawDistrictName = (String) d.get("DistrictName");
            if (rawDistrictName != null) {
                String normDistrictName = normalizeString(rawDistrictName);
                String cleanDistrictName = normDistrictName.replaceAll("^(quan|huyen|thi xa|thanh pho|tp)\\s+", "").trim();
                districtCleanNames.put(d, cleanDistrictName);
                districtNormNames.put(d, normDistrictName);
            }
        }

        List<Map<String, Object>> sortedDistricts = new java.util.ArrayList<>(districts);
        sortedDistricts.sort((d1, d2) -> {
            String name1 = districtCleanNames.getOrDefault(d1, "");
            String name2 = districtCleanNames.getOrDefault(d2, "");
            return Integer.compare(name2.length(), name1.length()); // Descending
        });

        // Pass 1: Match districts whose clean name is NOT equal to the clean province name
        for (Map<String, Object> d : sortedDistricts) {
            String cleanDistName = districtCleanNames.get(d);
            if (cleanDistName == null || cleanDistName.isEmpty() || cleanDistName.equals(cleanProvinceName)) {
                continue;
            }
            String normDistName = districtNormNames.get(d);
            if (cleanDistName.matches("^\\d+$")) {
                if (streetAddress.contains(normDistName)) {
                    districtId = (Integer) d.get("DistrictID");
                    matchedDistrict = d;
                    break;
                }
            } else {
                if (streetAddress.contains(cleanDistName)) {
                    districtId = (Integer) d.get("DistrictID");
                    matchedDistrict = d;
                    break;
                }
            }
        }

        // Pass 2: Match district whose clean name is equal to the clean province name (e.g. Thành phố Thái Nguyên in Thái Nguyên)
        if (districtId == null) {
            for (Map<String, Object> d : sortedDistricts) {
                String cleanDistName = districtCleanNames.get(d);
                if (cleanDistName != null && !cleanDistName.isEmpty() && cleanDistName.equals(cleanProvinceName)) {
                    String normDistName = districtNormNames.get(d);
                    if (cleanDistName.matches("^\\d+$")) {
                        if (streetAddress.contains(normDistName)) {
                            districtId = (Integer) d.get("DistrictID");
                            matchedDistrict = d;
                            break;
                        }
                    } else {
                        if (streetAddress.contains(cleanDistName)) {
                            districtId = (Integer) d.get("DistrictID");
                            matchedDistrict = d;
                            break;
                        }
                    }
                }
            }
        }

        // Pass 3: Fallback to old method (match against dc.getThanhPho())
        if (districtId == null) {
            String cleanFallback = fallbackDistrict.replaceAll("^(tinh|thanh pho|tp)\\s+", "").trim();
            for (Map<String, Object> d : sortedDistricts) {
                String cleanDistName = districtCleanNames.get(d);
                if (cleanDistName != null && !cleanDistName.isEmpty() && 
                    (cleanDistName.contains(cleanFallback) || cleanFallback.contains(cleanDistName))) {
                    districtId = (Integer) d.get("DistrictID");
                    matchedDistrict = d;
                    break;
                }
            }
        }

        if (districtId == null) {
            log.warn("Unable to resolve GHN district from address: {}",
                    dc.getDiaChiCuThe());
            return null;
        }

        log.debug("GHN District matched: {} -> {}",
                userDistrict,
                (String) matchedDistrict.get("DistrictName"));

        // 3. Fetch wards and match within dc.getDiaChiCuThe() first, then other fields
        List<Map<String, Object>> wards = getWards(districtId);
        String targetStreet = streetAddress; // dc.getDiaChiCuThe() normalized
        String targetOtherFields = normalizeString(dc.getTinhThanh() + " " + dc.getThanhPho());
        String wardCode = null;
        Map<String, Object> matchedWard = null;

        Map<Map<String, Object>, String> wardCleanNames = new java.util.HashMap<>();
        Map<Map<String, Object>, String> wardNormNames = new java.util.HashMap<>();
        for (Map<String, Object> w : wards) {
            String rawWardName = (String) w.get("WardName");
            if (rawWardName != null) {
                String normWardName = normalizeString(rawWardName);
                String cleanWardName = normWardName.replaceAll("^(phuong|xa|thi tran)\\s+", "").trim();
                wardCleanNames.put(w, cleanWardName);
                wardNormNames.put(w, normWardName);
            }
        }

        List<Map<String, Object>> sortedWards = new java.util.ArrayList<>(wards);
        sortedWards.sort((w1, w2) -> {
            String name1 = wardCleanNames.getOrDefault(w1, "");
            String name2 = wardCleanNames.getOrDefault(w2, "");
            return Integer.compare(name2.length(), name1.length()); // Descending
        });

        // Pass 1: Match ward inside dc.getDiaChiCuThe()
        for (Map<String, Object> w : sortedWards) {
            String cleanWardName = wardCleanNames.get(w);
            if (cleanWardName == null || cleanWardName.isEmpty()) continue;
            
            String normWardName = wardNormNames.get(w);
            if (cleanWardName.matches("^\\d+$")) {
                if (targetStreet.contains(normWardName)) {
                    wardCode = (String) w.get("WardCode");
                    matchedWard = w;
                    break;
                }
            } else {
                if (targetStreet.contains(cleanWardName)) {
                    wardCode = (String) w.get("WardCode");
                    matchedWard = w;
                    break;
                }
            }
        }

        // Pass 2: Fallback to match ward inside TinhThanh / ThanhPho
        if (wardCode == null) {
            for (Map<String, Object> w : sortedWards) {
                String cleanWardName = wardCleanNames.get(w);
                if (cleanWardName == null || cleanWardName.isEmpty()) continue;
                
                String normWardName = wardNormNames.get(w);
                if (cleanWardName.matches("^\\d+$")) {
                    if (targetOtherFields.contains(normWardName)) {
                        wardCode = (String) w.get("WardCode");
                        matchedWard = w;
                        break;
                    }
                } else {
                    if (targetOtherFields.contains(cleanWardName)) {
                        wardCode = (String) w.get("WardCode");
                        matchedWard = w;
                        break;
                    }
                }
            }
        }

        if (wardCode == null) {
            log.warn("Unable to resolve GHN ward from address: {}",
                    dc.getDiaChiCuThe());
            return null;
        }

        log.debug("GHN Ward matched: {} -> {}",
                userWard,
                (String) matchedWard.get("WardName"));

        // Backfill GHN IDs to SoDiaChi to avoid future fuzzy match/API calls
        if (dc.getId() != null) {
            dc.setProvinceId(provinceId);
            dc.setDistrictId(districtId);
            dc.setWardCode(wardCode);
            dc.setProvinceName(matchedProvince != null && matchedProvince.get("ProvinceName") != null ? matchedProvince.get("ProvinceName").toString() : null);
            dc.setDistrictName(matchedDistrict != null && matchedDistrict.get("DistrictName") != null ? matchedDistrict.get("DistrictName").toString() : null);
            dc.setWardName(matchedWard != null && matchedWard.get("WardName") != null ? matchedWard.get("WardName").toString() : null);
            try {
                soDiaChiRepository.save(dc);
                log.info("[GHN_BACKFILL] Successfully backfilled GHN IDs to SoDiaChi ID {}", dc.getId());
            } catch (Exception e) {
                log.warn("[GHN_BACKFILL] Failed to backfill GHN IDs for SoDiaChi ID {}: {}", dc.getId(), e.getMessage());
            }
        }

        return new GhnAddressMapping(provinceId, districtId, wardCode);
    }
}

