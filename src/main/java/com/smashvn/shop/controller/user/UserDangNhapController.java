package com.smashvn.shop.controller.user;

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
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.security.LoginRateLimiter;
import com.smashvn.shop.service.user.UserDangNhapService;

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
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final LoginRateLimiter loginRateLimiter;

    // Hiển thị form đăng nhập
    @GetMapping("/dang-nhap")
    public String hienThiFormFormDangNhap(@RequestParam(value = "loi", required = false) String loi, Model model) {
        if (loi != null) {
            model.addAttribute("loi", loi);
        }
        return "signin";
    }
    // Xử lý khi bấm nút "Đăng nhập"
    @PostMapping("/dang-nhap")
    public String xuLyDangNhap(@RequestParam("email") String email,
            @RequestParam("matKhau") String matKhau,
            HttpServletRequest request,
            HttpSession session, // Dùng để lưu phiên đăng nhập
            Model model) {
        String ip = request.getRemoteAddr();

        // 1. Kiểm tra giới hạn số lần thử (Rate limiting)
        if (loginRateLimiter.isBlocked(ip)) {
            model.addAttribute("loi", "Tài khoản tạm thời bị khóa đăng nhập do nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
            return "signin";
        }

        // Sanitize và Trim inputs
        String sanitizedEmail = sanitizeInput(email);
        String trimmedEmail = (sanitizedEmail != null) ? sanitizedEmail.trim() : "";

        // Sơ bộ validation tại controller
        boolean invalidInput = false;
        if (trimmedEmail.isEmpty() || trimmedEmail.length() > 100 || !trimmedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            invalidInput = true;
        }
        if (matKhau == null || matKhau.isEmpty()) {
            invalidInput = true;
        }

        if (invalidInput) {
            loginRateLimiter.loginFailed(ip);
            log.warn("[SECURITY_EVENT] INVALID_LOGIN_INPUT: IP: {}, Email: {}", ip, trimmedEmail);
            model.addAttribute("loi", "Email hoặc mật khẩu không chính xác!");
            return "signin";
        }

        try {
            TaiKhoan tkDangNhap = userDangNhapService.kiemTraDangNhap(trimmedEmail, matKhau);

            // Chỉ cho phép KH
            if (!Boolean.TRUE.equals(tkDangNhap.getLaKhachHang())) {
                loginRateLimiter.loginFailed(ip);
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_CUSTOMER_LOGIN_ATTEMPT: Email: {}, IP: {}", trimmedEmail, ip);
                model.addAttribute("loi", "Email hoặc mật khẩu không chính xác!");
                return "signin";
            }

            // Đăng nhập thành công -> Reset bộ đếm rate limiter
            loginRateLimiter.loginSucceeded(ip);

            // 2. Chống Session Fixation
            request.changeSessionId();
            session = request.getSession(true);

            // Lưu thông tin vào Session (ví dụ lưu email và id)
            session.setAttribute("nguoiDungDangNhap", tkDangNhap.getEmail());
            session.setAttribute("idNguoiDung", tkDangNhap.getId());
            session.setAttribute("vaiTro", "KH");
            session.setAttribute("activeRole", "KH");
            session.setAttribute("laKhachHang", true);
            session.setAttribute("laNhanVien", Boolean.TRUE.equals(tkDangNhap.getLaNhanVien()));
            session.setAttribute("laQuanLy", Boolean.TRUE.equals(tkDangNhap.getLaQuanLy()));

            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tkDangNhap.getId());
            if (kh != null) {
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
            } else {
                session.setAttribute("tenHienThi", "Khách hàng");
            }

            // Đăng nhập thành công, chuyển hướng về trang chủ
            return "redirect:/";

        } catch (RuntimeException e) {
            // Đăng nhập thất bại -> Tăng bộ đếm và ghi log an ninh ra file
            loginRateLimiter.loginFailed(ip);
            log.warn("[SECURITY_EVENT] FAILED_LOGIN: Email: {}, IP: {}, Lỗi: {}", trimmedEmail, ip, e.getMessage());

            // Luôn trả về thông báo lỗi chung
            model.addAttribute("loi", "Email hoặc mật khẩu không chính xác!");
            return "signin";
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        // Loại bỏ thẻ HTML/Script cơ bản để tránh XSS/injection thô sơ
        return input.replaceAll("<[^>]*>", "");
    }

    // Thêm luôn chức năng Đăng xuất cho tiện
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

            if (!Boolean.TRUE.equals(tk.getLaKhachHang())) {
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_GOOGLE_LOGIN_ATTEMPT: Email: {}, IP: {}", email, ip);
                return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode("Tài khoản không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }

            // Chống Session Fixation
            request.changeSessionId();
            session = request.getSession(true);

            // ÉP VÀO SESSION CỤC BỘ (Giúp Giỏ hàng và Dashboard nhận diện được user)
            session.setAttribute("nguoiDungDangNhap", tk.getEmail());
            session.setAttribute("idNguoiDung", tk.getId());
            session.setAttribute("vaiTro", "KH");
            session.setAttribute("activeRole", "KH");
            session.setAttribute("laKhachHang", true);
            session.setAttribute("laNhanVien", Boolean.TRUE.equals(tk.getLaNhanVien()));
            session.setAttribute("laQuanLy", Boolean.TRUE.equals(tk.getLaQuanLy()));

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
