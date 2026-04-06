package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.AdminBienTheService;

@Controller
@RequestMapping("/admin/san-pham/{idSanPham}/bien-the") // Link động chứa ID sản phẩm
@RequiredArgsConstructor
public class AdminBienTheController {

    private final SanPhamRepository sanPhamRepository;
    private final AdminBienTheService adminBienTheService;

    // 1. Hiển thị Trang Quản lý Biến thể (Gồm cả Form Thêm + Bảng Danh sách)
    @GetMapping
    public String hienThiTrangBienThe(@PathVariable("idSanPham") Integer idSanPham, Model model) {
        // Lấy thông tin sản phẩm gốc để hiển thị tiêu đề
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        model.addAttribute("sp", sp);
        
        // Lấy danh sách các biến thể của sản phẩm này
        model.addAttribute("danhSachBienThe", adminBienTheService.layDanhSachBienThe(idSanPham));
        
        return "admin/bienthe-list"; // Trả về file giao diện
    }

    // 2. Hứng dữ liệu khi Admin bấm "LƯU BIẾN THỂ MỚI"
    @PostMapping("/them")
    public String xuLyThemBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                  @RequestParam("giaBan") BigDecimal giaBan,
                                  @RequestParam("soLuongTon") Integer soLuongTon,
                                  @RequestParam("mauSac") String mauSac,
                                  @RequestParam("trongLuong") String trongLuong,
                                  @RequestParam("mucCang") String mucCang,
                                  @RequestParam("fileAnh") MultipartFile fileAnh) {
        try {
            adminBienTheService.themBienThe(idSanPham, giaBan, soLuongTon, mauSac, trongLuong, mucCang, fileAnh);
            return "redirect:/admin/san-pham/" + idSanPham + "/bien-the?thanhcong";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/san-pham/" + idSanPham + "/bien-the?loi";
        }
    }

    // 3. Xóa một biến thể (Xóa cứng)
    @GetMapping("/xoa/{idBienThe}")
    public String xuLyXoaBienThe(@PathVariable("idSanPham") Integer idSanPham, 
                                 @PathVariable("idBienThe") Integer idBienThe) {
        try {
            adminBienTheService.xoaBienThe(idBienThe);
            return "redirect:/admin/san-pham/" + idSanPham + "/bien-the?xoaThanhCong";
        } catch (Exception e) {
            // Lỗi xảy ra nếu biến thể này đang nằm trong Giỏ hàng hoặc Hóa đơn của khách
            return "redirect:/admin/san-pham/" + idSanPham + "/bien-the?loiXoa";
        }
    }
    
 // 4. Hiển thị Form Sửa Biến Thể
    @GetMapping("/sua/{idBienThe}")
    public String hienThiFormSuaBienThe(@PathVariable("idSanPham") Integer idSanPham, 
                                        @PathVariable("idBienThe") Integer idBienThe, Model model) {
        // Lấy SP gốc để hiện tiêu đề
        model.addAttribute("sp", sanPhamRepository.findById(idSanPham).orElseThrow());
        // Lấy biến thể cần sửa
        model.addAttribute("bt", adminBienTheService.layBienTheTheoId(idBienThe));
        
        return "admin/bienthe-edit";
    }

    // 5. Xử lý Cập nhật Biến Thể
    @PostMapping("/sua/{idBienThe}")
    public String xuLySuaBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                 @PathVariable("idBienThe") Integer idBienThe,
                                 @RequestParam("giaBan") BigDecimal giaBan,
                                 @RequestParam("soLuongTon") Integer soLuongTon,
                                 @RequestParam("mauSac") String mauSac,
                                 @RequestParam("trongLuong") String trongLuong,
                                 @RequestParam("mucCang") String mucCang,
                                 @RequestParam(value = "fileAnh", required = false) MultipartFile fileAnh) {
        try {
            adminBienTheService.capNhatBienThe(idBienThe, giaBan, soLuongTon, mauSac, trongLuong, mucCang, fileAnh);
            // Cập nhật xong thì quay lại trang danh sách biến thể của sản phẩm đó
            return "redirect:/admin/san-pham/" + idSanPham + "/bien-the?suaThanhCong";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/san-pham/" + idSanPham + "/bien-the/sua/" + idBienThe + "?loi";
        }
    }
}