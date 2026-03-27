package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;

@Service
@RequiredArgsConstructor
public class UserDangNhapService {

    private final TaiKhoanRepository taiKhoanRepository;

    public TaiKhoan kiemTraDangNhap(String email, String matKhau) {
        // Tìm tài khoản theo email
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email);
        
        // Kiểm tra xem tài khoản có tồn tại và mật khẩu có khớp không
        if (taiKhoan != null && taiKhoan.getMatKhau().equals(matKhau)) {
            // Kiểm tra thêm trạng thái phòng trường hợp tài khoản bị khóa
            if (!"hoat_dong".equals(taiKhoan.getTrangThai())) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
            }
            return taiKhoan; // Trả về thông tin tài khoản nếu đúng
        }
        
        throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
    }
}