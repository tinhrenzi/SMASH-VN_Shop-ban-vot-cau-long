package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.service.UserDangKyService;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDangKyController {

    private final UserDangKyService taiKhoanService;

    // Hiển thị form đăng ký (Giao diện)
    @GetMapping("/dang-ky")
    public String hienThiTrangDangKy() {
        return "user-dang-ky"; // Sẽ tìm file dang-ky.html trong thư mục templates
    }

    // Xử lý khi người dùng bấm nút Submit form
    @PostMapping("/dang-ky")
    public String xuLyDangKy(@RequestParam("email") String email,
                             @RequestParam("matKhau") String matKhau,
                             @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
                             Model model) {
        
        // Kiểm tra 2 mật khẩu có khớp nhau không
        if (!matKhau.equals(xacNhanMatKhau)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            return "user-dang-ky"; // Trả lại trang đăng ký kèm câu báo lỗi
        }

        try {
            // Gọi Service để lưu
            taiKhoanService.dangKy(email, matKhau);
            
            // Nếu thành công, chuyển hướng người dùng sang trang Đăng nhập
            return "redirect:/user/dang-nhap?thanhcong"; 
            
        } catch (RuntimeException e) {
            // Bắt lỗi (ví dụ: trùng email) và hiển thị lên giao diện
            model.addAttribute("loi", e.getMessage());
            return "user-dang-ky";
        }
    }
}
