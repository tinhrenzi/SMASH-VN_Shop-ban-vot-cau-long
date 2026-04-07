package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;

@Service
@RequiredArgsConstructor
public class UserDangNhapService {

    private final TaiKhoanRepository taiKhoanRepository;

    public TaiKhoan kiemTraDangNhap(String email, String matKhau) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email);
        
        // SO KHỚP MẬT KHẨU ĐÃ MÃ HÓA
        if (taiKhoan != null && BCrypt.checkpw(matKhau, taiKhoan.getMatKhau())) {
            if (!"hoat_dong".equals(taiKhoan.getTrangThai())) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
            }
            return taiKhoan;
        }
        throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
    }
}