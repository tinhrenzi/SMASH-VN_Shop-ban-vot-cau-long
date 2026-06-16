package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import java.util.List;

@SpringBootTest
public class DebugOrderTest {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Test
    public void debugOrder146() {
        System.out.println("=== DEBUGGING ORDER ===");
        hoaDonRepository.findById(146).ifPresentOrElse(hd -> {
            System.out.println("Order ID: " + hd.getId());
            System.out.println("Ma Don Hang: " + hd.getMaDonHang());
            System.out.println("Tong Tien: " + hd.getTongTien());
            System.out.println("Phi Van Chuyen: " + hd.getPhiVanChuyen());
            System.out.println("Voucher: " + (hd.getPhieuGiamGia() != null ? hd.getPhieuGiamGia().getMaPhieu() + " (Gia tri: " + hd.getPhieuGiamGia().getGiaTri() + " " + hd.getPhieuGiamGia().getDonVi() + ")" : "None"));
            
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
            for (HoaDonChiTiet item : items) {
                System.out.println(" - Item: " + item.getSanPhamChiTiet().getSanPham().getTenSanPham());
                System.out.println("   So Luong: " + item.getSoLuong());
                System.out.println("   Don Gia (HoaDonChiTiet): " + item.getDonGia());
                System.out.println("   Gia Ban (SanPhamChiTiet): " + item.getSanPhamChiTiet().getGiaBan());
            }
        }, () -> {
            System.out.println("Order 146 not found. Listing last 5 orders:");
            hoaDonRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .forEach(hd -> {
                    System.out.println("Order ID: " + hd.getId() + " | Ma: " + hd.getMaDonHang() + " | Tong Tien: " + hd.getTongTien() + " | Voucher: " + (hd.getPhieuGiamGia() != null ? hd.getPhieuGiamGia().getMaPhieu() : "None"));
                    List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
                    for (HoaDonChiTiet item : items) {
                        System.out.println("   - Item: " + item.getSanPhamChiTiet().getSanPham().getTenSanPham() + " | SL: " + item.getSoLuong() + " | DonGia: " + item.getDonGia() + " | GiaBan: " + item.getSanPhamChiTiet().getGiaBan());
                    }
                });
        });
        System.out.println("=======================");
    }
}
