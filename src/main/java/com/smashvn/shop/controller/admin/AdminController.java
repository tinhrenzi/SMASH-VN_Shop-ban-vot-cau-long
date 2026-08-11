package com.smashvn.shop.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
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
    private final com.smashvn.shop.repository.SoDiaChiRepository soDiaChiRepository;
    private final com.smashvn.shop.service.admin.AdminKhachHangService adminKhachHangService;
    private final com.smashvn.shop.service.common.FileStorageService fileStorageService;

    @GetMapping("/all")
    public String hienThiDashboard(Model model) {
        java.util.List<TaiKhoan> nvAccounts = taiKhoanRepository.findByVaiTroIn(java.util.List.of("NV", "QL"));
        java.util.List<TaiKhoan> khAccounts = taiKhoanRepository.findByVaiTro("KH");
        long employeeCount = nhanVienRepository.count();

        model.addAttribute("danhSachTaiKhoanNhanVien", nvAccounts);
        model.addAttribute("danhSachTaiKhoanKhachHang", khAccounts);
        model.addAttribute("soLuongNhanVien", employeeCount);
        model.addAttribute("soLuongTaiKhoanNhanVien", nvAccounts.size());
        model.addAttribute("soLuongTaiKhoanKhachHang", khAccounts.size());

        model.addAttribute("danhSachSanPham", sanPhamRepository.findAllByOrderByIdDesc());
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

        java.util.List<com.smashvn.shop.entity.HoaDon> onlineOrders = new java.util.ArrayList<>();
        java.util.List<com.smashvn.shop.entity.HoaDon> posOrders = new java.util.ArrayList<>();
        java.util.List<com.smashvn.shop.entity.HoaDon> returnOrders = new java.util.ArrayList<>();
        for (com.smashvn.shop.entity.HoaDon hd : orders) {
            com.smashvn.shop.entity.ReturnStatus resolvedReturn = orderViewService.resolveReturnStatus(hd.getId(), hd);
            if (resolvedReturn != null) {
                hd.setTrangThaiHoanHang(resolvedReturn);
                hd.setGhnReturnOrderCode(orderViewService.resolveGhnReturnOrderCode(hd.getId(), hd));
                returnOrders.add(hd);
            }
            if (hd.getMaDonHang() != null && hd.getMaDonHang().startsWith("HDSVN")) {
                posOrders.add(hd);
            } else {
                onlineOrders.add(hd);
            }
        }
        model.addAttribute("danhSachDonHangOnline", onlineOrders);
        model.addAttribute("danhSachDonHangPos", posOrders);
        model.addAttribute("danhSachDonHangReturn", returnOrders);

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
        model.addAttribute("orderViewService", orderViewService);

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

    private String friendlyErrorMessage(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                message += " " + cause.getMessage();
            }
            cause = cause.getCause();
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("uk_username") || lowerMessage.contains("username") || lowerMessage.contains("email")) {
            return "Email/Tên đăng nhập đã được sử dụng trong hệ thống.";
        }
        if (lowerMessage.contains("so_dien_thoai") || lowerMessage.contains("sodienthoai")) {
            return "Số điện thoại đã được sử dụng.";
        }
        if (lowerMessage.contains("duplicate") || lowerMessage.contains("unique") || lowerMessage.contains("constraint")) {
            return "Dữ liệu đã tồn tại hoặc không hợp lệ. Vui lòng kiểm tra lại.";
        }
        return message.isBlank() ? "Không thể thực hiện thao tác. Vui lòng kiểm tra lại." : ex.getMessage();
    }

    @GetMapping("/khach-hang")
    public String hienThiDanhSachKhachHang(Model model) {
        model.addAttribute("danhSachKhachHang", khachHangRepository.findByTaiKhoan_VaiTro("KH"));
        return "admin/khachhang-list";
    }

    @PostMapping("/khach-hang/them")
    public String xuLyThemKhachHang(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("hoTenKh") String hoTenKh,
            @RequestParam(value = "soDienThoaiKh", required = false) String soDienThoaiKh,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminKhachHangService.createKhachHang(email, matKhau, hoTenKh, soDienThoaiKh, actingTaiKhoanId, ipAddress);
            redirectAttributes.addFlashAttribute("success", "Tạo mới tài khoản khách hàng thành công!");
            return "redirect:/admin/khach-hang?themThanhCong";
        } catch (Exception e) {
            String errorMessage = friendlyErrorMessage(e);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            redirectAttributes.addFlashAttribute("loi", errorMessage);
            return "redirect:/admin/khach-hang";
        }
    }

    @PostMapping("/khach-hang/sua/{id}")
    public String xuLySuaKhachHang(
            @PathVariable("id") Integer id,
            @RequestParam("hoTenKh") String hoTenKh,
            @RequestParam(value = "soDienThoaiKh", required = false) String soDienThoaiKh,
            @RequestParam("trangThai") String trangThai,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminKhachHangService.updateKhachHang(id, hoTenKh, soDienThoaiKh, trangThai, actingTaiKhoanId, ipAddress);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin khách hàng thành công!");
            return "redirect:/admin/khach-hang?suaThanhCong";
        } catch (Exception e) {
            String errorMessage = friendlyErrorMessage(e);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            redirectAttributes.addFlashAttribute("loi", errorMessage);
            return "redirect:/admin/khach-hang";
        }
    }

    @GetMapping("/khach-hang/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getChiTietKhachHangApi(@PathVariable("id") Integer idKhachHang) {
        com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findById(idKhachHang).orElse(null);
        if (kh == null) {
            return ResponseEntity.notFound().build();
        }

        java.util.List<com.smashvn.shop.entity.HoaDon> hoaDons = hoaDonRepository.findByKhachHang_IdOrderByIdDesc(idKhachHang);

        long tongDonHoanThanh = hoaDons.stream()
                .filter(hd -> {
                    String st = hd.getTrangThaiDonHang();
                    return st != null && ("da_giao".equalsIgnoreCase(st) || "hoan_thanh".equalsIgnoreCase(st));
                })
                .count();

        java.math.BigDecimal tongChiTieu = hoaDons.stream()
                .filter(hd -> {
                    String st = hd.getTrangThaiDonHang();
                    return st != null && ("da_giao".equalsIgnoreCase(st) || "hoan_thanh".equalsIgnoreCase(st));
                })
                .map(hd -> hd.getTongTien() != null ? hd.getTongTien() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.util.List<com.smashvn.shop.entity.SoDiaChi> diaChis = soDiaChiRepository.findByKhachHang_Id(idKhachHang);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", kh.getId());
        response.put("maKhachHang", "KH" + kh.getId());
        response.put("hoTen", kh.getHoTenKh() != null ? kh.getHoTenKh() : "Khách Hàng");
        response.put("username", kh.getTaiKhoan() != null ? kh.getTaiKhoan().getUsername() : "Khách vãng lai / POS");
        response.put("soDienThoai", (kh.getSoDienThoaiKh() != null && !kh.getSoDienThoaiKh().isBlank()) ? kh.getSoDienThoaiKh() : "Chưa cập nhật");

        String trangThai = "Không có tài khoản";
        if (kh.getTaiKhoan() != null) {
            trangThai = "hoat_dong".equalsIgnoreCase(kh.getTaiKhoan().getTrangThai()) ? "Hoạt động" : "Ngừng hoạt động";
        }
        response.put("trangThaiTaiKhoan", trangThai);

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        response.put("ngayTaoFormatted", kh.getNgayTao() != null ? kh.getNgayTao().format(dtf) : "N/A");

        response.put("tongDonHoanThanh", tongDonHoanThanh);
        response.put("tongChiTieuRaw", tongChiTieu);
        response.put("tongChiTieuFormatted", String.format("%,d ₫", tongChiTieu.longValue()));
        response.put("tongSoDonHang", hoaDons.size());

        java.util.List<java.util.Map<String, Object>> listDiaChiMap = diaChis.stream().map(dc -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("hoTenNguoiNhan", dc.getHoVaTenNguoiNhan());
            item.put("sdtNguoiNhan", dc.getSdtNguoiNhan());
            StringBuilder fullAddr = new StringBuilder(dc.getDiaChiCuThe() != null ? dc.getDiaChiCuThe() : "");
            if (dc.getPhuongXa() != null && !dc.getPhuongXa().isBlank()) {
                fullAddr.append(", ").append(dc.getPhuongXa());
            }
            if (dc.getQuanHuyen() != null && !dc.getQuanHuyen().isBlank()) {
                fullAddr.append(", ").append(dc.getQuanHuyen());
            }
            if (dc.getTinhThanh() != null && !dc.getTinhThanh().isBlank()) {
                fullAddr.append(", ").append(dc.getTinhThanh());
            }
            item.put("diaChiFull", fullAddr.toString());
            item.put("laMacDinh", dc.isDiaChiMacDinh());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        response.put("danhSachDiaChi", listDiaChiMap);

        java.util.List<java.util.Map<String, Object>> listDonHangMap = hoaDons.stream().limit(10).map(hd -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", hd.getId());
            item.put("maDonHang", hd.getMaDonHang());
            item.put("ngayTaoFormatted", hd.getNgayTao() != null ? hd.getNgayTao().format(dtf) : "N/A");
            item.put("tongTienFormatted", hd.getTongTien() != null ? String.format("%,d ₫", hd.getTongTien().longValue()) : "0 ₫");
            item.put("trangThaiDonHang", hd.getTrangThaiDonHang());
            item.put("trangThaiThanhToan", hd.getTrangThaiThanhToan());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        response.put("danhSachDonHang", listDonHangMap);

        return ResponseEntity.ok(response);
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
        String role = (String) session.getAttribute("vaiTro");
        if (!"QL".equals(role) && !"NV".equals(role)) {
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
            map.put("donViVanChuyen", hd.getDonViVanChuyen() != null ? hd.getDonViVanChuyen().getTenDonVi() : "N/A");
            map.put("tongTien", hd.getTongTien() != null ? hd.getTongTien() : java.math.BigDecimal.ZERO);
            map.put("phiVanChuyen", hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : java.math.BigDecimal.ZERO);
            map.put("soTienGiamVoucher", hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : java.math.BigDecimal.ZERO);

            String maVoucher = hd.getMaVoucherApDung();
            String tenVoucher = hd.getTenVoucherApDung();
            String moTaVoucher = hd.getMoTaVoucherSnapshot();

            if (maVoucher == null || maVoucher.isEmpty()) {
                if (hd.getPhieuGiamGia() != null) {
                    maVoucher = hd.getPhieuGiamGia().getMaPhieu();
                } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    maVoucher = "Voucher";
                } else {
                    maVoucher = "Không áp dụng voucher";
                }
            }
            if (tenVoucher == null || tenVoucher.isEmpty()) {
                if (hd.getPhieuGiamGia() != null) {
                    tenVoucher = hd.getPhieuGiamGia().getTenPhieu();
                } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    tenVoucher = "Voucher giảm giá";
                } else {
                    tenVoucher = "Không áp dụng voucher";
                }
            }
            if (moTaVoucher == null || moTaVoucher.isEmpty()) {
                if (hd.getPhieuGiamGia() != null) {
                    var pg = hd.getPhieuGiamGia();
                    String limitDesc = pg.getGiaTriGiamToiDa() != null ? " (Giảm tối đa " + pg.getGiaTriGiamToiDa() + "đ)" : "";
                    moTaVoucher = "Giảm " + pg.getGiaTri() + ("%".equals(pg.getDonVi()) ? "%" : "đ") + limitDesc + " cho đơn hàng từ " + pg.getGiaTriDonHangToiThieu() + "đ";
                } else {
                    moTaVoucher = "Không áp dụng voucher";
                }
            }

            map.put("maVoucherApDung", maVoucher);
            map.put("tenVoucherApDung", tenVoucher);
            map.put("moTaVoucherSnapshot", moTaVoucher);
            map.put("trangThai", orderViewService.getStatusLabel(hd.getTrangThaiDonHang()));
            map.put("trangThaiRaw", hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang() : "");
            String returnCode = orderViewService.resolveGhnReturnOrderCode(hd.getId(), hd);
            com.smashvn.shop.entity.ReturnStatus resolvedReturnStatus = orderViewService.resolveReturnStatus(hd.getId(), hd);

            map.put("ghnOrderCode", hd.getGhnOrderCode() != null ? hd.getGhnOrderCode() : "");
            map.put("ghnReturnOrderCode", returnCode != null ? returnCode : "");

            // Phương thức thanh toán — null-safe
            String tenPhuongThuc = "N/A";
            if (hd.getPhuongThucThanhToan() != null && hd.getPhuongThucThanhToan().getTenPhuongThuc() != null) {
                tenPhuongThuc = hd.getPhuongThucThanhToan().getTenPhuongThuc();
            }
            map.put("paymentMethod", tenPhuongThuc);

            var paymentInfo = orderViewService.getPaymentStatusInfo(hd.getTrangThaiThanhToan());
            map.put("paymentStatus", hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "N/A");
            map.put("paymentStatusCode", paymentInfo.code());
            map.put("paymentStatusLabel", paymentInfo.label());
            map.put("paymentStatusBadgeClass", paymentInfo.badgeClass());

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
            map.put("trangThaiHoanHang", resolvedReturnStatus != null ? resolvedReturnStatus.name() : "");
            map.put("trangThaiHoanHangLabel", resolvedReturnStatus != null ? resolvedReturnStatus.getLabel() : "");
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
                if (tenSP == null) {
                    tenSP = "";
                }

                String thuocTinh = "";
                if (item.getThuocTinhSnapshot() != null && !item.getThuocTinhSnapshot().isBlank()) {
                    thuocTinh = item.getThuocTinhSnapshot().replace("Mức cảng:", "Sức căng khuyến nghị:");
                } else if (item.getSanPhamChiTiet() != null) {
                    thuocTinh = item.getSanPhamChiTiet().getPhanLoaiHienThi();
                }

                java.math.BigDecimal giaNiemYet = item.getGiaGoc();
                if (giaNiemYet == null) {
                    if (item.getSanPhamChiTiet() != null) {
                        giaNiemYet = item.getSanPhamChiTiet().getGiaBan();
                    }
                }
                if (giaNiemYet == null) {
                    giaNiemYet = java.math.BigDecimal.ZERO;
                }

                java.math.BigDecimal donGia = item.getDonGia() != null ? item.getDonGia() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal soTienGiamSanPham = giaNiemYet.subtract(donGia);
                if (soTienGiamSanPham.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    soTienGiamSanPham = java.math.BigDecimal.ZERO;
                }
                java.math.BigDecimal phanTramGiam = java.math.BigDecimal.ZERO;
                if (giaNiemYet.compareTo(java.math.BigDecimal.ZERO) > 0 && soTienGiamSanPham.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    try {
                        phanTramGiam = soTienGiamSanPham.multiply(java.math.BigDecimal.valueOf(100))
                                .divide(giaNiemYet, 0, java.math.RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        phanTramGiam = java.math.BigDecimal.ZERO;
                    }
                }
                String tenDotGiamGia = item.getTenDotGiamGiaSnapshot() != null ? item.getTenDotGiamGiaSnapshot() : "";
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

    @PostMapping("/don-hang/approve-return")
    public String approveReturn(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            String ghnCode = orderViewService.duyetYeuCauTraHangVaTaoDonGhn(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Đã duyệt yêu cầu trả hàng và tạo vận đơn GHN thu hồi thành công: " + ghnCode);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/reject-return")
    public String rejectReturn(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "lyDoTuChoi", required = false) String lyDoTuChoi,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.tuChoiYeuCauTraHang(idHoaDon, lyDoTuChoi, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Đã từ chối yêu cầu trả hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/cancel-return-pickup")
    public String cancelReturnPickup(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.huyDonThuHoiGhn(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Đã hủy vận đơn thu hồi GHN thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/confirm-restock")
    public String confirmRestock(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "ketQua", required = false, defaultValue = "BAN_LAI") String ketQua,
            @RequestParam(value = "lyDoTuChoi", required = false) String lyDoTuChoi,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.xacNhanKiemKhoVaNhapKho(idHoaDon, ketQua, lyDoTuChoi, actingTaiKhoanId, request.getRemoteAddr());
            if ("TU_CHOI".equalsIgnoreCase(ketQua)) {
                redirectAttributes.addFlashAttribute("successMsg", "Đã từ chối nhận hàng hoàn và tạo vận đơn gửi trả lại cho khách hàng thành công!");
            } else if ("HANG_LOI".equalsIgnoreCase(ketQua)) {
                redirectAttributes.addFlashAttribute("successMsg", "Đã kiểm hàng thành công và chuyển sản phẩm vào kho hàng lỗi!");
            } else {
                redirectAttributes.addFlashAttribute("successMsg", "Đã kiểm hàng thành công và hoàn lại tồn kho bán!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/confirm-refund")
    public String confirmRefund(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "phuongThucHoanTien", required = false) String phuongThucHoanTien,
            @RequestParam(value = "soTienHoan", required = false) java.math.BigDecimal soTienHoan,
            @RequestParam(value = "maGiaoDichHoanTien", required = false) String maGiaoDichHoanTien,
            @RequestParam(value = "ghiChuHoanTien", required = false) String ghiChuHoanTien,
            @RequestParam(value = "fileChungTu", required = false) org.springframework.web.multipart.MultipartFile fileChungTu,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }

        java.util.List<String> savedNames = null;
        try {
            String anhChungTuUrl = null;
            if (fileChungTu != null && !fileChungTu.isEmpty()) {
                savedNames = fileStorageService.saveImages(java.util.List.of(fileChungTu), "refunds");
                if (savedNames != null && !savedNames.isEmpty()) {
                    anhChungTuUrl = "/uploads/refunds/" + savedNames.get(0);
                }
            }

            orderViewService.xacNhanHoanTienChoKhach(
                idHoaDon,
                phuongThucHoanTien,
                soTienHoan,
                maGiaoDichHoanTien,
                ghiChuHoanTien,
                anhChungTuUrl,
                actingTaiKhoanId,
                request.getRemoteAddr()
            );
            redirectAttributes.addFlashAttribute("successMsg", "Đã xác nhận hoàn tiền thành công cho khách hàng!");
        } catch (Exception e) {
            if (savedNames != null && !savedNames.isEmpty()) {
                try {
                    fileStorageService.deleteFiles(savedNames, "refunds");
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup refund proof image: {}", cleanupEx.getMessage());
                }
            }
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi hoàn tiền: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    /**
     * Endpoint Admin xác nhận phân bổ kho và tạo vận đơn giao sản phẩm đổi mới cho khách hàng (Phase 6)
     */
    @PostMapping("/don-hang/confirm-exchange-shipment")
    public String confirmExchangeShipment(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.xacNhanGiaoHangDoiMoiChoKhach(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMsg", "Đã phân bổ kho và khởi tạo vận đơn giao hàng đổi thành công!");
        } catch (Exception e) {
            log.error("Lỗi chuẩn bị/giao hàng đổi cho đơn #{}: {}", idHoaDon, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xử lý giao hàng đổi: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }
}
