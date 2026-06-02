package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.EditLog;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.EditLogRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final EditLogRepository editLogRepository;
    private final JavaMailSender mailSender;
    private final KhachHangRepository khachHangRepository;

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
                tk.getEmail(),
                nv.getHoTenNv(),
                nv.getChucVu(),
                nv.getSoDienThoaiNv(),
                tk.getVaiTro(),
                tk.getTrangThai());
    }

    @Transactional
    public void createNhanVien(String email, String matKhau, String hoTenNv, String chucVu, String soDienThoaiNv, String vaiTro, Integer actingTaiKhoanId, String ipAddress) {
        if ("QL".equals(vaiTro)) {
            throw new RuntimeException("Bạn không có quyền tạo tài khoản quản lý!");
        }

        // 1. Kiểm tra Email tồn tại
        if (taiKhoanRepository.existsByEmail(email)) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        // 2. Tạo TaiKhoan
        TaiKhoan tk = new TaiKhoan();
        tk.setEmail(email);
        tk.setMatKhau(BCrypt.hashpw(matKhau, BCrypt.gensalt()));
        tk.setVaiTro(vaiTro);
        tk.setTrangThai("hoat_dong");
        tk = taiKhoanRepository.save(tk);

        // 3. Tạo NhanVien
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(tk);
        nv.setHoTenNv(hoTenNv);
        nv.setChucVu(chucVu);
        nv.setSoDienThoaiNv(soDienThoaiNv);
        nv = nhanVienRepository.save(nv);

        // --- NEW: Tạo KhachHang ---
        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        String[] nameParts = splitFullName(hoTenNv);
        kh.setHoKh(nameParts[0]);
        kh.setTenKh(nameParts[1]);
        kh.setSoDienThoaiKh(soDienThoaiNv);
        kh.setNhanBanTin(false);
        kh.setLaTaiKhoanNoiBo(true); // Internal account flag
        kh = khachHangRepository.save(kh);

        // 4. Lưu EditLog (Audit Logging)
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            EditLog log = new EditLog();
            log.setTaiKhoan(actingUser);
            log.setTenBang("NhanVien");
            log.setIdBanGhi(nv.getId().longValue());
            log.setHanhDong("INSERT");
            log.setGiaTriCu(null);
            log.setGiaTriMoi(formatState(nv, tk));
            log.setThoiGian(LocalDateTime.now());
            log.setDiaChiIp(ipAddress);
            log.setGhiChu("Tạo nhân viên mới: " + email);
            log.setVaiTroThucHien(actingUser.getVaiTro());
            editLogRepository.save(log);

            // Audit log for customer profile creation
            EditLog khLog = new EditLog();
            khLog.setTaiKhoan(actingUser);
            khLog.setTenBang("KhachHang");
            khLog.setIdBanGhi(kh.getId().longValue());
            khLog.setHanhDong("INSERT");
            khLog.setGiaTriCu(null);
            khLog.setGiaTriMoi(String.format("id=%s, id_tai_khoan=%s, hoKh=%s, tenKh=%s, soDienThoaiKh=%s, laTaiKhoanNoiBo=true", 
                    kh.getId(), tk.getId(), kh.getHoKh(), kh.getTenKh(), kh.getSoDienThoaiKh()));
            khLog.setThoiGian(LocalDateTime.now());
            khLog.setDiaChiIp(ipAddress);
            khLog.setGhiChu("Tạo hồ sơ khách hàng thử nghiệm cho nhân viên: " + email);
            khLog.setVaiTroThucHien(actingUser.getVaiTro());
            editLogRepository.save(khLog);
        }
    }

    @Transactional
    public void updateNhanVien(Integer id, String hoTenNv, String chucVu, String soDienThoaiNv, String vaiTro, String trangThai, String newPassword, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if ("QL".equals(tk.getVaiTro())) {
            throw new RuntimeException("Bạn chỉ có thể theo dõi tài khoản quản lý khác, không thể chỉnh sửa!");
        }
        if ("QL".equals(vaiTro)) {
            throw new RuntimeException("Bạn không thể nâng quyền tài khoản thành quản lý!");
        }

        // 1. Lưu lại trạng thái cũ
        String oldStateStr = formatState(nv, tk);

        // 2. Cập nhật thông tin
        nv.setHoTenNv(hoTenNv);
        nv.setChucVu(chucVu);
        nv.setSoDienThoaiNv(soDienThoaiNv);
        nhanVienRepository.save(nv);

        // Update the linked KhachHang profile if it exists
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        if (kh != null) {
            String[] nameParts = splitFullName(hoTenNv);
            kh.setHoKh(nameParts[0]);
            kh.setTenKh(nameParts[1]);
            kh.setSoDienThoaiKh(soDienThoaiNv);
            khachHangRepository.save(kh);
        }

        tk.setVaiTro(vaiTro);
        tk.setTrangThai(trangThai);
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            tk.setMatKhau(BCrypt.hashpw(newPassword.trim(), BCrypt.gensalt()));
        }
        taiKhoanRepository.save(tk);

        // 3. Lưu EditLog (Audit Logging)
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            EditLog log = new EditLog();
            log.setTaiKhoan(actingUser);
            log.setTenBang("NhanVien");
            log.setIdBanGhi(nv.getId().longValue());
            log.setHanhDong("UPDATE");
            log.setGiaTriCu(oldStateStr);
            log.setGiaTriMoi(formatState(nv, tk));
            log.setThoiGian(LocalDateTime.now());
            log.setDiaChiIp(ipAddress);
            log.setGhiChu("Cập nhật thông tin nhân viên: " + tk.getEmail());
            log.setVaiTroThucHien(actingUser.getVaiTro());
            editLogRepository.save(log);
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
            logMessage = "Yêu cầu khóa tài khoản nhân viên (chờ phê duyệt): " + tk.getEmail();
            
            // Gửi email cho các admin hệ thống kèm link phê duyệt/từ chối trực tiếp
            guiEmailXacNhanKhoa(nv, tk, appUrl);
        } else if ("bi_khoa".equals(oldStatus)) {
            newStatus = "hoat_dong"; // Mở khóa trực tiếp
            logMessage = "Mở khóa tài khoản nhân viên: " + tk.getEmail();
        } else {
            // Đang ở trạng thái cho_khoa, bấm lại thì hủy yêu cầu khóa (trở lại hoạt động)
            newStatus = "hoat_dong";
            logMessage = "Hủy yêu cầu khóa tài khoản nhân viên: " + tk.getEmail();
        }

        tk.setTrangThai(newStatus);
        taiKhoanRepository.save(tk);

        // 3. Lưu EditLog (Audit Logging)
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            EditLog log = new EditLog();
            log.setTaiKhoan(actingUser);
            log.setTenBang("NhanVien");
            log.setIdBanGhi(nv.getId().longValue());
            log.setHanhDong("UPDATE");
            log.setGiaTriCu(oldStateStr);
            log.setGiaTriMoi(formatState(nv, tk));
            log.setThoiGian(LocalDateTime.now());
            log.setDiaChiIp(ipAddress);
            log.setGhiChu(logMessage);
            log.setVaiTroThucHien(actingUser.getVaiTro());
            editLogRepository.save(log);
        }
    }

    private void guiEmailXacNhanKhoa(NhanVien nv, TaiKhoan tk, String appUrl) {
        String[] admins = {"tinhluc02@gmail.com", "luonghiep334@gmail.com"};
        for (String email : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("[Smash VN] Yêu cầu xác nhận khóa tài khoản nhân viên");
                message.setText(String.format(
                        "Chào Admin hệ thống,\n\n" +
                        "Một yêu cầu khóa tài khoản nhân viên vừa được tạo và cần bạn xác nhận:\n" +
                        "- Nhân viên: %s\n" +
                        "- Email: %s\n" +
                        "- Chức vụ: %s\n" +
                        "- Số điện thoại: %s\n\n" +
                        "Vui lòng nhấp vào một trong các liên kết dưới đây để thực hiện hành động:\n" +
                        "1. PHÊ DUYỆT KHÓA TÀI KHOẢN: %s/admin/nhan-vien/approve-lock/%d\n" +
                        "2. TỪ CHỐI KHÓA TÀI KHOẢN: %s/admin/nhan-vien/reject-lock/%d\n\n" +
                        "Yêu cầu này cũng hiển thị trên bảng điều khiển quản trị (Dashboard).\n" +
                        "Trân trọng,\n" +
                        "Hệ thống Quản trị Smash VN",
                        nv.getHoTenNv(), tk.getEmail(), nv.getChucVu(), nv.getSoDienThoaiNv(),
                        appUrl, nv.getId(),
                        appUrl, nv.getId()
                ));
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail phê duyệt khóa đến " + email + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public void approveLock(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if (!"cho_khoa".equals(tk.getTrangThai())) {
            throw new RuntimeException("Tài khoản này không ở trạng thái chờ khóa!");
        }

        String oldStateStr = formatState(nv, tk);
        tk.setTrangThai("bi_khoa");
        taiKhoanRepository.save(tk);

        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            EditLog log = new EditLog();
            log.setTaiKhoan(actingUser);
            log.setTenBang("NhanVien");
            log.setIdBanGhi(nv.getId().longValue());
            log.setHanhDong("UPDATE");
            log.setGiaTriCu(oldStateStr);
            log.setGiaTriMoi(formatState(nv, tk));
            log.setThoiGian(LocalDateTime.now());
            log.setDiaChiIp(ipAddress);
            log.setGhiChu("Phê duyệt khóa tài khoản nhân viên: " + tk.getEmail());
            log.setVaiTroThucHien(actingUser.getVaiTro());
            editLogRepository.save(log);
        }
    }

    @Transactional
    public void rejectLock(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if (!"cho_khoa".equals(tk.getTrangThai())) {
            throw new RuntimeException("Tài khoản này không ở trạng thái chờ khóa!");
        }

        String oldStateStr = formatState(nv, tk);
        tk.setTrangThai("hoat_dong");
        taiKhoanRepository.save(tk);

        // Gửi mail thông báo từ chối khóa về các admin
        guiEmailTuChoiKhoa(nv, tk);

        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            EditLog log = new EditLog();
            log.setTaiKhoan(actingUser);
            log.setTenBang("NhanVien");
            log.setIdBanGhi(nv.getId().longValue());
            log.setHanhDong("UPDATE");
            log.setGiaTriCu(oldStateStr);
            log.setGiaTriMoi(formatState(nv, tk));
            log.setThoiGian(LocalDateTime.now());
            log.setDiaChiIp(ipAddress);
            log.setGhiChu("Từ chối khóa tài khoản nhân viên: " + tk.getEmail());
            log.setVaiTroThucHien(actingUser.getVaiTro());
            editLogRepository.save(log);
        }
    }

    private void guiEmailTuChoiKhoa(NhanVien nv, TaiKhoan tk) {
        String[] admins = {"tinhluc02@gmail.com", "luonghiep334@gmail.com"};
        for (String email : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("[Smash VN] Yêu cầu khóa tài khoản bị từ chối");
                message.setText(String.format(
                        "Chào Admin hệ thống,\n\n" +
                        "Yêu cầu khóa tài khoản nhân viên sau đây đã bị TỪ CHỐI:\n" +
                        "- Nhân viên: %s\n" +
                        "- Email: %s\n" +
                        "- Chức vụ: %s\n\n" +
                        "Tài khoản của nhân viên này vẫn tiếp tục hoạt động bình thường trên hệ thống.\n\n" +
                        "Trân trọng,\n" +
                        "Hệ thống Quản trị Smash VN",
                        nv.getHoTenNv(), tk.getEmail(), nv.getChucVu()
                ));
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail thông báo từ chối khóa đến " + email + ": " + e.getMessage());
            }
        }
    }
}
