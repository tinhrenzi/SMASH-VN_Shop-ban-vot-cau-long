package com.smashvn.shop.controller.user;

import java.util.Locale;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.exception.AccountNotFoundException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.security.LoginRateLimiter;
import com.smashvn.shop.service.user.UserDangNhapService;
import com.smashvn.shop.util.LoginIdentifierClassifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserDangNhapController {

    private final UserDangNhapService userDangNhapService;
    private final KhachHangRepository khachHangRepository;
    private final LoginRateLimiter loginRateLimiter;

    // Hiển thị form đăng nhập
    @GetMapping("/dang-nhap")
    public String hienThiFormFormDangNhap(
            @RequestParam(value = "loi", required = false) String loi,
            @RequestParam(value = "thanhcong", required = false) String thanhcong,
            Model model) {
        if (loi != null) {
            model.addAttribute("loi", loi);
        }
        if (thanhcong != null) {
            model.addAttribute("thanhcong", thanhcong);
        }
        return "signin";
    }

    // Xử lý khi bấm nút "Đăng nhập"
    @PostMapping("/dang-nhap")
    public String xuLyDangNhap(@RequestParam("username") String username,
            @RequestParam("matKhau") String matKhau,
            HttpServletRequest request,
            HttpSession session,
            Model model) {
        String ip = request.getRemoteAddr();

        // Sanitize và Trim inputs
        String sanitizedUsername = sanitizeInput(username);
        String trimmedUsername = (sanitizedUsername != null) ? sanitizedUsername.trim() : "";
        String loginLimitKey = buildLoginLimitKey(trimmedUsername);

        // 1. Kiểm tra giới hạn số lần thử theo tài khoản, không khóa theo trình duyệt/IP
        if (!loginLimitKey.isEmpty() && loginRateLimiter.isBlocked(loginLimitKey)) {
            model.addAttribute("loi", "Tài khoản tạm thời bị khóa đăng nhập do nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
            return "signin";
        }

        // Sơ bộ validation tại controller bằng classifier dùng chung
        boolean invalidInput = false;
        try {
            LoginIdentifierClassifier.classifyAndNormalize(trimmedUsername);
        } catch (IllegalArgumentException e) {
            invalidInput = true;
        }
        if (matKhau == null || matKhau.isEmpty()) {
            invalidInput = true;
        }

        if (invalidInput) {
            if (!loginLimitKey.isEmpty()) {
                loginRateLimiter.loginFailed(loginLimitKey);
            }
            log.warn("[SECURITY_EVENT] INVALID_LOGIN_INPUT: IP: {}, Username: {}", ip, trimmedUsername);
            model.addAttribute("loi", "Email hoặc mật khẩu không chính xác!");
            return "signin";
        }

        try {
            TaiKhoan tkDangNhap = userDangNhapService.kiemTraDangNhap(trimmedUsername, matKhau);

            // Chỉ cho phép KH
            String userRole = tkDangNhap.getVaiTro();
            if (!"KH".equals(userRole)) {
                loginRateLimiter.loginFailed(loginLimitKey);
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_CUSTOMER_LOGIN_ATTEMPT: Username: {}, IP: {}", trimmedUsername, ip);
                model.addAttribute("loi", "Tài khoản này không được phép đăng nhập tại trang khách hàng.");
                return "signin";
            }

            // Đăng nhập thành công -> Reset bộ đếm rate limiter
            loginRateLimiter.loginSucceeded(loginLimitKey);

            // 2. Chống Session Fixation
            request.changeSessionId();
            session = request.getSession(true);

            // Xóa bỏ hoàn toàn trạng thái Guest nếu có
            session.removeAttribute("isGuestView");
            session.removeAttribute("guestCheckoutEmail");
            session.removeAttribute("allowedGuestOrderAccesses");

            // Lưu thông tin vào Session (giữ nguyên kiểu dữ liệu String cho nguoiDungDangNhap)
            session.setAttribute("nguoiDungDangNhap", tkDangNhap.getUsername());
            session.setAttribute("idNguoiDung", tkDangNhap.getId());
            session.setAttribute("vaiTro", tkDangNhap.getVaiTro());
            session.setAttribute("activeRole", "KH");

            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tkDangNhap.getId());
            if (kh != null) {
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
            } else {
                session.setAttribute("tenHienThi", "Khách hàng");
            }

            // Đăng nhập thành công, chuyển hướng về trang chủ
            return "redirect:/";

        } catch (AccountNotFoundException e) {
            // Đăng nhập thất bại do username chưa đăng ký -> Đưa ra luồng riêng
            loginRateLimiter.loginFailed(loginLimitKey);
            log.warn("[SECURITY_EVENT] UNREGISTERED_USERNAME_LOGIN: Username: {}, IP: {}", trimmedUsername, ip);
            model.addAttribute("emailChuaDangKy", true);
            model.addAttribute("emailNhap", trimmedUsername);
            return "signin";
        } catch (RuntimeException e) {
            // Đăng nhập thất bại -> Tăng bộ đếm và ghi log an ninh ra file
            loginRateLimiter.loginFailed(loginLimitKey);
            log.warn("[SECURITY_EVENT] FAILED_LOGIN: Username: {}, IP: {}, Lỗi: {}", trimmedUsername, ip, e.getMessage());

            // Luôn trả về thông báo lỗi chung
            model.addAttribute("loi", "Email hoặc mật khẩu không chính xác!");
            return "signin";
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // Loại bỏ thẻ HTML/Script cơ bản để tránh XSS/injection thô sơ
        return input.replaceAll("<[^>]*>", "");
    }

    private String buildLoginLimitKey(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    // Đăng xuất
    @GetMapping("/dang-xuat")
    public String xuLyDangXuat(HttpSession session) {
        if (session != null) {
            session.invalidate(); // Xóa toàn bộ dữ liệu trong Session
        }
        SecurityContextHolder.clearContext(); // Xóa context của Spring Security
        return "redirect:/user/dang-nhap";
    }

    @GetMapping("/google-success")
    public String googleSuccess(OAuth2AuthenticationToken oauth2Token, HttpServletRequest request, HttpSession session) {
        // Rút trích Email và Tên từ Google
        String email = oauth2Token.getPrincipal().getAttribute("email");
        String name = oauth2Token.getPrincipal().getAttribute("name");
        String ip = request.getRemoteAddr();

        try {
            // Xử lý tạo/lấy tài khoản từ DB
            TaiKhoan tk = userDangNhapService.xuLyDangNhapGoogle(email, name);

            String userRole = tk.getVaiTro();
            if (!"KH".equals(userRole)) {
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_GOOGLE_LOGIN_ATTEMPT: Email: {}, IP: {}", email, ip);
                return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode("Tài khoản này không được phép đăng nhập tại trang khách hàng.", java.nio.charset.StandardCharsets.UTF_8);
            }

            // Chống Session Fixation
            request.changeSessionId();
            session = request.getSession(true);

            // ÉP VÀO SESSION CỤC BỘ (Giúp Giỏ hàng và Dashboard nhận diện được user)
            session.setAttribute("nguoiDungDangNhap", tk.getUsername());
            session.setAttribute("idNguoiDung", tk.getId());
            session.setAttribute("vaiTro", tk.getVaiTro());
            session.setAttribute("activeRole", "KH");

            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
            } else {
                session.setAttribute("tenHienThi", name != null ? name : "Người dùng Google");
            }

            return "redirect:/";
        } catch (RuntimeException e) {
            log.warn("[SECURITY_EVENT] FAILED_LOGIN: Google Email: {}, IP: {}, Lỗi: {}", email, ip, e.getMessage());
            return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
