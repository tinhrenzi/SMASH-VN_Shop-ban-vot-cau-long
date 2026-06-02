package com.smashvn.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tạm tắt CSRF để dễ Test POST requests
            .authorizeHttpRequests(auth -> auth
                // Phân quyền cho POS, Đơn hàng, Khách hàng cho QL và NV
                .requestMatchers("/admin/pos/**", "/admin/don-hang/**", "/admin/khach-hang/**").hasAnyRole("QL", "NV")
                // Chỉ QL được phép truy cập tất cả các trang /admin còn lại
                .requestMatchers("/admin/**").hasRole("QL")
                // Tất cả các request khác đều được permit
                .anyRequest().permitAll() 
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect(request.getContextPath() + "/user/dang-nhap");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String role = (String) request.getSession().getAttribute("vaiTro");
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
}