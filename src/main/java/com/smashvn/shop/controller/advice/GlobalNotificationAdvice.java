package com.smashvn.shop.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.OrderStatus;
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
    private final KhachHangRepository khachHangRepository;
    private final SanPhamYeuThichRepository wishlistRepository;
    private final HoaDonRepository hoaDonRepository;

    @ModelAttribute("orderPlaced")
    public long getOrderPlacedCount(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return 0;
        }
        try {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            if (kh == null) return 0;
            List<HoaDon> orders = hoaDonRepository.findByKhachHang_Id(kh.getId());
            if (orders == null) return 0;
            long cancelled = orders.stream()
                    .filter(o -> OrderStatus.DA_HUY.getValue().equalsIgnoreCase(o.getTrangThaiDonHang()))
                    .count();
            return orders.size() - cancelled;
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("cancelOrders")
    public long getCancelOrdersCount(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return 0;
        }
        try {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            if (kh == null) return 0;
            List<HoaDon> orders = hoaDonRepository.findByKhachHang_Id(kh.getId());
            if (orders == null) return 0;
            return orders.stream()
                    .filter(o -> OrderStatus.DA_HUY.getValue().equalsIgnoreCase(o.getTrangThaiDonHang()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("wishlist")
    public long getWishlistCount(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return 0;
        }
        try {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            if (kh == null) return 0;
            return wishlistRepository.countByKhachHang_Id(kh.getId());
        } catch (Exception e) {
            return 0;
        }
    }

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
