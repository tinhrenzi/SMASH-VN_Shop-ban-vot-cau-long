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
                // Cho phép truy cập tất cả mọi nơi không cần đăng nhập
                .anyRequest().permitAll() 
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/user/dang-nhap") // Trỏ về trang đăng nhập của bạn
                .defaultSuccessUrl("/user/google-success", true) // Thành công thì gọi về API này
            );
        
        return http.build();
    }
}