package com.smashvn.shop.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalNotificationAdvice {

    private final ThongBaoRepository thongBaoRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;

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

    @ModelAttribute("globalCategories")
    public List<DanhMuc> getGlobalCategories() {
        try {
            return danhMucRepository.findAll().stream()
                    .filter(d -> Boolean.TRUE.equals(d.getTrangThai()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @ModelAttribute("globalBrands")
    public List<ThuongHieu> getGlobalBrands() {
        try {
            return thuongHieuRepository.findAll().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getTrangThai()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
