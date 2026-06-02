package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.service.UserDangNhapService;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.entity.NhanVien;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDangNhapController {

    private final UserDangNhapService userDangNhapService;
    private final NhanVienRepository nhanVienRepository;

    // Hiển thị form đăng nhập
    @GetMapping("/dang-nhap")
    public String hienThiFormDangNhap() {
        return "signin"; 
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

            if ("QL".equals(tkDangNhap.getVaiTro()) || "NV".equals(tkDangNhap.getVaiTro())) {
                NhanVien nv = nhanVienRepository.findByTaiKhoanId(tkDangNhap.getId());
                if (nv != null) {
                    session.setAttribute("tenHienThi", nv.getHoTenNv());
                } else {
                    session.setAttribute("tenHienThi", "Nhân viên hệ thống");
                }
            }

            // Đăng nhập thành công, chuyển hướng theo vai trò
            if ("QL".equals(tkDangNhap.getVaiTro())) {
                return "redirect:/admin/all";
            } else if ("NV".equals(tkDangNhap.getVaiTro())) {
                return "redirect:/admin/don-hang";
            } else {
                return "redirect:/";
            }
            
        } catch (RuntimeException e) {
            // Đăng nhập thất bại, báo lỗi ra màn hình
            model.addAttribute("loi", e.getMessage());
            return "signin";
        }
    }
    
    // Thêm luôn chức năng Đăng xuất cho tiện
    @GetMapping("/dang-xuat")
    public String xuLyDangXuat(HttpSession session) {
        session.invalidate(); // Xóa toàn bộ dữ liệu trong Session
        return "redirect:/user/dang-nhap";
    }
    @GetMapping("/google-success")
    public String googleSuccess(OAuth2AuthenticationToken oauth2Token, HttpSession session) {
        // Rút trích Email và Tên từ Google
        String email = oauth2Token.getPrincipal().getAttribute("email");
        String name = oauth2Token.getPrincipal().getAttribute("name");

        try {
            // Xử lý tạo/lấy tài khoản từ DB
            TaiKhoan tk = userDangNhapService.xuLyDangNhapGoogle(email, name);
            
            // ÉP VÀO SESSION CỤC BỘ (Giúp Giỏ hàng và Dashboard nhận diện được user)
            session.setAttribute("nguoiDungDangNhap", tk.getEmail());
            session.setAttribute("idNguoiDung", tk.getId());
            session.setAttribute("vaiTro", tk.getVaiTro());
            
            // Đăng nhập thành công, chuyển hướng theo vai trò
            if ("QL".equals(tk.getVaiTro())) {
                return "redirect:/admin/all";
            } else if ("NV".equals(tk.getVaiTro())) {
                return "redirect:/admin/don-hang";
            } else {
                return "redirect:/";
            }
        } catch (RuntimeException e) {
            return "redirect:/user/dang-nhap?loi=" + e.getMessage();
        }
    }
}
