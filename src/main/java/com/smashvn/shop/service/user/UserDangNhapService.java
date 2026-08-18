package com.smashvn.shop.service.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.exception.AccountNotFoundException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.util.LoginIdentifierClassifier;
import com.smashvn.shop.util.LoginIdentifierClassifier.NormalizedLoginIdentifier;
import com.smashvn.shop.util.LoginIdentifierClassifier.LoginIdentifierType;
import com.smashvn.shop.util.PhoneUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDangNhapService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.transaction.annotation.Transactional
    public TaiKhoan kiemTraDangNhap(String username, String matKhau) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Tài khoản không được để trống!");
        }

        // 1. Phân loại và chuẩn hóa định danh bằng component dùng chung
        NormalizedLoginIdentifier normalized = LoginIdentifierClassifier.classifyAndNormalize(username);
        String normalizedValue = normalized.value();

        // 2. Tìm trực tiếp TaiKhoan.username trước
        TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(normalizedValue);

        // 3. Nếu không tìm thấy và type = PHONE, fallback qua KhachHang.soDienThoaiKh
        if (taiKhoan == null && normalized.type() == LoginIdentifierType.PHONE) {
            KhachHang kh = khachHangRepository.findBySoDienThoaiKh(normalizedValue);
            if (kh != null) {
                taiKhoan = kh.getTaiKhoan();
                if (taiKhoan == null) {
                    throw new RuntimeException("Tài khoản liên kết không tồn tại!");
                }
            }
        }

        // 4. Timing-safe password check
        String dbPass = (taiKhoan != null)
                ? taiKhoan.getMatKhau()
                : "$2a$10$NXyH1kUoY7G7ZlE8w8rL1eA5gR4wD2O4hIeJ1F7H6v8tM9dY0mK1e";

        boolean matches = false;
        try {
            matches = passwordEncoder.matches(matKhau, dbPass);
        } catch (IllegalArgumentException e) {
            matches = false;
        }

        // 5. Ném exception nếu không tìm thấy hoặc sai mật khẩu
        if (taiKhoan == null) {
            throw new AccountNotFoundException(normalizedValue);
        }

        if (!matches) {
            throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
        }

        // 6. Kiểm tra trạng thái tài khoản
        if (taiKhoan.getTrangThaiTaiKhoan() == AccountStatus.LOCKED || "bi_khoa".equalsIgnoreCase(taiKhoan.getTrangThai())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }
        if (taiKhoan.getTrangThaiTaiKhoan() == AccountStatus.GUEST && (taiKhoan.getMatKhau() == null || taiKhoan.getMatKhau().trim().isEmpty())) {
            throw new RuntimeException("Tài khoản vãng lai chưa được kích hoạt mật khẩu. Vui lòng đăng nhập bằng Google hoặc kích hoạt qua email!");
        }
        if (taiKhoan.getTrangThaiTaiKhoan() == AccountStatus.GUEST || "khach_vang_lai".equalsIgnoreCase(taiKhoan.getTrangThai())) {
            taiKhoan.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
            taiKhoan = taiKhoanRepository.saveAndFlush(taiKhoan);
            log.info("[LOGIN] Upgraded guest account {} to ACTIVE on password login.", taiKhoan.getUsername());
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
        if (email == null) {
            throw new RuntimeException("Không tìm thấy email từ tài khoản Google!");
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        TaiKhoan tk = taiKhoanRepository.findByUsername(normalizedEmail);

        if (tk == null) {
            // 1. CHƯA TỒN TẠI: Tự động đăng ký mới
            tk = new TaiKhoan();
            tk.setUsername(normalizedEmail);
            // Tạo mật khẩu ảo ngẫu nhiên cực khó
            String randomPass = java.util.UUID.randomUUID().toString();
            tk.setMatKhau(passwordEncoder.encode(randomPass));
            tk.setVaiTro("KH");
            tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
            tk = taiKhoanRepository.save(tk);

            // 2. Tạo ngay hồ sơ Khách Hàng đi kèm
            KhachHang kh = new KhachHang();
            kh.setTaiKhoan(tk);
            kh.setHoKh("");
            kh.setTenKh(name != null ? name : "Người dùng Google");
            kh.setSoDienThoaiKh(null); // Explicitly NULL for email registration
            kh.setNhanBanTin(false);
            khachHangRepository.save(kh);
        } else {
            // ĐÃ TỒN TẠI: Kiểm tra xem tài khoản có bị khóa không
            if (tk.getTrangThaiTaiKhoan() == AccountStatus.LOCKED || "bi_khoa".equalsIgnoreCase(tk.getTrangThai())) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
            }
            // Nếu là tài khoản GUEST (khách vãng lai mua hàng trước đó), tự động nâng cấp thành ACTIVE khi đăng nhập Google thành công
            if (tk.getTrangThaiTaiKhoan() == AccountStatus.GUEST || "khach_vang_lai".equalsIgnoreCase(tk.getTrangThai())) {
                tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
                if (tk.getMatKhau() == null || tk.getMatKhau().trim().isEmpty()) {
                    String randomPass = java.util.UUID.randomUUID().toString();
                    tk.setMatKhau(passwordEncoder.encode(randomPass));
                }
                tk = taiKhoanRepository.save(tk);
                log.info("[GOOGLE_LOGIN] Upgraded guest account {} to ACTIVE status on Google OAuth login.", normalizedEmail);
            }
        }
        return tk;
    }
}
