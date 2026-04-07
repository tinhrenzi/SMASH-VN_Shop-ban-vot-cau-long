package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.KhachHangRepository;

@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private final KhachHangRepository khachHangRepository;

    // Lấy thông tin khách hàng từ ID tài khoản (lấy từ Session)
    public KhachHang layThongTinKhachHang(Integer idTaiKhoan) {
        return khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
    }

    // Cập nhật hồ sơ cá nhân
    @Transactional
    public void capNhatHoSo(Integer idTaiKhoan, String ho, String ten, String sdt) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh != null) {
            kh.setHoKh(ho);
            kh.setTenKh(ten);
            kh.setSoDienThoaiKh(sdt);
            khachHangRepository.save(kh);
        }
    }
}