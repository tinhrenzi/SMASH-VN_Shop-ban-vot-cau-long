package com.smashvn.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDangKyService {
    
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository; // Khai báo thêm Repository này

    @Transactional // Rất quan trọng: Đảm bảo lưu cả 2 bảng cùng lúc, lỗi 1 cái là hoàn tác cả 2
    public TaiKhoan dangKy(String email, String matKhau) {
        // Trim email
        email = (email != null) ? email.trim() : null;

        // Validation email format and length
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email không được để trống!");
        }
        if (email.length() > 100) {
            throw new RuntimeException("Email không được vượt quá 100 ký tự!");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new RuntimeException("Định dạng email không hợp lệ!");
        }

        // Validation password strength: 8-30 chars, containing both letters and numbers, no whitespace
        if (matKhau == null || matKhau.isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }
        if (matKhau.length() < 8 || matKhau.length() > 30) {
            throw new RuntimeException("Mật khẩu phải dài từ 8 đến 30 ký tự!");
        }
        if (matKhau.contains(" ") || matKhau.contains("\t") || matKhau.contains("\n") || matKhau.contains("\r")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }
        if (!matKhau.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            throw new RuntimeException("Mật khẩu phải chứa cả chữ và số!");
        }

        // 1. Kiểm tra trùng lặp
        if (taiKhoanRepository.existsByEmail(email)) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }
        
        // 2. Tạo Tài khoản
        TaiKhoan taiKhoanMoi = new TaiKhoan();
        taiKhoanMoi.setEmail(email);
        
        // Mã hóa mật khẩu
        String hashed = BCrypt.hashpw(matKhau, BCrypt.gensalt());
        taiKhoanMoi.setMatKhau(hashed); 
        
        taiKhoanMoi.setVaiTro("KH");
        taiKhoanMoi.setTrangThai("hoat_dong");
        taiKhoanMoi.setLaKhachHang(true);
        taiKhoanMoi.setLaNhanVien(false);
        taiKhoanMoi.setLaQuanLy(false);
        
        // Lưu Tài khoản để lấy ID
        TaiKhoan tkDaLuu = taiKhoanRepository.save(taiKhoanMoi);

        // 3. TỰ ĐỘNG TẠO HỒ SƠ KHÁCH HÀNG (Sửa lỗi vòng lặp)
        KhachHang khMoi = new KhachHang();
        khMoi.setTaiKhoan(tkDaLuu); // Móc nối với tài khoản vừa tạo
        khMoi.setHoKh(""); 
        khMoi.setTenKh("Người dùng mới"); // Đặt tên mặc định
        khMoi.setSoDienThoaiKh(""); // Để trống cho khách tự cập nhật sau
        khMoi.setNhanBanTin(false);
        
        // Lưu Khách hàng
        khachHangRepository.save(khMoi);

        return tkDaLuu;
    }
}