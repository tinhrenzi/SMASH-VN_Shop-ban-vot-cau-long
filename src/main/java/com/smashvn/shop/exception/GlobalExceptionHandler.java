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

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(HttpServletRequest request, Exception ex) {
        // Log stack trace nội bộ
        log.error("Lỗi hệ thống chưa được bắt giữ tại URL: " + request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("loi", "Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên!");
        mav.setViewName("error/generic");
        return mav;
    }
}
