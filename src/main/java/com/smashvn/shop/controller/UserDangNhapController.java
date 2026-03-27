package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.service.UserDangNhapService;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDangNhapController {

    private final UserDangNhapService userDangNhapService;

    // Hiển thị form đăng nhập
    @GetMapping("/dang-nhap")
    public String hienThiFormDangNhap() {
        return "user-dang-nhap"; 
    }

    // Xử lý khi bấm nút "Đăng nhập"
    @PostMapping("/dang-nhap")
    public String xuLyDangNhap(@RequestParam("email") String email,
                               @RequestParam("matKhau") String matKhau,
                               HttpSession session, // Dùng để lưu phiên đăng nhập
                               Model model) {
        try {
            TaiKhoan tkDangNhap = userDangNhapService.kiemTraDangNhap(email, matKhau);
            
            // Lưu thông tin vào Session (ví dụ lưu email và id)
            session.setAttribute("nguoiDungDangNhap", tkDangNhap.getEmail());
            session.setAttribute("idNguoiDung", tkDangNhap.getId());
            session.setAttribute("vaiTro", tkDangNhap.getVaiTro());

            // Đăng nhập thành công, chuyển hướng về Trang chủ
            return "redirect:/"; 
            
        } catch (RuntimeException e) {
            // Đăng nhập thất bại, báo lỗi ra màn hình
            model.addAttribute("loi", e.getMessage());
            return "user-dang-nhap";
        }
    }
    
    // Thêm luôn chức năng Đăng xuất cho tiện
    @GetMapping("/dang-xuat")
    public String xuLyDangXuat(HttpSession session) {
        session.invalidate(); // Xóa toàn bộ dữ liệu trong Session
        return "redirect:/user/dang-nhap";
    }
}
