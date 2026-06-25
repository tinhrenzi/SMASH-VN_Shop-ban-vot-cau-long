package com.smashvn.shop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminShippingService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/shipping-config")
@RequiredArgsConstructor
public class AdminShippingController {

    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AdminShippingService adminShippingService;

    @GetMapping
    public String viewConfig(Model model, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return "redirect:/admin/dang-nhap";
        }

        TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        boolean isManager = tk != null && Boolean.TRUE.equals(tk.getLaQuanLy());

        List<DonViVanChuyen> list = adminShippingService.getAllCarriers();
        
        // Find GHN by matching name
        DonViVanChuyen ghn = list.stream()
                .filter(dv -> dv.getTenDonVi() != null && 
                        (dv.getTenDonVi().toUpperCase().contains("GIAO HÀNG NHANH") || 
                         dv.getTenDonVi().toUpperCase().contains("GHN")))
                .findFirst()
                .orElse(null);

        // Fallback if GHN is missing
        if (ghn == null) {
            ghn = new DonViVanChuyen();
            ghn.setTenDonVi("Giao Hàng Nhanh (GHN)");
            ghn.setHotline("1900 636677");
            ghn.setWebsite("https://ghn.vn");
            ghn = donViVanChuyenDAO.save(ghn);
            // Invalidate cache since we modified data directly
            // (Standard save from DAO is not evicted automatically, so we clear manually or re-retrieve)
            list = adminShippingService.getAllCarriers();
        }

        model.addAttribute("isManager", isManager);
        model.addAttribute("ghn", ghn);
        model.addAttribute("danhSachDonVi", list);
        return "admin/shipping-config";
    }

    @PostMapping("/save")
    public String saveConfig(
            @RequestParam("id") Integer id,
            @RequestParam("token") String token,
            @RequestParam("clientId") String clientId,
            @RequestParam("diaChiKho") String diaChiKho,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            adminShippingService.updateGhnConfig(id, token, clientId, diaChiKho, idNguoiDung, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật kết nối GHN thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/admin/shipping-config";
    }

    @PostMapping("/save-fees")
    public String saveFees(
            @RequestParam("id") Integer id,
            @RequestParam("phiLocal") BigDecimal phiLocal,
            @RequestParam("phiNationwide") BigDecimal phiNationwide,
            @RequestParam("version") Long version,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            adminShippingService.updateShippingFee(id, phiLocal, phiNationwide, version, idNguoiDung, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật phí vận chuyển thủ công thành công!");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này.");
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Dữ liệu đã được người khác cập nhật. Vui lòng tải lại trang.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/shipping-config";
    }
}
