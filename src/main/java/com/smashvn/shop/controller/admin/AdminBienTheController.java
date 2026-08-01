package com.smashvn.shop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.admin.AdminBienTheService;

@Controller
@RequestMapping("/admin/san-pham/{idSanPham}/bien-the") // Link động chứa ID sản phẩm
@RequiredArgsConstructor
public class AdminBienTheController {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final AdminBienTheService adminBienTheService;

    // 1. Hiển thị Trang Quản lý Biến thể (Gồm cả Form Thêm + Bảng Danh sách)
    @GetMapping
    public String hienThiTrangBienThe(@PathVariable("idSanPham") Integer idSanPham, Model model) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        model.addAttribute("sp", sp);
        model.addAttribute("danhSachBienThe", adminBienTheService.layDanhSachBienThe(idSanPham));
        populateCategoryIds(model);
        return "admin/bienthe-list";
    }

    // 2. Hứng dữ liệu khi Admin bấm "LƯU BIẾN THỂ MỚI"
    @PostMapping("/them")
    public String xuLyThemBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                  @RequestParam(value = "giaBan", required = false) BigDecimal giaBan,
                                  @RequestParam(value = "soLuongTon", required = false) Integer soLuongTon,
                                  @RequestParam(value = "mauSac", required = false) String mauSac,
                                  @RequestParam(value = "trongLuong", required = false) String trongLuong,
                                  @RequestParam(value = "kichThuoc", required = false) String kichThuoc,
                                  @RequestParam(value = "mucCang", required = false) String mucCang,
                                  @RequestParam(value = "fileAnh", required = false) MultipartFile fileAnh,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminBienTheService.themBienThe(idSanPham, giaBan, soLuongTon, mauSac, trongLuong, kichThuoc, mucCang, fileAnh);
            redirectAttributes.addFlashAttribute("success", "Thêm biến thể mới thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống khi thêm biến thể!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    // 3. Ẩn một biến thể khỏi khách hàng (xóa mềm)
    @GetMapping("/xoa/{idBienThe}")
    public String xuLyXoaBienThe(@PathVariable("idSanPham") Integer idSanPham, 
                                 @PathVariable("idBienThe") Integer idBienThe,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminBienTheService.xoaBienThe(idBienThe);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn biến thể khỏi khách hàng thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể ẩn biến thể này.");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    @GetMapping("/mo-ban-lai/{idBienThe}")
    public String xuLyMoBanLaiBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                      @PathVariable("idBienThe") Integer idBienThe,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminBienTheService.moBanLaiBienThe(idBienThe);
            redirectAttributes.addFlashAttribute("success", "Đã mở bán lại biến thể thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể mở bán lại biến thể này.");
        }
        return "redirect:/admin/san-pham/sua/" + idSanPham;
    }
    
    // 4. Hiển thị Form Sửa Biến Thể
    @GetMapping("/sua/{idBienThe}")
    public String hienThiFormSuaBienThe(@PathVariable("idSanPham") Integer idSanPham, 
                                        @PathVariable("idBienThe") Integer idBienThe, Model model) {
        model.addAttribute("sp", sanPhamRepository.findById(idSanPham).orElseThrow());
        model.addAttribute("bt", adminBienTheService.layBienTheTheoId(idBienThe));
        populateCategoryIds(model);
        return "admin/bienthe-edit";
    }

    // 5. Xử lý Cập nhật Biến Thể
    @PostMapping("/sua/{idBienThe}")
    public String xuLySuaBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                 @PathVariable("idBienThe") Integer idBienThe,
                                 @RequestParam(value = "giaBan", required = false) BigDecimal giaBan,
                                 @RequestParam(value = "soLuongTon", required = false) Integer soLuongTon,
                                 @RequestParam(value = "mauSac", required = false) String mauSac,
                                 @RequestParam(value = "trongLuong", required = false) String trongLuong,
                                 @RequestParam(value = "kichThuoc", required = false) String kichThuoc,
                                 @RequestParam(value = "mucCang", required = false) String mucCang,
                                 @RequestParam(value = "fileAnh", required = false) MultipartFile fileAnh,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminBienTheService.capNhatBienThe(idBienThe, giaBan, soLuongTon, mauSac, trongLuong, kichThuoc, mucCang, fileAnh);
            redirectAttributes.addFlashAttribute("success", "Cập nhật biến thể thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống khi cập nhật biến thể!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    private void populateCategoryIds(Model model) {
        java.util.Map<String, Integer> categoryIds = new java.util.HashMap<>();
        for (com.smashvn.shop.entity.DanhMuc dm : danhMucRepository.findAll()) {
            com.smashvn.shop.constant.CategoryType type = com.smashvn.shop.constant.CategoryType.fromIdOrName(dm, dm.getId());
            if (type != com.smashvn.shop.constant.CategoryType.OTHER) {
                categoryIds.put(type.name(), dm.getId());
            }
        }
        model.addAttribute("categoryIds", categoryIds);
    }
}
