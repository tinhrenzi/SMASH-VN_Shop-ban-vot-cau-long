package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpServletRequest;
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

    // 1. Hiển thị trang nhập Email
    @GetMapping("/quen-mat-khau")
    public String hienThiTrangQuenMK() {
        return "lost-password"; 
    }

    // 2. Xử lý Gửi Email
    @PostMapping("/quen-mat-khau")
    public String xuLyQuenMK(@RequestParam("email") String email, HttpServletRequest request, Model model) {
        try {
            // Lấy địa chỉ gốc của web (VD: http://localhost:8080)
            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            
            quenMatKhauService.guiYeuCauKhoiPhuc(email, appUrl);
            model.addAttribute("thongBao", "Đường link khôi phục mật khẩu đã được gửi vào Email của bạn. Vui lòng kiểm tra hộp thư!");
        } catch (RuntimeException e) {
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
            return "reset-password"; // Cần tạo thêm file HTML này
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
                               Model model) {
        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            quenMatKhauService.datLaiMatKhau(token, matKhauMoi);
            return "redirect:/user/dang-nhap?khoiphucthanhcong";
        } catch (RuntimeException e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }
    }
}