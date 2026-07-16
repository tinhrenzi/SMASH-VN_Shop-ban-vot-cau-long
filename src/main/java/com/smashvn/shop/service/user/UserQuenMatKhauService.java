package com.smashvn.shop.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.util.PhoneUtils;

@Service
@RequiredArgsConstructor
public class UserQuenMatKhauService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // 1. Tạo và gửi Link khôi phục
    @Transactional
    public void guiYeuCauKhoiPhuc(String inputIdentifier, String appUrl) {
        if (inputIdentifier == null || inputIdentifier.trim().isEmpty()) {
            throw new RuntimeException("Tài khoản không được để trống!");
        }
        String trimmedIdentifier = inputIdentifier.trim();

        // Find account directly by username
        TaiKhoan tk = taiKhoanRepository.findByUsername(trimmedIdentifier);
        
        // Fallback to check KhachHang.soDienThoaiKh to support legacy data
        if (tk == null) {
            String normalizedPhone = PhoneUtils.normalize(trimmedIdentifier);
            if (PhoneUtils.isValid(normalizedPhone)) {
                KhachHang kh = khachHangRepository.findBySoDienThoaiKh(normalizedPhone);
                if (kh != null) {
                    tk = kh.getTaiKhoan();
                }
            }
        }

        if (tk == null) {
            throw new RuntimeException("Tài khoản không tồn tại trên hệ thống!");
        }

        // Check if the username is a valid email
        String destinationEmail = tk.getUsername();
        if (!destinationEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new RuntimeException("Tài khoản của bạn đăng ký bằng số điện thoại và không có email khôi phục. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        }

        // Tạo chuỗi Token ngẫu nhiên không trùng lặp
        String token = UUID.randomUUID().toString();

        // Lưu vào Database
        TokenKhoiPhuc tkp = new TokenKhoiPhuc();
        tkp.setTaiKhoan(tk);
        tkp.setMaXacNhan(token);
        tkp.setLoaiXacNhan("EMAIL");
        tkp.setThoiGianHetHan(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15 phút
        tkp.setDaSuDung(false);
        tokenRepository.save(tkp);

        // Tạo Link trỏ về trang đổi mật khẩu
        String resetUrl = appUrl + "/user/dat-lai-mat-khau?token=" + token;

        // Gửi Mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinationEmail);
        message.setSubject("Yêu cầu khôi phục mật khẩu - Smash VN");
        message.setText("Chào bạn,\n\n" +
                "Bạn vừa yêu cầu khôi phục mật khẩu. Vui lòng click vào đường link bên dưới để đặt lại mật khẩu mới:\n" +
                resetUrl + "\n\n" +
                "Đường link này sẽ hết hạn sau 15 phút.\n" +
                "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.");
        
        mailSender.send(message);
    }

    // 2. Xác thực Token có hợp lệ không (Chưa hết hạn, chưa dùng)
    public TokenKhoiPhuc kiemTraToken(String token) {
        TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
        if (tkp == null) {
            throw new RuntimeException("Đường link khôi phục không hợp lệ!");
        }
        if (tkp.isDaSuDung()) {
            throw new RuntimeException("Đường link này đã được sử dụng!");
        }
        if (tkp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đường link này đã hết hạn!");
        }
        return tkp;
    }

    // 3. Tiến hành đổi mật khẩu
    @Transactional
    public void datLaiMatKhau(String token, String matKhauMoi) {
        TokenKhoiPhuc tkp = kiemTraToken(token); // Kiểm tra lại lần cuối cho chắc

        if (matKhauMoi == null || matKhauMoi.isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống!");
        }
        if (matKhauMoi.length() < 8 || matKhauMoi.length() > 30) {
            throw new RuntimeException("Mật khẩu phải dài từ 8 đến 30 ký tự!");
        }
        if (matKhauMoi.contains(" ") || matKhauMoi.contains("\t") || matKhauMoi.contains("\n") || matKhauMoi.contains("\r")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }
        if (!matKhauMoi.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            throw new RuntimeException("Mật khẩu phải chứa cả chữ và số!");
        }

        TaiKhoan tk = tkp.getTaiKhoan();
        tk.setMatKhau(passwordEncoder.encode(matKhauMoi)); // Mã hóa Pass mới
        taiKhoanRepository.save(tk);

        tkp.setDaSuDung(true);
        tokenRepository.save(tkp);
    }
}