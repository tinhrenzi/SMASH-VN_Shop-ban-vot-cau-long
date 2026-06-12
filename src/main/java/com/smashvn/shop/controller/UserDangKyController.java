package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.service.UserDangKyService;
import com.smashvn.shop.security.RegisterRateLimiter;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDangKyController {

    private final UserDangKyService taiKhoanService;
    private final RegisterRateLimiter registerRateLimiter;

    // Hiển thị form đăng ký (Giao diện)
    @GetMapping("/dang-ky")
    public String hienThiTrangDangKy() {
        return "signup"; // Sẽ tìm file dang-ky.html trong thư mục templates
    }

    // Xử lý khi người dùng bấm nút Submit form
    @PostMapping("/dang-ky")
    public String xuLyDangKy(@RequestParam("email") String email,
                             @RequestParam("matKhau") String matKhau,
                             @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
                             HttpServletRequest request,
                             Model model) {
        
        String ip = request.getRemoteAddr();

        // 1. Kiểm tra giới hạn số lần thử (Rate limiting)
        if (registerRateLimiter.isBlocked(ip)) {
            model.addAttribute("loi", "Hành động bị chặn tạm thời do đăng ký quá nhiều lần liên tiếp. Vui lòng thử lại sau 15 phút.");
            return "signup";
        }

        // Sanitize và Trim inputs
        String sanitizedEmail = sanitizeInput(email);
        String trimmedEmail = (sanitizedEmail != null) ? sanitizedEmail.trim() : "";

        // Validate email format and length
        if (trimmedEmail.isEmpty()) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Email không được để trống!");
            return "signup";
        }
        if (trimmedEmail.length() > 100) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Email không được vượt quá 100 ký tự!");
            return "signup";
        }
        if (!trimmedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Định dạng email không hợp lệ!");
            return "signup";
        }

        // Validate password strength: 8-30 chars, contains both letter and digit, no spaces
        if (matKhau == null || matKhau.isEmpty()) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Mật khẩu không được để trống!");
            return "signup";
        }
        if (matKhau.length() < 8 || matKhau.length() > 30) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Mật khẩu phải dài từ 8 đến 30 ký tự!");
            return "signup";
        }
        if (matKhau.contains(" ") || matKhau.contains("\t") || matKhau.contains("\n") || matKhau.contains("\r")) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Mật khẩu không được chứa khoảng trắng!");
            return "signup";
        }
        if (!matKhau.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Mật khẩu phải chứa cả chữ và số!");
            return "signup";
        }

        // Kiểm tra 2 mật khẩu có khớp nhau không
        if (!matKhau.equals(xacNhanMatKhau)) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            return "signup";
        }

        try {
            // Gọi Service để lưu
            taiKhoanService.dangKy(trimmedEmail, matKhau);
            
            // Thành công -> Reset bộ đếm
            registerRateLimiter.registerSucceeded(ip);
            
            // Nếu thành công, chuyển hướng người dùng sang trang Đăng nhập
            return "redirect:/user/dang-nhap?thanhcong"; 
            
        } catch (RuntimeException e) {
            // Thất bại -> Tăng bộ đếm và hiển thị lỗi
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", e.getMessage());
            return "signup";
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        // Loại bỏ thẻ HTML/Script cơ bản để tránh XSS/injection thô sơ
        return input.replaceAll("<[^>]*>", "");
    }
}
