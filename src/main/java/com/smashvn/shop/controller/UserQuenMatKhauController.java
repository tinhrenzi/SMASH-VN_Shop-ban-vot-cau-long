package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.smashvn.shop.service.UserQuenMatKhauService;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserQuenMatKhauController {

    private final UserQuenMatKhauService quenMatKhauService;

    // Hiển thị giao diện Quên mật khẩu
    @GetMapping("/quen-mat-khau")
    public String hienThiTrangQuenMK() {
        return "lost-password"; // Trỏ đúng tên file HTML bạn đã upload
    }

    // Xử lý khi khách bấm nút "GỬI MẬT KHẨU MỚI"
    @PostMapping("/quen-mat-khau")
    public String xuLyQuenMK(@RequestParam("email") String email, Model model) {
        try {
            quenMatKhauService.guiLaiMatKhau(email);
            // Gửi thông báo thành công sang HTML
            model.addAttribute("thongBao", "Mật khẩu mới đã được gửi vào Email của bạn. Vui lòng kiểm tra hộp thư!");
        } catch (RuntimeException e) {
            // Gửi thông báo lỗi nếu email không đúng
            model.addAttribute("loi", e.getMessage());
        }
        return "lost-password";
    }
}