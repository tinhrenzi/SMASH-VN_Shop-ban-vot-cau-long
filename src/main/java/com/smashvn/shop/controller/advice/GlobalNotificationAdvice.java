package com.smashvn.shop.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smashvn.shop.repository.ThongBaoRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalNotificationAdvice {

    private final ThongBaoRepository thongBaoRepository;

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
}
