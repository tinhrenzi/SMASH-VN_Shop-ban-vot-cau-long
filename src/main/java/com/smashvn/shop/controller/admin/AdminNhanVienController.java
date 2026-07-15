package com.smashvn.shop.controller.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.service.admin.AdminNhanVienService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Controller
@RequestMapping("/admin/nhan-vien")
@RequiredArgsConstructor
public class AdminNhanVienController {

    private final AdminNhanVienService adminNhanVienService;

    private String friendlyErrorMessage(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                message += " " + cause.getMessage();
            }
            cause = cause.getCause();
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("ux_nhanvien_sodienthoai")
                || lowerMessage.contains("so_dien_thoai")
                || lowerMessage.contains("sodienthoai")) {
            return "Số điện thoại nhân viên đã tồn tại. Vui lòng nhập số khác.";
        }
        if (lowerMessage.contains("uk_email")
                || lowerMessage.contains("duplicate key")
                && lowerMessage.contains("email")) {
            return "Email đã được sử dụng.";
        }
        if (lowerMessage.contains("uk_tendangnhap")
                || lowerMessage.contains("ten_dang_nhap")
                || lowerMessage.contains("tendangnhap")) {
            return "Tên đăng nhập đã tồn tại.";
        }
        if (lowerMessage.contains("id_tai_khoan")
                || lowerMessage.contains("tai_khoan")
                || lowerMessage.contains("taikhoan")) {
            return "Tài khoản nhân viên đã tồn tại.";
        }
        if (lowerMessage.contains("cannot insert duplicate key")
                || lowerMessage.contains("duplicate key")
                || lowerMessage.contains("constraint")
                || lowerMessage.contains("hibernate")
                || lowerMessage.contains("sql")) {
            return "Dữ liệu đã tồn tại hoặc không hợp lệ. Vui lòng kiểm tra lại thông tin.";
        }
        return message.isBlank() ? "Không thể lưu nhân viên. Vui lòng kiểm tra lại thông tin." : ex.getMessage();
    }

    @GetMapping
    public String hienThiDanhSach(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {
        List<NhanVien> ds = adminNhanVienService.searchNhanVien(keyword);
        model.addAttribute("danhSachNhanVien", ds);
        model.addAttribute("keyword", keyword);
        return "admin/nhanvien-list";
    }

    @GetMapping("/them")
    public String hienThiFormThem() {
        return "admin/nhanvien-add";
    }

    @PostMapping("/them")
    public String xuLyThemNhanVien(
            @RequestParam("email") String email,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("hoTenNv") String hoTenNv,
            @RequestParam("chucVu") String chucVu,
            @RequestParam("soDienThoaiNv") String soDienThoaiNv,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminNhanVienService.createNhanVien(email, matKhau, hoTenNv, chucVu, soDienThoaiNv, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/nhan-vien?themThanhCong";
        } catch (Exception e) {
            String errorMessage = friendlyErrorMessage(e);
            model.addAttribute("error", errorMessage);
            model.addAttribute("loi", errorMessage);
            model.addAttribute("email", email);
            model.addAttribute("hoTenNv", hoTenNv);
            model.addAttribute("chucVu", chucVu);
            model.addAttribute("soDienThoaiNv", soDienThoaiNv);
            return "admin/nhanvien-add";
        }
    }

    @GetMapping("/sua/{id}")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            NhanVien nv = adminNhanVienService.findById(id);
            model.addAttribute("nv", nv);
            return "admin/nhanvien-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", friendlyErrorMessage(e));
            return "redirect:/admin/nhan-vien";
        }
    }

    @PostMapping("/sua/{id}")
    public String xuLySuaNhanVien(
            @PathVariable("id") Integer id,
            @RequestParam("hoTenNv") String hoTenNv,
            @RequestParam("chucVu") String chucVu,
            @RequestParam("soDienThoaiNv") String soDienThoaiNv,
            @RequestParam("trangThai") String trangThai,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminNhanVienService.updateNhanVien(id, hoTenNv, chucVu, soDienThoaiNv, trangThai, newPassword, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/nhan-vien?suaThanhCong";
        } catch (Exception e) {
            NhanVien nv = adminNhanVienService.findById(id);
            nv.setHoTenNv(hoTenNv);
            nv.setChucVu(chucVu);
            nv.setSoDienThoaiNv(soDienThoaiNv);
            if (nv.getTaiKhoan() != null) {
                nv.getTaiKhoan().setTrangThai(trangThai);
            }
            String errorMessage = friendlyErrorMessage(e);
            model.addAttribute("nv", nv);
            model.addAttribute("error", errorMessage);
            model.addAttribute("loi", errorMessage);
            return "admin/nhanvien-edit";
        }
    }

    @PostMapping("/toggle/{id}")
    public String xuLyToggleTrangThai(
            @PathVariable("id") Integer id,
            HttpSession session,
            HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();
            String appUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;

            adminNhanVienService.toggleStatus(id, actingTaiKhoanId, ipAddress, appUrl);
            return "redirect:/admin/nhan-vien?toggleThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/nhan-vien?loi=" + java.net.URLEncoder.encode(friendlyErrorMessage(e), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/approve-lock/{id}")
    public Object xuLyPheDuyetKhoa(
            @PathVariable("id") Integer id,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "ajax", required = false) Boolean ajax,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminNhanVienService.approveLock(id, token, actingTaiKhoanId, ipAddress);
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã phê duyệt khóa tài khoản thành công!"));
            }
            model.addAttribute("success", true);
            model.addAttribute("title", "Phê Duyệt Khóa Thành Công");
            model.addAttribute("message", "Yêu cầu khóa tài khoản nhân viên đã được duyệt thành công. Tài khoản này hiện đã bị khóa.");
            return "admin/confirm-result";
        } catch (Exception e) {
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
            }
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Thao Tác");
            model.addAttribute("message", e.getMessage());
            return "admin/confirm-result";
        }
    }

    @GetMapping("/reject-lock/{id}")
    public Object xuLyTuChoiKhoa(
            @PathVariable("id") Integer id,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "ajax", required = false) Boolean ajax,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminNhanVienService.rejectLock(id, token, actingTaiKhoanId, ipAddress);
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã từ chối khóa tài khoản nhân viên."));
            }
            model.addAttribute("success", true);
            model.addAttribute("title", "Từ Chối Khóa Thành Công");
            model.addAttribute("message", "Đã từ chối khóa tài khoản nhân viên. Trạng thái hoạt động của nhân viên được giữ nguyên.");
            return "admin/confirm-result";
        } catch (Exception e) {
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
            }
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Thao Tác");
            model.addAttribute("message", e.getMessage());
            return "admin/confirm-result";
        }
    }
}
