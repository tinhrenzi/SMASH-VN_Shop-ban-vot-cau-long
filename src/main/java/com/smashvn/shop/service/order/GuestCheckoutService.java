package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.entity.HoaDonChiTiet;
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
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCheckoutService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final JavaMailSender mailSender;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public String checkEmailStatus(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "NEW";
        }
        
        TaiKhoan tk = taiKhoanRepository.findByUsername(email.trim());
        if (tk == null) {
            return "NEW";
        }

        if (tk.getTrangThaiTaiKhoan() == AccountStatus.ACTIVE) {
            // Auto-correct accounts created without a password
            if (tk.getMatKhau() == null || tk.getMatKhau().trim().isEmpty()) {
                log.info("[GUEST_CHECKOUT] Correcting corrupt account status ACTIVE -> GUEST for email: {}", email);
                tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
                taiKhoanRepository.save(tk);
            } else {
                return "ACTIVE";
            }
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

        TaiKhoan existingTk = taiKhoanRepository.findByUsername(trimmedEmail);
        if (existingTk != null) {
            log.info("[GUEST_CHECKOUT] Linked order with existing guest account: {}", trimmedEmail);
            // Auto-correct corrupt guest account status if it was set to ACTIVE without password
            if ((existingTk.getMatKhau() == null || existingTk.getMatKhau().trim().isEmpty()) && existingTk.getTrangThaiTaiKhoan() == AccountStatus.ACTIVE) {
                log.info("[GUEST_CHECKOUT] Auto-correcting corrupt guest account status to GUEST for: {}", trimmedEmail);
                existingTk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
                taiKhoanRepository.save(existingTk);
            }

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
        tk.setUsername(trimmedEmail);
        tk.setMatKhau(null); // No password initially
        tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        tk.setSoLanMuaThanhCong(0); // Will be incremented when order is created
        tk.setVaiTro("KH");

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

        // Tạo thông báo hệ thống cho tài khoản vãng lai được khởi tạo tự động
        try {
            ThongBao thongBaoAcc = ThongBao.builder()
                    .taiKhoan(savedTk)
                    .tieuDe("Tài khoản của bạn đã được khởi tạo tự động")
                    .noiDung("Cảm ơn bạn đã mua sắm tại Smash VN! Tài khoản của bạn đã được hệ thống tự động khởi tạo theo email " 
                            + trimmedEmail + ". Vui lòng kiểm tra hộp thư email của bạn để nhận đường dẫn kích hoạt và thiết lập lại mật khẩu.")
                    .daDoc(false)
                    .loaiThongBao("tai_khoan")
                    .ngayTao(LocalDateTime.now())
                    .build();
            thongBaoRepository.save(thongBaoAcc);
            log.info("[GUEST_CHECKOUT] Created system notification for auto-registered account ID {}", savedTk.getId());
        } catch (Exception e) {
            log.error("[GUEST_CHECKOUT] Failed to create system notification for account ID {}: {}", savedTk.getId(), e.getMessage());
        }

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

        if (recipientEmail.contains("test") || recipientEmail.contains("tester") || recipientEmail.endsWith("@smashvn.com") || recipientEmail.endsWith("@example.com")) {
            log.info("[TEST] Skipping sending order and account notification email for test recipient: {}", recipientEmail);
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            log.warn("[EmailService] Activation token is null or empty, skipping email notification.");
            return;
        }

        String activationUrl = appUrl + "/user/thiet-lap-mat-khau?token=" + token;

        String htmlMsg = "<!DOCTYPE html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Xác nhận đặt hàng và kích hoạt tài khoản - Smash VN</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; -webkit-font-smoothing: antialiased;\">" +
                "    <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #f1f5f9; padding: 40px 10px;\">" +
                "        <tr>" +
                "            <td align=\"center\">" +
                "                <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08); border: 1px solid #e2e8f0;\">" +
                "                    <tr>" +
                "                        <td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 32px 40px; text-align: center; border-bottom: 3px solid #ff4500;\">" +
                "                            <h1 style=\"margin: 0; color: #ffffff; font-size: 26px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase;\">" +
                "                                SMASH <span style=\"color: #ff4500;\">VN</span>" +
                "                            </h1>" +
                "                            <p style=\"margin: 6px 0 0 0; color: #94a3b8; font-size: 13px; font-weight: 500; text-transform: uppercase; letter-spacing: 1.5px;\">" +
                "                                Cửa Hàng Cầu Lông Chuyên Nghiệp" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 36px 40px 20px 40px; text-align: center;\">" +
                "                            <div style=\"display: inline-block; width: 64px; height: 64px; line-height: 64px; background-color: #ecfdf5; border-radius: 50%; color: #10b981; font-size: 32px; margin-bottom: 16px; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);\">" +
                "                                &#10004;" +
                "                            </div>" +
                "                            <h2 style=\"margin: 0; color: #0f172a; font-size: 22px; font-weight: 700;\">" +
                "                                Đặt Hàng Thành Công!" +
                "                            </h2>" +
                "                            <p style=\"margin: 10px 0 0 0; color: #64748b; font-size: 15px; line-height: 1.6;\">" +
                "                                Cảm ơn bạn đã tin tưởng mua sắm tại <strong>Smash VN</strong>. Đơn hàng của bạn đã được ghi nhận thành công trên hệ thống." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 0 40px 24px 40px;\">" +
                "                            <div style=\"background-color: #f8fafc; border: 1px solid #e2e8f0; border-left: 4px solid #3b82f6; border-radius: 12px; padding: 20px;\">" +
                "                                <h3 style=\"margin: 0 0 8px 0; color: #1e293b; font-size: 15px; font-weight: 700;\">" +
                "                                    &#128274; Tự Động Khởi Tạo Tài Khoản" +
                "                                </h3>" +
                "                                <p style=\"margin: 0; color: #475569; font-size: 14px; line-height: 1.6;\">" +
                "                                    Để hỗ trợ bạn theo dõi tiến độ đơn hàng và mua sắm dễ dàng hơn trong tương lai, hệ thống đã tự động tạo một tài khoản liên kết với địa chỉ email này." +
                "                                </p>" +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 0 40px 32px 40px; text-align: center;\">" +
                "                            <p style=\"margin: 0 0 20px 0; color: #334155; font-size: 14px; font-weight: 600;\">" +
                "                                Vui lòng thiết lập mật khẩu cho tài khoản bằng cách truy cập nút dưới đây:" +
                "                            </p>" +
                "                            <a href=\"" + activationUrl + "\" target=\"_blank\" style=\"display: inline-block; padding: 16px 36px; background: linear-gradient(135deg, #ff4500 0%, #e02424 100%); color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; border-radius: 10px; box-shadow: 0 4px 14px rgba(255, 69, 0, 0.35); letter-spacing: 0.5px;\">" +
                "                                THIẾT LẬP MẬT KHẨU NGAY &rarr;" +
                "                            </a>" +
                "                            <p style=\"margin: 16px 0 0 0; color: #94a3b8; font-size: 12px;\">" +
                "                                (Liên kết kích hoạt có hiệu lực trong vòng 30 ngày)" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 20px 40px; background-color: #f8fafc; border-top: 1px solid #f1f5f9;\">" +
                "                            <p style=\"margin: 0 0 6px 0; color: #64748b; font-size: 12px; font-weight: 600;\">" +
                "                                Nếu nút bấm trên không hoạt động, bạn có thể truy cập qua đường dẫn sau:" +
                "                            </p>" +
                "                            <p style=\"margin: 0; word-break: break-all; font-size: 12px;\">" +
                "                                <a href=\"" + activationUrl + "\" style=\"color: #2563eb; text-decoration: underline;\">" + activationUrl + "</a>" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"background-color: #0f172a; padding: 24px 40px; text-align: center;\">" +
                "                            <p style=\"margin: 0 0 6px 0; color: #f8fafc; font-size: 13px; font-weight: 600;\">" +
                "                                SMASH VN - Hệ Thống Shop Cầu Lông Chuyên Nghiệp" +
                "                            </p>" +
                "                            <p style=\"margin: 0; color: #64748b; font-size: 11px;\">" +
                "                                &copy; 2026 Smash VN. All rights reserved." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("Xác nhận đặt hàng thành công và kích hoạt tài khoản - Smash VN");
            helper.setText(htmlMsg, true);

            mailSender.send(message);
            long endEmailThread = System.currentTimeMillis();
            log.info("[EmailService] HTML Email sent successfully in {}ms to {}", (endEmailThread - startEmailThread), recipientEmail);
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

        // Password Activation Race Protection: only GUEST state allowed (unless password is missing)
        if (tk.getTrangThaiTaiKhoan() != AccountStatus.GUEST && (tk.getMatKhau() != null && !tk.getMatKhau().trim().isEmpty())) {
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

        tk.setMatKhau(passwordEncoder.encode(password));
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
        if (maDonHang.startsWith("TEST-") || recipientEmail.contains("test") || recipientEmail.contains("tester") || recipientEmail.endsWith("@smashvn.com") || recipientEmail.endsWith("@example.com")) {
            log.info("[TEST] Skipping sending order confirmation email for test order: {} to {}", maDonHang, recipientEmail);
            return;
        }

        String tenNguoiNhan = hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Quý khách";
        String sdt = hd.getSdtNhan() != null ? hd.getSdtNhan() : "N/A";
        String diaChi = hd.getDiaChiNhan() != null ? hd.getDiaChiNhan() : "N/A";
        String phuongThuc = hd.getPaymentMethod() != null ? hd.getPaymentMethod() : (hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "N/A");
        String formattedTongTien = hd.getTongTien() != null ? String.format("%,.0f", hd.getTongTien()) : "0";
        String trackingUrl = appUrl + "/user/track-order?id=" + maDonHang;

        // Query product list for invoice email
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
        StringBuilder itemsHtml = new StringBuilder();
        BigDecimal tamTinh = BigDecimal.ZERO;
        if (items != null) {
            for (HoaDonChiTiet ct : items) {
                String tenSp = ct.getTenSanPhamSnapshot() != null ? ct.getTenSanPhamSnapshot() : (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getSanPham() != null ? ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : "N/A");
                String thuocTinh = (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getPhanLoaiHienThi() != null)
                        ? ct.getSanPhamChiTiet().getPhanLoaiHienThi()
                        : (ct.getThuocTinhSnapshot() != null ? ct.getThuocTinhSnapshot() : "Mặc định");
                int sl = ct.getSoLuong();
                BigDecimal donGia = ct.getDonGia() != null ? ct.getDonGia() : BigDecimal.ZERO;
                BigDecimal thanhTien = donGia.multiply(new BigDecimal(sl));
                tamTinh = tamTinh.add(thanhTien);
                
                String imgName = "product9.jpg";
                if (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getHinhAnhSanPham() != null && !ct.getSanPhamChiTiet().getHinhAnhSanPham().isEmpty()) {
                    imgName = ct.getSanPhamChiTiet().getHinhAnhSanPham();
                }
                String imgUrl = appUrl + "/uploads/product/" + imgName;

                itemsHtml.append("<tr style=\"border-bottom: 1px solid #f3f4f6;\">")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: center; width: 60px;\">")
                    .append("    <img src=\"").append(imgUrl).append("\" alt=\"product\" style=\"width: 50px; height: 50px; object-fit: cover; border-radius: 6px; border: 1px solid #e5e7eb;\" onerror=\"this.style.display='none';\">")
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: left;\">")
                    .append("    <strong style=\"color: #1f2937; font-size: 14px;\">").append(tenSp).append("</strong><br>")
                    .append("    <span style=\"color: #6b7280; font-size: 12px;\">").append(thuocTinh).append("</span>")
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: center; color: #4b5563; font-size: 14px;\">")
                    .append(sl)
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: right; color: #4b5563; font-size: 14px;\">")
                    .append(String.format("%,.0f", donGia)).append(" đ")
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: right; color: #111827; font-weight: 600; font-size: 14px;\">")
                    .append(String.format("%,.0f", thanhTien)).append(" đ")
                    .append("  </td>")
                    .append("</tr>");
            }
        }

        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal voucherGiam = hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : BigDecimal.ZERO;

        String detailsHtml = "";
        if (hd.getMaGiaoDich() != null && !hd.getMaGiaoDich().isEmpty()) {
            detailsHtml += "                    <tr>" +
                    "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Mã giao dịch:</td>" +
                    "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + hd.getMaGiaoDich() + "</td>" +
                    "                    </tr>";
        } else if (hd.getTransactionId() != null && !hd.getTransactionId().isEmpty()) {
            detailsHtml += "                    <tr>" +
                    "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Transaction ID:</td>" +
                    "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + hd.getTransactionId() + "</td>" +
                    "                    </tr>";
        }

        String htmlMsg = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"utf-8\">" +
                "    <title>Xác nhận đặt hàng thành công</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Inter', system-ui, -apple-system, sans-serif;\">" +
                "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 650px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e9ecef;\">" +
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
                "                    Đơn hàng của bạn đã được ghi nhận thành công. Dưới đây là thông tin chi tiết hóa đơn đơn hàng của bạn:" +
                "                </p>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f9fafb; border-radius: 12px; padding: 20px; margin-bottom: 24px; border: 1px solid #f3f4f6;\">" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Mã đơn hàng:</td>" +
                "                        <td style=\"padding: 8px 0; color: #e02424; font-size: 15px; font-weight: 700;\">" + maDonHang + "</td>" +
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
                detailsHtml +
                "                </table>" +
                "                <h3 style=\"color: #111827; font-size: 16px; font-weight: 700; margin-bottom: 12px; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px;\">CHI TIẾT SẢN PHẨM</h3>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"border-collapse: collapse; margin-bottom: 24px;\">" +
                "                    <thead>" +
                "                        <tr style=\"background-color: #f3f4f6; border-bottom: 2px solid #e5e7eb;\">" +
                "                            <th style=\"padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: #374151; width: 60px;\">Ảnh</th>" +
                "                            <th style=\"padding: 8px; text-align: left; font-size: 12px; font-weight: 600; color: #374151;\">Sản phẩm</th>" +
                "                            <th style=\"padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: #374151; width: 40px;\">SL</th>" +
                "                            <th style=\"padding: 8px; text-align: right; font-size: 12px; font-weight: 600; color: #374151; width: 90px;\">Đơn giá</th>" +
                "                            <th style=\"padding: 8px; text-align: right; font-size: 12px; font-weight: 600; color: #374151; width: 100px;\">Thành tiền</th>" +
                "                        </tr>" +
                "                    </thead>" +
                "                    <tbody>" +
                itemsHtml.toString() +
                "                    </tbody>" +
                "                </table>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"right\" width=\"300px\" style=\"margin-bottom: 30px; font-size: 14px;\">" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #4b5563;\">Tạm tính:</td>" +
                "                        <td style=\"padding: 6px 0; text-align: right; color: #111827; font-weight: 600;\">" + String.format("%,.0f", tamTinh) + " đ</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #4b5563;\">Phí vận chuyển:</td>" +
                "                        <td style=\"padding: 6px 0; text-align: right; color: #111827; font-weight: 600;\">" + String.format("%,.0f", phiShip) + " đ</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #4b5563;\">Voucher giảm giá:</td>" +
                "                        <td style=\"padding: 6px 0; text-align: right; color: #10b981; font-weight: 600;\">-" + String.format("%,.0f", voucherGiam) + " đ</td>" +
                "                    </tr>" +
                "                    <tr style=\"border-top: 2px solid #e5e7eb;\">" +
                "                        <td style=\"padding: 12px 0; color: #111827; font-weight: 700; font-size: 16px;\">Tổng thanh toán:</td>" +
                "                        <td style=\"padding: 12px 0; text-align: right; color: #e02424; font-size: 18px; font-weight: 700;\">" + formattedTongTien + " đ</td>" +
                "                    </tr>" +
                "                </table>" +
                "                <div style=\"clear: both;\"></div>" +
                "                <p style=\"margin: 20px 0 24px 0; color: #4b5563; font-size: 15px; line-height: 1.6;\">" +
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
