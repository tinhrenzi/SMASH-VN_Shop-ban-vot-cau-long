package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.UserAddressService;
import com.smashvn.shop.service.UserDashboardService;

@Controller
@RequestMapping("/user/address") // Gom chung đường dẫn gốc
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;
    private final UserDashboardService dashboardService;

    // Hàm private dùng chung hỗ trợ kiểm tra Session
    private KhachHang getLoggedInCustomer(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        return (idTaiKhoan != null) ? dashboardService.layThongTinKhachHang(idTaiKhoan) : null;
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

    // 1. Trang danh sách
    @GetMapping
    public String hienThiSoDiaChi(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        model.addAttribute("danhSachDiaChi", addressService.layDanhSachDiaChi(kh.getId()));
        return "dash-address-book"; 
    }

    // 2. Form thêm mới
    @GetMapping("/add")
    public String hienThiThemDiaChi(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        model.addAttribute("kh", kh);
        return "dash-address-add";
    }

    // 3. Xử lý thêm mới (Sử dụng RedirectAttributes để ẩn URL)
    @PostMapping("/add")
    public String xuLyThemDiaChi(HttpSession session, RedirectAttributes redirectAttributes,
                                 @RequestParam("hoNguoiNhan") String ho,
                                 @RequestParam("tenNguoiNhan") String ten,
                                 @RequestParam("sdtNguoiNhan") String sdt,
                                 @RequestParam("diaChiCuThe") String diaChiCuThe,
                                 @RequestParam("tinhThanh") String tinhThanh,
                                 @RequestParam("quocGia") String quocGia,
                                 @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault) {
                                 
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";

        try {
            addressService.themDiaChiMoi(kh, ho, ten, sdt, diaChiCuThe, tinhThanh, quocGia, isDefault);
            // Dùng Flash Attribute truyền thông báo an toàn
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Đã thêm địa chỉ mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Có lỗi xảy ra khi thêm địa chỉ.");
        }
        return "redirect:/user/address";
    }

    // 4. Form cập nhật
    @GetMapping("/edit/{id}")
    public String hienThiSuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";
        
        try {
            SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
            model.addAttribute("kh", kh);
            model.addAttribute("dc", dc);
            return "dash-address-edit";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Địa chỉ không tồn tại hoặc bạn không có quyền truy cập.");
            return "redirect:/user/address"; 
        }
    }

    // 5. Xử lý cập nhật
    @PostMapping("/edit/{id}")
    public String xuLySuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session, RedirectAttributes redirectAttributes,
                                @RequestParam("hoNguoiNhan") String ho,
                                @RequestParam("tenNguoiNhan") String ten,
                                @RequestParam("sdtNguoiNhan") String sdt,
                                @RequestParam("diaChiCuThe") String diaChiCuThe,
                                @RequestParam("tinhThanh") String tinhThanh,
                                @RequestParam("quocGia") String quocGia,
                                @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault) {
                                 
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";

        try {
            addressService.capNhatDiaChi(idDiaChi, kh.getId(), ho, ten, sdt, diaChiCuThe, tinhThanh, quocGia, isDefault);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Cập nhật địa chỉ thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Không thể cập nhật: " + e.getMessage());
        }
        return "redirect:/user/address";
    }

    // 6. Xử lý Đặt làm mặc định
    @GetMapping("/set-default/{id}")
    public String thietLapDiaChiMacDinh(@PathVariable("id") Integer idDiaChi, HttpSession session, RedirectAttributes redirectAttributes) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) return redirect;

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) return "redirect:/user/dang-nhap";

        try {
            addressService.datLamMacDinh(idDiaChi, kh.getId());
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Đã thay đổi địa chỉ giao hàng mặc định.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Lỗi: Không thể thay đổi địa chỉ mặc định.");
        }
        return "redirect:/user/address";
    }
 // 7. API Xóa địa chỉ bằng AJAX
    @GetMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> xoaDiaChiAjax(@PathVariable("id") Integer idDiaChi, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);
        
        if (kh == null) {
            response.put("trangThai", "chuadangnhap");
            return ResponseEntity.ok(response);
        }

        try {
            addressService.xoaDiaChi(idDiaChi, kh.getId());
            response.put("trangThai", "ok");
        } catch (RuntimeException e) {
            response.put("trangThai", "loi");
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}