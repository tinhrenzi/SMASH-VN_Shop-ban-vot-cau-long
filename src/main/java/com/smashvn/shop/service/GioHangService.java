package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private final KhachHangRepository khachHangRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TrangThaiGioHangRepository trangThaiGioHangRepository;

    public Map<String, Object> themVaoGio(Integer idTaiKhoan, Integer idSanPhamChiTiet, Integer soLuong) {
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            throw new RuntimeException("Tài khoản này chưa được cập nhật thông tin Khách Hàng!");
        }

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setKhachHang(khachHang);
            gioHang = gioHangRepository.save(gioHang);
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), idSanPhamChiTiet);
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // --- BÀI TOÁN VALIDATE TỒN KHO THỰC TẾ ---
        // 1. Xem trong giỏ của khách đã có bao nhiêu chiếc này rồi?
        int soLuongHienCo = (chiTiet != null) ? chiTiet.getSoLuong() : 0;
        
        // 2. Tính tổng số lượng khách sẽ có nếu thêm thành công
        int tongYeuCau = soLuongHienCo + soLuong;

        // 3. Khóa chặn: Nếu tổng yêu cầu vượt quá kho
        if (spct.getSoLuongTon() < tongYeuCau) {
            if (soLuongHienCo > 0) {
                throw new RuntimeException("Bạn đã có " + soLuongHienCo + " sản phẩm này trong giỏ. Không thể thêm vượt quá số lượng tồn kho (" + spct.getSoLuongTon() + ")!");
            } else {
                throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + spct.getSoLuongTon() + " sản phẩm.");
            }
        }

        // --- XỬ LÝ LƯU VÀO GIỎ ---
        if (chiTiet != null) {
            chiTiet.setSoLuong(tongYeuCau); // Đã có thì cộng dồn
        } else {
            chiTiet = new GioHangChiTiet(); // Chưa có thì tạo mới
            chiTiet.setGioHang(gioHang);
            chiTiet.setSanPhamChiTiet(spct);
            chiTiet.setSoLuong(soLuong);
            
            TrangThaiGioHang trangThai = trangThaiGioHangRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Lỗi cấu hình CSDL: Không tìm thấy ID trạng thái 1"));
            chiTiet.setTrangThai(trangThai);
        }

        gioHangChiTietRepository.save(chiTiet);

        // Đóng gói dữ liệu trả về cho Modal JS
        Map<String, Object> result = new HashMap<>();
        result.put("tenSanPham", spct.getSanPham().getTenSanPham());
        result.put("phanLoai", spct.getMauSac() + " | " + spct.getTrongLuong());
        result.put("giaBan", spct.getGiaBan());
        result.put("hinhAnh", spct.getHinhAnhSanPham());
        result.put("soLuongThem", soLuong);
        
        return result;
    }

    public List<GioHangChiTiet> layDanhSachSanPhamTrongGio(Integer idTaiKhoan) {
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) return new ArrayList<>();
        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) return new ArrayList<>();
        return gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
    }

    // SỬA ĐỔI 2: Gom toàn bộ logic tính tiền, gom JSON của Mini Cart vào Service
    public Map<String, Object> layDuLieuMiniCart(Integer idTaiKhoan) {
        List<GioHangChiTiet> danhSach = layDanhSachSanPhamTrongGio(idTaiKhoan);
        
        BigDecimal tongTien = BigDecimal.ZERO;
        int tongSoLuong = 0;
        List<Map<String, Object>> danhSachMini = new ArrayList<>();

        for (GioHangChiTiet item : danhSach) {
            SanPham sp = item.getSanPhamChiTiet().getSanPham();
            int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
            String trangThai = sp.getTrangThai();

            boolean hopLe = tonKho > 0 && (trangThai == null || trangThai.equals("dang_ban"));

            if (hopLe) {
                BigDecimal gia = item.getSanPhamChiTiet().getGiaBan();
                BigDecimal soLuong = new BigDecimal(item.getSoLuong());
                tongTien = tongTien.add(gia.multiply(soLuong));
                tongSoLuong += item.getSoLuong();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("idChiTiet", item.getId());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("hinhAnh", item.getSanPhamChiTiet().getHinhAnhSanPham());
            map.put("giaBan", item.getSanPhamChiTiet().getGiaBan());
            map.put("soLuong", item.getSoLuong());
            map.put("idSanPham", sp.getId());
            danhSachMini.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("trangThai", "ok");
        response.put("tongSoLuong", tongSoLuong);
        response.put("tongTien", tongTien);
        response.put("danhSach", danhSachMini);

        return response;
    }

    public void xoaSanPhamKhoiGio(Integer idGioHangChiTiet) {
        gioHangChiTietRepository.deleteById(idGioHangChiTiet);
    }

    public void capNhatSoLuong(Integer idGioHangChiTiet, Integer soLuongMoi) {
        GioHangChiTiet chiTiet = gioHangChiTietRepository.findById(idGioHangChiTiet)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));
        if (soLuongMoi > 0) {
            chiTiet.setSoLuong(soLuongMoi);
            gioHangChiTietRepository.save(chiTiet);
        } else {
            gioHangChiTietRepository.deleteById(idGioHangChiTiet);
        }
    }
}