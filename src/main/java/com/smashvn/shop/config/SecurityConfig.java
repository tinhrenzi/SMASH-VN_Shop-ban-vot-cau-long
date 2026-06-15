package com.smashvn.shop.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Kích hoạt lại bảo mật CSRF
                .csrf(csrf -> csrf.ignoringRequestMatchers("/user/dang-xuat", "/admin/dang-xuat", "/api/payment/zalopay/callback", "/api/payment/sepay/ipn", "/api/ghn/webhook"))
                // Cấu hình các Header bảo mật nâng cao
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
                })
                .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập công khai trang đăng nhập/đăng xuất admin và tài nguyên tĩnh
                .requestMatchers("/admin/dang-nhap", "/admin/dang-xuat").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/webfonts/**").permitAll()
                .requestMatchers("/user/**").permitAll()
                // Phân quyền chi tiết cho Admin/Staff theo Backend Enforcement (cả endpoint gốc và sub-paths)
                .requestMatchers("/admin/nguoi-dung", "/admin/nguoi-dung/**").hasRole("QL")
                .requestMatchers("/admin/nhan-vien", "/admin/nhan-vien/**").hasRole("QL")
                .requestMatchers("/admin/thong-ke", "/admin/thong-ke/**").hasRole("QL")
                .requestMatchers("/admin/shipping-config", "/admin/shipping-config/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/don-hang", "/admin/don-hang/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/san-pham", "/admin/san-pham/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/danh-muc", "/admin/danh-muc/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/pos", "/admin/pos/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/khach-hang", "/admin/khach-hang/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/transactions", "/admin/transactions/**").hasAnyRole("QL", "NV")
                // Các trang quản trị còn lại chỉ dành cho QL
                .requestMatchers("/admin", "/admin/**").hasRole("QL")
                // Tất cả các request khác đều được permit
                .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect(request.getContextPath() + "/admin/dang-nhap");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String ip = request.getRemoteAddr();
                    String uri = request.getRequestURI();
                    Integer idNguoiDung = (Integer) request.getSession().getAttribute("idNguoiDung");
                    String role = (String) request.getSession().getAttribute("vaiTro");

                    // Phân biệt CSRF Deny vs Access Deny để ghi log an ninh
                    if (accessDeniedException instanceof org.springframework.security.web.csrf.CsrfException) {
                        log.warn("[SECURITY_EVENT] CSRF_DENY: IP: {}, URL: {}, UserID: {}", ip, uri, idNguoiDung);
                    } else {
                        log.warn("[SECURITY_EVENT] ACCESS_DENY: IP: {}, UserID: {}, URL: {}, Lỗi: {}",
                                ip, idNguoiDung, uri, accessDeniedException != null ? accessDeniedException.getMessage() : "Unknown error");
                    }

                    if ("NV".equals(role)) {
                        request.getSession().setAttribute("warningMsg", "Bạn không có quyền thực hiện chức năng này!");
                        response.sendRedirect(request.getContextPath() + "/admin/don-hang");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/");
                    }
                })
                )
                .oauth2Login(oauth2 -> oauth2
                .loginPage("/user/dang-nhap") // Trỏ về trang đăng nhập của bạn
                .defaultSuccessUrl("/user/google-success", true) // Thành công thì gọi về API này
                );

        return http.build();
    }

    // Đăng ký UploadSecurityFilter riêng biệt cho đường dẫn uploads
    @Bean
    public FilterRegistrationBean<UploadSecurityFilter> uploadSecurityFilterRegistration() {
        FilterRegistrationBean<UploadSecurityFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new UploadSecurityFilter());
        registrationBean.addUrlPatterns("/uploads/*");
        return registrationBean;
    }
}
