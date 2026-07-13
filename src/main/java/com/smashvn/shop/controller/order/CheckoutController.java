package com.smashvn.shop.controller.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentMethod;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.order.GuestCheckoutService;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.service.user.UserAddressService;
import com.smashvn.shop.service.user.UserDangNhapService;
import com.smashvn.shop.util.VoucherCalculator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final GioHangService gioHangService;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final UserAddressService userAddressService;
    private final SepayConfig sepayConfig;
    private final com.smashvn.shop.repository.KhachHangRepository khachHangRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PricingService pricingService;
    private final GuestCartService guestCartService;
    private final GuestCheckoutService guestCheckoutService;
    private final UserDangNhapService userDangNhapService;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final SoDiaChiRepository soDiaChiRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private boolean isDangBan(String trangThai) {
        return trangThai == null || trangThai.isBlank() || "dang_ban".equals(trangThai);
    }

    private boolean isSanPhamChiTietDangBan(com.smashvn.shop.entity.SanPhamChiTiet spct) {
        return spct != null
                && spct.getSanPham() != null
                && isDangBan(spct.getSanPham().getTrangThai())
                && isDangBan(spct.getTrangThai());
    }

    @GetMapping({"/checkout", "/checkout.html"})
    public String viewCheckout(HttpSession session, Model model) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);

        List<GioHangChiTiet> danhSachChiTiet = new java.util.ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;

        if (!activeAccount) {
            List<com.smashvn.shop.service.order.GuestCartService.GuestCartItem> guestItems = guestCartService.getGuestCartItems(session);
            if (guestItems.isEmpty()) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng của bạn đang trống!", java.nio.charset.StandardCharsets.UTF_8);
            }
            for (com.smashvn.shop.service.order.GuestCartService.GuestCartItem item : guestItems) {
                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(item.getIdSanPhamChiTiet()).orElse(null);
                if (spct == null) {
                    continue;
                }

                GioHangChiTiet detail = new GioHangChiTiet();
                detail.setId(spct.getId());
                detail.setSanPhamChiTiet(spct);
                detail.setSoLuong(item.getSoLuong());

                SanPham sp = spct.getSanPham();
                int tonKho = spct.getSoLuongTon();

                if (!isSanPhamChiTietDangBan(spct)) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Phân loại sản phẩm '" + sp.getTenSanPham() + "' đã ngưng kinh doanh!", java.nio.charset.StandardCharsets.UTF_8);
                }
                if (tonKho <= 0) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' đã hết hàng!", java.nio.charset.StandardCharsets.UTF_8);
                }
                if (item.getSoLuong() > tonKho) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' không đủ số lượng tồn kho (Còn lại: " + tonKho + ")!", java.nio.charset.StandardCharsets.UTF_8);
                }

                BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(spct);
                tongTien = tongTien.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
                danhSachChiTiet.add(detail);
            }
        } else {
            // Clean up any old unpaid pending orders when loading the checkout page
            gioHangService.cleanPendingOrders(idNguoiDung);

            danhSachChiTiet = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
            if (danhSachChiTiet.isEmpty()) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng của bạn đang trống!", java.nio.charset.StandardCharsets.UTF_8);
            }

            for (GioHangChiTiet item : danhSachChiTiet) {
                if (item.getSanPhamChiTiet() == null || item.getSanPhamChiTiet().getSanPham() == null) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng chứa sản phẩm không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
                }
                SanPham sp = item.getSanPhamChiTiet().getSanPham();
                int tonKho = item.getSanPhamChiTiet().getSoLuongTon();

                if (item.getSoLuong() == null || item.getSoLuong() <= 0) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Số lượng sản phẩm trong giỏ hàng không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
                }
                if (!isSanPhamChiTietDangBan(item.getSanPhamChiTiet())) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Phân loại sản phẩm '" + sp.getTenSanPham() + "' đã ngưng kinh doanh!", java.nio.charset.StandardCharsets.UTF_8);
                }
                if (tonKho <= 0) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' đã hết hàng!", java.nio.charset.StandardCharsets.UTF_8);
                }
                if (item.getSoLuong() > tonKho) {
                    return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' không đủ số lượng tồn kho (Còn lại: " + tonKho + ")!", java.nio.charset.StandardCharsets.UTF_8);
                }
                BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(item.getSanPhamChiTiet());
                tongTien = tongTien.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
            }
        }

        List<DonViVanChuyen> listDvvc = donViVanChuyenDAO.findAll().stream()
                .filter(dv -> {
                    if (dv.getTenDonVi() == null) {
                        return false;
                    }
                    String tenLower = dv.getTenDonVi().toLowerCase();
                    return !tenLower.contains("quầy")
                            && !tenLower.contains("quay")
                            && !tenLower.contains("chỗ")
                            && !tenLower.contains("cho")
                            && !tenLower.contains("mua")
                            && !tenLower.contains("tại")
                            && !tenLower.contains("tai")
                            && !tenLower.contains("tiết kiệm")
                            && !tenLower.contains("tiet kiem")
                            && !tenLower.contains("tietkiem")
                            && !tenLower.contains("ghtk");
                })
                .collect(java.util.stream.Collectors.toList());

        List<SoDiaChi> listDiaChi = new java.util.ArrayList<>();
        boolean hasDefaultAddress = false;
        if (activeAccount) {
            com.smashvn.shop.entity.KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            Integer idKhachHang = (khachHang != null) ? khachHang.getId() : idNguoiDung;
            listDiaChi = userAddressService.layDanhSachDiaChi(idKhachHang);
            hasDefaultAddress = listDiaChi.stream().anyMatch(SoDiaChi::isDefaultShipping);
        }

        Map<Integer, Map<String, Object>> addressMap = new java.util.HashMap<>();
        for (SoDiaChi dc : listDiaChi) {
            Map<String, Object> details = new java.util.HashMap<>();
            details.put("hoTen", dc.getHoNguoiNhan() + " " + dc.getTenNguoiNhan());
            details.put("sdt", dc.getSdtNguoiNhan());
            details.put("diaChiCuThe", dc.getDiaChiCuThe());
            details.put("tinhThanh", dc.getTinhThanh());
            details.put("thanhPho", dc.getThanhPho());
            details.put("quocGia", dc.getQuocGia());
            details.put("latitude", dc.getLatitude());
            details.put("longitude", dc.getLongitude());
            details.put("wardName", dc.getWardName());
            String fullAddress = dc.getDiaChiCuThe();
            if (dc.getWardName() != null && !dc.getWardName().trim().isEmpty() && !fullAddress.contains(dc.getWardName())) {
                fullAddress += ", " + dc.getWardName().trim();
            }
            if (dc.getThanhPho() != null && !dc.getThanhPho().trim().isEmpty() && !dc.getThanhPho().equalsIgnoreCase(dc.getTinhThanh()) && !fullAddress.contains(dc.getThanhPho())) {
                fullAddress += ", " + dc.getThanhPho().trim();
            }
            if (dc.getTinhThanh() != null && !dc.getTinhThanh().trim().isEmpty() && !fullAddress.contains(dc.getTinhThanh())) {
                fullAddress += ", " + dc.getTinhThanh().trim();
            }
            if (dc.getQuocGia() != null && !dc.getQuocGia().trim().isEmpty() && !fullAddress.contains(dc.getQuocGia())) {
                fullAddress += ", " + dc.getQuocGia().trim();
            }
            details.put("diaChi", fullAddress);
            addressMap.put(dc.getId(), details);
        }

        String addressMapJson = "{}";
        try {
            addressMapJson = objectMapper.writeValueAsString(addressMap);
        } catch (Exception e) {
            log.error("Failed to serialize address map for checkout", e);
        }

        model.addAttribute("danhSachCart", danhSachChiTiet);
        model.addAttribute("tongTien", tongTien);
        model.addAttribute("listDvvc", listDvvc);
        model.addAttribute("listDiaChi", listDiaChi);
        model.addAttribute("hasDefaultAddress", hasDefaultAddress);
        model.addAttribute("addressMapJson", addressMapJson);
        model.addAttribute("sepayBankAccount", sepayConfig.getBankAccount());
        model.addAttribute("sepayBankName", sepayConfig.getBankName());
        model.addAttribute("sepayMemoPrefix", sepayConfig.getMemoPrefix());

        boolean isGuest = true;
        if (activeAccount) {
            isGuest = false;
        }
        model.addAttribute("isGuest", isGuest);

        return "checkout";
    }

    private boolean isActiveAccount(Integer idNguoiDung) {
        if (idNguoiDung == null) {
            return false;
        }
        TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        return tk != null
                && tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.ACTIVE
                && "hoat_dong".equalsIgnoreCase(tk.getTrangThai());
    }

    @PostMapping("/checkout/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCheckout(
            @RequestParam(value = "hoTenNhan", required = false) String hoTenNhan,
            @RequestParam(value = "sdtNhan", required = false) String sdtNhan,
            @RequestParam(value = "diaChiNhan", required = false) String diaChiNhan,
            @RequestParam(value = "idDonViVanChuyen", required = false) Integer idDonViVanChuyen,
            @RequestParam(value = "phuongThucThanhToan", required = false) String phuongThucThanhToan,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            @RequestParam(value = "ghnToDistrictId", required = false) Integer ghnToDistrictId,
            @RequestParam(value = "ghnToWardCode", required = false) String ghnToWardCode,
            @RequestParam(value = "ghnProvinceId", required = false) Integer ghnProvinceId,
            @RequestParam(value = "idDiaChiLuu", required = false) Integer idDiaChiLuu,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "tinhThanhText", required = false) String tinhThanhText,
            @RequestParam(value = "thanhPhoText", required = false) String thanhPhoText,
            @RequestParam(value = "phuongXaText", required = false) String phuongXaText,
            @RequestParam(value = "diaChiCuThe", required = false) String diaChiCuTheParam,
            @RequestParam(value = "saveAddress", required = false) Boolean saveAddress,
            HttpSession session,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean startedAsAnonymousGuest = (idNguoiDung == null);
        String emailStatus = "NEW";
        String activationToken = null;

        long startPipeline = System.currentTimeMillis();
        log.info("[GuestCheckout] Starting guest checkout pipeline execution.");

        // Sanitize text inputs
        hoTenNhan = sanitizeInput(hoTenNhan);
        sdtNhan = sanitizeInput(sdtNhan);
        diaChiNhan = sanitizeInput(diaChiNhan);
        ghiChu = sanitizeInput(ghiChu);
        tinhThanhText = sanitizeInput(tinhThanhText);
        thanhPhoText = sanitizeInput(thanhPhoText);
        phuongXaText = sanitizeInput(phuongXaText);
        diaChiCuTheParam = sanitizeInput(diaChiCuTheParam);

        // Validate inputs if user is not using a saved address
        if (idDiaChiLuu == null) {
            // Validate presence first
            if (hoTenNhan == null || hoTenNhan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Họ và tên người nhận không được để trống.");
                return ResponseEntity.ok(response);
            }
            if (sdtNhan == null || sdtNhan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Số điện thoại không được để trống.");
                return ResponseEntity.ok(response);
            }
            if (diaChiNhan == null || diaChiNhan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Địa chỉ nhận hàng không được để trống.");
                return ResponseEntity.ok(response);
            }

            // Validate length & format next
            if (hoTenNhan.trim().length() < 2 || hoTenNhan.trim().length() > 100) {
                response.put("trangThai", "loi");
                response.put("message", "Họ và tên người nhận phải từ 2 đến 100 ký tự.");
                return ResponseEntity.ok(response);
            }

            String normalizedPhone = com.smashvn.shop.util.PhoneUtils.normalize(sdtNhan);
            if (!com.smashvn.shop.util.PhoneUtils.isValid(normalizedPhone)) {
                response.put("trangThai", "loi");
                response.put("message", "Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
                return ResponseEntity.ok(response);
            }
        }

        if (idNguoiDung == null) {
            if (email == null || email.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Email không được để trống.");
                return ResponseEntity.ok(response);
            }

            emailStatus = guestCheckoutService.checkEmailStatus(email);
            if ("ACTIVE".equals(emailStatus)) {
                response.put("trangThai", "yeucaudangnhap");
                response.put("message", "Tài khoản thành viên đã tồn tại. Vui lòng đăng nhập mật khẩu để đặt hàng.");
                return ResponseEntity.ok(response);
            } else if ("GUEST_EXPIRED".equals(emailStatus)) {
                response.put("trangThai", "yeucaudoimatkhau");
                response.put("message", "Tài khoản vãng lai của bạn đã mua hàng quá 3 lần. Vui lòng đặt mật khẩu bảo mật để tiếp tục.");
                return ResponseEntity.ok(response);
            }

            long startAccount = System.currentTimeMillis();
            try {
                com.smashvn.shop.service.order.GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest(hoTenNhan, sdtNhan, email);
                TaiKhoan tk = regResult.getTaiKhoan();
                activationToken = regResult.getToken();
                com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());

                guestCartService.transferGuestCartToDb(session, kh.getId());

                request.changeSessionId();
                session = request.getSession(true);

                session.setAttribute("nguoiDungDangNhap", tk.getEmail());
                session.setAttribute("idNguoiDung", tk.getId());
                session.setAttribute("vaiTro", "KH");
                session.setAttribute("laKhachHang", true);
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                session.setAttribute("guestCheckoutEmail", tk.getEmail());

                idNguoiDung = tk.getId();
                long endAccount = System.currentTimeMillis();
                log.info("[GuestCheckout] Auto-register guest account: {}ms - SUCCESS", (endAccount - startAccount));
            } catch (Exception e) {
                long endAccount = System.currentTimeMillis();
                log.error("[GuestCheckout] Create inactive account: {}ms - FAILED. Exception: {}", (endAccount - startAccount), e.getMessage(), e);
                response.put("trangThai", "loi");
                if (isPhoneUniqueConstraintViolation(e)) {
                    response.put("message", "Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
                } else if (e instanceof org.springframework.dao.DataIntegrityViolationException || isDatabaseException(e)) {
                    response.put("message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
                } else {
                    response.put("message", e.getMessage());
                }
                return ResponseEntity.ok(response);
            }
        } else {
            log.info("[GuestCheckout] Inactive account exists or already registered: Skip autoRegisterGuest.");
        }

        com.smashvn.shop.entity.KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
        Integer idKhachHang = (khachHang != null) ? khachHang.getId() : idNguoiDung;

        if (idDiaChiLuu != null) {
            try {
                userAddressService.layDiaChiTheoId(idDiaChiLuu, idKhachHang);
            } catch (Exception e) {
                response.put("trangThai", "loi");
                response.put("message", "Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới.");
                return ResponseEntity.ok(response);
            }
        } else {
            // Mandatory check for GHN IDs on new address checkout
            if (ghnProvinceId == null || ghnToDistrictId == null || ghnToWardCode == null || ghnToWardCode.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã để sử dụng địa chỉ nhận hàng.");
                return ResponseEntity.ok(response);
            }
        }

        if (idDonViVanChuyen == null) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng chọn đơn vị vận chuyển.");
            return ResponseEntity.ok(response);
        }
        DonViVanChuyen dvvc = donViVanChuyenDAO.findById(idDonViVanChuyen).orElse(null);
        if (dvvc == null) {
            response.put("trangThai", "loi");
            response.put("message", "Đơn vị vận chuyển không tồn tại.");
            return ResponseEntity.ok(response);
        }
        String tenDv = dvvc.getTenDonVi() != null ? dvvc.getTenDonVi().toLowerCase() : "";
        if (tenDv.contains("quầy") || tenDv.contains("quay") || tenDv.contains("chỗ") || tenDv.contains("cho") || tenDv.contains("mua") || tenDv.contains("tại") || tenDv.contains("tai")) {
            response.put("trangThai", "loi");
            response.put("message", "Phương thức vận chuyển này không áp dụng cho mua hàng online.");
            return ResponseEntity.ok(response);
        }

        if (phuongThucThanhToan == null || phuongThucThanhToan.trim().isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng chọn phương thức thanh toán.");
            return ResponseEntity.ok(response);
        }

        Integer resolvedDiaChiLuuId = idDiaChiLuu;

        // Save address to SoDiaChi first if applicable (so we can pass the ID to createOrder)
        if (idDiaChiLuu == null && khachHang != null) {
            TaiKhoan currentTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
            boolean shouldSave = false;
            boolean setAsDefault = false;

            if (currentTk != null) {
                if (currentTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST) {
                    // Guest account: automatically save
                    shouldSave = true;
                    long existingCount = soDiaChiRepository.findByKhachHang_Id(khachHang.getId()).size();
                    if (existingCount == 0) {
                        setAsDefault = true;
                    }
                } else {
                    // Registered user: always save to store GHN district/ward mappings
                    shouldSave = true;
                    long existingCount = soDiaChiRepository.findByKhachHang_Id(khachHang.getId()).size();
                    if (existingCount == 0) {
                        setAsDefault = true;
                    }
                }
            }

            if (shouldSave) {
                String tinhThanhVal = tinhThanhText != null ? tinhThanhText.trim() : "";
                String thanhPhoVal = thanhPhoText != null ? thanhPhoText.trim() : "";
                String diaChiCuTheVal = diaChiCuTheParam != null ? diaChiCuTheParam.trim() : "";
                String sdtVal = sdtNhan != null ? sdtNhan.trim() : "";

                if (tinhThanhVal.isEmpty() || tinhThanhVal.contains("--")) {
                    if (diaChiNhan != null && !diaChiNhan.trim().isEmpty()) {
                        String[] parts = diaChiNhan.split(",");
                        if (parts.length >= 3) {
                            tinhThanhVal = parts[parts.length - 1].trim();
                            thanhPhoVal = parts[parts.length - 2].trim();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < parts.length - 2; i++) {
                                if (i > 0) {
                                    sb.append(", ");
                                }
                                sb.append(parts[i].trim());
                            }
                            diaChiCuTheVal = sb.toString();
                        } else if (parts.length == 2) {
                            tinhThanhVal = parts[1].trim();
                            thanhPhoVal = parts[1].trim();
                            diaChiCuTheVal = parts[0].trim();
                        } else {
                            tinhThanhVal = diaChiNhan.trim();
                            thanhPhoVal = diaChiNhan.trim();
                            diaChiCuTheVal = diaChiNhan.trim();
                        }
                    }
                }

                // Check duplicate
                SoDiaChi existingAddress = null;
                List<SoDiaChi> existingAddresses = soDiaChiRepository.findByKhachHang_Id(khachHang.getId());
                for (SoDiaChi dc : existingAddresses) {
                    String existingDiaChiCuThe = dc.getDiaChiCuThe() != null ? dc.getDiaChiCuThe().trim() : "";
                    String existingTinhThanh = dc.getTinhThanh() != null ? dc.getTinhThanh().trim() : "";
                    String existingThanhPho = dc.getThanhPho() != null ? dc.getThanhPho().trim() : "";
                    String existingSdt = dc.getSdtNguoiNhan() != null ? dc.getSdtNguoiNhan().trim() : "";

                    if (existingDiaChiCuThe.equalsIgnoreCase(diaChiCuTheVal)
                            && existingTinhThanh.equalsIgnoreCase(tinhThanhVal)
                            && existingThanhPho.equalsIgnoreCase(thanhPhoVal)
                            && existingSdt.equalsIgnoreCase(sdtVal)) {
                        existingAddress = dc;
                        break;
                    }
                }

                if (existingAddress == null) {
                    SoDiaChi newAddress = new SoDiaChi();
                    newAddress.setKhachHang(khachHang);

                    // Split name
                    String ho = "Khách";
                    String ten = "Vãng Lai";
                    if (hoTenNhan != null && !hoTenNhan.trim().isEmpty()) {
                        String name = hoTenNhan.trim();
                        int lastSpace = name.lastIndexOf(' ');
                        if (lastSpace >= 0) {
                            ho = name.substring(0, lastSpace).trim();
                            ten = name.substring(lastSpace + 1).trim();
                        } else {
                            ho = "";
                            ten = name;
                        }
                    }
                    newAddress.setHoNguoiNhan(ho);
                    newAddress.setTenNguoiNhan(ten);
                    newAddress.setSdtNguoiNhan(sdtVal);
                    newAddress.setDiaChiCuThe(diaChiCuTheVal);
                    newAddress.setTinhThanh(tinhThanhVal);
                    newAddress.setThanhPho(thanhPhoVal);
                    newAddress.setQuocGia("Việt Nam");
                    newAddress.setMaBuuDien("700000");
                    newAddress.setProvinceId(ghnProvinceId);
                    newAddress.setDistrictId(ghnToDistrictId);
                    newAddress.setWardCode(ghnToWardCode);
                    newAddress.setProvinceName(tinhThanhVal);
                    newAddress.setDistrictName(thanhPhoVal);
                    newAddress.setWardName(phuongXaText != null ? phuongXaText.trim() : null);

                    if (setAsDefault) {
                        newAddress.setDefaultShipping(true);
                        newAddress.setDefaultBilling(true);
                    } else {
                        newAddress.setDefaultShipping(false);
                        newAddress.setDefaultBilling(false);
                    }

                    newAddress = soDiaChiRepository.save(newAddress);
                    resolvedDiaChiLuuId = newAddress.getId();
                    log.info("[Checkout] Saved new address to SoDiaChi: id={}, isDefault={}", newAddress.getId(), setAsDefault);
                } else {
                    // Update existing address with GHN IDs if they are missing
                    if (existingAddress.getProvinceId() == null || existingAddress.getDistrictId() == null || existingAddress.getWardCode() == null) {
                        existingAddress.setProvinceId(ghnProvinceId);
                        existingAddress.setDistrictId(ghnToDistrictId);
                        existingAddress.setWardCode(ghnToWardCode);
                        soDiaChiRepository.save(existingAddress);
                        log.info("[Checkout] Updated existing address ID {} with GHN details.", existingAddress.getId());
                    }
                    resolvedDiaChiLuuId = existingAddress.getId();
                    log.info("[Checkout] Address already exists in SoDiaChi: id={}, skipped saving.", resolvedDiaChiLuuId);
                }
            }
        }

        try {
            long startOrder = System.currentTimeMillis();
            HoaDon hd = gioHangService.createOrder(idNguoiDung, hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyen, phuongThucThanhToan, ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId, resolvedDiaChiLuuId, voucherCode);
            long endOrder = System.currentTimeMillis();
            log.info("[GuestCheckout] Create order: {}ms - SUCCESS", (endOrder - startOrder));

            TaiKhoan checkoutTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
            boolean isGuestCheckout = checkoutTk != null && checkoutTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST;

            // Guest checkout may create a GUEST account in-session; grant access only to the new order.
            if (startedAsAnonymousGuest || isGuestCheckout) {
                synchronized (session) {
                    List<GuestOrderAccess> allowedAccesses = (List<GuestOrderAccess>) session.getAttribute("allowedGuestOrderAccesses");
                    if (allowedAccesses == null) {
                        allowedAccesses = new java.util.ArrayList<>();
                    }
                    allowedAccesses.add(new GuestOrderAccess(hd.getId(), java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES)));
                    session.setAttribute("allowedGuestOrderAccesses", allowedAccesses);
                }
            }

            guestCheckoutService.incrementPurchaseCount(idNguoiDung);

            TaiKhoan tk = (checkoutTk != null) ? checkoutTk : taiKhoanRepository.findById(idNguoiDung).orElse(null);
            if (tk != null) {
                response.put("isGuest", tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST);
                response.put("soLanMuaThanhCong", tk.getSoLanMuaThanhCong());

                if ("NEW".equals(emailStatus)) {
                    long startEmail = System.currentTimeMillis();
                    try {
                        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
                        guestCheckoutService.sendOrderAndAccountNotification(tk.getEmail(), activationToken, appUrl);
                        long endEmail = System.currentTimeMillis();
                        log.info("[GuestCheckout] Send notification email triggered: {}ms - SUCCESS (Asynchronous)", (endEmail - startEmail));
                    } catch (Exception e) {
                        long endEmail = System.currentTimeMillis();
                        log.error("[GuestCheckout] Send notification email triggered: {}ms - FAILED. Exception: {}", (endEmail - startEmail), e.getMessage(), e);
                    }
                }
            } else {
                response.put("isGuest", false);
                response.put("soLanMuaThanhCong", 0);
            }

            // Trigger order confirmation email
            String userEmail = null;
            if (tk != null) {
                userEmail = tk.getEmail();
            } else if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                userEmail = hd.getKhachHang().getTaiKhoan().getEmail();
            } else if (email != null && !email.trim().isEmpty()) {
                userEmail = email.trim();
            }
            if (userEmail != null && !userEmail.trim().isEmpty()) {
                try {
                    String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
                    guestCheckoutService.sendOrderConfirmationEmail(userEmail, hd, appUrl);
                } catch (Exception e) {
                    log.error("Failed to trigger order confirmation email", e);
                }
            }

            // Payment initialization & validation
            long startPayment = System.currentTimeMillis();
            if ("SePay".equalsIgnoreCase(phuongThucThanhToan)) {
                if (hd.getMaDonHang() == null || hd.getMaDonHang().isEmpty()
                        || hd.getTongTien() == null || hd.getTongTien().compareTo(BigDecimal.ZERO) <= 0
                        || !PaymentMethod.SEPAY.getValue().equalsIgnoreCase(hd.getPaymentMethod())) {

                    long endPayment = System.currentTimeMillis();
                    log.error("[GuestCheckout] Create payment request: {}ms - FAILED (Validation error: maDonHang={}, amount={}, method={})",
                            (endPayment - startPayment), hd.getMaDonHang(), hd.getTongTien(), hd.getPaymentMethod());

                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("status", "error");
                    errorMap.put("trangThai", "loi");
                    errorMap.put("errorCode", "PAYMENT_INIT_FAILED");
                    errorMap.put("message", "Không thể khởi tạo thanh toán SePay.");
                    return ResponseEntity.ok(errorMap);
                }
                long endPayment = System.currentTimeMillis();
                log.info("[GuestCheckout] Create payment request: {}ms - SUCCESS", (endPayment - startPayment));
            }

            long endPipeline = System.currentTimeMillis();
            log.info("[GuestCheckout] Guest checkout pipeline completed successfully in {}ms.", (endPipeline - startPipeline));

            response.put("trangThai", "ok");
            response.put("orderId", hd.getId());
            response.put("paymentMethod", hd.getPaymentMethod());
            response.put("tongTien", hd.getTongTien());
            response.put("maDonHang", hd.getMaDonHang());
            response.put("ghnToDistrictId", hd.getGhnToDistrictId());
            response.put("ghnToWardCode", hd.getGhnToWardCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long endPipeline = System.currentTimeMillis();
            log.error("[GuestCheckout] Guest checkout pipeline failed in {}ms. Exception: {}", (endPipeline - startPipeline), e.getMessage(), e);
            response.put("trangThai", "loi");
            if (isPhoneUniqueConstraintViolation(e)) {
                response.put("message", "Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
            } else if (e instanceof org.springframework.dao.DataIntegrityViolationException || isDatabaseException(e)) {
                response.put("message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
            } else {
                response.put("message", e.getMessage());
            }
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/api/voucher/apply")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyVoucher(
            @RequestParam("voucherCode") String voucherCode,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            response.put("trangThai", "chuadangnhap");
            response.put("message", "Vui lòng đăng nhập để sử dụng mã giảm giá.");
            return ResponseEntity.ok(response);
        }

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng nhập mã giảm giá.");
            return ResponseEntity.ok(response);
        }

        String uppercaseCode = voucherCode.trim().toUpperCase();

        // Check active cart items to compute server-side tamTinh
        List<GioHangChiTiet> cartItems = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
        if (cartItems.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Giỏ hàng trống, không thể áp dụng mã giảm giá.");
            return ResponseEntity.ok(response);
        }

        BigDecimal tamTinh = BigDecimal.ZERO;
        for (GioHangChiTiet item : cartItems) {
            if (item.getSanPhamChiTiet() != null) {
                BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(item.getSanPhamChiTiet());
                tamTinh = tamTinh.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
            }
        }

        Optional<PhieuGiamGia> optVoucher = phieuGiamGiaRepository.findByMaPhieu(uppercaseCode);
        if (optVoucher.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá '" + uppercaseCode + "' không tồn tại.");
            return ResponseEntity.ok(response);
        }

        PhieuGiamGia voucher = optVoucher.get();

        if (!voucher.getActive()) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá này đã ngưng hoạt động.");
            return ResponseEntity.ok(response);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getNgayBatDau())) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá chưa đến thời gian áp dụng.");
            return ResponseEntity.ok(response);
        }

        if (now.isAfter(voucher.getNgayKetThuc())) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá đã hết hạn sử dụng.");
            return ResponseEntity.ok(response);
        }

        if (voucher.getSoLuongConLai() <= 0) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá đã hết lượt sử dụng.");
            return ResponseEntity.ok(response);
        }

        BigDecimal minOrder = voucher.getGiaTriDonHangToiThieu();
        if (minOrder != null && minOrder.compareTo(BigDecimal.ZERO) > 0
                && tamTinh.compareTo(minOrder) < 0) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
            response.put("trangThai", "loi");
            response.put("message", "Hạn mức tối thiểu để sử dụng mã này là " + df.format(voucher.getGiaTriDonHangToiThieu()) + " đ (Đơn của bạn: " + df.format(tamTinh) + " đ).");
            return ResponseEntity.ok(response);
        }

        BigDecimal giamGia = VoucherCalculator.calculateVoucherDiscount(tamTinh, voucher);

        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        response.put("trangThai", "ok");
        response.put("maPhieu", voucher.getMaPhieu());
        response.put("giamGia", giamGia);
        response.put("donVi", voucher.getDonVi());
        response.put("giaTri", voucher.getGiaTri());
        response.put("giaTriGiamToiDa", voucher.getGiaTriGiamToiDa());
        response.put("giamGiaFormatted", df.format(giamGia) + " đ");
        response.put("message", "Áp dụng thành công: Giảm " + df.format(voucher.getGiaTri()) + ("%".equals(voucher.getDonVi()) ? "%" : " đ"));
        if (voucher.getGiaTriGiamToiDa() != null) {
            response.put("message", "Áp dụng thành công: Giảm " + df.format(voucher.getGiaTri()) + "%" + " (Tối đa " + df.format(voucher.getGiaTriGiamToiDa()) + " đ)");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/api/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkEmail(@RequestParam("email") String email) {
        Map<String, String> response = new HashMap<>();
        String status = guestCheckoutService.checkEmailStatus(email);
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/api/verify-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            com.smashvn.shop.entity.TaiKhoan tk = userDangNhapService.kiemTraDangNhap(email, password);

            request.changeSessionId();
            session = request.getSession(true);

            session.setAttribute("nguoiDungDangNhap", tk.getEmail());
            session.setAttribute("idNguoiDung", tk.getId());
            session.setAttribute("vaiTro", "KH");
            session.setAttribute("laKhachHang", true);

            com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                guestCartService.transferGuestCartToDb(session, kh.getId());
            } else {
                session.setAttribute("tenHienThi", "Khách hàng");
            }

            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/checkout/api/set-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setPassword(
            @RequestParam("password") String password,
            @RequestParam(value = "email", required = false) String email,
            HttpSession session,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);
        if (activeAccount) {
            response.put("success", false);
            response.put("message", "Phien khach vang lai khong hop le. Vui long dung lien ket thiet lap mat khau trong email.");
            return ResponseEntity.ok(response);
        }
        if (idNguoiDung == null) {
            String sessionEmail = (String) session.getAttribute("guestCheckoutEmail");
            String targetEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : sessionEmail;

            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Chưa đăng nhập và không xác định được email đặt hàng.");
                return ResponseEntity.ok(response);
            }

            TaiKhoan tk = taiKhoanRepository.findByEmail(targetEmail.trim());
            if (tk == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản");
                return ResponseEntity.ok(response);
            }

            if (tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.GUEST) {
                response.put("success", false);
                response.put("message", "Tài khoản đã được kích hoạt trước đó, vui lòng đăng nhập bằng mật khẩu");
                return ResponseEntity.ok(response);
            }

            idNguoiDung = tk.getId();
        }

        TaiKhoan sessionTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        String sessionEmail = (String) session.getAttribute("guestCheckoutEmail");
        if (sessionTk == null
                || sessionTk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.GUEST
                || sessionEmail == null
                || !sessionEmail.equalsIgnoreCase(sessionTk.getEmail())
                || (email != null && !email.trim().isEmpty() && !email.trim().equalsIgnoreCase(sessionTk.getEmail()))) {
            response.put("success", false);
            response.put("message", "Phien khach vang lai khong hop le hoac khong khop email dat hang.");
            return ResponseEntity.ok(response);
        }

        try {
            guestCheckoutService.setPasswordForGuest(idNguoiDung, password);

            // Success! Rotate Session ID to prevent session fixation
            TaiKhoan activatedTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);

            // Invalidate the old guest session completely (clears guestCheckoutEmail, allowedGuestOrderAccesses)
            session.removeAttribute("guestCheckoutEmail");
            session.removeAttribute("allowedGuestOrderAccesses");
            session.invalidate();

            // Create a brand new authenticated session
            HttpSession newSession = request.getSession(true);

            if (activatedTk != null) {
                newSession.setAttribute("nguoiDungDangNhap", activatedTk.getEmail());
                newSession.setAttribute("idNguoiDung", activatedTk.getId());
                newSession.setAttribute("vaiTro", "KH");
                newSession.setAttribute("laKhachHang", true);

                com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(activatedTk.getId());
                if (kh != null) {
                    newSession.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                    guestCartService.transferGuestCartToDb(newSession, kh.getId());
                } else {
                    newSession.setAttribute("tenHienThi", "Khách hàng");
                }
            }

            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/user/thiet-lap-mat-khau")
    public String viewThietLapMatKhau(@RequestParam("token") String token, Model model) {
        try {
            com.smashvn.shop.entity.TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
            if (tkp == null || tkp.isDaSuDung() || tkp.getThoiGianHetHan().isBefore(java.time.LocalDateTime.now())) {
                model.addAttribute("loi", "Đường link thiết lập mật khẩu không hợp lệ hoặc đã hết hạn!");
                return "signin";
            }
            model.addAttribute("token", token);
            return "set-password-by-token";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            return "signin";
        }
    }

    @PostMapping("/user/thiet-lap-mat-khau")
    public String submitThietLapMatKhau(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            model.addAttribute("token", token);
            return "set-password-by-token";
        }

        try {
            guestCheckoutService.setPasswordByToken(token, password);
            return "redirect:/user/dang-nhap?thanhcong=" + java.net.URLEncoder.encode("Thiết lập mật khẩu thành công! Vui lòng đăng nhập.", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("token", token);
            return "set-password-by-token";
        }
    }

    public static class GuestOrderAccess implements java.io.Serializable {

        private static final long serialVersionUID = 1L;
        private final Integer orderId;
        private final java.time.Instant expiresAt;

        public GuestOrderAccess(Integer orderId, java.time.Instant expiresAt) {
            this.orderId = orderId;
            this.expiresAt = expiresAt;
        }

        public Integer getOrderId() {
            return orderId;
        }

        public java.time.Instant getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return java.time.Instant.now().isAfter(expiresAt);
        }
    }

    private boolean isPhoneUniqueConstraintViolation(Throwable e) {
        if (e == null) {
            return false;
        }
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.contains("UX_KhachHang_SoDienThoaiKh")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDatabaseException(Throwable e) {
        if (e == null) {
            return false;
        }
        Throwable current = e;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataAccessException
                    || current instanceof java.sql.SQLException
                    || current instanceof org.hibernate.JDBCException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return org.jsoup.Jsoup.clean(input, org.jsoup.safety.Safelist.none()).trim();
    }
}
