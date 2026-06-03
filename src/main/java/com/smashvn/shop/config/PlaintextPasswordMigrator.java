package com.smashvn.shop.config;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Component
@Profile("migration")
@RequiredArgsConstructor
@Slf4j
public class PlaintextPasswordMigrator implements CommandLineRunner {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("[MIGRATION] Bắt đầu quét cơ sở dữ liệu để tìm và mã hóa mật khẩu plaintext...");
        
        List<TaiKhoan> accounts = taiKhoanRepository.findAll();
        int count = 0;
        
        for (TaiKhoan tk : accounts) {
            String dbPass = tk.getMatKhau();
            
            // Nếu mật khẩu không trống và không có tiền tố của BCrypt ($2a$, $2b$, $2y$) -> đây là plaintext
            if (dbPass != null && !dbPass.startsWith("$2a$") && !dbPass.startsWith("$2b$") && !dbPass.startsWith("$2y$")) {
                String hashed = BCrypt.hashpw(dbPass, BCrypt.gensalt());
                tk.setMatKhau(hashed);
                taiKhoanRepository.save(tk);
                count++;
                log.info("[MIGRATION] Đã mã hóa thành công tài khoản email: {}", tk.getEmail());
            }
        }
        
        log.info("[MIGRATION] Hoàn thành di trú! Tổng cộng đã mã hóa thành công {} tài khoản.", count);
    }
}
