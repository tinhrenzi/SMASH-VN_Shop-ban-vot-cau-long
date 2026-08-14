package com.smashvn.shop.controller.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.api.GhnService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class CheckoutRuntimeRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.smashvn.shop.service.api.GhnService ghnService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-123");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        when(ghnService.resolveGhnAddress(any())).thenAnswer(invocation -> {
            SoDiaChi dc = invocation.getArgument(0);
            if (dc == null) return null;
            return new GhnService.GhnAddressMapping(
                    dc.getProvinceId() != null ? dc.getProvinceId() : 201,
                    dc.getDistrictId() != null ? dc.getDistrictId() : 1442,
                    dc.getWardCode() != null ? dc.getWardCode() : "20101"
            );
        });
        try {
            when(ghnService.createShippingOrder(any(), any(), any(), any())).thenReturn("GHN-TEST-123456");
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Checkout Test 1: Cart -> Start Checkout Token Generation & View")
    void testStartCheckoutAndTokenView() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add SPCT 25 (709,000 đ) to guest cart
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "2"))
                .andExpect(status().isOk());

        // Start Checkout for selected SPCT 25
        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();

        String startJson = startResult.getResponse().getContentAsString();
        Map<String, Object> startMap = objectMapper.readValue(startJson, Map.class);
        assertEquals("ok", startMap.get("trangThai"));

        String token = (String) startMap.get("checkoutToken");
        assertNotNull(token, "Checkout token must be generated");

        // Render Checkout page with token and CSRF token mock
        mockMvc.perform(get("/checkout")
                        .session(session)
                        .requestAttr("_csrf", csrfToken)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("danhSachCart"))
                .andExpect(model().attributeExists("tongTien"))
                .andExpect(view().name("checkout"));
    }

    @Test
    @DisplayName("Checkout Test 2: Server-Side Price Authority & Subtotal Calculation")
    void testServerSidePriceAuthorityAndSubtotal() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add SPCT 25 (709,000 đ) x 2 = 1,418,000 đ
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "2"))
                .andExpect(status().isOk());

        // Add SPCT 112 (150,000 đ) x 1 = 150,000 đ
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "112")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        // Start Checkout
        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25", "112"))
                .andExpect(status().isOk())
                .andReturn();

        String token = (String) objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        MvcResult checkoutView = mockMvc.perform(get("/checkout")
                        .session(session)
                        .requestAttr("_csrf", csrfToken)
                        .param("token", token))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal tongTien = (BigDecimal) checkoutView.getModelAndView().getModel().get("tongTien");
        // Server calculates 709,000 * 2 + 150,000 = 1,568,000 đ
        assertEquals(new BigDecimal("1568000.00"), tongTien, "Server price authority must calculate exact subtotal");
    }

    @Test
    @DisplayName("Checkout Test 3: Address Input & Phone Validation")
    void testAddressAndPhoneValidation() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();
        String token = (String) objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        // Submit without recipient name
        MvcResult res1 = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", "")
                        .param("sdtNhan", "0912345678")
                        .param("email", "testvalidation@smashvn.com")
                        .param("diaChiNhan", "123 Nguyen Hue, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map1 = objectMapper.readValue(res1.getResponse().getContentAsString(), Map.class);
        assertEquals("loi", map1.get("trangThai"));

        // Submit with invalid phone number (using fresh checkout session/token)
        MvcResult startResult2 = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();
        String token2 = (String) objectMapper.readValue(startResult2.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        MvcResult res2 = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token2)
                        .param("hoTenNhan", "Nguyen Van A")
                        .param("sdtNhan", "123456")
                        .param("email", "testvalidation2@smashvn.com")
                        .param("diaChiNhan", "123 Nguyen Hue, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map2 = objectMapper.readValue(res2.getResponse().getContentAsString(), Map.class);
        assertEquals("loi", map2.get("trangThai"));
        assertTrue(map2.get("message").toString().contains("Số điện thoại"));
    }

    @Test
    @DisplayName("Checkout Test 4: Invalid Voucher Rejection")
    void testInvalidVoucherRejection() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();
        String token = (String) objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        // Submit with fake voucher code
        MvcResult res = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", "Nguyen Van A")
                        .param("sdtNhan", "0987654321")
                        .param("email", "testvoucher@smashvn.com")
                        .param("diaChiNhan", "123 Le Loi, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101")
                        .param("voucherCode", "INVALID_VOUCHER_CODE_9999"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map = objectMapper.readValue(res.getResponse().getContentAsString(), Map.class);
        assertEquals("loi", map.get("trangThai"));
    }

    @Test
    @DisplayName("Checkout Test 5: Stock Revalidation at Checkout")
    void testStockRevalidationAtCheckout() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // SPCT 25 has stock 20. Request quantity = 25 (> stock)
        MvcResult startResult = mockMvc.perform(post("/checkout/buy-now")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "25"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map = objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class);
        assertEquals("loi", map.get("trangThai"), "Checkout start must reject quantity exceeding stock");
        assertTrue(map.get("message").toString().contains("Số lượng tồn kho không đủ"));
    }

    @Test
    @DisplayName("Checkout Test 6: Double Submit Protection (Idempotency Claim)")
    void testDoubleSubmitProtection() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();
        String token = (String) objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        // First submit: fails on mandatory email/address validation, but claims context
        mockMvc.perform(post("/checkout/submit")
                .session(session)
                .param("checkoutToken", token)
                .param("hoTenNhan", "Nguyen Van A")
                .param("sdtNhan", "0987654321")
                .param("email", "testdouble@smashvn.com")
                .param("diaChiNhan", "123 Le Loi, Quan 1, TP HCM")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1442")
                .param("ghnToWardCode", "20101"));

        // Second submit with same token -> should be claimed already
        MvcResult res2 = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", "Nguyen Van A")
                        .param("sdtNhan", "0987654321")
                        .param("email", "testdouble@smashvn.com")
                        .param("diaChiNhan", "123 Le Loi, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map2 = objectMapper.readValue(res2.getResponse().getContentAsString(), Map.class);
        assertEquals("loi", map2.get("trangThai"));
        assertTrue(map2.get("message").toString().contains("đang được xử lý hoặc đã hoàn tất"), "Double submit must be blocked by tryClaim()");
    }

    @Test
    @DisplayName("Checkout Test 7: Successful Order Submission & Cart Cleanup (Transactional)")
    void testSuccessfulOrderSubmissionAndCartCleanup() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add SPCT 25 to guest cart
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();
        String token = (String) objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        // Submit checkout with COD and valid GHN location IDs and unique phone/email
        String uniquePhone = "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String uniqueEmail = "guesttest" + System.currentTimeMillis() + "@smashvn.com";

        MvcResult submitResult = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", "Tran Van B")
                        .param("sdtNhan", uniquePhone)
                        .param("email", uniqueEmail)
                        .param("diaChiNhan", "456 Tran Hung Dao, Quan 5, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101")
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = objectMapper.readValue(submitResult.getResponse().getContentAsString(), Map.class);
        assertEquals("ok", respMap.get("trangThai"), "Checkout submission failed with message: " + respMap.get("message"));
        assertNotNull(respMap.get("orderId"), "Order ID must be returned");

        // Verify guest cart has been cleaned up for checked out item
        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andReturn();

        List cartList = (List) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertTrue(cartList.isEmpty(), "Cart items must be cleaned up after successful checkout");
    }
}
