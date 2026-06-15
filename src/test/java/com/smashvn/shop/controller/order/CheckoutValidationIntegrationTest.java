package com.smashvn.shop.controller.order;
import com.smashvn.shop.dto.payment.ZaloPayCallbackDTO;
import com.smashvn.shop.dto.payment.ZaloPayCreateOrderRequestDTO;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@Transactional
public class CheckoutValidationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private TrangThaiGioHangRepository trangThaiGioHangRepository;

    @Autowired
    private SoDiaChiRepository soDiaChiRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    private MockMvc mockMvc;
    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private SanPhamChiTiet testSpct;
    private DonViVanChuyen testDvvc;
    private CsrfToken csrfToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        // Clear all caches to avoid stale DonViVanChuyen cache
        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        }

        // Seed test user
        testUser = new TaiKhoan();
        testUser.setEmail("checkout_tester@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");
        testUser.setLaKhachHang(true);
        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Checkout");
        testKhachHang.setTenKh("Tester");
        testKhachHang.setSoDienThoaiKh("0987654321");
        testKhachHang = khachHangRepository.save(testKhachHang);

        // Retrieve or seed DanhMuc
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Mặc định");
            return danhMucRepository.save(newDm);
        });

        // Retrieve or seed ThuongHieu
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Mặc định");
            return thuongHieuRepository.save(newTh);
        });

        // Retrieve or seed NhanVien
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan nvUser = new TaiKhoan();
            nvUser.setEmail("checkout_staff@gmail.com");
            nvUser.setMatKhau("testpass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");
            nvUser.setLaNhanVien(true);
            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff Tester");
            newNv.setChucVu("Nhân viên bán hàng");
            newNv.setSoDienThoaiNv("0912345670");
            return nhanVienRepository.save(newNv);
        });

        // Seed a test product
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Cầu Lông Yonex Astrox");
        sp.setTrangThai("dang_ban");
        sp.setMoTa("Mô tả sản phẩm");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);

        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Đỏ");
        testSpct.setTrongLuong("4U");
        testSpct.setMucCang("26 lbs");
        testSpct.setSoLuongTon(100);
        testSpct.setGiaBan(new BigDecimal("2000000"));
        testSpct = sanPhamChiTietRepository.save(testSpct);

        // Ensure TrangThaiGioHang ID 1 exists
        if (!trangThaiGioHangRepository.existsById(1)) {
            TrangThaiGioHang tt = new TrangThaiGioHang();
            tt.setTenTrangThai("Trạng thái mặc định");
            trangThaiGioHangRepository.save(tt);
        }

        // Seed carrier
        testDvvc = new DonViVanChuyen();
        testDvvc.setTenDonVi("Giao Hàng Tiết Kiệm");
        testDvvc = donViVanChuyenDAO.save(testDvvc);

        // Seed payment method
        PhuongThucThanhToan pttt = new PhuongThucThanhToan();
        pttt.setTenPhuongThuc("COD");
        phuongThucThanhToanDAO.save(pttt);

        // Seed user cart item
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .sessionAttr("laKhachHang", true)
                        .sessionAttr("laNhanVien", false)
                        .sessionAttr("laQuanLy", false)
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testSubmitCheckout_EmptyValues() throws Exception {
        // Missing name
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Họ và tên người nhận không được để trống."));

        // Missing phone
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "")
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại không được để trống."));

        // Missing address
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ nhận hàng không được để trống."));
    }

    @Test
    void testSubmitCheckout_InvalidPhoneFormat() throws Exception {
        // Invalid phone prefix (legacy format or wrong start digits)
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0123456789") // Invalid prefix
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 0 hoặc +84)."));

        // Non-numeric phone number
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "091abc3456") // Invalid characters
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 0 hoặc +84)."));
    }

    @Test
    void testSubmitCheckout_ValidPhoneFormats() throws Exception {
        String[] validPhones = {"0912345678", "+84912345678", "0391234567"};
        for (String phone : validPhones) {
            MvcResult result = mockMvc.perform(post("/checkout/submit")
                            .sessionAttr("idNguoiDung", testUser.getId())
                            .requestAttr("_csrf", csrfToken)
                            .param("hoTenNhan", "Nguyễn Văn A")
                            .param("sdtNhan", phone)
                            .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                            .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                            .param("phuongThucThanhToan", "COD"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThai").value("ok"))
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
            Integer orderId = (Integer) respMap.get("orderId");
            assertNotNull(orderId);

            // Re-seed cart for next loop iteration
            mockMvc.perform(post("/gio-hang/them")
                            .sessionAttr("idNguoiDung", testUser.getId())
                            .requestAttr("_csrf", csrfToken)
                            .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                            .param("soLuong", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSubmitCheckout_VietnameseUnicodeSupport() throws Exception {
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn Ánh")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Số 12 Phố Huế, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        assertEquals("Số 12 Phố Huế, Hà Nội", savedOrder.getDiaChiNhan());
        assertEquals("0912345678", savedOrder.getSdtNhan());
    }

    @Test
    void testSubmitCheckout_NoteLengthBoundaries() throws Exception {
        // 499 chars -> Valid
        String note499 = "a".repeat(499);
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("ghiChu", note499))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"));

        // Re-seed cart
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        // 500 chars -> Valid
        String note500 = "a".repeat(500);
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("ghiChu", note500))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"));

        // 501 chars -> Invalid
        String note501 = "a".repeat(501);
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("ghiChu", note501))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Ghi chú đơn hàng tối đa 500 ký tự."));
    }

    @Test
    void testSubmitCheckout_XssSanitization() throws Exception {
        String xssName = "<b>Nguyễn Văn A</b>";
        String xssAddress = "<script>alert('XSS')</script>Hà Nội";
        String xssNote = "<img src=x onerror=alert(1)>Ghi chú";

        MvcResult result = mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", xssName)
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", xssAddress)
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("ghiChu", xssNote))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        // Verify clean text is saved to DB
        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);

        // check that HTML tag was stripped, retaining pure text
        assertEquals("Hà Nội", savedOrder.getDiaChiNhan());
        assertEquals("Ghi chú", savedOrder.getGhiChu());
    }

    private static String computeHmacSha256(String data, String key) throws Exception {
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        javax.crypto.spec.SecretKeySpec signingKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void testZaloPayAuthorization_IDOR() throws Exception {
        // Create an order owned by testUser
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setTongTien(new BigDecimal("100000"));
        hd.setPaymentStatus("PENDING");
        hd.setTrangThaiDonHang("cho_thanh_toan");
        hd.setAppTransId("260612_9999_123");
        
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd.setMaDonHang("TEST_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setDonViVanChuyen(testDvvc);
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        hd.setPhuongThucThanhToan(pttt);

        hd = hoaDonRepository.save(hd);

        // Create user B
        TaiKhoan userB = new TaiKhoan();
        userB.setEmail("userb@gmail.com");
        userB.setMatKhau("testpass123");
        userB.setVaiTro("KH");
        userB.setTrangThai("hoat_dong");
        userB.setLaKhachHang(true);
        userB = taiKhoanRepository.save(userB);

        // User B attempts to create payment for User A's order -> HTTP 403
        ZaloPayCreateOrderRequestDTO createReq = new ZaloPayCreateOrderRequestDTO();
        createReq.setOrderId(hd.getId());
        mockMvc.perform(post("/api/payment/zalopay/create")
                        .sessionAttr("idNguoiDung", userB.getId())
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // User B attempts to query User A's payment -> HTTP 403
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payment/zalopay/query/260612_9999_123")
                        .sessionAttr("idNguoiDung", userB.getId()))
                .andExpect(status().isForbidden());

        // User B attempts to cancel User A's payment -> HTTP 403
        mockMvc.perform(post("/api/payment/zalopay/cancel/260612_9999_123")
                        .sessionAttr("idNguoiDung", userB.getId())
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testZaloPayCallback_AmountValidation() throws Exception {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setTongTien(new BigDecimal("100000"));
        hd.setPaymentStatus("PENDING");
        hd.setTrangThaiDonHang("cho_thanh_toan");
        hd.setAppTransId("260612_amount_val");
        
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd.setMaDonHang("TEST_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setDonViVanChuyen(testDvvc);
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        hd.setPhuongThucThanhToan(pttt);

        hd = hoaDonRepository.save(hd);

        // Seed order details to avoid null pointer when fetching items
        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("100000"));
        com.smashvn.shop.repository.HoaDonChiTietRepository hdctRepo = webApplicationContext.getBean(com.smashvn.shop.repository.HoaDonChiTietRepository.class);
        hdctRepo.save(hdct);

        // 1. Wrong amount -> Rejected (code = -1)
        Map<String, Object> dataMapWrong = Map.of(
                "app_trans_id", "260612_amount_val",
                "zp_trans_id", "12345678",
                "amount", 50000
        );
        String rawDataWrong = objectMapper.writeValueAsString(dataMapWrong);
        ZaloPayCallbackDTO callbackDtoWrong = new ZaloPayCallbackDTO();
        callbackDtoWrong.setData(rawDataWrong);
        
        com.smashvn.shop.config.ZaloPayConfig zaloPayConfig = webApplicationContext.getBean(com.smashvn.shop.config.ZaloPayConfig.class);
        String macWrong = computeHmacSha256(rawDataWrong, zaloPayConfig.getKey2());
        callbackDtoWrong.setMac(macWrong);

        mockMvc.perform(post("/api/payment/zalopay/callback")
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDtoWrong)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.return_code").value(-1))
                .andExpect(jsonPath("$.return_message").value("amount mismatch"));

        // 2. Correct amount -> Success (code = 1)
        Map<String, Object> dataMapCorrect = Map.of(
                "app_trans_id", "260612_amount_val",
                "zp_trans_id", "12345678",
                "amount", 100000
        );
        String rawDataCorrect = objectMapper.writeValueAsString(dataMapCorrect);
        ZaloPayCallbackDTO callbackDtoCorrect = new ZaloPayCallbackDTO();
        callbackDtoCorrect.setData(rawDataCorrect);
        String macCorrect = computeHmacSha256(rawDataCorrect, zaloPayConfig.getKey2());
        callbackDtoCorrect.setMac(macCorrect);

        mockMvc.perform(post("/api/payment/zalopay/callback")
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDtoCorrect)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.return_code").value(1));
    }

    @Test
    void testZaloPayCallback_ReplayProtection() throws Exception {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setTongTien(new BigDecimal("100000"));
        hd.setPaymentStatus("PENDING");
        hd.setTrangThaiDonHang("cho_thanh_toan");
        hd.setAppTransId("260612_replay");
        
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd.setMaDonHang("TEST_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setDonViVanChuyen(testDvvc);
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        hd.setPhuongThucThanhToan(pttt);

        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("100000"));
        com.smashvn.shop.repository.HoaDonChiTietRepository hdctRepo = webApplicationContext.getBean(com.smashvn.shop.repository.HoaDonChiTietRepository.class);
        hdctRepo.save(hdct);

        int originalStock = testSpct.getSoLuongTon();

        // Callback 1
        Map<String, Object> data = Map.of(
                "app_trans_id", "260612_replay",
                "zp_trans_id", "zp_rep_1",
                "amount", 100000
        );
        String rawData = objectMapper.writeValueAsString(data);
        ZaloPayCallbackDTO callbackDto = new ZaloPayCallbackDTO();
        callbackDto.setData(rawData);
        com.smashvn.shop.config.ZaloPayConfig zaloPayConfig = webApplicationContext.getBean(com.smashvn.shop.config.ZaloPayConfig.class);
        callbackDto.setMac(computeHmacSha256(rawData, zaloPayConfig.getKey2()));

        mockMvc.perform(post("/api/payment/zalopay/callback")
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.return_code").value(1));

        // Verify stock reduced by 1
        SanPhamChiTiet spctAfterFirst = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(spctAfterFirst);
        assertEquals(originalStock - 1, spctAfterFirst.getSoLuongTon());

        // Callback 2 (Replay)
        mockMvc.perform(post("/api/payment/zalopay/callback")
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.return_code").value(1))
                .andExpect(jsonPath("$.return_message").value("success (already processed)"));

        // Verify stock is still the same (not reduced again)
        SanPhamChiTiet spctAfterSecond = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(spctAfterSecond);
        assertEquals(originalStock - 1, spctAfterSecond.getSoLuongTon());
    }

    @Test
    void testZaloPayCallback_CancelledOrder() throws Exception {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setTongTien(new BigDecimal("100000"));
        hd.setPaymentStatus("PENDING");
        hd.setTrangThaiDonHang("da_huy");
        hd.setAppTransId("260612_cancelled");
        
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd.setMaDonHang("TEST_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setDonViVanChuyen(testDvvc);
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        hd.setPhuongThucThanhToan(pttt);

        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("100000"));
        com.smashvn.shop.repository.HoaDonChiTietRepository hdctRepo = webApplicationContext.getBean(com.smashvn.shop.repository.HoaDonChiTietRepository.class);
        hdctRepo.save(hdct);

        int originalStock = testSpct.getSoLuongTon();

        Map<String, Object> data = Map.of(
                "app_trans_id", "260612_cancelled",
                "zp_trans_id", "zp_cancel_1",
                "amount", 100000
        );
        String rawData = objectMapper.writeValueAsString(data);
        ZaloPayCallbackDTO callbackDto = new ZaloPayCallbackDTO();
        callbackDto.setData(rawData);
        com.smashvn.shop.config.ZaloPayConfig zaloPayConfig = webApplicationContext.getBean(com.smashvn.shop.config.ZaloPayConfig.class);
        callbackDto.setMac(computeHmacSha256(rawData, zaloPayConfig.getKey2()));

        mockMvc.perform(post("/api/payment/zalopay/callback")
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackDto)))
                .andExpect(status().isOk());

        // Verify order remains cancelled
        HoaDon orderAfter = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(orderAfter);
        assertEquals("da_huy", orderAfter.getTrangThaiDonHang());
        assertEquals("PAID_RECEIVED_AFTER_CANCEL", orderAfter.getPaymentStatus());

        // Verify stock was not modified
        SanPhamChiTiet spctAfter = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(spctAfter);
        assertEquals(originalStock, spctAfter.getSoLuongTon());
    }

    @Autowired
    private com.smashvn.shop.config.GhnConfig ghnConfig;

    @Test
    void testGhnWebhook_Authentication() throws Exception {
        Map<String, Object> payload = Map.of(
                "OrderCode", "GHN123456",
                "Status", "ready_to_pick"
        );

        // 1. No token -> 401
        mockMvc.perform(post("/api/ghn/webhook")
                        .requestAttr("_csrf", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // 2. Wrong token -> 401
        mockMvc.perform(post("/api/ghn/webhook")
                        .requestAttr("_csrf", csrfToken)
                        .param("token", "wrong_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // 3. Valid token -> 200/Ok (order not found returns 200 with not_found status)
        mockMvc.perform(post("/api/ghn/webhook")
                        .requestAttr("_csrf", csrfToken)
                        .param("token", ghnConfig.getWebhookToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_found"));
    }

    @Test
    void testCheckoutPageValidation_OutofStock() throws Exception {
        testSpct.setSoLuongTon(0);
        sanPhamChiTietRepository.save(testSpct);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gio-hang?loi=*"));
    }

    @Test
    void testCheckoutPageValidation_NotSelling() throws Exception {
        SanPham sp = testSpct.getSanPham();
        sp.setTrangThai("ngung_ban");
        sanPhamRepository.save(sp);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gio-hang?loi=*"));
    }

    @Test
    void testCheckoutPageValidation_QuantityExceeded() throws Exception {
        // Add more items to cart to make quantity 2 (original cart had 1)
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        // Now set stock to 1 (cart quantity is 2, stock is 1)
        testSpct.setSoLuongTon(1);
        sanPhamChiTietRepository.save(testSpct);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gio-hang?loi=*"));
    }

    @Test
    void testShippingFeeTampering() throws Exception {
        // Submit ghnProvinceId=201 (Hanoi), but with district = 999999 (non-local)
        // Verify the server ignores the frontend's province ID and calculates the correct nationwide fee.
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Đà Nẵng")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId())) // GHTK
                        .param("phuongThucThanhToan", "COD")
                        .param("ghnToDistrictId", "999999") // Non-local district
                        .param("ghnProvinceId", "201")) // Pretending to be Hanoi
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        // The resolved fee should be GHTK Nationwide (30000), not Local (22000)
        assertEquals(0, new BigDecimal("30000").compareTo(savedOrder.getPhiVanChuyen()));
    }

    @Test
    void testSavedAddressOwnership() throws Exception {
        // Create user B
        TaiKhoan userB = new TaiKhoan();
        userB.setEmail("userb_address@gmail.com");
        userB.setMatKhau("testpass123");
        userB.setVaiTro("KH");
        userB.setTrangThai("hoat_dong");
        userB.setLaKhachHang(true);
        userB = taiKhoanRepository.save(userB);

        KhachHang khachHangB = new KhachHang();
        khachHangB.setTaiKhoan(userB);
        khachHangB.setHoKh("Khach");
        khachHangB.setTenKh("B");
        khachHangB.setSoDienThoaiKh("0987654322");
        khachHangB = khachHangRepository.save(khachHangB);

        // Create saved address for user B
        SoDiaChi soDiaChiB = new SoDiaChi();
        soDiaChiB.setKhachHang(khachHangB);
        soDiaChiB.setHoNguoiNhan("Khach");
        soDiaChiB.setTenNguoiNhan("B");
        soDiaChiB.setSdtNguoiNhan("0987654322");
        soDiaChiB.setDiaChiCuThe("123 Street B");
        soDiaChiB.setTinhThanh("Hà Nội");
        soDiaChiB.setThanhPho("Quận Ba Đình");
        soDiaChiB.setQuocGia("Việt Nam");
        soDiaChiB.setMaBuuDien("10000");
        soDiaChiB.setDefaultShipping(false);
        soDiaChiB.setDefaultBilling(false);
        soDiaChiB = soDiaChiRepository.save(soDiaChiB);

        // Attempt checkout as testUser (Customer A) but using user B's saved address ID
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("idDiaChiLuu", String.valueOf(soDiaChiB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới."));
    }

    @Test
    void testSavedAddressDeleted() throws Exception {
        // Create saved address for testUser
        SoDiaChi soDiaChi = new SoDiaChi();
        soDiaChi.setKhachHang(testKhachHang);
        soDiaChi.setHoNguoiNhan("Tester");
        soDiaChi.setTenNguoiNhan("Address");
        soDiaChi.setSdtNguoiNhan("0987654321");
        soDiaChi.setDiaChiCuThe("456 Street A");
        soDiaChi.setTinhThanh("Hà Nội");
        soDiaChi.setThanhPho("Quận Cầu Giấy");
        soDiaChi.setQuocGia("Việt Nam");
        soDiaChi.setMaBuuDien("10000");
        soDiaChi.setDefaultShipping(false);
        soDiaChi.setDefaultBilling(false);
        soDiaChi = soDiaChiRepository.save(soDiaChi);

        Integer savedAddressId = soDiaChi.getId();

        // Delete the address to simulate concurrent deletion after loading page
        soDiaChiRepository.delete(soDiaChi);

        // Attempt checkout and verify it fails transaction-safely
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("idDiaChiLuu", String.valueOf(savedAddressId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới."));
    }

    @Test
    void testShippingFeeRecalculation() throws Exception {
        // Submit a valid checkout request
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Nguyễn Văn A")
                        .param("sdtNhan", "0912345678")
                        .param("diaChiNhan", "Hà Nội")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("ghnToDistrictId", "1454")
                        .param("ghnProvinceId", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        // The server computes the fee independently and stores it.
        assertNotNull(savedOrder.getPhiVanChuyen());
        assertTrue(savedOrder.getPhiVanChuyen().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGhnMappingMissing() throws Exception {
        // Create saved address with unmappable values
        SoDiaChi unmappableAddress = new SoDiaChi();
        unmappableAddress.setKhachHang(testKhachHang);
        unmappableAddress.setHoNguoiNhan("Tester");
        unmappableAddress.setTenNguoiNhan("Unmappable");
        unmappableAddress.setSdtNguoiNhan("0987654321");
        unmappableAddress.setDiaChiCuThe("Something Weird");
        unmappableAddress.setTinhThanh("Fake Province");
        unmappableAddress.setThanhPho("Fake City");
        unmappableAddress.setQuocGia("Việt Nam");
        unmappableAddress.setMaBuuDien("10000");
        unmappableAddress.setDefaultShipping(false);
        unmappableAddress.setDefaultBilling(false);
        unmappableAddress = soDiaChiRepository.save(unmappableAddress);

        // Attempt checkout using this saved address ID
        mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .requestAttr("_csrf", csrfToken)
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD")
                        .param("idDiaChiLuu", String.valueOf(unmappableAddress.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ đã lưu của bạn chưa được chuẩn hóa địa chỉ GHN. Vui lòng cập nhật sổ địa chỉ hoặc chọn \"Nhập địa chỉ mới\"."));
    }
}
