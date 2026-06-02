package com.smashvn.shop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;

import org.springframework.web.servlet.ModelAndView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        // 1. Kiểm tra đăng nhập
        if (session == null || session.getAttribute("nguoiDungDangNhap") == null || session.getAttribute("vaiTro") == null) {
            response.sendRedirect(request.getContextPath() + "/user/dang-nhap");
            return false;
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        String vaiTro = (String) session.getAttribute("vaiTro");

        // 2. Kiểm tra tài khoản trong DB để xem trạng thái có bị khóa không
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        if (taiKhoan == null || (!"hoat_dong".equals(taiKhoan.getTrangThai()) && !"cho_khoa".equals(taiKhoan.getTrangThai()))) {
            // Hủy phiên đăng nhập
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/user/dang-nhap?loi=" + URLEncoder.encode("Tài khoản của bạn đã bị khóa.", StandardCharsets.UTF_8.toString()));
            return false;
        }

        // 3. Phân quyền
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());

        // Nếu là khách hàng (KH), cấm truy cập bất kỳ trang /admin nào, đẩy về trang chủ
        if ("KH".equals(vaiTro)) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }

        // Nếu là nhân viên (NV)
        if ("NV".equals(vaiTro)) {
            // Chỉ cho phép truy cập /admin/don-hang/** và /admin/khach-hang/**
            boolean isAllowed = path.startsWith("/admin/don-hang") || path.startsWith("/admin/khach-hang");
            if (!isAllowed) {
                // Đẩy về trang /admin/don-hang kèm theo thông điệp cảnh báo trong session
                HttpSession activeSession = request.getSession(true);
                activeSession.setAttribute("warningMsg", "Bạn không có quyền thực hiện chức năng này!");
                response.sendRedirect(request.getContextPath() + "/admin/don-hang");
                return false;
            }
        }

        // Nếu là quản lý (QL), cho phép tất cả các tài nguyên /admin
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && modelAndView != null) {
            String warningMsg = (String) session.getAttribute("warningMsg");
            if (warningMsg != null) {
                modelAndView.addObject("warningMsg", warningMsg);
                session.removeAttribute("warningMsg");
            }
        }
    }
}
