package com.smashvn.shop.service;

import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDangKyService {
	// Spring sẽ tự động tiêm (inject) TaiKhoanRepository vào đây nhờ @RequiredArgsConstructor của Lombok
    private final TaiKhoanRepository taiKhoanRepository;

    public TaiKhoan dangKy(String email, String matKhau) {
        // 1. Kiểm tra email tồn tại
        if (taiKhoanRepository.existsByEmail(email)) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        // 2. Tạo đối tượng tài khoản mới
        TaiKhoan taiKhoanMoi = new TaiKhoan();
        taiKhoanMoi.setEmail(email);
        
        // Lưu ý: Tạm thời chúng ta lưu mật khẩu dạng chữ thuần (plain text) để dễ test.
        // Sau này khi làm dự án thực tế, bạn PHẢI dùng BCryptPasswordEncoder để mã hóa.
        taiKhoanMoi.setMatKhau(matKhau); 
        
        taiKhoanMoi.setVaiTro("KH"); // Mặc định là Khách hàng
        taiKhoanMoi.setTrangThai("hoat_dong");

        // 3. Lưu xuống database
        return taiKhoanRepository.save(taiKhoanMoi);
    }
}
