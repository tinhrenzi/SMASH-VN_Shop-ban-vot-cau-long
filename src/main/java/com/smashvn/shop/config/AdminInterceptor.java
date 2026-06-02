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

        // 3. Phân quyền đã được bàn giao cho Spring Security. 
        // Interceptor này chỉ kiểm tra trạng thái hoạt động của tài khoản ở trên.
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
