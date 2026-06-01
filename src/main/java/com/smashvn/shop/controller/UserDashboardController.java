package com.smashvn.shop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.service.OrderViewService;
import com.smashvn.shop.service.UserDashboardService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService dashboardService;
    private final OrderViewService orderViewService;

    // Hàm dùng chung để kiểm tra đăng nhập và lấy KhachHang
    private KhachHang getLoggedInCustomer(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        return dashboardService.layThongTinKhachHang(idTaiKhoan);
    }

    @GetMapping("/dashboard")
    public String hienThiDashboard(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        model.addAttribute("orderPlaced", ordersList.size());
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", 0);

        return "dashboard"; // Trỏ đến dashboard.html
    }

    @GetMapping("/profile")
    public String hienThiHoSo(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        model.addAttribute("orderPlaced", ordersList.size());
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", 0);

        return "dash-my-profile"; // Trỏ đến dash-my-profile.html
    }

    @GetMapping("/profile/edit")
    public String hienThiSuaHoSo(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

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
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        model.addAttribute("orders", ordersList);

        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();

        model.addAttribute("orderPlaced", ordersList.size());
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", 0);
        return "dash-my-order"; // Trỏ đến dash-my-order.html
    }

    @GetMapping("/cancellation")
    public String hienThiCancellation(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        model.addAttribute("orderPlaced", ordersList.size());
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", 0);

        return "dash-cancellation";
    }

    @GetMapping("/payment-option")
    public String hienThiPaymentOption(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        model.addAttribute("orderPlaced", ordersList.size());
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", 0);

        return "dash-payment-option";
    }

    @GetMapping("/track-order")
    public String hienThiTrackOrder(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        model.addAttribute("orderPlaced", ordersList.size());
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", 0);

        return "dash-track-order";
    }

    @GetMapping({"/manage-order/{id}", "/manage-order"})
    public String hienThiManageOrder(@PathVariable(value = "id", required = false) Integer pathId,
            @RequestParam(value = "id", required = false) Integer paramId,
            HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        Integer targetId = (pathId != null) ? pathId : paramId;
        if (targetId == null) {
            return "redirect:/user/my-order";
        }

        try {
            Map<String, Object> details = orderViewService.layChiTietOrder(targetId, kh.getId());
            if (details == null) {
                return "redirect:/user/my-order?loi=donhangkhongton";
            }

            model.addAttribute("kh", kh);
            model.addAllAttributes(details);

            return "dash-manage-order";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/my-order?loi=loihethong";
        }
    }
}
