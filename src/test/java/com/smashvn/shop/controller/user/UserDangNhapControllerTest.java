package com.smashvn.shop.controller.user;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.security.LoginRateLimiter;
import com.smashvn.shop.service.user.UserDangNhapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDangNhapControllerTest {

    @Mock
    private UserDangNhapService userDangNhapService;



    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    private UserDangNhapController userDangNhapController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDangNhapController = new UserDangNhapController(
                userDangNhapService,
                khachHangRepository,
                loginRateLimiter
        );

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(anyBoolean())).thenReturn(session);
    }

    @Test
    void testHienThiFormFormDangNhap() {
        Model model = new ConcurrentModel();
        String view = userDangNhapController.hienThiFormFormDangNhap("Lỗi test", null, model);
        assertEquals("signin", view);
        assertEquals("Lỗi test", model.getAttribute("loi"));
    }

    @Test
    void testXuLyDangNhap_Success() {
        String email = "customer@gmail.com";
        String matKhau = "password";
        String accountKey = "customer@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setEmail(email);
        tk.setVaiTro("KH");

        KhachHang kh = new KhachHang();
        kh.setHoKh("Nguyen");
        kh.setTenKh("Van A");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("redirect:/", view);
        verify(loginRateLimiter).loginSucceeded(accountKey);
        verify(request).changeSessionId();
        verify(session).setAttribute("nguoiDungDangNhap", email);
        verify(session).setAttribute("idNguoiDung", 1);
        verify(session).setAttribute("vaiTro", "KH");
        verify(session).setAttribute("tenHienThi", "Nguyen Van A");
    }

    @Test
    void testXuLyDangNhap_BlockedAccount() {
        String email = "customer@gmail.com";
        String matKhau = "password";
        String accountKey = "customer@gmail.com";

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertTrue(model.getAttribute("loi").toString().contains("tạm thời bị khóa"));
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_BlockedAccountDoesNotBlockOtherAccountInSameBrowser() {
        String blockedEmail = "locked@gmail.com";
        String email = "customer@gmail.com";
        String matKhau = "password";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setEmail(email);
        tk.setVaiTro("KH");

        when(loginRateLimiter.isBlocked(blockedEmail)).thenReturn(true);
        when(loginRateLimiter.isBlocked(email)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("redirect:/", view);
        verify(loginRateLimiter, never()).isBlocked("127.0.0.1");
        verify(userDangNhapService).kiemTraDangNhap(email, matKhau);
    }

    @Test
    void testXuLyDangNhap_InvalidCredentials() {
        String email = "wrong@gmail.com";
        String matKhau = "wrongpass";
        String accountKey = "wrong@gmail.com";

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau))
                .thenThrow(new RuntimeException("Email hoặc mật khẩu không chính xác!"));

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Email hoặc mật khẩu không chính xác!", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_NotCustomerAccount() {
        String email = "admin@gmail.com";
        String matKhau = "adminpass";
        String accountKey = "admin@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(2);
        tk.setEmail(email);
        tk.setVaiTro("NV"); // Not a customer account

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_BlankEmail() {
        String email = "";
        String matKhau = "password";

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Email hoặc mật khẩu không chính xác!", model.getAttribute("loi"));
        verifyNoInteractions(loginRateLimiter);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_BlankPassword() {
        String email = "customer@gmail.com";
        String matKhau = "";
        String accountKey = "customer@gmail.com";

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Email hoặc mật khẩu không chính xác!", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_InvalidEmailFormat() {
        String email = "invalid-email";
        String matKhau = "password";
        String accountKey = "invalid-email";

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Email hoặc mật khẩu không chính xác!", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_TrimsEmail() {
        String email = " customer@gmail.com ";
        String matKhau = "password";
        String accountKey = "customer@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setEmail("customer@gmail.com");
        tk.setVaiTro("KH");

        KhachHang kh = new KhachHang();
        kh.setHoKh("Nguyen");
        kh.setTenKh("Van A");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap("customer@gmail.com", matKhau)).thenReturn(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("redirect:/", view);
        verify(userDangNhapService).kiemTraDangNhap("customer@gmail.com", matKhau);
    }

    @Test
    void testXuLyDangXuat() {
        String view = userDangNhapController.xuLyDangXuat(session);
        assertEquals("redirect:/user/dang-nhap", view);
        verify(session).invalidate();
    }

    @Test
    void testXuLyDangNhap_NullRole() {
        String email = "nullrole@gmail.com";
        String matKhau = "password";
        String accountKey = "nullrole@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(3);
        tk.setEmail(email);
        tk.setVaiTro(null);

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_EmptyRole() {
        String email = "emptyrole@gmail.com";
        String matKhau = "password";
        String accountKey = "emptyrole@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(4);
        tk.setEmail(email);
        tk.setVaiTro("");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_InvalidRole() {
        String email = "invalidrole@gmail.com";
        String matKhau = "password";
        String accountKey = "invalidrole@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(5);
        tk.setEmail(email);
        tk.setVaiTro("UNKNOWN_ROLE");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }
}
