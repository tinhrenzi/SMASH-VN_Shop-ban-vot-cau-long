package com.smashvn.shop.security;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GuestCheckoutService;
import com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess;

@SpringBootTest
@Transactional
public class GuestCheckoutSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private TokenKhoiPhucRepository tokenRepository;
    @Autowired
    private com.smashvn.shop.dao.DonViVanChuyenDAO donViVanChuyenDAO;
    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    @Autowired
    private GuestCheckoutService guestCheckoutService;

    private MockMvc mockMvc;
    private DonViVanChuyen testDvvc;
    private PhuongThucThanhToan testPttt;
    private CsrfToken csrfToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        List<DonViVanChuyen> vcs = donViVanChuyenDAO.findAll();
        if (vcs.isEmpty()) {
            DonViVanChuyen vc = new DonViVanChuyen();
            vc.setTenDonVi("Standard Shipping");
            vc.setHotline("1900");
            vc.setWebsite("https://smashvn.com");
            testDvvc = donViVanChuyenDAO.save(vc);
        } else {
            testDvvc = vcs.get(0);
        }

        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        if (ptts.isEmpty()) {
            PhuongThucThanhToan pt = new PhuongThucThanhToan();
            pt.setTenPhuongThuc("COD");
            testPttt = phuongThucThanhToanDAO.save(pt);
        } else {
            testPttt = ptts.get(0);
        }
    }

    @Test
    void testGuestActivationTokenCreatedSynchronously() {
        String email = "guest-token-" + System.nanoTime() + "@example.com";
        String phone = "09" + String.format("%08d", (int) (System.nanoTime() % 100000000));

        GuestCheckoutService.GuestRegisterResult result =
                guestCheckoutService.autoRegisterGuest("Guest Token", phone, email);

        assertNotNull(result.getTaiKhoan());
        assertNotNull(result.getToken());

        com.smashvn.shop.entity.TokenKhoiPhuc token = tokenRepository.findByMaXacNhan(result.getToken());
        assertNotNull(token);
        assertEquals(result.getTaiKhoan().getId(), token.getTaiKhoan().getId());
        assertEquals("EMAIL", token.getLoaiXacNhan());
        assertFalse(token.isDaSuDung());
    }

    private TaiKhoan createGuestAccount(String email) {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(email);
        tk.setMatKhau(null);
        tk.setVaiTro("KH");
        // Set GUEST status AFTER setTrangThai to avoid the else→ACTIVE overwrite
        tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);

        tk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoKh("");
        kh.setTenKh("Guest User");
        String uniqueSdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        kh.setSoDienThoaiKh(uniqueSdt);
        kh.setLaTaiKhoanNoiBo(false);
        khachHangRepository.save(kh);

        return tk;
    }

    private HoaDon createGuestOrder(KhachHang kh) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setTrangThaiDonHang("cho_xac_nhan");
        hd.setTongTien(new BigDecimal("500000"));
        hd.setDiaChiNhan("123 Street");
        hd.setSdtNhan(kh.getSoDienThoaiKh());
        hd.setMaDonHang("GUEST-ORDER-" + System.nanoTime());
        hd.setDonViVanChuyen(testDvvc);
        hd.setPhuongThucThanhToan(testPttt);
        return hoaDonRepository.save(hd);
    }

    @Test
    void testGuestCheckoutSessionIsolation() throws Exception {
        // Assert that a guest checkout session does NOT have idNguoiDung (Anonymous) and cannot access /user/dashboard
        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("guestCheckoutEmail", "guest@example.com");

        mockMvc.perform(get("/user/dashboard").session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testGuestAccountSessionCannotAccessDashboard() throws Exception {
        String email = "guest-dashboard-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("idNguoiDung", tk.getId());
        guestSession.setAttribute("guestCheckoutEmail", email);
        guestSession.setAttribute("vaiTro", "KH");

        mockMvc.perform(get("/user/dashboard").session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testGuestOrderAccess_ValidDuration() throws Exception {
        String email = "guest-valid-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        HoaDon hd = createGuestOrder(kh);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("guestCheckoutEmail", email);
        
        List<GuestOrderAccess> allowedAccesses = new ArrayList<>();
        allowedAccesses.add(new GuestOrderAccess(hd.getId(), Instant.now().plus(30, ChronoUnit.MINUTES)));
        guestSession.setAttribute("allowedGuestOrderAccesses", allowedAccesses);

        mockMvc.perform(get("/user/manage-order/" + hd.getId()).session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-manage-order"));
    }

    @Test
    void testGuestOrderAccess_Expired() throws Exception {
        String email = "guest-expired-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        HoaDon hd = createGuestOrder(kh);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("guestCheckoutEmail", email);
        
        List<GuestOrderAccess> allowedAccesses = new ArrayList<>();
        // Expired 1 second ago
        allowedAccesses.add(new GuestOrderAccess(hd.getId(), Instant.now().minus(1, ChronoUnit.SECONDS)));
        guestSession.setAttribute("allowedGuestOrderAccesses", allowedAccesses);

        mockMvc.perform(get("/user/manage-order/" + hd.getId()).session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testGuestOrderAccess_IDOR_CrossGuestBlocked() throws Exception {
        // Guest A
        String emailA = "guest-a-" + System.nanoTime() + "@example.com";
        TaiKhoan tkA = createGuestAccount(emailA);
        KhachHang khA = khachHangRepository.findByTaiKhoan_Id(tkA.getId());
        HoaDon hdA = createGuestOrder(khA);

        // Guest B
        String emailB = "guest-b-" + System.nanoTime() + "@example.com";
        TaiKhoan tkB = createGuestAccount(emailB);
        KhachHang khB = khachHangRepository.findByTaiKhoan_Id(tkB.getId());
        HoaDon hdB = createGuestOrder(khB);

        // Guest B's session trying to access Guest A's order hdA
        MockHttpSession guestSessionB = new MockHttpSession();
        guestSessionB.setAttribute("guestCheckoutEmail", emailB);
        
        List<GuestOrderAccess> allowedAccesses = new ArrayList<>();
        // Guest B tries to manipulate their session or query directly, but even if they inject hdA.getId():
        allowedAccesses.add(new GuestOrderAccess(hdA.getId(), Instant.now().plus(30, ChronoUnit.MINUTES)));
        guestSessionB.setAttribute("allowedGuestOrderAccesses", allowedAccesses);

        mockMvc.perform(get("/user/manage-order/" + hdA.getId()).session(guestSessionB).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testSessionIdRotationOnPasswordUpgrade() throws Exception {
        String email = "guest-upgrade-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("idNguoiDung", tk.getId());
        guestSession.setAttribute("guestCheckoutEmail", email);

        MvcResult result = mockMvc.perform(post("/checkout/api/set-password")
                .param("password", "strongpassword123")
                .session(guestSession)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        // Check that old session is invalidated and new session is created with login attributes
        MockHttpSession returnedSession = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(returnedSession);
        
        // Assert old session attributes (like guestCheckoutEmail) are NOT present in the rotated session
        assertNull(returnedSession.getAttribute("guestCheckoutEmail"));
        
        // Assert member authenticated attributes are present
        assertEquals(email, returnedSession.getAttribute("nguoiDungDangNhap"));
        assertEquals(tk.getId(), returnedSession.getAttribute("idNguoiDung"));
        assertEquals("KH", returnedSession.getAttribute("vaiTro"));

        // Verify status in DB is active
        TaiKhoan updatedTk = taiKhoanRepository.findById(tk.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, updatedTk.getTrangThaiTaiKhoan());
    }

    @Test
    void testAnonymousCannotUpgradeGuestByEmail() throws Exception {
        String email = "guest-attack-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        MvcResult result = mockMvc.perform(post("/checkout/api/set-password")
                .param("email", email)
                .param("password", "strongpassword123")
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("\"success\":false"));

        TaiKhoan unchangedTk = taiKhoanRepository.findById(tk.getId()).orElseThrow();
        assertEquals(AccountStatus.GUEST, unchangedTk.getTrangThaiTaiKhoan());
        assertNull(unchangedTk.getMatKhau());
    }

    @Test
    void testPasswordActivationRaceProtection() {
        String email = "guest-race-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        // First upgrade should succeed
        guestCheckoutService.setPasswordForGuest(tk.getId(), "mypassword123");

        // Second upgrade must fail with IllegalStateException (not GUEST status anymore)
        assertThrows(IllegalStateException.class, () -> {
            guestCheckoutService.setPasswordForGuest(tk.getId(), "anotherpassword123");
        });
    }
}
