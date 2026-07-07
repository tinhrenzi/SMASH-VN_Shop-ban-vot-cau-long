package com.smashvn.shop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối của thư mục "uploads" nằm ngang hàng với source code
        Path uploadDir = Paths.get("uploads").toAbsolutePath();
        String uploadLocations = uploadDir.toUri().toString();

        // Map đường dẫn URL bắt đầu bằng /uploads/ tới thư mục vật lý
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocations.endsWith("/") ? uploadLocations : uploadLocations + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/dang-nhap", "/admin/dang-xuat", "/css/**", "/js/**", "/images/**", "/uploads/**", "/webfonts/**");
    }
}