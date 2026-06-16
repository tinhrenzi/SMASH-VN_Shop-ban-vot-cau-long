package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.admin.AdminKhuyenMaiService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/khuyen-mai")
@RequiredArgsConstructor
public class AdminKhuyenMaiController {

    private final AdminKhuyenMaiService adminKhuyenMaiService;
    private final SanPhamRepository sanPhamRepository;

    // ==========================================
    // CAMPAIGN (ĐỢT GIẢM GIÁ) MAPPINGS
    // ==========================================

    @GetMapping("/dot-giam-gia/them")
    public String viewThemDotGiamGia(Model model) {
        model.addAttribute("sanPhams", sanPhamRepository.findAll());
        return "admin/dotgiamgia-add";
    }

    @PostMapping("/dot-giam-gia/them")
    public String processThemDotGiamGia(
            @RequestParam("tenChienDich") String tenChienDich,
            @RequestParam("ngayBatDau") String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("phanTramGiam") Integer phanTramGiam,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            @RequestParam(value = "productIds", required = false) List<Integer> productIds,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            adminKhuyenMaiService.createDotGiamGia(tenChienDich, start, end, phanTramGiam, loaiGiamGia, productIds, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?themChienDichThanhCong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("sanPhams", sanPhamRepository.findAll());
            model.addAttribute("tenChienDich", tenChienDich);
            model.addAttribute("ngayBatDau", ngayBatDauStr);
            model.addAttribute("ngayKetThuc", ngayKetThucStr);
            model.addAttribute("phanTramGiam", phanTramGiam);
            model.addAttribute("loaiGiamGia", loaiGiamGia);
            model.addAttribute("selectedProductIds", productIds);
            return "admin/dotgiamgia-add";
        }
    }

    @GetMapping("/dot-giam-gia/sua/{id}")
    public String viewSuaDotGiamGia(@PathVariable("id") Integer id, Model model) {
        try {
            DotGiamGia dgg = adminKhuyenMaiService.getDotGiamGiaById(id);
            model.addAttribute("campaign", dgg);
            model.addAttribute("sanPhams", sanPhamRepository.findAll());
            model.addAttribute("selectedProductIds", dgg.getSanPhams().stream().map(SanPham::getId).toList());
            return "admin/dotgiamgia-edit";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/dot-giam-gia/sua/{id}")
    public String processSuaDotGiamGia(
            @PathVariable("id") Integer id,
            @RequestParam("tenChienDich") String tenChienDich,
            @RequestParam("ngayBatDau") String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("phanTramGiam") Integer phanTramGiam,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            @RequestParam(value = "productIds", required = false) List<Integer> productIds,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            adminKhuyenMaiService.updateDotGiamGia(id, tenChienDich, start, end, phanTramGiam, loaiGiamGia, productIds, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?suaChienDichThanhCong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("campaign", adminKhuyenMaiService.getDotGiamGiaById(id));
            model.addAttribute("sanPhams", sanPhamRepository.findAll());
            model.addAttribute("selectedProductIds", productIds);
            return "admin/dotgiamgia-edit";
        }
    }

    @PostMapping("/dot-giam-gia/deactivate/{id}")
    public String processDeactivateDotGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deactivateDotGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?deactivateChienDichThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/dot-giam-gia/delete/{id}")
    public String processDeleteDotGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deleteDotGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?xoaChienDichThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ==========================================
    // VOUCHER (PHIẾU GIẢM GIÁ) MAPPINGS
    // ==========================================

    @GetMapping("/phieu-giam-gia/them")
    public String viewThemPhieuGiamGia(Model model) {
        return "admin/phieugiamgia-add";
    }

    @PostMapping("/phieu-giam-gia/them")
    public String processThemPhieuGiamGia(
            @RequestParam("maPhieu") String maPhieu,
            @RequestParam("giaTri") BigDecimal giaTri,
            @RequestParam("donVi") String donVi,
            @RequestParam("ngayBatDau") String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("soLuongConLai") Integer soLuongConLai,
            @RequestParam(value = "giaTriDonHangToiThieu", required = false) BigDecimal giaTriDonHangToiThieu,
            @RequestParam(value = "giaTriGiamToiDa", required = false) BigDecimal giaTriGiamToiDa,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            adminKhuyenMaiService.createPhieuGiamGia(maPhieu, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?themPhieuThanhCong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("maPhieu", maPhieu);
            model.addAttribute("giaTri", giaTri);
            model.addAttribute("donVi", donVi);
            model.addAttribute("ngayBatDau", ngayBatDauStr);
            model.addAttribute("ngayKetThuc", ngayKetThucStr);
            model.addAttribute("soLuongConLai", soLuongConLai);
            model.addAttribute("giaTriDonHangToiThieu", giaTriDonHangToiThieu);
            model.addAttribute("giaTriGiamToiDa", giaTriGiamToiDa);
            model.addAttribute("loaiGiamGia", loaiGiamGia);
            return "admin/phieugiamgia-add";
        }
    }

    @GetMapping("/phieu-giam-gia/sua/{id}")
    public String viewSuaPhieuGiamGia(@PathVariable("id") Integer id, Model model) {
        try {
            PhieuGiamGia pgg = adminKhuyenMaiService.getPhieuGiamGiaById(id);
            model.addAttribute("voucher", pgg);
            return "admin/phieugiamgia-edit";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/phieu-giam-gia/sua/{id}")
    public String processSuaPhieuGiamGia(
            @PathVariable("id") Integer id,
            @RequestParam("maPhieu") String maPhieu,
            @RequestParam("giaTri") BigDecimal giaTri,
            @RequestParam("donVi") String donVi,
            @RequestParam("ngayBatDau") String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("soLuongConLai") Integer soLuongConLai,
            @RequestParam(value = "giaTriDonHangToiThieu", required = false) BigDecimal giaTriDonHangToiThieu,
            @RequestParam(value = "giaTriGiamToiDa", required = false) BigDecimal giaTriGiamToiDa,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            adminKhuyenMaiService.updatePhieuGiamGia(id, maPhieu, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?suaPhieuThanhCong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("voucher", adminKhuyenMaiService.getPhieuGiamGiaById(id));
            return "admin/phieugiamgia-edit";
        }
    }

    @PostMapping("/phieu-giam-gia/deactivate/{id}")
    public String processDeactivatePhieuGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deactivatePhieuGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?deactivatePhieuThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/phieu-giam-gia/delete/{id}")
    public String processDeletePhieuGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deletePhieuGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?xoaPhieuThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
