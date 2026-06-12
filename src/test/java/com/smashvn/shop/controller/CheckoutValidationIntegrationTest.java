package com.smashvn.shop.controller;

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
}
