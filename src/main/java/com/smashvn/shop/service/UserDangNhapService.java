package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;


@Service
@RequiredArgsConstructor
public class UserDangNhapService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    
    @org.springframework.transaction.annotation.Transactional
    public TaiKhoan kiemTraDangNhap(String email, String matKhau) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email);
        
        if (taiKhoan != null) {
            String dbPass = taiKhoan.getMatKhau();
            boolean matches = false;
            boolean isPlaintext = dbPass == null || (!dbPass.startsWith("$2a$") && !dbPass.startsWith("$2b$") && !dbPass.startsWith("$2y$"));

            if (isPlaintext) {
                matches = matKhau.equals(dbPass);
                if (matches) {
                    // Tự động mã hóa lại mật khẩu bằng BCrypt và lưu lại vào DB
                    taiKhoan.setMatKhau(BCrypt.hashpw(matKhau, BCrypt.gensalt()));
                    taiKhoanRepository.save(taiKhoan);
                }
            } else {
                try {
                    matches = BCrypt.checkpw(matKhau, dbPass);
                } catch (IllegalArgumentException e) {
                    matches = false;
                }
            }

            if (matches) {
                if (!"hoat_dong".equals(taiKhoan.getTrangThai()) && !"cho_khoa".equals(taiKhoan.getTrangThai())) {
                    throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
                }
                return taiKhoan;
            }
        }
        throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
    }
    
    @org.springframework.transaction.annotation.Transactional
    public TaiKhoan xuLyDangNhapGoogle(String email, String name) {
        TaiKhoan tk = taiKhoanRepository.findByEmail(email);
        
        if (tk == null) {
            // 1. CHƯA TỒN TẠI: Tự động đăng ký mới
            tk = new TaiKhoan();
            tk.setEmail(email);
            // Tạo mật khẩu ảo ngẫu nhiên cực khó để không ai dùng email này đăng nhập tay được
            String randomPass = java.util.UUID.randomUUID().toString();
            tk.setMatKhau(BCrypt.hashpw(randomPass, BCrypt.gensalt()));
            tk.setVaiTro("KH");
            tk.setTrangThai("hoat_dong");
            tk = taiKhoanRepository.save(tk);

            // 2. Tạo ngay hồ sơ Khách Hàng đi kèm
            com.smashvn.shop.entity.KhachHang kh = new com.smashvn.shop.entity.KhachHang();
            kh.setTaiKhoan(tk);
            kh.setHoKh("");
            kh.setTenKh(name != null ? name : "Người dùng Google");
            kh.setSoDienThoaiKh("");
            kh.setNhanBanTin(false);
            khachHangRepository.save(kh);
        } else {
            // ĐÃ TỒN TẠI: Kiểm tra xem tài khoản có bị khóa không
            if (!"hoat_dong".equals(tk.getTrangThai())) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
            }
        }
        return tk;
    }
}