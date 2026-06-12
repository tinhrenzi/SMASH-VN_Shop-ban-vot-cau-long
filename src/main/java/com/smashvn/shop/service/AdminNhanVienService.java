package com.smashvn.shop.service;

import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final JavaMailSender mailSender;
    private final KhachHangRepository khachHangRepository;

    @Value("${app.admin.emails}")
    private String adminEmailsConfig;

    public static String[] splitFullName(String fullName) {
        if (fullName == null) {
            return new String[]{"", ""};
        }
        String hoTen = fullName.trim();
        if (hoTen.isEmpty()) {
            return new String[]{"", ""};
        }
        int lastSpaceIndex = hoTen.lastIndexOf(' ');
        if (lastSpaceIndex == -1) {
            return new String[]{"", hoTen};
        }
        String hoKh = hoTen.substring(0, lastSpaceIndex).trim();
        String tenKh = hoTen.substring(lastSpaceIndex + 1).trim();
        return new String[]{hoKh, tenKh};
    }

    public List<NhanVien> getAllNhanVien() {
        return nhanVienRepository.findAll();
    }

    public List<NhanVien> searchNhanVien(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllNhanVien();
        }
        return nhanVienRepository.searchNhanVien("%" + keyword.trim() + "%");
    }

    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));
    }

    private String formatState(NhanVien nv, TaiKhoan tk) {
        if (nv == null || tk == null) {
            return "";
        }
        return String.format("id=%s, email=%s, hoTenNv=%s, chucVu=%s, soDienThoaiNv=%s, vaiTro=%s, trangThai=%s",
                nv.getId() != null ? nv.getId().toString() : "null",
                com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail()),
                nv.getHoTenNv(),
                nv.getChucVu(),
                com.smashvn.shop.util.ValidationUtils.maskPhone(nv.getSoDienThoaiNv()),
                tk.getVaiTro(),
                tk.getTrangThai());
    }

    @Transactional
    public void createNhanVien(String email, String matKhau, String hoTenNv, String chucVu, String soDienThoaiNv, String vaiTro, Integer actingTaiKhoanId, String ipAddress) {
        String trimmedEmail = (email == null) ? "" : email.trim();
        String sanitizedEmail = org.jsoup.Jsoup.clean(trimmedEmail, org.jsoup.safety.Safelist.none());

        String trimmedName = (hoTenNv == null) ? "" : hoTenNv.trim();
        String sanitizedName = org.jsoup.Jsoup.clean(trimmedName, org.jsoup.safety.Safelist.none());

        String trimmedChucVu = (chucVu == null) ? "" : chucVu.trim();
        String sanitizedChucVu = org.jsoup.Jsoup.clean(trimmedChucVu, org.jsoup.safety.Safelist.none());

        String trimmedPhone = (soDienThoaiNv == null) ? "" : soDienThoaiNv.trim();

        if (sanitizedEmail.isEmpty()) {
            throw new RuntimeException("Email không được để trống!");
        }
        if (sanitizedEmail.length() > 100) {
            throw new RuntimeException("Email không được vượt quá 100 ký tự!");
        }
        if (!sanitizedEmail.matches(com.smashvn.shop.util.ValidationUtils.EMAIL_REGEX)) {
            throw new RuntimeException("Email không đúng định dạng!");
        }

        if (matKhau == null || matKhau.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }
        String trimmedPassword = matKhau.trim();
        if (trimmedPassword.length() < 8 || trimmedPassword.length() > 50) {
            throw new RuntimeException("Mật khẩu phải có độ dài từ 8 đến 50 ký tự!");
        }

        if (sanitizedName.isEmpty()) {
            throw new RuntimeException("Họ tên nhân viên không được để trống!");
        }
        if (sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new RuntimeException("Họ tên nhân viên phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (sanitizedChucVu.isEmpty()) {
            throw new RuntimeException("Chức vụ không được để trống!");
        }
        if (sanitizedChucVu.length() < 2 || sanitizedChucVu.length() > 50) {
            throw new RuntimeException("Chức vụ phải có độ dài từ 2 đến 50 ký tự!");
        }

        if (trimmedPhone.isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        if (!trimmedPhone.matches(com.smashvn.shop.util.ValidationUtils.PHONE_REGEX)) {
            throw new RuntimeException("Số điện thoại không đúng định dạng!");
        }

        if (!"NV".equals(vaiTro) && !"QL".equals(vaiTro)) {
            throw new RuntimeException("Vai trò không hợp lệ!");
        }

        // 1. Kiểm tra Email tồn tại
        if (taiKhoanRepository.existsByEmail(sanitizedEmail)) {
            TaiKhoan existingTk = taiKhoanRepository.findByEmail(sanitizedEmail);
            if (Boolean.TRUE.equals(existingTk.getLaNhanVien()) || Boolean.TRUE.equals(existingTk.getLaQuanLy())) {
                throw new RuntimeException("Email này đã được sử dụng bởi một nhân viên/quản lý khác!");
            }

            // Nâng quyền tài khoản khách hàng thành nhân viên
            existingTk.setLaKhachHang(true);
            existingTk.setLaNhanVien(true);
            existingTk.setLaQuanLy("QL".equals(vaiTro));
            existingTk.setVaiTro(vaiTro);
            existingTk = taiKhoanRepository.save(existingTk);

            // Tạo NhanVien
            NhanVien nv = new NhanVien();
            nv.setTaiKhoan(existingTk);
            nv.setHoTenNv(sanitizedName);
            nv.setChucVu(sanitizedChucVu);
            nv.setSoDienThoaiNv(trimmedPhone);
            nv = nhanVienRepository.save(nv);

            // Lưu Audit Logs
            TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
            if (actingUser != null) {
                auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "INSERT", null, formatState(nv, existingTk), ipAddress, "Nâng quyền tài khoản khách hàng thành nhân viên mới: " + com.smashvn.shop.util.ValidationUtils.maskEmail(sanitizedEmail), actingUser.getVaiTro());
            }
            return;
        }

        // 2. Tạo TaiKhoan mới
        TaiKhoan tk = new TaiKhoan();
        tk.setEmail(sanitizedEmail);
        tk.setMatKhau(BCrypt.hashpw(trimmedPassword, BCrypt.gensalt()));
        tk.setVaiTro(vaiTro);
        tk.setTrangThai("hoat_dong");
        tk.setLaKhachHang(true); // new employees also have customer role by default
        tk.setLaNhanVien("NV".equals(vaiTro) || "QL".equals(vaiTro));
        tk.setLaQuanLy("QL".equals(vaiTro));
        tk = taiKhoanRepository.save(tk);

        // 3. Tạo NhanVien
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(tk);
        nv.setHoTenNv(sanitizedName);
        nv.setChucVu(sanitizedChucVu);
        nv.setSoDienThoaiNv(trimmedPhone);
        nv = nhanVienRepository.save(nv);

        // --- NEW: Tạo KhachHang ---
        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        String[] nameParts = splitFullName(sanitizedName);
        kh.setHoKh(nameParts[0]);
        kh.setTenKh(nameParts[1]);
        kh.setSoDienThoaiKh(trimmedPhone);
        kh.setNhanBanTin(false);
        kh.setLaTaiKhoanNoiBo(true); // Internal account flag
        kh = khachHangRepository.save(kh);

        // 4. Lưu Audit Logs
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "INSERT", null, formatState(nv, tk), ipAddress, "Tạo nhân viên mới: " + com.smashvn.shop.util.ValidationUtils.maskEmail(sanitizedEmail), actingUser.getVaiTro());
            auditService.log(actingTaiKhoanId, "KhachHang", kh.getId().longValue(), "INSERT", null, String.format("id=%s, id_tai_khoan=%s, hoKh=%s, tenKh=%s, soDienThoaiKh=%s, laTaiKhoanNoiBo=true", kh.getId(), tk.getId(), kh.getHoKh(), kh.getTenKh(), com.smashvn.shop.util.ValidationUtils.maskPhone(kh.getSoDienThoaiKh())), ipAddress, "Tạo hồ sơ khách hàng thử nghiệm cho nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(sanitizedEmail), actingUser.getVaiTro());
        }
    }

    @Transactional
    public void updateNhanVien(Integer id, String hoTenNv, String chucVu, String soDienThoaiNv, Boolean laKhachHang, Boolean laNhanVien, Boolean laQuanLy, String trangThai, String newPassword, Integer actingTaiKhoanId, String ipAddress) {
        String trimmedName = (hoTenNv == null) ? "" : hoTenNv.trim();
        String sanitizedName = org.jsoup.Jsoup.clean(trimmedName, org.jsoup.safety.Safelist.none());

        String trimmedChucVu = (chucVu == null) ? "" : chucVu.trim();
        String sanitizedChucVu = org.jsoup.Jsoup.clean(trimmedChucVu, org.jsoup.safety.Safelist.none());

        String trimmedPhone = (soDienThoaiNv == null) ? "" : soDienThoaiNv.trim();

        if (sanitizedName.isEmpty()) {
            throw new RuntimeException("Họ tên nhân viên không được để trống!");
        }
        if (sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new RuntimeException("Họ tên nhân viên phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (sanitizedChucVu.isEmpty()) {
            throw new RuntimeException("Chức vụ không được để trống!");
        }
        if (sanitizedChucVu.length() < 2 || sanitizedChucVu.length() > 50) {
            throw new RuntimeException("Chức vụ phải có độ dài từ 2 đến 50 ký tự!");
        }

        if (trimmedPhone.isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        if (!trimmedPhone.matches(com.smashvn.shop.util.ValidationUtils.PHONE_REGEX)) {
            throw new RuntimeException("Số điện thoại không đúng định dạng!");
        }

        if (trangThai == null || (!"hoat_dong".equals(trangThai) && !"cho_khoa".equals(trangThai) && !"bi_khoa".equals(trangThai))) {
            throw new RuntimeException("Trạng thái không hợp lệ!");
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            String trimmedPassword = newPassword.trim();
            if (trimmedPassword.length() < 8 || trimmedPassword.length() > 50) {
                throw new RuntimeException("Mật khẩu mới phải có độ dài từ 8 đến 50 ký tự!");
            }
        }

        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if (laKhachHang == null) {
            laKhachHang = false;
        }
        if (laNhanVien == null) {
            laNhanVien = false;
        }
        if (laQuanLy == null) {
            laQuanLy = false;
        }

        if (!laKhachHang && !laNhanVien && !laQuanLy) {
            throw new RuntimeException("Tài khoản phải có ít nhất một vai trò!");
        }

        // 1. Lưu lại trạng thái cũ
        String oldStateStr = formatState(nv, tk);

        // 2. Cập nhật thông tin
        nv.setHoTenNv(sanitizedName);
        nv.setChucVu(sanitizedChucVu);
        nv.setSoDienThoaiNv(trimmedPhone);
        nhanVienRepository.save(nv);

        // Update the linked KhachHang profile if it exists
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        if (kh != null) {
            String[] nameParts = splitFullName(sanitizedName);
            kh.setHoKh(nameParts[0]);
            kh.setTenKh(nameParts[1]);
            kh.setSoDienThoaiKh(trimmedPhone);
            khachHangRepository.save(kh);
        }

        // Cập nhật các cờ vai trò
        tk.setLaKhachHang(laKhachHang);
        tk.setLaNhanVien(laNhanVien);
        tk.setLaQuanLy(laQuanLy);

        // Cập nhật vai_tro và vai_tro_hien_tai cho tương thích ngược
        if (laQuanLy) {
            tk.setVaiTro("QL");
        } else if (laNhanVien) {
            tk.setVaiTro("NV");
        } else {
            tk.setVaiTro("KH");
        }

        tk.setTrangThai(trangThai);
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            tk.setMatKhau(BCrypt.hashpw(newPassword.trim(), BCrypt.gensalt()));
        }
        taiKhoanRepository.save(tk);

        // Soft deactivation via role flags is handled, NhanVien profile remains in database.
        // 3. Lưu Audit Log
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Cập nhật thông tin và vai trò nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail()), actingUser.getVaiTro());
        }
    }

    @Transactional
    public void toggleStatus(Integer id, Integer actingTaiKhoanId, String ipAddress, String appUrl) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if ("QL".equals(tk.getVaiTro())) {
            throw new RuntimeException("Bạn không thể khóa/mở khóa tài khoản quản lý khác!");
        }

        // 1. Lưu trạng thái cũ
        String oldStateStr = formatState(nv, tk);

        // 2. Đổi trạng thái
        String oldStatus = tk.getTrangThai();
        String newStatus;
        String logMessage;

        if ("hoat_dong".equals(oldStatus)) {
            newStatus = "cho_khoa"; // Chờ phê duyệt khóa
            logMessage = "Yêu cầu khóa tài khoản nhân viên (chờ phê duyệt): " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail());

            // Tạo token ngẫu nhiên khi status chuyển thành cho_khoa
            String token = java.util.UUID.randomUUID().toString();
            tk.setTokenXacThucKhoa(token);

            // Gửi email cho các admin hệ thống kèm link phê duyệt/từ chối trực tiếp
            guiEmailXacNhanKhoa(nv, tk, token, appUrl);
        } else if ("bi_khoa".equals(oldStatus)) {
            newStatus = "hoat_dong"; // Mở khóa trực tiếp
            logMessage = "Mở khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail());
            tk.setTokenXacThucKhoa(null);
        } else {
            // Đang ở trạng thái cho_khoa, bấm lại thì hủy yêu cầu khóa (trở lại hoạt động)
            newStatus = "hoat_dong";
            logMessage = "Hủy yêu cầu khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail());
            tk.setTokenXacThucKhoa(null);
        }

        tk.setTrangThai(newStatus);
        taiKhoanRepository.save(tk);

        // 3. Lưu Audit Log
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, logMessage, actingUser.getVaiTro());
        }
    }

    private void guiEmailXacNhanKhoa(NhanVien nv, TaiKhoan tk, String token, String appUrl) {
        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            System.err.println("Không có email quản trị nào được cấu hình trong app.admin.emails!");
            return;
        }
        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email.trim());
                message.setSubject("[Smash VN] Yêu cầu xác nhận khóa tài khoản nhân viên");
                message.setText(String.format(
                        "Chào Admin hệ thống,\n\n"
                        + "Một yêu cầu khóa tài khoản nhân viên vừa được tạo và cần bạn xác nhận:\n"
                        + "- Nhân viên: %s\n"
                        + "- Email: %s\n"
                        + "- Chức vụ: %s\n"
                        + "- Số điện thoại: %s\n\n"
                        + "Vui lòng nhấp vào một trong các liên kết dưới đây để thực hiện hành động:\n"
                        + "1. PHÊ DUYỆT KHÓA TÀI KHOẢN: %s/admin/nhan-vien/approve-lock/%d?token=%s\n"
                        + "2. TỪ CHỐI KHÓA TÀI KHOẢN: %s/admin/nhan-vien/reject-lock/%d?token=%s\n\n"
                        + "Yêu cầu này cũng hiển thị trên bảng điều khiển quản trị (Dashboard).\n"
                        + "Trân trọng,\n"
                        + "Hệ thống Quản trị Smash VN",
                        nv.getHoTenNv(), tk.getEmail(), nv.getChucVu(), nv.getSoDienThoaiNv(),
                        appUrl, nv.getId(), token,
                        appUrl, nv.getId(), token
                ));
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail phê duyệt khóa đến " + email + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public void approveLock(Integer id, String token, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if (!"cho_khoa".equals(tk.getTrangThai())) {
            throw new RuntimeException("Tài khoản này không ở trạng thái chờ khóa!");
        }

        // Kiểm tra quyền: phải là Quản lý đăng nhập HOẶC token phải trùng khớp
        boolean authorized = false;
        TaiKhoan actingUser = null;
        if (actingTaiKhoanId != null) {
            actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
            if (actingUser != null && "QL".equals(actingUser.getVaiTro())) {
                authorized = true;
            }
        }

        if (!authorized) {
            if (token != null && token.equals(tk.getTokenXacThucKhoa())) {
                authorized = true;
            }
        }

        if (!authorized) {
            throw new RuntimeException("Bạn không có quyền thực hiện phê duyệt khóa tài khoản này!");
        }

        String oldStateStr = formatState(nv, tk);
        tk.setTrangThai("bi_khoa");
        tk.setTokenXacThucKhoa(null); // Clear token
        taiKhoanRepository.save(tk);

        if (actingUser == null) {
            auditService.log(tk.getId(), "TaiKhoan", tk.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Phê duyệt khóa tài khoản qua token Email", "SYSTEM_EMAIL");
        } else {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Phê duyệt khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail()), actingUser.getVaiTro());
        }
    }

    @Transactional
    public void rejectLock(Integer id, String token, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if (!"cho_khoa".equals(tk.getTrangThai())) {
            throw new RuntimeException("Tài khoản này không ở trạng thái chờ khóa!");
        }

        // Kiểm tra quyền: phải là Quản lý đăng nhập HOẶC token phải trùng khớp
        boolean authorized = false;
        TaiKhoan actingUser = null;
        if (actingTaiKhoanId != null) {
            actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
            if (actingUser != null && "QL".equals(actingUser.getVaiTro())) {
                authorized = true;
            }
        }

        if (!authorized) {
            if (token != null && token.equals(tk.getTokenXacThucKhoa())) {
                authorized = true;
            }
        }

        if (!authorized) {
            throw new RuntimeException("Bạn không có quyền thực hiện từ chối khóa tài khoản này!");
        }

        String oldStateStr = formatState(nv, tk);
        tk.setTrangThai("hoat_dong");
        tk.setTokenXacThucKhoa(null); // Clear token
        taiKhoanRepository.save(tk);

        // Gửi mail thông báo từ chối khóa về các admin
        guiEmailTuChoiKhoa(nv, tk);

        if (actingUser == null) {
            auditService.log(tk.getId(), "TaiKhoan", tk.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Từ chối khóa tài khoản qua token Email", "SYSTEM_EMAIL");
        } else {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Từ chối khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getEmail()), actingUser.getVaiTro());
        }
    }

    private void guiEmailTuChoiKhoa(NhanVien nv, TaiKhoan tk) {
        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            return;
        }
        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email.trim());
                message.setSubject("[Smash VN] Yêu cầu khóa tài khoản bị từ chối");
                message.setText(String.format(
                        "Chào Admin hệ thống,\n\n"
                        + "Yêu cầu khóa tài khoản nhân viên sau đây đã bị TỪ CHỐI:\n"
                        + "- Nhân viên: %s\n"
                        + "- Email: %s\n"
                        + "- Chức vụ: %s\n\n"
                        + "Tài khoản của nhân viên này vẫn tiếp tục hoạt động bình thường trên hệ thống.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống Quản trị Smash VN",
                        nv.getHoTenNv(), tk.getEmail(), nv.getChucVu()
                ));
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail thông báo từ chối khóa đến " + email + ": " + e.getMessage());
            }
        }
    }
}
