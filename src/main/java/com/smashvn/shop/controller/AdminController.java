package com.smashvn.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AdminKhuyenMaiService;
import com.smashvn.shop.service.OrderViewService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final HoaDonRepository hoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final AdminKhuyenMaiService adminKhuyenMaiService;
    private final OrderViewService orderViewService;
    private final com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;

    @GetMapping("/all")
    public String hienThiDashboard(Model model) {
        java.util.List<TaiKhoan> nvAccounts = taiKhoanRepository.findByLaNhanVienTrueOrLaQuanLyTrue();
        java.util.List<TaiKhoan> khAccounts = taiKhoanRepository.findByLaKhachHangTrue();
        long employeeCount = nhanVienRepository.count();

        long countStaff = nvAccounts.stream().filter(tk -> Boolean.TRUE.equals(tk.getLaNhanVien())).count();
        long countManager = nvAccounts.stream().filter(tk -> Boolean.TRUE.equals(tk.getLaQuanLy())).count();

        model.addAttribute("danhSachTaiKhoanNhanVien", nvAccounts);
        model.addAttribute("danhSachTaiKhoanKhachHang", khAccounts);
        model.addAttribute("soLuongNhanVien", employeeCount);
        model.addAttribute("soLuongTaiKhoanNhanVien", nvAccounts.size());
        model.addAttribute("soLuongTaiKhoanNhanVienOnly", countStaff);
        model.addAttribute("soLuongTaiKhoanQuanLy", countManager);
        model.addAttribute("soLuongTaiKhoanKhachHang", khAccounts.size());

        model.addAttribute("danhSachSanPham", sanPhamRepository.findAll());
        model.addAttribute("danhSachChoKhoa", nhanVienRepository.findPendingLockEmployees());
        return "admin/admin-dashboard";
    }

    @GetMapping("/don-hang")
    public String hienThiDanhSachDonHang(Model model) {
        model.addAttribute("danhSachDonHang", hoaDonRepository.findAll());
        return "admin/donhang-list";
    }

    @GetMapping("/khach-hang")
    public String hienThiDanhSachKhachHang(Model model) {
        model.addAttribute("danhSachKhachHang", khachHangRepository.findByLaKhachHangTrue());
        return "admin/khachhang-list";
    }

    @GetMapping("/khuyen-mai")
    public String hienThiDanhSachKhuyenMai(Model model) {
        model.addAttribute("danhSachDotGiamGia", adminKhuyenMaiService.getAllDotGiamGia());
        model.addAttribute("danhSachPhieuGiamGia", adminKhuyenMaiService.getAllPhieuGiamGia());
        return "admin/khuyenmai-list";
    }

    @PostMapping("/don-hang/update-status")
    public String capNhatTrangThaiDonHang(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam("trangThai") String trangThai,
            @RequestParam("expectedStatus") String expectedStatus,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }
        
        try {
            orderViewService.updateOrderStatusByAdmin(idHoaDon, trangThai, expectedStatus, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật trạng thái đơn hàng #" + idHoaDon + " thành công!");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/next-status")
    public String moveOrderToNextStatus(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }
        
        TaiKhoan tk = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (tk == null || (!Boolean.TRUE.equals(tk.getLaQuanLy()) && !Boolean.TRUE.equals(tk.getLaNhanVien()))) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này.");
            return "redirect:/admin/don-hang";
        }
        
        try {
            orderViewService.moveOrderToNextStatus(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật trạng thái đơn hàng #" + idHoaDon + " thành công!");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/don-hang";
    }

    @GetMapping("/don-hang/detail-json")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> getOrderDetailJson(
            @RequestParam("id") Integer id,
            HttpSession session) {
        
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        
        TaiKhoan tk = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (tk == null || (!Boolean.TRUE.equals(tk.getLaQuanLy()) && !Boolean.TRUE.equals(tk.getLaNhanVien()))) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }
        
        java.util.Optional<com.smashvn.shop.entity.HoaDon> opt = hoaDonRepository.findById(id);
        if (opt.isEmpty()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        try {
            com.smashvn.shop.entity.HoaDon hd = opt.get();
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", hd.getId());
            map.put("maDonHang", hd.getMaDonHang() != null ? hd.getMaDonHang() : "#" + hd.getId());
            map.put("ngayTao", hd.getNgayTao() != null ? hd.getNgayTao().toString() : "");
            
            // Thông tin khách hàng — null-safe
            String tenKH = "";
            if (hd.getKhachHang() != null) {
                String ho = hd.getKhachHang().getHoKh() != null ? hd.getKhachHang().getHoKh() : "";
                String ten = hd.getKhachHang().getTenKh() != null ? hd.getKhachHang().getTenKh() : "";
                tenKH = (ho + " " + ten).trim();
            }
            map.put("khachHang", tenKH);
            map.put("sdt", hd.getSdtNhan() != null ? hd.getSdtNhan() : "");
            map.put("diaChi", hd.getDiaChiNhan() != null ? hd.getDiaChiNhan() : "");
            map.put("tongTien", hd.getTongTien() != null ? hd.getTongTien() : java.math.BigDecimal.ZERO);
            map.put("phiVanChuyen", hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : java.math.BigDecimal.ZERO);
            map.put("trangThai", orderViewService.getStatusLabel(hd.getTrangThaiDonHang()));
            map.put("trangThaiRaw", hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang() : "");
            
            // Phương thức thanh toán — null-safe
            String tenPhuongThuc = "N/A";
            if (hd.getPhuongThucThanhToan() != null && hd.getPhuongThucThanhToan().getTenPhuongThuc() != null) {
                tenPhuongThuc = hd.getPhuongThucThanhToan().getTenPhuongThuc();
            }
            map.put("paymentMethod", tenPhuongThuc);
            map.put("paymentStatus", hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "N/A");
            
            // Danh sách sản phẩm — null-safe
            java.util.List<com.smashvn.shop.entity.HoaDonChiTiet> items =
                    hoaDonChiTietRepository.findByHoaDon_Id(id);
            java.util.List<java.util.Map<String, Object>> itemsList = new java.util.ArrayList<>();
            for (com.smashvn.shop.entity.HoaDonChiTiet item : items) {
                java.util.Map<String, Object> itemMap = new java.util.LinkedHashMap<>();
                String tenSP = "";
                String mauSac = "";
                java.math.BigDecimal giaBan = java.math.BigDecimal.ZERO;
                if (item.getSanPhamChiTiet() != null) {
                    if (item.getSanPhamChiTiet().getSanPham() != null) {
                        tenSP = item.getSanPhamChiTiet().getSanPham().getTenSanPham() != null
                                ? item.getSanPhamChiTiet().getSanPham().getTenSanPham() : "";
                    }
                    mauSac = item.getSanPhamChiTiet().getMauSac() != null
                            ? item.getSanPhamChiTiet().getMauSac() : "";
                    giaBan = item.getSanPhamChiTiet().getGiaBan() != null
                            ? item.getSanPhamChiTiet().getGiaBan() : java.math.BigDecimal.ZERO;
                }
                itemMap.put("tenSanPham", tenSP);
                itemMap.put("mauSac", mauSac);
                itemMap.put("soLuong", item.getSoLuong());
                itemMap.put("giaBan", giaBan);
                itemsList.add(itemMap);
            }
            map.put("items", itemsList);
            
            return org.springframework.http.ResponseEntity.ok(map);
        } catch (Exception e) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("error", "Lỗi khi tải chi tiết đơn hàng: " + e.getMessage());
            return org.springframework.http.ResponseEntity.status(500).body(err);
        }
    }
}
