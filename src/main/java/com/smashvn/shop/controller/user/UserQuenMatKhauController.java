package com.smashvn.shop.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.smashvn.shop.service.user.UserQuenMatKhauService;
import com.smashvn.shop.security.ForgotPasswordRateLimiter;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserQuenMatKhauController {

    private final UserQuenMatKhauService quenMatKhauService;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    // 1. Hiển thị trang nhập Email
    @GetMapping("/quen-mat-khau")
    public String hienThiTrangQuenMK() {
        return "lost-password"; 
    }

    // 2. Xử lý Gửi Email
    @PostMapping("/quen-mat-khau")
    public String xuLyQuenMK(@RequestParam("email") String email, HttpServletRequest request, Model model) {
        String ip = request.getRemoteAddr();

        // Kiểm tra Rate Limiting
        if (forgotPasswordRateLimiter.isBlocked(ip)) {
            model.addAttribute("loi", "Hành động bị chặn tạm thời do yêu cầu quá nhiều lần liên tiếp. Vui lòng thử lại sau 15 phút.");
            return "lost-password";
        }

        // Sanitize và Trim email
        String sanitizedEmail = sanitizeInput(email);
        String trimmedEmail = (sanitizedEmail != null) ? sanitizedEmail.trim() : "";

        // Validate email format and length
        if (trimmedEmail.isEmpty()) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", "Email không được để trống!");
            return "lost-password";
        }
        if (trimmedEmail.length() > 100) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", "Email không được vượt quá 100 ký tự!");
            return "lost-password";
        }
        if (!trimmedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", "Định dạng email không hợp lệ!");
            return "lost-password";
        }

        try {
            // Lấy địa chỉ gốc của web (VD: http://localhost:8080)
            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            
            quenMatKhauService.guiYeuCauKhoiPhuc(trimmedEmail, appUrl);
            forgotPasswordRateLimiter.forgotPasswordSucceeded(ip);
            model.addAttribute("thongBao", "Đường link khôi phục mật khẩu đã được gửi vào Email của bạn. Vui lòng kiểm tra hộp thư!");
        } catch (RuntimeException e) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", e.getMessage());
        }
        return "lost-password";
    }

    // 3. Khách click vào Link trong Email -> Hiển thị form nhập Pass mới
    @GetMapping("/dat-lai-mat-khau")
    public String hienThiTrangDatLaiMK(@RequestParam("token") String token, Model model) {
        try {
            quenMatKhauService.kiemTraToken(token); // Nếu lỗi sẽ văng ra Exception
            model.addAttribute("token", token);
            return "reset-password"; 
        } catch (RuntimeException e) {
            model.addAttribute("loi", e.getMessage());
            return "lost-password"; // Lỗi thì đá về lại trang Quên MK
        }
    }

    // 4. Khách bấm lưu Pass mới
    @PostMapping("/dat-lai-mat-khau")
    public String xuLyDatLaiMK(@RequestParam("token") String token, 
                               @RequestParam("matKhauMoi") String matKhauMoi, 
                               @RequestParam("xacNhanMatKhau") String xacNhanMatKhau, 
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                               Model model) {
        
        // Validate password strength: 8-30 chars, contains both letter and digit, no spaces
        if (matKhauMoi == null || matKhauMoi.isEmpty()) {
            model.addAttribute("loi", "Mật khẩu mới không được để trống!");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (matKhauMoi.length() < 8 || matKhauMoi.length() > 30) {
            model.addAttribute("loi", "Mật khẩu phải dài từ 8 đến 30 ký tự!");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (matKhauMoi.contains(" ") || matKhauMoi.contains("\t") || matKhauMoi.contains("\n") || matKhauMoi.contains("\r")) {
            model.addAttribute("loi", "Mật khẩu không được chứa khoảng trắng!");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (!matKhauMoi.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            model.addAttribute("loi", "Mật khẩu phải chứa cả chữ và số!");
            model.addAttribute("token", token);
            return "reset-password";
        }

        // Kiểm tra 2 mật khẩu có khớp nhau không
        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            quenMatKhauService.datLaiMatKhau(token, matKhauMoi);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Khôi phục mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/user/dang-nhap";
        } catch (RuntimeException e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "");
    }
}