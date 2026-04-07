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
        // Assume dashboardService has method to get orders
        // model.addAttribute("orders", dashboardService.layDanhSachDonHang(idTaiKhoan));
        // For now, add dummy data or implement later
        model.addAttribute("orderPlaced", 4);
        model.addAttribute("cancelOrders", 0);
        model.addAttribute("wishlist", 0);
        return "dash-my-order"; // Trỏ đến dash-my-order.html
    }
    
 // 1. Hiển thị danh sách Sổ địa chỉ
    @GetMapping("/address")
    public String hienThiSoDiaChi(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        model.addAttribute("danhSachDiaChi", addressService.layDanhSachDiaChi(kh.getId()));
        
        return "dash-address-book"; // Trỏ đến file HTML
    }

    // 2. Hiển thị Form Thêm địa chỉ
    @GetMapping("/address/add")
    public String hienThiThemDiaChi(HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        return "dash-address-add";
    }

    // 3. Xử lý Hứng dữ liệu lưu Địa chỉ mới
    @PostMapping("/address/add")
    public String xuLyThemDiaChi(HttpSession session,
                                 @RequestParam("hoNguoiNhan") String ho,
                                 @RequestParam("tenNguoiNhan") String ten,
                                 @RequestParam("sdtNguoiNhan") String sdt,
                                 @RequestParam("diaChiCuThe") String diaChiCuThe,
                                 @RequestParam("tinhThanh") String tinhThanh,
                                 @RequestParam("quocGia") String quocGia,
                                 @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault) {
                                 
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";

        addressService.themDiaChiMoi(kh, ho, ten, sdt, diaChiCuThe, tinhThanh, quocGia, isDefault);
        
        // Lưu xong quay lại trang Sổ địa chỉ
        return "redirect:/user/address?thanhcong";
    }

    @GetMapping("/address/set-default/{id}")
    public String thietLapDiaChiMacDinh(@PathVariable("id") Integer idDiaChi, HttpSession session) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";

        try {
            addressService.datLamMacDinh(idDiaChi, kh.getId());
        } catch (Exception e) {
            // Xử lý lỗi nếu có
            return "redirect:/user/address?loi";
        }

        // Cập nhật xong thì quay lại trang Sổ địa chỉ kèm thông báo
        return "redirect:/user/address?macdinhthanhcong";
    }
    
 // 4. Hiển thị Form Sửa địa chỉ
    @GetMapping("/address/edit/{id}")
    public String hienThiSuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session, Model model) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        try {
            SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
            model.addAttribute("kh", kh);
            model.addAttribute("dc", dc); // Ném dữ liệu địa chỉ sang giao diện
            return "dash-address-edit";
        } catch (RuntimeException e) {
            return "redirect:/user/address?loi"; // Nếu cố tình sửa địa chỉ người khác thì văng ra ngoài
        }
    }

    // 5. Xử lý Hứng dữ liệu cập nhật
    @PostMapping("/address/edit/{id}")
    public String xuLySuaDiaChi(@PathVariable("id") Integer idDiaChi,
                                HttpSession session,
                                @RequestParam("hoNguoiNhan") String ho,
                                @RequestParam("tenNguoiNhan") String ten,
                                @RequestParam("sdtNguoiNhan") String sdt,
                                @RequestParam("diaChiCuThe") String diaChiCuThe,
                                @RequestParam("tinhThanh") String tinhThanh,
                                @RequestParam("quocGia") String quocGia,
                                @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault) {
                                 
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";

        try {
            addressService.capNhatDiaChi(idDiaChi, kh.getId(), ho, ten, sdt, diaChiCuThe, tinhThanh, quocGia, isDefault);
            return "redirect:/user/address?capnhatthanhcong";
        } catch (RuntimeException e) {
            return "redirect:/user/address?loi";
        }
    }
}