package com.smashvn.shop.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ConstraintViolation;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        // Rethrow để Spring Security tự xử lý qua AccessDeniedHandler đã cấu hình
        throw ex;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ModelAndView handleConstraintViolation(HttpServletRequest request, ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Validation failed on request: {} - Message: {}", request.getRequestURI(), message);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("loi", message);
        
        // Xác định trang quay lại dựa vào URI yêu cầu
        String uri = request.getRequestURI();
        if (uri.contains("/user/dang-ky")) {
            mav.setViewName("signup");
        } else if (uri.contains("/user/dang-nhap")) {
            mav.setViewName("signin");
        } else if (uri.contains("/admin/san-pham/them")) {
            mav.setViewName("admin/sanpham-add");
        } else {
            mav.setViewName("error/generic");
            mav.addObject("loi", "Dữ liệu đầu vào không hợp lệ: " + message);
        }
        return mav;
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ModelAndView handleResponseStatus(HttpServletRequest request, org.springframework.web.server.ResponseStatusException ex) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("loi", ex.getReason());
        mav.setViewName("error/generic");
        return mav;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ModelAndView handleNoResourceFound(HttpServletRequest request, org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Không tìm thấy tài nguyên: {}", request.getRequestURI());
        ModelAndView mav = new ModelAndView();
        mav.setViewName("404");
        return mav;
    }

    @ExceptionHandler({org.apache.catalina.connector.ClientAbortException.class, java.io.IOException.class})
    public void handleClientAbort(HttpServletRequest request, Exception ex) {
        // Khách hàng ngắt kết nối thủ công (tắt tab, F5, reload trang).
        // Ghi log nhẹ và không render lại view để tránh lỗi IllegalStateException (getOutputStream already called).
        log.debug("Khách hàng ngắt kết nối khi đang tải trang tại URL: {}", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Exception ex) {
        // Kiểm tra nếu kết nối đã bị đóng hoặc response đã gửi xong dữ liệu
        if (response.isCommitted() || isClientAbort(ex)) {
            log.debug("Response đã được gửi hoặc khách ngắt kết nối tại URL: {}", request.getRequestURI());
            return null;
        }

        // Log stack trace nội bộ cho các lỗi thực sự
        log.error("Lỗi hệ thống chưa được bắt giữ tại URL: " + request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("loi", "Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên!");
        mav.setViewName("error/generic");
        return mav;
    }

    private boolean isClientAbort(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            String name = cause.getClass().getName();
            if (name.contains("ClientAbortException") || cause instanceof java.io.IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
