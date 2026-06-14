package com.smashvn.shop.migration;

import com.smashvn.shop.entity.EditLog;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TrangThaiGioHang;
import com.smashvn.shop.repository.EditLogRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TrangThaiGioHangRepository;
import com.smashvn.shop.service.admin.AdminNhanVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@Profile("migration")
@RequiredArgsConstructor
public class EmployeeMigrationRunner implements CommandLineRunner {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final EditLogRepository editLogRepository;
    private final TrangThaiGioHangRepository trangThaiGioHangRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Ensure default TrangThaiGioHang with ID 1 exists
        if (trangThaiGioHangRepository.count() == 0) {
            TrangThaiGioHang activeStatus = new TrangThaiGioHang();
            activeStatus.setTenTrangThai("Hoạt động");
            trangThaiGioHangRepository.save(activeStatus);
        }

        List<TaiKhoan> employees = taiKhoanRepository.findByVaiTroIn(Arrays.asList("QL", "NV"));
        
        int migrated = 0;
        int skipped = 0;
        TaiKhoan firstEmployee = null;

        for (TaiKhoan tk : employees) {
            if (firstEmployee == null) {
                firstEmployee = tk;
            }
            KhachHang existingKh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (existingKh != null) {
                skipped++;
            } else {
                NhanVien nv = nhanVienRepository.findByTaiKhoanId(tk.getId());
                String hoTen = (nv != null && nv.getHoTenNv() != null) ? nv.getHoTenNv() : "Nhân viên hệ thống";
                String sdt = (nv != null && nv.getSoDienThoaiNv() != null) ? nv.getSoDienThoaiNv() : "";

                String[] nameParts = AdminNhanVienService.splitFullName(hoTen);

                KhachHang kh = new KhachHang();
                kh.setTaiKhoan(tk);
                kh.setHoKh(nameParts[0]);
                kh.setTenKh(nameParts[1]);
                kh.setSoDienThoaiKh(sdt);
                kh.setNhanBanTin(false);
                kh.setLaTaiKhoanNoiBo(true); // Internal account flag

                khachHangRepository.save(kh);
                migrated++;
            }
        }

        // Print output to console in the exact format required
        System.out.println("=====================================");
        System.out.println("Employee Customer Profile Migration");
        System.out.println("=====================================");
        System.out.println("");
        System.out.println("Migrated Profiles : " + migrated);
        System.out.println("Skipped Profiles  : " + skipped);
        System.out.println("");
        System.out.println("Completed Successfully");
        System.out.println("=====================================");

        // Create a single system-level audit entry in EditLog table if any records were migrated
        if (migrated > 0 && firstEmployee != null) {
            EditLog log = new EditLog();
            log.setTaiKhoan(firstEmployee); // Use first available employee account as non-null link
            log.setTenBang("KhachHang");
            log.setIdBanGhi(0L); // System-level
            log.setHanhDong("INSERT");
            log.setGiaTriCu(null);
            log.setGiaTriMoi(String.format("Migrated: %d, Skipped: %d", migrated, skipped));
            log.setThoiGian(LocalDateTime.now());
            log.setDiaChiIp("127.0.0.1");
            log.setGhiChu(String.format("Employee Customer Profile Migration Completed. Migrated: %d, Skipped: %d", migrated, skipped));
            log.setVaiTroThucHien("SYSTEM");
            editLogRepository.save(log);
        }
    }
}
