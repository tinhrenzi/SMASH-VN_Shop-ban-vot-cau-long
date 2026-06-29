package com.smashvn.shop.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.KhachHangRepository;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoleSwitchController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    @GetMapping("/switch-role")
    public String switchRole(@RequestParam("role") String newRole,
                             HttpServletRequest request,
                             HttpSession session) {
        // 1. Validate session user
        Integer userId = (Integer) session.getAttribute("idNguoiDung");
        if (userId == null) {
            log.warn("[SECURITY_EVENT] UNAUTHORIZED_ROLE_SWITCH_ATTEMPT: Anonymous session tried to access /switch-role");
            return "redirect:/user/dang-nhap";
        }

        // 2. Fetch and validate account ownership
        TaiKhoan tk = taiKhoanRepository.findById(userId).orElse(null);
        if (tk == null || (!"hoat_dong".equals(tk.getTrangThai()) && !"cho_khoa".equals(tk.getTrangThai())) || tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.ACTIVE) {
            log.warn("[SECURITY_EVENT] FAILED_ROLE_SWITCH: Account ID {} not found, inactive, or not fully registered", userId);
            session.invalidate();
            SecurityContextHolder.clearContext();
            return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode("Tài khoản không hợp lệ, không tồn tại hoặc đã bị khóa!", java.nio.charset.StandardCharsets.UTF_8);
        }

        // 3. Validate role ownership
        boolean ownsRole = false;
        if ("KH".equals(newRole) && Boolean.TRUE.equals(tk.getLaKhachHang())) ownsRole = true;
        if ("NV".equals(newRole) && Boolean.TRUE.equals(tk.getLaNhanVien())) ownsRole = true;
        if ("QL".equals(newRole) && Boolean.TRUE.equals(tk.getLaQuanLy())) ownsRole = true;

        if (!ownsRole) {
            log.warn("[SECURITY_EVENT] ILLEGAL_ROLE_SWITCH_ATTEMPT: Email: {} tried switching to unowned role: {}", tk.getEmail(), newRole);
            return "redirect:/";
        }

        String currentRole = (String) session.getAttribute("activeRole");
        if (currentRole == null) {
            currentRole = tk.getVaiTro(); // Fallback to legacy field
        }

        // Check if this switch is a privilege elevation or reduction
        // Elevation: KH -> NV, KH -> QL, NV -> QL
        // Reduction / Same level: QL -> NV, QL -> KH, NV -> KH, or NV -> NV (same), QL -> QL (same)
        boolean isElevation = false;
        if ("KH".equals(currentRole) && ("NV".equals(newRole) || "QL".equals(newRole))) {
            isElevation = true;
        } else if ("NV".equals(currentRole) && "QL".equals(newRole)) {
            isElevation = true;
        }

        if (isElevation) {
            log.info("[SECURITY_EVENT] PRIVILEGE_ELEVATION_ATTEMPT: Email: {} from {} to {}. Redirecting to re-authenticate.", tk.getEmail(), currentRole, newRole);
            // Require re-authentication -> redirect to admin login
            return "redirect:/admin/dang-nhap";
        }

        // Privilege reduction or same-level switch -> allowed directly
        // Invalidate current session and create a new fresh session for the new context
        String email = tk.getEmail();
        
        session.invalidate();
        SecurityContextHolder.clearContext(); // Clean security context first
        
        HttpSession newSession = request.getSession(true);

        // Reinitialize session attributes
        newSession.setAttribute("nguoiDungDangNhap", email);
        newSession.setAttribute("idNguoiDung", userId);
        newSession.setAttribute("vaiTro", newRole);
        newSession.setAttribute("activeRole", newRole);
        newSession.setAttribute("laKhachHang", Boolean.TRUE.equals(tk.getLaKhachHang()));
        newSession.setAttribute("laNhanVien", Boolean.TRUE.equals(tk.getLaNhanVien()));
        newSession.setAttribute("laQuanLy", Boolean.TRUE.equals(tk.getLaQuanLy()));

        // Establish appropriate SecurityContext and Display Name
        if ("NV".equals(newRole) || "QL".equals(newRole)) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(userId);
            if (nv != null) {
                newSession.setAttribute("tenHienThi", nv.getHoTenNv());
            } else {
                newSession.setAttribute("tenHienThi", "QL".equals(newRole) ? "Quản lý hệ thống" : "Nhân viên hệ thống");
            }

            // Grant Security Context
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + newRole));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContext sc = SecurityContextHolder.getContext();
            sc.setAuthentication(auth);
            newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
            
            log.info("[SECURITY_EVENT] ROLE_SWITCH_SUCCESS: Email: {} switched from {} to {}", email, currentRole, newRole);
            return "QL".equals(newRole) ? "redirect:/admin/all" : "redirect:/admin/don-hang";
        } else {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(userId);
            if (kh != null) {
                newSession.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
            } else {
                newSession.setAttribute("tenHienThi", "Khách hàng");
            }
            
            // Security Context is already cleared
            log.info("[SECURITY_EVENT] ROLE_SWITCH_SUCCESS: Email: {} switched from {} to {}", email, currentRole, newRole);
            return "redirect:/";
        }
    }
}
