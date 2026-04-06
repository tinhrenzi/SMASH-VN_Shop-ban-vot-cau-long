package com.smashvn.shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.service.GioHangService;

@Controller
@RequestMapping("/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHangService gioHangService;

    @PostMapping("/them")
    public String xuLyThemVaoGio(@RequestParam("idSanPhamChiTiet") Integer idSanPhamChiTiet,
                                 @RequestParam("soLuong") Integer soLuong,
                                 HttpSession session) {
        
        // 1. Kiểm tra xem người dùng đã đăng nhập chưa
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            // Nếu chưa đăng nhập, đá về trang Đăng nhập
            return "redirect:/user/dang-nhap";
        }

        try {
            // 2. Gọi Service để thêm vào giỏ
            gioHangService.themVaoGio(idNguoiDung, idSanPhamChiTiet, soLuong);
            
            // 3. Tạm thời chuyển hướng về trang chủ sau khi thêm thành công
            // (Sau này chúng ta sẽ chuyển hướng sang trang xem Giỏ Hàng)
            return "redirect:/"; 

        } catch (RuntimeException e) {
            // Nếu có lỗi (ví dụ: chưa có profile Khách hàng), có thể in ra console để debug
            System.err.println("LỖI THÊM GIỎ HÀNG: " + e.getMessage());
            return "redirect:/";
        }
    }
    @GetMapping
    public String hienThiGioHang(HttpSession session, Model model) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return "redirect:/user/dang-nhap"; // Chưa đăng nhập thì bắt đăng nhập
        }

        // Lấy danh sách sản phẩm trong giỏ
        List<GioHangChiTiet> danhSachChiTiet = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);

        // Tính toán tổng tiền của cả giỏ hàng (Giá bán * Số lượng)
        BigDecimal tongTien = BigDecimal.ZERO;
        for (GioHangChiTiet item : danhSachChiTiet) {
            BigDecimal gia = item.getSanPhamChiTiet().getGiaBan();
            BigDecimal soLuong = new BigDecimal(item.getSoLuong());
            tongTien = tongTien.add(gia.multiply(soLuong));
        }

        // Gửi dữ liệu sang View
        model.addAttribute("danhSachCart", danhSachChiTiet);
        model.addAttribute("tongTien", tongTien);

        return "cart"; // Gọi file cart.html
    }
    @GetMapping("/xoa/{id}")
    public String xoaSanPhamKhoiGio(@PathVariable("id") Integer idChiTiet, HttpSession session) {
        // Kiểm tra đăng nhập bảo mật
        if (session.getAttribute("idNguoiDung") == null) {
            return "redirect:/user/dang-nhap";
        }

        // Gọi Service thực hiện xóa
        gioHangService.xoaSanPhamKhoiGio(idChiTiet);
        
        // Xóa xong thì load lại (redirect) về chính trang Giỏ hàng để thấy kết quả
        return "redirect:/gio-hang";
    }
    @PostMapping("/cap-nhat")
    public String capNhatSoLuong(@RequestParam("idChiTiet") Integer idChiTiet,
                                 @RequestParam("soLuong") Integer soLuong,
                                 HttpSession session) {
        if (session.getAttribute("idNguoiDung") == null) return "redirect:/user/dang-nhap";
        
        gioHangService.capNhatSoLuong(idChiTiet, soLuong);
        return "redirect:/gio-hang"; // Load lại trang để thấy tổng tiền mới
    }
}
