package com.smashvn.shop.controller.advice;

import com.smashvn.shop.repository.ThongBaoRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import javax.sql.DataSource;
import java.sql.Connection;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalNotificationAdvice {

    private final ThongBaoRepository thongBaoRepository;
    private final DataSource dataSource;

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return 0;
        }
        try {
            return thongBaoRepository.countByTaiKhoan_IdAndDaDocFalse(idNguoiDung);
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("dbName")
    public String getDbName() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getCatalog();
        } catch (Exception e) {
            return "BadmintonShopDB3";
        }
    }

    @ModelAttribute("dbVersion")
    public String getDbVersion() {
        try (Connection conn = dataSource.getConnection()) {
            String fullVersion = conn.getMetaData().getDatabaseProductVersion();
            if (fullVersion != null && fullVersion.contains("\n")) {
                fullVersion = fullVersion.split("\n")[0];
            }
            return fullVersion;
        } catch (Exception e) {
            return "SQL Server 2022";
        }
    }
}
