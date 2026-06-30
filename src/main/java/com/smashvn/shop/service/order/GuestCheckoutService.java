package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.util.PhoneUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
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

    public static class GuestRegisterResult {
        private final TaiKhoan taiKhoan;
        private final String token;

        public GuestRegisterResult(TaiKhoan taiKhoan, String token) {
            this.taiKhoan = taiKhoan;
            this.token = token;
        }

        public TaiKhoan getTaiKhoan() {
            return taiKhoan;
        }

        public String getToken() {
            return token;
        }
    }

    @Transactional
    public GuestRegisterResult autoRegisterGuest(String hoTen, String soDienThoai, String email) {
        String trimmedEmail = (email != null) ? email.trim() : "";
        if (trimmedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        // Normalize phone number
        String normalizedPhone = PhoneUtils.normalize(soDienThoai);
        if (!normalizedPhone.isEmpty()) {
            if (!PhoneUtils.isValid(normalizedPhone)) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
            }
        }

        TaiKhoan existingTk = taiKhoanRepository.findByEmail(trimmedEmail);
        if (existingTk != null) {
            log.info("[GUEST_CHECKOUT] Linked order with existing guest account: {}", trimmedEmail);
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(existingTk.getId());
            if (kh != null) {
                if (!normalizedPhone.isEmpty()) {
                    KhachHang otherKh = khachHangRepository.findBySoDienThoaiKh(normalizedPhone);
                    if (otherKh != null && !otherKh.getId().equals(kh.getId())) {
                        throw new IllegalArgumentException("Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
                    }
                    kh.setSoDienThoaiKh(normalizedPhone);
                }
                
                // Update name if provided
                if (hoTen != null && !hoTen.trim().isEmpty()) {
                    String name = hoTen.trim();
                    String ho = "Khách";
                    String ten = "Vãng Lai";
                    int lastSpace = name.lastIndexOf(' ');
                    if (lastSpace >= 0) {
                        ho = name.substring(0, lastSpace).trim();
                        ten = name.substring(lastSpace + 1).trim();
                    } else {
                        ho = "";
                        ten = name;
                    }
                    kh.setHoKh(ho);
                    kh.setTenKh(ten);
                }
                khachHangRepository.save(kh);
            }
            return new GuestRegisterResult(existingTk, null);
        }

        // Check duplicate phone conflict for new registration against all customers
        if (!normalizedPhone.isEmpty()) {
            KhachHang otherKh = khachHangRepository.findBySoDienThoaiKh(normalizedPhone);
            if (otherKh != null) {
                throw new IllegalArgumentException("Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
            }
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

        String ho = "Khách";
        String ten = "Vãng Lai";
        if (hoTen != null && !hoTen.trim().isEmpty()) {
            String name = hoTen.trim();
            int lastSpace = name.lastIndexOf(' ');
            if (lastSpace >= 0) {
                ho = name.substring(0, lastSpace).trim();
                ten = name.substring(lastSpace + 1).trim();
            } else {
                ho = "";
                ten = name;
            }
        }

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(savedTk);
        kh.setHoKh(ho);
        kh.setTenKh(ten);
        kh.setSoDienThoaiKh(normalizedPhone);
        kh.setNhanBanTin(false);

        khachHangRepository.save(kh);
        
        // Generate and save token synchronously in the same transaction
        String token = UUID.randomUUID().toString();
        TokenKhoiPhuc tkp = new TokenKhoiPhuc();
        tkp.setTaiKhoan(savedTk);
        tkp.setMaXacNhan(token);
        tkp.setLoaiXacNhan("EMAIL");
        tkp.setThoiGianHetHan(LocalDateTime.now().plusDays(30)); // 30 days validation limit
        tkp.setDaSuDung(false);
        tokenRepository.save(tkp);

        log.info("[GUEST_CHECKOUT] Auto-registered GUEST account & generated token: {}", trimmedEmail);

        return new GuestRegisterResult(savedTk, token);
    }

    @Transactional
    public void incrementPurchaseCount(Integer idTaiKhoan) {
        TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        tk.setSoLanMuaThanhCong(tk.getSoLanMuaThanhCong() + 1);
        taiKhoanRepository.save(tk);
        log.info("[GUEST_CHECKOUT] Incremented successful purchase count for TaiKhoan ID {} to {}.", idTaiKhoan, tk.getSoLanMuaThanhCong());
    }

    @org.springframework.scheduling.annotation.Async
    public void sendOrderAndAccountNotification(String recipientEmail, String token, String appUrl) {
        long startEmailThread = System.currentTimeMillis();
        log.info("[EmailService] Starting asynchronous email sending process.");
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.warn("[EmailService] Recipient email is null or empty, skipping email notification.");
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            log.warn("[EmailService] Activation token is null or empty, skipping email notification.");
            return;
        }

        String activationUrl = appUrl + "/user/thiet-lap-mat-khau?token=" + token;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("Xác nhận đặt hàng thành công và kích hoạt tài khoản - Smash VN");
            message.setText("Chào bạn,\n\n" +
                    "Cảm ơn bạn đã đặt hàng tại Smash VN. Đơn hàng của bạn đã được ghi nhận thành công trên hệ thống.\n\n" +
                    "Để hỗ trợ bạn theo dõi tiến độ đơn hàng và mua sắm dễ dàng hơn trong tương lai, hệ thống đã tự động tạo một tài khoản liên kết với địa chỉ email này.\n\n" +
                    "Vui lòng thiết lập mật khẩu cho tài khoản bằng cách truy cập vào đường dẫn dưới đây:\n" +
                    activationUrl + "\n\n" +
                    "Trân trọng,\nSmash VN Team");

            mailSender.send(message);
            long endEmailThread = System.currentTimeMillis();
            log.info("[EmailService] Email sent successfully in {}ms to {}", (endEmailThread - startEmailThread), recipientEmail);
        } catch (Exception e) {
            long endEmailThread = System.currentTimeMillis();
            log.error("[EmailService] Failed to send email in {}ms to {}. Exception: {}", (endEmailThread - startEmailThread), recipientEmail, e.getMessage(), e);
            // Do not fail order checkout if mail sending fails
        }
    }

    @Transactional
    public void setPasswordForGuest(Integer idTaiKhoan, String password) {
        // Concurrency lock: Load TaiKhoan with Pessimistic Write Lock
        TaiKhoan tk = taiKhoanRepository.findByIdForUpdate(idTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Password Activation Race Protection: only GUEST state allowed
        if (tk.getTrangThaiTaiKhoan() != AccountStatus.GUEST) {
            throw new IllegalStateException("Tài khoản đã được kích hoạt trước đó.");
        }

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

    @org.springframework.scheduling.annotation.Async
    public void sendOrderConfirmationEmail(String recipientEmail, com.smashvn.shop.entity.HoaDon hd, String appUrl) {
        long startEmailThread = System.currentTimeMillis();
        log.info("[EmailService] Starting asynchronous order confirmation email sending process.");
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.warn("[EmailService] Recipient email is null or empty, skipping order confirmation email.");
            return;
        }

        String maDonHang = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
        
        // Skip real emails in test environment
        if (maDonHang.startsWith("TEST-")) {
            log.info("[TEST] Skipping sending order confirmation email for test order: {}", maDonHang);
            return;
        }

        String tenNguoiNhan = hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Quý khách";
        String sdt = hd.getSdtNhan() != null ? hd.getSdtNhan() : "N/A";
        String diaChi = hd.getDiaChiNhan() != null ? hd.getDiaChiNhan() : "N/A";
        String phuongThuc = hd.getPaymentMethod() != null ? hd.getPaymentMethod() : (hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "N/A");
        String formattedTongTien = hd.getTongTien() != null ? String.format("%,.0f", hd.getTongTien()) : "0";
        String trackingUrl = appUrl + "/user/track-order?id=" + maDonHang;

        String htmlMsg = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"utf-8\">" +
                "    <title>Xác nhận đặt hàng thành công</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Inter', system-ui, -apple-system, sans-serif;\">" +
                "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e9ecef;\">" +
                "        <tr>" +
                "            <td style=\"padding: 32px 40px; background-color: #e02424; text-align: center;\">" +
                "                <h2 style=\"margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;\">SMASH VN</h2>" +
                "                <p style=\"margin: 4px 0 0 0; color: #fecaca; font-size: 14px;\">Cảm ơn bạn đã mua sắm tại cửa hàng của chúng tôi!</p>" +
                "            </td>" +
                "        </tr>" +
                "        <tr>" +
                "            <td style=\"padding: 40px;\">" +
                "                <p style=\"margin: 0 0 16px 0; color: #111827; font-size: 18px; font-weight: 600;\">Xin chào " + tenNguoiNhan + ",</p>" +
                "                <p style=\"margin: 0 0 24px 0; color: #4b5563; font-size: 16px; line-height: 1.6;\">" +
                "                    Đơn hàng của bạn đã được ghi nhận thành công. Dưới đây là thông tin chi tiết đơn hàng của bạn:" +
                "                </p>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f9fafb; border-radius: 12px; padding: 20px; margin-bottom: 32px; border: 1px solid #f3f4f6;\">" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Mã đơn hàng:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px; font-weight: 700; color: #e02424;\">" + maDonHang + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; font-size: 15px;\">Số điện thoại:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + sdt + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; font-size: 15px;\">Địa chỉ nhận hàng:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px; line-height: 1.4;\">" + diaChi + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; font-size: 15px;\">Thanh toán:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + phuongThuc + "</td>" +
                "                    </tr>" +
                "                    <tr style=\"border-top: 1px solid #e5e7eb;\">" +
                "                        <td style=\"padding: 16px 0 8px 0; color: #111827; font-weight: 700; font-size: 16px;\">Tổng thanh toán:</td>" +
                "                        <td style=\"padding: 16px 0 8px 0; color: #e02424; font-size: 18px; font-weight: 700;\">" + formattedTongTien + " đ</td>" +
                "                    </tr>" +
                "                </table>" +
                "                <p style=\"margin: 0 0 24px 0; color: #4b5563; font-size: 15px; line-height: 1.6;\">" +
                "                    Bạn có thể sử dụng mã đơn hàng trên để theo dõi hành trình giao nhận hàng trực tiếp tại trang web của chúng tôi bằng nút bên dưới:" +
                "                </p>" +
                "                <div style=\"text-align: center; margin-bottom: 30px;\">" +
                "                    <a href=\"" + trackingUrl + "\" style=\"display: inline-block; padding: 14px 30px; background-color: #e02424; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; box-shadow: 0 4px 6px rgba(224, 36, 36, 0.2);\">" +
                "                        Theo Dõi Đơn Hàng" +
                "                    </a>" +
                "                </div>" +
                "                <p style=\"margin: 0; color: #6b7280; font-size: 14px; line-height: 1.5;\">" +
                "                    Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ hotline bộ phận CSKH của chúng tôi để được giải đáp sớm nhất." +
                "                </p>" +
                "            </td>" +
                "        </tr>" +
                "        <tr>" +
                "            <td style=\"padding: 24px; background-color: #f9fafb; text-align: center; border-top: 1px solid #f3f4f6;\">" +
                "                <p style=\"margin: 0; color: #9ca3af; font-size: 12px;\">Hệ thống Cửa hàng Smash VN &copy; 2026</p>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("[Smash VN] Xác nhận đặt hàng thành công - Đơn hàng " + maDonHang);
            helper.setText(htmlMsg, true);
            mailSender.send(message);
            long endEmailThread = System.currentTimeMillis();
            log.info("[EmailService] Order confirmation email sent successfully in {}ms to {}", (endEmailThread - startEmailThread), recipientEmail);
        } catch (Exception e) {
            long endEmailThread = System.currentTimeMillis();
            log.error("[EmailService] Failed to send order confirmation email in {}ms to {}. Exception: {}", (endEmailThread - startEmailThread), recipientEmail, e.getMessage(), e);
        }
    }
}
