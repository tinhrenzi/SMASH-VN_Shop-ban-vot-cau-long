package com.smashvn.shop.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.security.RegisterRateLimiter;
import com.smashvn.shop.service.user.UserDangKyService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDangKyController {

    private final UserDangKyService taiKhoanService;
    private final RegisterRateLimiter registerRateLimiter;

    // Hiển thị form đăng ký (Giao diện)
    @GetMapping("/dang-ky")
    public String hienThiTrangDangKy(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email != null) {
            model.addAttribute("email", email);
        }
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

        // Kiểm tra 2 mật khẩu có khớp nhau không
        if (matKhau == null || matKhau.isEmpty()) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("loi", "Mật khẩu không được để trống!");
            return "signup";
        }
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
        if (input == null) {
            return null;
        }
        // Loại bỏ thẻ HTML/Script cơ bản để tránh XSS/injection thô sơ
        return input.replaceAll("<[^>]*>", "");
    }
}
