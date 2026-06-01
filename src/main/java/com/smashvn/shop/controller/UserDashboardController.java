package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.UserAddressService;
import com.smashvn.shop.service.UserDashboardService;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDashboardController {

	private final UserAddressService addressService;
    private final UserDashboardService dashboardService;

    // Hàm dùng chung để kiểm tra đăng nhập và lấy KhachHang
    private KhachHang getLoggedInCustomer(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) return null;
        return dashboardService.layThongTinKhachHang(idTaiKhoan);
    }

    @GetMapping("/dashboard")
    public String hienThiDashboard(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        return "dashboard"; // Trỏ đến dashboard.html
    }

    @GetMapping("/profile")
    public String hienThiHoSo(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        return "dash-my-profile"; // Trỏ đến dash-my-profile.html
    }

    @GetMapping("/profile/edit")
    public String hienThiSuaHoSo(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        return "dash-edit-profile"; // Trỏ đến dash-edit-profile.html
    }

    @PostMapping("/profile/edit")
    public String xuLySuaHoSo(HttpSession session,
                              @RequestParam("ho") String ho,
                              @RequestParam("ten") String ten,
                              @RequestParam("sdt") String sdt) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        dashboardService.capNhatHoSo(idTaiKhoan, ho, ten, sdt);
        return "redirect:/user/profile?capNhatThanhCong";
    }

    @GetMapping("/my-order")
    public String hienThiMyOrders(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        model.addAttribute("orderPlaced", 4);
        model.addAttribute("cancelOrders", 0);
        model.addAttribute("wishlist", 0);
        return "dash-my-order"; // Trỏ đến dash-my-order.html
    }
}