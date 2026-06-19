package com.smashvn.shop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminKhuyenMaiService;
import com.smashvn.shop.service.order.OrderViewService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    private final PaymentTransactionRepository paymentTransactionRepository;

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
    public String hienThiDanhSachDonHang(
            @RequestParam(value = "orderCode", required = false) String orderCode,
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            Model model) {

        String role = (String) session.getAttribute("vaiTro");
        if (role == null) {
            return "redirect:/admin/dang-nhap";
        }

        java.util.List<com.smashvn.shop.entity.HoaDon> orders = hoaDonRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        model.addAttribute("danhSachDonHang", orders);

        java.util.Map<Integer, String> currentStatusLabels = new java.util.HashMap<>();
        java.util.Map<Integer, String> nextStatusLabels = new java.util.HashMap<>();
        for (com.smashvn.shop.entity.HoaDon hd : orders) {
            if (hd.getTrangThaiDonHang() != null) {
                currentStatusLabels.put(hd.getId(), orderViewService.getStatusLabel(hd.getTrangThaiDonHang()));
                String nextStatus = orderViewService.getNextStatus(hd.getTrangThaiDonHang());
                if (nextStatus != null) {
                    nextStatusLabels.put(hd.getId(), orderViewService.getStatusLabel(nextStatus));
                }
            }
        }
        model.addAttribute("currentStatusLabels", currentStatusLabels);
        model.addAttribute("nextStatusLabels", nextStatusLabels);

        // Parse dates
        java.time.LocalDateTime start = parseStartDate(startDate);
        java.time.LocalDateTime end = parseEndDate(endDate);

        // Filter transactions (clean, dynamic JPQL query)
        java.util.List<PaymentTransaction> list = paymentTransactionRepository.filterTransactions(
                cleanParam(orderCode),
                cleanParam(transactionId),
                cleanParam(status),
                start,
                end
        );

        boolean isManager = "QL".equals(role);

        model.addAttribute("danhSachGiaoDich", list);
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("transactionId", transactionId);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("isManager", isManager);

        String resolvedTab = activeTab;
        if (resolvedTab == null) {
            if ((orderCode != null && !orderCode.trim().isEmpty())
                    || (transactionId != null && !transactionId.trim().isEmpty())
                    || (status != null && !status.trim().isEmpty())
                    || (startDate != null && !startDate.trim().isEmpty())
                    || (endDate != null && !endDate.trim().isEmpty())) {
                resolvedTab = "transactions";
            } else {
                resolvedTab = "orders";
            }
        }
        model.addAttribute("activeTab", resolvedTab);

        return "admin/donhang-list";
    }

    private java.time.LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private java.time.LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr).atTime(java.time.LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanParam(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return val.trim();
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
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
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
            orderViewService.updateOrderStatusByAdmin(idHoaDon, trangThai, expectedStatus, actingTaiKhoanId, request.getRemoteAddr(), lyDoHuy);
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

    @GetMapping("/don-hang/approve-refund")
    public String approveRefundEmail(
            @RequestParam("id") Integer id,
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model) {
        try {
            Integer managerId = taiKhoanRepository.findByLaNhanVienTrueOrLaQuanLyTrue().stream()
                    .filter(tk -> Boolean.TRUE.equals(tk.getLaQuanLy()))
                    .map(TaiKhoan::getId)
                    .findFirst().orElse(1);

            orderViewService.approveRefund(id, token, managerId, request.getRemoteAddr());

            model.addAttribute("success", true);
            model.addAttribute("title", "Phê Duyệt Hoàn Tiền Thành Công");
            model.addAttribute("message", "Đơn hàng #" + id + " đã được phê duyệt hoàn tiền. Số tiền đã được chính thức trừ khỏi thống kê doanh thu.");
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Phê Duyệt Hoàn Tiền");
            model.addAttribute("message", e.getMessage());
        }
        return "admin/confirm-result";
    }

    @GetMapping("/don-hang/reject-refund")
    public String rejectRefundEmail(
            @RequestParam("id") Integer id,
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model) {
        try {
            Integer managerId = taiKhoanRepository.findByLaNhanVienTrueOrLaQuanLyTrue().stream()
                    .filter(tk -> Boolean.TRUE.equals(tk.getLaQuanLy()))
                    .map(TaiKhoan::getId)
                    .findFirst().orElse(1);

            orderViewService.rejectRefund(id, token, managerId, request.getRemoteAddr());

            model.addAttribute("success", true);
            model.addAttribute("title", "Từ Chối Hoàn Tiền Thành Công");
            model.addAttribute("message", "Yêu cầu hoàn tiền cho đơn hàng #" + id + " đã bị từ chối. Trạng thái thanh toán được giữ nguyên.");
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Thao Tác");
            model.addAttribute("message", e.getMessage());
        }
        return "admin/confirm-result";
    }

    @PostMapping("/don-hang/approve-refund-ui")
    public String approveRefundUi(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }
        TaiKhoan tk = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (tk == null || !Boolean.TRUE.equals(tk.getLaQuanLy())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này. Chỉ Quản lý mới có thể phê duyệt hoàn tiền.");
            return "redirect:/admin/don-hang";
        }

        try {
            com.smashvn.shop.entity.HoaDon hd = hoaDonRepository.findById(idHoaDon)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
            String response = hd.getGatewayResponse();
            String token = "";
            if (response != null && response.contains("REFUND_TOKEN:")) {
                int start = response.indexOf("REFUND_TOKEN:") + 13;
                int end = response.indexOf(";", start);
                if (end == -1) {
                    end = response.length();
                }
                token = response.substring(start, end);
            }

            orderViewService.approveRefund(idHoaDon, token, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Đã phê duyệt hoàn tiền thành công cho đơn hàng #" + idHoaDon);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi phê duyệt hoàn tiền: " + e.getMessage());
        }

        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/reject-refund-ui")
    public String rejectRefundUi(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }
        TaiKhoan tk = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (tk == null || !Boolean.TRUE.equals(tk.getLaQuanLy())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này. Chỉ Quản lý mới có thể từ chối hoàn tiền.");
            return "redirect:/admin/don-hang";
        }

        try {
            com.smashvn.shop.entity.HoaDon hd = hoaDonRepository.findById(idHoaDon)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
            String response = hd.getGatewayResponse();
            String token = "";
            if (response != null && response.contains("REFUND_TOKEN:")) {
                int start = response.indexOf("REFUND_TOKEN:") + 13;
                int end = response.indexOf(";", start);
                if (end == -1) {
                    end = response.length();
                }
                token = response.substring(start, end);
            }

            orderViewService.rejectRefund(idHoaDon, token, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Đã từ chối hoàn tiền cho đơn hàng #" + idHoaDon);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi từ chối hoàn tiền: " + e.getMessage());
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
            map.put("soTienGiamVoucher", hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : java.math.BigDecimal.ZERO);
            map.put("maVoucherApDung", hd.getMaVoucherApDung() != null ? hd.getMaVoucherApDung() : "");
            map.put("tenVoucherApDung", hd.getTenVoucherApDung() != null ? hd.getTenVoucherApDung() : "");
            map.put("moTaVoucherSnapshot", hd.getMoTaVoucherSnapshot() != null ? hd.getMoTaVoucherSnapshot() : "");
            map.put("trangThai", orderViewService.getStatusLabel(hd.getTrangThaiDonHang()));
            map.put("trangThaiRaw", hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang() : "");

            // Phương thức thanh toán — null-safe
            String tenPhuongThuc = "N/A";
            if (hd.getPhuongThucThanhToan() != null && hd.getPhuongThucThanhToan().getTenPhuongThuc() != null) {
                tenPhuongThuc = hd.getPhuongThucThanhToan().getTenPhuongThuc();
            }
            map.put("paymentMethod", tenPhuongThuc);
            map.put("paymentStatus", hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "N/A");
            map.put("maGiaoDich", hd.getMaGiaoDich() != null ? hd.getMaGiaoDich() : "");
            map.put("nguoiXacNhan", hd.getNguoiXacNhanThanhToan() != null ? hd.getNguoiXacNhanThanhToan() : "Nhân viên hệ thống");
            String formattedXacNhanAt = "";
            if (hd.getThoiGianXacNhan() != null) {
                formattedXacNhanAt = hd.getThoiGianXacNhan().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            }
            map.put("thoiGianXacNhan", formattedXacNhanAt);

            // Additional payment transaction fields
            String formattedPaidAt = "";
            if (hd.getPaidAt() != null) {
                formattedPaidAt = hd.getPaidAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            }
            map.put("thoiGianThanhToan", formattedPaidAt);
            map.put("transactionId", hd.getTransactionId() != null ? hd.getTransactionId() : "");
            map.put("gatewayResponse", hd.getGatewayResponse() != null ? hd.getGatewayResponse() : "");
            map.put("ghiChu", hd.getGhiChu() != null ? hd.getGhiChu() : "");

            // Return status details
            map.put("trangThaiHoanHang", hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "");
            map.put("trangThaiHoanHangLabel", hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().getLabel() : "");
            map.put("ngayXacNhanHoanHang", hd.getNgayXacNhanHoanHang() != null ? hd.getNgayXacNhanHoanHang().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            map.put("nhanVienXacNhan", hd.getNhanVienXacNhan() != null ? hd.getNhanVienXacNhan().getHoTenNv() : "");

            // Refund status details
            map.put("refundStatus", hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "");
            map.put("refundStatusLabel", hd.getRefundStatus() != null ? hd.getRefundStatus().getLabel() : "");
            map.put("refundTime", hd.getRefundTime() != null ? hd.getRefundTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            map.put("refundConfirmedBy", hd.getRefundConfirmedBy() != null ? hd.getRefundConfirmedBy().getHoTenNv() : "");

            // Linked transactions from PaymentTransaction
            java.util.List<com.smashvn.shop.entity.PaymentTransaction> txs = paymentTransactionRepository.findByOrder_Id(id);
            java.util.List<java.util.Map<String, Object>> txsList = new java.util.ArrayList<>();
            for (com.smashvn.shop.entity.PaymentTransaction tx : txs) {
                java.util.Map<String, Object> txMap = new java.util.LinkedHashMap<>();
                txMap.put("id", tx.getId());
                txMap.put("transactionId", tx.getTransactionId() != null ? tx.getTransactionId() : "");
                txMap.put("amount", tx.getAmount() != null ? tx.getAmount() : java.math.BigDecimal.ZERO);
                txMap.put("gateway", tx.getGateway() != null ? tx.getGateway() : "");
                txMap.put("status", tx.getStatus() != null ? tx.getStatus() : "");
                String txDate = "";
                if (tx.getCreatedAt() != null) {
                    txDate = tx.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                }
                txMap.put("createdAt", txDate);
                txsList.add(txMap);
            }
            map.put("transactions", txsList);

            // Danh sách sản phẩm — null-safe
            java.util.List<com.smashvn.shop.entity.HoaDonChiTiet> items
                    = hoaDonChiTietRepository.findByHoaDon_Id(id);
            java.util.List<java.util.Map<String, Object>> itemsList = new java.util.ArrayList<>();
            for (com.smashvn.shop.entity.HoaDonChiTiet item : items) {
                java.util.Map<String, Object> itemMap = new java.util.LinkedHashMap<>();
                String tenSP = item.getTenSanPhamSnapshot();
                if (tenSP == null || tenSP.isBlank()) {
                    if (item.getSanPhamChiTiet() != null && item.getSanPhamChiTiet().getSanPham() != null) {
                        tenSP = item.getSanPhamChiTiet().getSanPham().getTenSanPham();
                    }
                }
                if (tenSP == null) tenSP = "";

                String thuocTinh = item.getThuocTinhSnapshot();
                if (thuocTinh == null || thuocTinh.isBlank()) {
                    if (item.getSanPhamChiTiet() != null) {
                        thuocTinh = "Màu sắc: " + (item.getSanPhamChiTiet().getMauSac() != null ? item.getSanPhamChiTiet().getMauSac() : "N/A");
                    }
                }
                if (thuocTinh == null) thuocTinh = "";

                java.math.BigDecimal giaNiemYet = item.getGiaNiemYet();
                if (giaNiemYet == null) {
                    if (item.getSanPhamChiTiet() != null) {
                        giaNiemYet = item.getSanPhamChiTiet().getGiaBan();
                    }
                }
                if (giaNiemYet == null) giaNiemYet = java.math.BigDecimal.ZERO;

                java.math.BigDecimal donGia = item.getDonGia() != null ? item.getDonGia() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal phanTramGiam = item.getPhanTramGiam() != null ? item.getPhanTramGiam() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal soTienGiamSanPham = item.getSoTienGiamSanPham() != null ? item.getSoTienGiamSanPham() : java.math.BigDecimal.ZERO;
                String tenDotGiamGia = item.getTenDotGiamGia() != null ? item.getTenDotGiamGia() : "";
                String skuSnapshot = item.getSkuSnapshot() != null ? item.getSkuSnapshot() : "";

                String hinhAnh = "";
                if (item.getSanPhamChiTiet() != null) {
                    hinhAnh = item.getSanPhamChiTiet().getHinhAnhSanPham() != null
                            ? item.getSanPhamChiTiet().getHinhAnhSanPham() : "";
                }

                itemMap.put("tenSanPham", tenSP);
                itemMap.put("thuocTinh", thuocTinh);
                itemMap.put("sku", skuSnapshot);
                itemMap.put("soLuong", item.getSoLuong());
                itemMap.put("giaNiemYet", giaNiemYet);
                itemMap.put("giaBan", donGia); // Keep 'giaBan' representing purchase price (donGia) for backward compatibility
                itemMap.put("phanTramGiam", phanTramGiam);
                itemMap.put("soTienGiamSanPham", soTienGiamSanPham);
                itemMap.put("tenDotGiamGia", tenDotGiamGia);
                itemMap.put("hinhAnh", hinhAnh);

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

    @PostMapping("/don-hang/update-return-status")
    public String updateReturnStatus(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam("trangThaiHoanHang") String trangThaiHoanHang,
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
            orderViewService.updateReturnStatusByAdmin(idHoaDon, trangThaiHoanHang, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật trạng thái hoàn hàng thành công!");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: Bạn không có quyền thực hiện chức năng này.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/don-hang";
    }

    /**
     * Endpoint thủ công để admin sửa trạng thái thanh toán khi gặp lỗi hệ thống.
     * Chỉ cập nhật trang_thai_thanh_toan, không đụng vào payment_status hay paid_at.
     */
    @PostMapping("/don-hang/update-payment-status")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> updatePaymentStatus(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam("trangThaiThanhToan") String trangThaiThanhToan,
            HttpSession session) {

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();

        Integer actingId = (Integer) session.getAttribute("idNguoiDung");
        if (actingId == null) {
            resp.put("success", false);
            resp.put("message", "Phiên đăng nhập đã hết hạn.");
            return org.springframework.http.ResponseEntity.status(401).body(resp);
        }
        TaiKhoan tk = taiKhoanRepository.findById(actingId).orElse(null);
        if (tk == null || !Boolean.TRUE.equals(tk.getLaQuanLy())) {
            resp.put("success", false);
            resp.put("message", "Chỉ quản lý mới có quyền thực hiện thao tác này.");
            return org.springframework.http.ResponseEntity.status(403).body(resp);
        }

        java.util.Set<String> allowedStatuses = java.util.Set.of(
                "CHO_THANH_TOAN", "DA_THANH_TOAN", "CHO_HOAN_TIEN", "REFUNDED", "THAT_BAI");
        if (!allowedStatuses.contains(trangThaiThanhToan)) {
            resp.put("success", false);
            resp.put("message", "Trạng thái không hợp lệ: " + trangThaiThanhToan);
            return org.springframework.http.ResponseEntity.badRequest().body(resp);
        }

        com.smashvn.shop.entity.HoaDon hd = hoaDonRepository.findById(idHoaDon).orElse(null);
        if (hd == null) {
            resp.put("success", false);
            resp.put("message", "Không tìm thấy đơn hàng #" + idHoaDon);
            return org.springframework.http.ResponseEntity.status(404).body(resp);
        }

        String oldStatus = hd.getTrangThaiThanhToan();
        hd.setTrangThaiThanhToan(trangThaiThanhToan);
        hoaDonRepository.save(hd);

        resp.put("success", true);
        resp.put("message", "Đã cập nhật trạng thái thanh toán từ [" + oldStatus + "] → [" + trangThaiThanhToan + "] cho đơn #" + idHoaDon);
        return org.springframework.http.ResponseEntity.ok(resp);
    }
}
