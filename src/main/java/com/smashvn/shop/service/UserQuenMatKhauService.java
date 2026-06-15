package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;
import java.util.UUID;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;

@Service
@RequiredArgsConstructor
public class UserQuenMatKhauService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final JavaMailSender mailSender;

    // 1. Tạo và gửi Link khôi phục
    @Transactional
    public void guiYeuCauKhoiPhuc(String email, String appUrl) {
        TaiKhoan tk = taiKhoanRepository.findByEmail(email);
        if (tk == null) {
            throw new RuntimeException("Email này không tồn tại trên hệ thống!");
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
        message.setTo(email);
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
        
        TaiKhoan tk = tkp.getTaiKhoan();
        tk.setMatKhau(BCrypt.hashpw(matKhauMoi, BCrypt.gensalt())); // Mã hóa Pass mới
        taiKhoanRepository.save(tk);

        tkp.setDaSuDung(true); // Khóa Token lại
        tokenRepository.save(tkp);
    }
}