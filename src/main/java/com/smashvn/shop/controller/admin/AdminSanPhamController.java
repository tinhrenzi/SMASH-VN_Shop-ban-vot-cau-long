package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminSanPhamService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/san-pham")
@RequiredArgsConstructor
public class AdminSanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final AdminSanPhamService adminSanPhamService;

    // Các thuộc tính thuộc phân loại vợt cầu lông
    private final List<String> listMauSacConfig = List.of("Đỏ", "Xanh dương", "Đen", "Trắng", "Vàng", "Cam");
    private final List<String> listTrongLuongConfig = List.of("3U", "4U", "5U");
    private final List<String> listMucCangConfig = List.of("10.5 kg", "11.0 kg", "11.5 kg", "12.0 kg", "12.5 kg");

    @GetMapping
    public String hienThiDanhSach(Model model) {
        model.addAttribute("danhSachSanPham", sanPhamRepository.findAll());
        return "admin/sanpham-list";
    }

    @GetMapping("/them")
    public String hienThiFormThem(Model model) {
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());

        // Đổ động thuộc tính ra Model phục vụ checkbox
        model.addAttribute("listMauSac", listMauSacConfig);
        model.addAttribute("listTrongLuong", listTrongLuongConfig);
        model.addAttribute("listMucCang", listMucCangConfig);

        return "admin/sanpham-add";
    }

    @PostMapping("/them")
    public String xuLyThemSanPham(
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("idDanhMuc") Integer idDanhMuc,
            @RequestParam("idThuongHieu") Integer idThuongHieu,
            @RequestParam("moTa") String moTa,
            @RequestParam(value = "giaBan", required = false) BigDecimal giaBan,
            @RequestParam(value = "soLuongTon", required = false) Integer soLuongTon,
            @RequestParam("fileAnh") MultipartFile fileAnh,
            @RequestParam(value = "mauSacs", required = false) List<String> mauSacs,
            @RequestParam(value = "trongLuongs", required = false) List<String> trongLuongs,
            @RequestParam(value = "mucCangs", required = false) List<String> mucCangs,
            org.springframework.web.multipart.MultipartHttpServletRequest request,
            HttpSession session,
            Model model) {
        try {
            java.util.Map<String, MultipartFile> variantImageMap = new java.util.HashMap<>();
            request.getFileMap().forEach((key, file) -> {
                if (key.startsWith("variantImages[")) {
                    String variantKey = key.substring(key.indexOf("[") + 1, key.lastIndexOf("]"));
                    variantImageMap.put(variantKey, file);
                }
            });

            java.util.Map<String, java.math.BigDecimal> variantPriceMap = new java.util.HashMap<>();
            java.util.Map<String, Integer> variantQuantityMap = new java.util.HashMap<>();

            request.getParameterMap().forEach((key, values) -> {
                if (key.startsWith("variantPrices[")) {
                    String variantKey = key.substring(key.indexOf("[") + 1, key.lastIndexOf("]"));
                    if (values != null && values.length > 0) {
                        String trimmed = values[0].trim();
                        if (!trimmed.isEmpty()) {
                            variantPriceMap.put(variantKey, new java.math.BigDecimal(trimmed));
                        }
                    }
                } else if (key.startsWith("variantQuantities[")) {
                    String variantKey = key.substring(key.indexOf("[") + 1, key.lastIndexOf("]"));
                    if (values != null && values.length > 0) {
                        String trimmed = values[0].trim();
                        if (!trimmed.isEmpty()) {
                            variantQuantityMap.put(variantKey, Integer.parseInt(trimmed));
                        }
                    }
                }
            });

            BigDecimal defaultGia = (giaBan != null) ? giaBan : new BigDecimal("3500000");
            Integer defaultKho = (soLuongTon != null) ? soLuongTon : 10;

            adminSanPhamService.themSanPhamVaBienThe(
                    tenSanPham, idDanhMuc, idThuongHieu, moTa,
                    defaultGia, defaultKho, fileAnh,
                    mauSacs, trongLuongs, mucCangs,
                    variantImageMap,
                    variantPriceMap,
                    variantQuantityMap,
                    (Integer) session.getAttribute("idNguoiDung"),
                    request.getRemoteAddr()
            );
            return "redirect:/admin/san-pham?thanhcong";
        } catch (Exception e) {
            // Khi lỗi, giữ lại thông tin nhập, ném lỗi ra màn hình
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("tenSanPham", tenSanPham);
            model.addAttribute("idDanhMuc", idDanhMuc);
            model.addAttribute("idThuongHieu", idThuongHieu);
            model.addAttribute("moTa", moTa);
            model.addAttribute("giaBan", giaBan);
            model.addAttribute("soLuongTon", soLuongTon);
            model.addAttribute("selectedMauSacs", mauSacs);
            model.addAttribute("selectedTrongLuongs", trongLuongs);
            model.addAttribute("selectedMucCangs", mucCangs);

            // Re-populate lists
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            model.addAttribute("listMauSac", listMauSacConfig);
            model.addAttribute("listTrongLuong", listTrongLuongConfig);
            model.addAttribute("listMucCang", listMucCangConfig);

            return "admin/sanpham-add";
        }
    }

    @GetMapping("/sua/{id}")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model) {
        SanPham sp = sanPhamRepository.findById(id).orElseThrow();
        model.addAttribute("sp", sp);
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
        return "admin/sanpham-edit";
    }

    @PostMapping("/sua/{id}")
    public String xuLySuaSanPham(@PathVariable("id") Integer idSanPham,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("idDanhMuc") Integer idDanhMuc,
            @RequestParam("idThuongHieu") Integer idThuongHieu,
            @RequestParam("moTa") String moTa,
            HttpSession session,
            HttpServletRequest request) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.capNhatSanPham(idSanPham, tenSanPham, idDanhMuc, idThuongHieu, moTa, idNguoiDung, request.getRemoteAddr());
            return "redirect:/admin/san-pham?suaThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham/sua/" + idSanPham + "?loi=LoiHeThong";
        }
    }

    @PostMapping("/xoa/{id}")
    public String xuLyXoaSanPham(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.xoaSanPham(id, idNguoiDung, request.getRemoteAddr());
            return "redirect:/admin/san-pham?xoaThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham?loiXoa";
        }
    }

    @PostMapping("/mo-ban-lai/{id}")
    public String xuLyMoBanLaiSanPham(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.moBanLaiSanPham(id, idNguoiDung, request.getRemoteAddr());
            return "redirect:/admin/san-pham?moBanLaiThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/san-pham?loiMoBanLai";
        }
    }
}
