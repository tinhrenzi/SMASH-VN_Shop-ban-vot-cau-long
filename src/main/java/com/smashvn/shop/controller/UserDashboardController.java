package com.smashvn.shop.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
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
    private final SanPhamYeuThichRepository wishlistRepository;

    // Hàm dùng chung để kiểm tra đăng nhập và lấy KhachHang
    private KhachHang getLoggedInCustomer(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        return dashboardService.layThongTinKhachHang(idTaiKhoan);
    }

    private String checkRoleAndRedirect(HttpSession session) {
        String vaiTro = (String) session.getAttribute("vaiTro");
        if ("QL".equals(vaiTro)) {
            return "redirect:/admin/all";
        }
        if ("NV".equals(vaiTro)) {
            return "redirect:/admin/don-hang";
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String hienThiDashboard(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        // Lấy địa chỉ giao hàng và hóa đơn mặc định
        SoDiaChi defaultShipping = null;
        SoDiaChi defaultBilling = null;
        if (kh.getSoDiaChis() != null) {
            defaultShipping = kh.getSoDiaChis().stream()
                    .filter(SoDiaChi::isDefaultShipping)
                    .findFirst()
                    .orElse(null);
            defaultBilling = kh.getSoDiaChis().stream()
                    .filter(SoDiaChi::isDefaultBilling)
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("defaultShipping", defaultShipping);
        model.addAttribute("defaultBilling", defaultBilling);

        // Đơn hàng gần đây (giới hạn 5 đơn)
        List<Map<String, Object>> recentOrders = ordersList.stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("orders", recentOrders);

        return "dashboard"; // Trỏ đến dashboard.html
    }

    @GetMapping("/profile")
    public String hienThiHoSo(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-my-profile"; // Trỏ đến dash-my-profile.html
    }

    @GetMapping("/profile/edit")
    public String hienThiSuaHoSo(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

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
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        dashboardService.capNhatHoSo(idTaiKhoan, ho, ten, sdt);
        return "redirect:/user/profile?capNhatThanhCong";
    }

    @GetMapping("/my-order")
    public String hienThiMyOrders(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        model.addAttribute("orders", ordersList);

        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);
        return "dash-my-order"; // Trỏ đến dash-my-order.html
    }

    @GetMapping("/cancellation")
    public String hienThiCancellation(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-cancellation";
    }

    @GetMapping("/payment-option")
    public String hienThiPaymentOption(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-payment-option";
    }

    @GetMapping("/track-order")
    public String hienThiTrackOrder(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-track-order";
    }

    @GetMapping({"/manage-order/{id}", "/manage-order"})
    public String hienThiManageOrder(@PathVariable(value = "id", required = false) Integer pathId,
            @RequestParam(value = "id", required = false) Integer paramId,
            HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

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

            long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());
            model.addAttribute("wishlistCount", wishlistCount);

            return "dash-manage-order";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/my-order?loi=loihethong";
        }
    }

    @PostMapping("/manage-order/cancel/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyHuyDonHang(
            @PathVariable("id") Integer idHoaDon,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thực hiện thao tác này.");
            return org.springframework.http.ResponseEntity.ok(response);
        }
        
        String ipAddress = request.getRemoteAddr();
        try {
            boolean success = orderViewService.huyDonHang(idHoaDon, kh.getId(), ipAddress, lyDoHuy);
            if (success) {
                response.put("success", true);
                response.put("message", "Hủy đơn hàng thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể hủy đơn hàng này. Đơn hàng có thể đã được giao hoặc đang được xử lý.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }
}
