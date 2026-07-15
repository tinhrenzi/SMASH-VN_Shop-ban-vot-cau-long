package com.smashvn.shop.service.user;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.exception.AccountNotFoundException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDangNhapService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;

    @org.springframework.transaction.annotation.Transactional
    public TaiKhoan kiemTraDangNhap(String email, String matKhau) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email);

        // ── Timing-safe password check (chống Timing Attack) ──────────────────
        // Dùng hash giả lập nếu tài khoản không tồn tại để BCrypt.checkpw luôn chạy
        String dbPass = (taiKhoan != null)
                ? taiKhoan.getMatKhau()
                : "$2a$10$NXyH1kUoY7G7ZlE8w8rL1eA5gR4wD2O4hIeJ1F7H6v8tM9dY0mK1e";

        boolean matches = false;
        try {
            matches = BCrypt.checkpw(matKhau, dbPass);
        } catch (IllegalArgumentException e) {
            matches = false;
        }

        // ── Phân biệt "email chưa đăng ký" vs "sai mật khẩu" ────────────────
        // Lưu ý: chỉ ném AccountNotFoundException SAU khi timing-safe check đã hoàn tất
        // để tránh tiết lộ sự tồn tại của email qua thời gian phản hồi.
        if (taiKhoan == null) {
            throw new AccountNotFoundException(email);
        }

        if (!matches) {
            throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
        }

        // ── Kiểm tra trạng thái tài khoản ────────────────────────────────────
        if (!"hoat_dong".equals(taiKhoan.getTrangThai()) && !"cho_khoa".equals(taiKhoan.getTrangThai())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }
        return taiKhoan;
    }


    @org.springframework.cache.annotation.Cacheable(value = "taiKhoanStatus", key = "#idNguoiDung")
    public String layTrangThaiTaiKhoan(Integer idNguoiDung) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        return (taiKhoan != null) ? taiKhoan.getTrangThai() : "bi_khoa";
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
