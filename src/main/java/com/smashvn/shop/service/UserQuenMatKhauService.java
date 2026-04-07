package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt; // Dùng mã hóa mật khẩu

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;

@Service
@RequiredArgsConstructor
public class UserQuenMatKhauService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final JavaMailSender mailSender; // Đối tượng dùng để gửi mail

    @Transactional
    public void guiLaiMatKhau(String email) {
        // 1. Kiểm tra email có tồn tại không
        TaiKhoan tk = taiKhoanRepository.findByEmail(email);
        if (tk == null) {
            throw new RuntimeException("Email này không tồn tại trên hệ thống!");
        }

        // 2. Tạo mật khẩu mới ngẫu nhiên (6 số)
        String passMoi = String.valueOf((int) (Math.random() * 899999 + 100000));

        // 3. Cập nhật mật khẩu mới vào DB (Nhớ mã hóa)
        String passMaHoa = BCrypt.hashpw(passMoi, BCrypt.gensalt());
        tk.setMatKhau(passMaHoa);
        taiKhoanRepository.save(tk);

        // 4. Gửi Mail cho khách
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Khôi phục mật khẩu - Smash VN");
        message.setText("Chào bạn,\n\nBạn vừa yêu cầu khôi phục mật khẩu tại Smash VN.\n" +
                "Mật khẩu mới của bạn là: " + passMoi + "\n\n" +
                "Vui lòng đăng nhập và đổi lại mật khẩu ngay để bảo mật tài khoản.\n" +
                "Trân trọng!");
        
        mailSender.send(message);
    }
}