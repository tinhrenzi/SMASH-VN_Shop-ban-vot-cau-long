package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCheckoutService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final JavaMailSender mailSender;

    public String checkEmailStatus(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "NEW";
        }
        
        TaiKhoan tk = taiKhoanRepository.findByEmail(email.trim());
        if (tk == null) {
            return "NEW";
        }

        if (tk.getTrangThaiTaiKhoan() == AccountStatus.ACTIVE) {
            return "ACTIVE";
        }

        // status is GUEST
        if (tk.getSoLanMuaThanhCong() < 3) {
            return "GUEST_VALID";
        } else {
            return "GUEST_EXPIRED";
        }
    }

    @Transactional
    public TaiKhoan autoRegisterGuest(String hoTen, String soDienThoai, String email) {
        String trimmedEmail = (email != null) ? email.trim() : "";
        if (trimmedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        TaiKhoan existingTk = taiKhoanRepository.findByEmail(trimmedEmail);
        if (existingTk != null) {
            log.info("[GUEST_CHECKOUT] Linked order with existing guest account: {}", trimmedEmail);
            return existingTk;
        }

        TaiKhoan tk = new TaiKhoan();
        tk.setEmail(trimmedEmail);
        tk.setMatKhau(null); // No password initially
        tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        tk.setSoLanMuaThanhCong(0); // Will be incremented when order is created
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk.setLaKhachHang(true);
        tk.setLaNhanVien(false);
        tk.setLaQuanLy(false);

        TaiKhoan savedTk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(savedTk);
        kh.setHoKh("");
        kh.setTenKh(hoTen != null ? hoTen.trim() : "Khách vãng lai");
        kh.setSoDienThoaiKh(soDienThoai != null ? soDienThoai.trim() : "");
        kh.setNhanBanTin(false);

        khachHangRepository.save(kh);
        log.info("[GUEST_CHECKOUT] Auto-registered GUEST account: {}", trimmedEmail);

        return savedTk;
    }

    @Transactional
    public void incrementPurchaseCount(Integer idTaiKhoan) {
        TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        tk.setSoLanMuaThanhCong(tk.getSoLanMuaThanhCong() + 1);
        taiKhoanRepository.save(tk);
        log.info("[GUEST_CHECKOUT] Incremented successful purchase count for TaiKhoan ID {} to {}.", idTaiKhoan, tk.getSoLanMuaThanhCong());
    }

    @Transactional
    public void sendOrderAndAccountNotification(TaiKhoan tk, String appUrl) {
        // Tạo token kích hoạt/thiết lập mật khẩu
        String token = UUID.randomUUID().toString();

        TokenKhoiPhuc tkp = new TokenKhoiPhuc();
        tkp.setTaiKhoan(tk);
        tkp.setMaXacNhan(token);
        tkp.setLoaiXacNhan("EMAIL");
        tkp.setThoiGianHetHan(LocalDateTime.now().plusDays(30)); // 30 days validation limit
        tkp.setDaSuDung(false);
        tokenRepository.save(tkp);

        String activationUrl = appUrl + "/user/thiet-lap-mat-khau?token=" + token;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tk.getEmail());
            message.setSubject("Xác nhận đặt hàng thành công và kích hoạt tài khoản - Smash VN");
            message.setText("Chào bạn,\n\n" +
                    "Cảm ơn bạn đã đặt hàng tại Smash VN. Đơn hàng của bạn đã được ghi nhận thành công trên hệ thống.\n\n" +
                    "Để hỗ trợ bạn theo dõi tiến độ đơn hàng và mua sắm dễ dàng hơn trong tương lai, hệ thống đã tự động tạo một tài khoản liên kết với địa chỉ email này.\n\n" +
                    "Vui lòng thiết lập mật khẩu cho tài khoản bằng cách truy cập vào đường dẫn dưới đây:\n" +
                    activationUrl + "\n\n" +
                    "Trân trọng,\nSmash VN Team");

            mailSender.send(message);
            log.info("[GUEST_CHECKOUT] Sent order confirmation & activation link email to {}", tk.getEmail());
        } catch (Exception e) {
            log.error("[GUEST_CHECKOUT] Failed to send email to {}: {}", tk.getEmail(), e.getMessage());
            // Do not fail order checkout if mail sending fails
        }
    }

    @Transactional
    public void setPasswordForGuest(Integer idTaiKhoan, String password) {
        TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống!");
        }
        if (password.length() < 8 || password.length() > 30) {
            throw new RuntimeException("Mật khẩu phải dài từ 8 đến 30 ký tự!");
        }
        if (password.contains(" ") || password.contains("\t") || password.contains("\n") || password.contains("\r")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            throw new RuntimeException("Mật khẩu phải chứa cả chữ và số!");
        }

        tk.setMatKhau(BCrypt.hashpw(password, BCrypt.gensalt()));
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        taiKhoanRepository.save(tk);

        log.info("[GUEST_CHECKOUT] Upgraded guest account ID {} to ACTIVE status and saved password.", idTaiKhoan);
    }

    @Transactional
    public void setPasswordByToken(String token, String password) {
        TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
        if (tkp == null) {
            throw new RuntimeException("Đường link thiết lập mật khẩu không hợp lệ!");
        }
        if (tkp.isDaSuDung()) {
            throw new RuntimeException("Đường link này đã được sử dụng!");
        }
        if (tkp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đường link này đã hết hạn!");
        }
        if (!"EMAIL".equals(tkp.getLoaiXacNhan())) {
            throw new RuntimeException("Loại xác nhận không hợp lệ!");
        }

        TaiKhoan tk = tkp.getTaiKhoan();
        setPasswordForGuest(tk.getId(), password);

        tkp.setDaSuDung(true);
        tokenRepository.save(tkp);
    }
}
