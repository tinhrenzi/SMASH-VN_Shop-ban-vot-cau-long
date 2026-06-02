package com.smashvn.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.service.AdminPosService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/pos")
@RequiredArgsConstructor
public class AdminPosController {

    private final AdminPosService adminPosService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final com.smashvn.shop.repository.DanhMucRepository danhMucRepository;
    private final com.smashvn.shop.repository.ThuongHieuRepository thuongHieuRepository;

    // Hiển thị trang bán hàng tại quầy (POS)
    @GetMapping
    public String viewPos(Model model, HttpSession session) {
        model.addAttribute("phuongThucThanhToans", phuongThucThanhToanDAO.findAll());
        model.addAttribute("customers", adminPosService.searchCustomers(""));
        model.addAttribute("variants", adminPosService.searchActiveVariants("", -1, -1));
        model.addAttribute("categories", danhMucRepository.findAll());
        model.addAttribute("brands", thuongHieuRepository.findAll());
        return "admin/pos";
    }

    // API tìm kiếm sản phẩm chi tiết qua AJAX
    @GetMapping("/search-products")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "danhMucId", required = false) Integer danhMucId,
            @RequestParam(value = "thuongHieuId", required = false) Integer thuongHieuId) {
        
        List<Map<String, Object>> results = adminPosService.searchActiveVariants(query, danhMucId, thuongHieuId).stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("tenSanPham", v.getSanPham().getTenSanPham());
            map.put("mauSac", v.getMauSac());
            map.put("trongLuong", v.getTrongLuong());
            map.put("mucCang", v.getMucCang());
            map.put("giaBan", v.getGiaBan());
            map.put("soLuongTon", v.getSoLuongTon());
            map.put("hinhAnh", v.getHinhAnhSanPham() != null ? v.getHinhAnhSanPham() : "product9.jpg");
            return map;
        }).toList();
        return ResponseEntity.ok(results);
    }

    // API tìm kiếm khách hàng qua AJAX
    @GetMapping("/search-customers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchCustomers(@RequestParam(value = "q", required = false) String query) {
        List<Map<String, Object>> results = adminPosService.searchCustomers(query).stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("hoTen", c.getHoKh() + " " + c.getTenKh());
            map.put("sdt", c.getSoDienThoaiKh());
            map.put("email", c.getTaiKhoan().getEmail());
            return map;
        }).toList();
        return ResponseEntity.ok(results);
    }

    // API kiểm tra voucher qua AJAX
    @GetMapping("/check-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkVoucher(@RequestParam("code") String code, @RequestParam("total") BigDecimal total) {
        Map<String, Object> response = new HashMap<>();
        try {
            PhieuGiamGia voucher = adminPosService.checkVoucher(code, total);
            if (voucher != null) {
                response.put("success", true);
                response.put("maPhieu", voucher.getMaPhieu());
                response.put("giaTri", voucher.getGiaTri());
                response.put("donVi", voucher.getDonVi());
                response.put("giaTriDonHangToiThieu", voucher.getGiaTriDonHangToiThieu());
            } else {
                response.put("success", false);
                response.put("message", "Voucher không hợp lệ hoặc đã hết hạn.");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // DTO phục vụ request checkout từ UI
    public static class PosCheckoutRequest {
        public Integer idKhachHang;
        public String maVoucher;
        public List<AdminPosService.PosItem> items;
        public Integer idPhuongThucThanhToan;
        public String ghiChu;
    }

    // Xử lý thanh toán POS với Double Submit Protection
    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody PosCheckoutRequest req, HttpServletRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // 1. Kiểm tra tài khoản nhân viên đang thao tác
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            // Tìm dự phòng qua Spring Security Authentication
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String email = auth.getName();
                TaiKhoan tk = taiKhoanRepository.findByEmail(email);
                if (tk != null) {
                    idNguoiDung = tk.getId();
                    session.setAttribute("idNguoiDung", idNguoiDung);
                    session.setAttribute("vaiTro", tk.getVaiTro());
                }
            }
        }

        if (idNguoiDung == null) {
            response.put("success", false);
            response.put("message", "Phiên làm việc đã hết hạn hoặc chưa đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        // 2. Chống trùng lặp submit (Double Submit Protection) dùng Session-based Lock
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute("pos_processing"))) {
                response.put("success", false);
                response.put("message", "Hệ thống đang xử lý giao dịch trước đó. Vui lòng không click liên tục!");
                return ResponseEntity.badRequest().body(response);
            }
            session.setAttribute("pos_processing", true);
        }

        String ipAddress = request.getRemoteAddr();

        try {
            HoaDon hd = adminPosService.thanhToanPos(
                req.idKhachHang,
                req.maVoucher,
                req.items,
                req.idPhuongThucThanhToan,
                req.ghiChu,
                idNguoiDung,
                ipAddress
            );

            response.put("success", true);
            response.put("message", "Thanh toán thành công!");
            response.put("hoaDonId", hd.getId());
            response.put("maHoaDon", "HD-" + hd.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } finally {
            // Giải phóng khóa
            session.removeAttribute("pos_processing");
        }
    }

    // In hóa đơn nhiệt tại quầy
    @GetMapping("/print/{id}")
    public String printInvoice(@PathVariable("id") Integer id, Model model) {
        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + id));
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(id);

        BigDecimal tongTienTruocGiam = items.stream()
                .map(item -> item.getDonGia().multiply(new BigDecimal(item.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tienGiam = tongTienTruocGiam.subtract(hd.getTongTien());
        if (tienGiam.compareTo(BigDecimal.ZERO) < 0) {
            tienGiam = BigDecimal.ZERO;
        }

        model.addAttribute("hoaDon", hd);
        model.addAttribute("maHoaDon", "HD-" + hd.getId());
        model.addAttribute("items", items);
        model.addAttribute("tongTienTruocGiam", tongTienTruocGiam);
        model.addAttribute("tienGiam", tienGiam);
        return "admin/pos-print";
    }
}
