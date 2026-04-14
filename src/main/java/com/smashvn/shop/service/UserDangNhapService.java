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