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
import com.smashvn.shop.dto.order.*;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.service.order.CheckoutContextService;
import com.smashvn.shop.service.order.PendingCheckoutRegistry;
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
    private final CheckoutContextService checkoutContextService;
    private final PendingCheckoutRegistry pendingCheckoutRegistry;
    private final GioHangChiTietRepository gioHangChiTietRepository;
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

    @PostMapping("/checkout/start-all")
    @ResponseBody
    public ResponseEntity<FullCartCheckoutResult> startAllCheckout(HttpSession session) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            FullCartCheckoutResult result = checkoutContextService.createFullCartContext(session, idNguoiDung);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            log.error("Error in startAllCheckout", e);
            FullCartCheckoutResult errorResult = FullCartCheckoutResult.builder()
                    .trangThai("error")
                    .thongBao("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.")
                    .message("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.")
                    .build();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @PostMapping("/checkout/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startCheckout(
            @RequestParam(value = "selectedItemIds", required = false) List<Integer> selectedItemIds,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng chọn ít nhất một sản phẩm để thanh toán!");
            return ResponseEntity.ok(response);
        }

        List<Integer> distinctIds = selectedItemIds.stream().distinct().collect(java.util.stream.Collectors.toList());
        if (distinctIds.size() > 100) {
            response.put("trangThai", "loi");
            response.put("message", "Số lượng sản phẩm thanh toán không được vượt quá 100 sản phẩm.");
            return ResponseEntity.ok(response);
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);

        List<CheckoutItemContext> contextItems = new java.util.ArrayList<>();

        if (!activeAccount) {
            List<GuestCartService.GuestCartItem> guestCart = guestCartService.getGuestCartItems(session);
            for (Integer spctId : distinctIds) {
                GuestCartService.GuestCartItem match = guestCart.stream()
                        .filter(item -> item.getIdSanPhamChiTiet().equals(spctId))
                        .findFirst()
                        .orElse(null);
                if (match == null || match.getSoLuong() == null || match.getSoLuong() <= 0) {
                    response.put("trangThai", "loi");
                    response.put("message", "Một số sản phẩm đã chọn không còn tồn tại trong giỏ hàng. Vui lòng tải lại giỏ hàng!");
                    return ResponseEntity.ok(response);
                }

                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(spctId).orElse(null);
                if (spct == null) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm đã chọn không tồn tại trong hệ thống.");
                    return ResponseEntity.ok(response);
                }
                boolean dangBan = isSanPhamChiTietDangBan(spct);
                if (!dangBan) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "") + "\" đã ngừng kinh doanh.");
                    return ResponseEntity.ok(response);
                }
                int tonKho = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
                if (match.getSoLuong() > tonKho) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + spct.getSanPham().getTenSanPham() + "\" số lượng (" + match.getSoLuong() + ") vượt quá tồn kho (" + tonKho + "). Vui lòng điều chỉnh lại!");
                    return ResponseEntity.ok(response);
                }

                contextItems.add(CheckoutItemContext.builder()
                        .cartItemId(null)
                        .idSanPhamChiTiet(spctId)
                        .soLuong(match.getSoLuong())
                        .fromCart(true)
                        .build());
            }
        } else {
            com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            if (kh == null) {
                response.put("trangThai", "loi");
                response.put("message", "Thông tin tài khoản không hợp lệ.");
                return ResponseEntity.ok(response);
            }
            List<GioHangChiTiet> dbCartItems = gioHangChiTietRepository.findAllByIdInAndGioHang_KhachHang_Id(distinctIds, kh.getId());
            if (dbCartItems.size() != distinctIds.size()) {
                response.put("trangThai", "loi");
                response.put("message", "Một số sản phẩm đã chọn không còn tồn tại trong giỏ hàng. Vui lòng tải lại giỏ hàng!");
                return ResponseEntity.ok(response);
            }

            for (GioHangChiTiet item : dbCartItems) {
                com.smashvn.shop.entity.SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct == null) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm trong giỏ không hợp lệ.");
                    return ResponseEntity.ok(response);
                }
                boolean dangBan = isSanPhamChiTietDangBan(spct);
                if (!dangBan) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "") + "\" đã ngừng kinh doanh.");
                    return ResponseEntity.ok(response);
                }
                int tonKho = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
                if (item.getSoLuong() == null || item.getSoLuong() <= 0) {
                    response.put("trangThai", "loi");
                    response.put("message", "Số lượng sản phẩm trong giỏ hàng không hợp lệ.");
                    return ResponseEntity.ok(response);
                }
                if (item.getSoLuong() > tonKho) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + spct.getSanPham().getTenSanPham() + "\" số lượng (" + item.getSoLuong() + ") vượt quá tồn kho (" + tonKho + "). Vui lòng điều chỉnh lại!");
                    return ResponseEntity.ok(response);
                }

                contextItems.add(CheckoutItemContext.builder()
                        .cartItemId(item.getId())
                        .idSanPhamChiTiet(spct.getId())
                        .soLuong(item.getSoLuong())
                        .fromCart(true)
                        .build());
            }
        }


        if (contextItems.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Không tìm thấy sản phẩm hợp lệ trong giỏ hàng để thanh toán!");
            return ResponseEntity.ok(response);
        }

        CheckoutContext context = checkoutContextService.createCartContext(session, idNguoiDung, contextItems);
        response.put("trangThai", "ok");
        response.put("checkoutToken", context.getToken());
        response.put("checkoutUrl", "/checkout?token=" + context.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/buy-now")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buyNow(
            @RequestParam(value = "idSanPhamChiTiet", required = false) Integer idSanPhamChiTiet,
            @RequestParam(value = "soLuong", required = false, defaultValue = "1") Integer soLuong,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        if (idSanPhamChiTiet == null) {
            response.put("trangThai", "loi");
            response.put("message", "Sản phẩm không hợp lệ.");
            return ResponseEntity.ok(response);
        }
        if (soLuong == null || soLuong <= 0) {
            response.put("trangThai", "loi");
            response.put("message", "Số lượng mua không hợp lệ.");
            return ResponseEntity.ok(response);
        }

        com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet).orElse(null);
        if (spct == null || !isSanPhamChiTietDangBan(spct)) {
            response.put("trangThai", "loi");
            response.put("message", "Sản phẩm hoặc phân loại đã ngưng kinh doanh.");
            return ResponseEntity.ok(response);
        }
        if (spct.getSoLuongTon() < soLuong) {
            response.put("trangThai", "loi");
            response.put("message", "Số lượng tồn kho không đủ (Còn lại: " + spct.getSoLuongTon() + ").");
            return ResponseEntity.ok(response);
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        CheckoutContext context = checkoutContextService.createBuyNowContext(session, idNguoiDung, idSanPhamChiTiet, soLuong);

        response.put("trangThai", "ok");
        response.put("checkoutToken", context.getToken());
        response.put("checkoutUrl", "/checkout?token=" + context.getToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/checkout", "/checkout.html"})
    public String viewCheckout(
            @RequestParam(value = "token", required = false) String token,
            HttpSession session,
            Model model) {

        if (token == null || token.isBlank()) {
            return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Vui lòng chọn sản phẩm để thanh toán!", java.nio.charset.StandardCharsets.UTF_8);
        }

        CheckoutContext context = checkoutContextService.getContext(session, token);
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);


        if (context == null || !checkoutContextService.validateOwnership(context, idNguoiDung, session.getId())) {
            return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Phiên thanh toán không hợp lệ hoặc đã hết hạn!", java.nio.charset.StandardCharsets.UTF_8);
        }

        List<GioHangChiTiet> danhSachChiTiet = new java.util.ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;

        for (CheckoutItemContext itemCtx : context.getItems()) {
            com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(itemCtx.getIdSanPhamChiTiet()).orElse(null);
            if (spct == null) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng chứa sản phẩm không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }

            SanPham sp = spct.getSanPham();
            int tonKho = spct.getSoLuongTon();
            int reqQty = itemCtx.getSoLuong() != null ? itemCtx.getSoLuong() : 0;

            if (reqQty <= 0) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Số lượng sản phẩm không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (!isSanPhamChiTietDangBan(spct)) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Phân loại sản phẩm '" + sp.getTenSanPham() + "' đã ngưng kinh doanh!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (tonKho <= 0) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' đã hết hàng!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (reqQty > tonKho) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' không đủ số lượng tồn kho (Còn lại: " + tonKho + ")!", java.nio.charset.StandardCharsets.UTF_8);
            }

            GioHangChiTiet detail = new GioHangChiTiet();
            detail.setId(itemCtx.getCartItemId() != null ? itemCtx.getCartItemId() : spct.getId());
            detail.setSanPhamChiTiet(spct);
            detail.setSoLuong(reqQty);

            BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(spct);
            tongTien = tongTien.add(giaBanSauGiam.multiply(new BigDecimal(reqQty)));
            danhSachChiTiet.add(detail);
        }

        model.addAttribute("checkoutToken", token);


        List<DonViVanChuyen> listDvvc = donViVanChuyenDAO.findAll().stream()
                .filter(dv -> {
                    if (dv.getTenDonVi() == null) {
                        return false;
                    }
                    String tenLower = dv.getTenDonVi().toLowerCase();
                    return tenLower.contains("giao hàng nhanh") || tenLower.contains("ghn");
                })
                .collect(java.util.stream.Collectors.toList());

        // Fallback for tests if Giao Hang Nhanh is not found in database
        if (listDvvc.isEmpty()) {
            listDvvc = donViVanChuyenDAO.findAll().stream()
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
        }

        List<SoDiaChi> listDiaChi = new java.util.ArrayList<>();
        boolean hasDefaultAddress = false;
        if (idNguoiDung != null) {
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
            details.put("tinhThanh", dc.getProvinceName());
            details.put("thanhPho", dc.getDistrictName());
            details.put("quocGia", dc.getQuocGia());
            details.put("latitude", dc.getLatitude());
            details.put("longitude", dc.getLongitude());
            details.put("wardName", dc.getWardName());
            String fullAddress = dc.getDiaChiCuThe();
            if (dc.getWardName() != null && !dc.getWardName().trim().isEmpty() && !fullAddress.contains(dc.getWardName())) {
                fullAddress += ", " + dc.getWardName().trim();
            }
            if (dc.getDistrictName() != null && !dc.getDistrictName().trim().isEmpty() && !fullAddress.contains(dc.getDistrictName())) {
                fullAddress += ", " + dc.getDistrictName().trim();
            }
            if (dc.getProvinceName() != null && !dc.getProvinceName().trim().isEmpty() && !fullAddress.contains(dc.getProvinceName())) {
                fullAddress += ", " + dc.getProvinceName().trim();
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
                && tk.getMatKhau() != null && !tk.getMatKhau().trim().isEmpty()
                && "hoat_dong".equalsIgnoreCase(tk.getTrangThai());
    }

    @PostMapping("/checkout/submit")


    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCheckout(
            @RequestParam(value = "checkoutToken", required = false) String checkoutToken,
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

        CheckoutContext context = null;
        if (checkoutToken != null && !checkoutToken.isBlank()) {
            context = checkoutContextService.getContext(session, checkoutToken);
        }

        if (context == null) {
            List<CheckoutItemContext> contextItems = new java.util.ArrayList<>();
            if (idNguoiDung != null) {
                com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
                if (kh != null) {
                    List<GioHangChiTiet> dbCartItems = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
                    for (GioHangChiTiet item : dbCartItems) {
                        if (item.getSoLuong() != null && item.getSoLuong() > 0) {
                            contextItems.add(CheckoutItemContext.builder()
                                    .cartItemId(item.getId())
                                    .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                                    .soLuong(item.getSoLuong())
                                    .fromCart(true)
                                    .build());
                        }
                    }
                }
            } else {
                List<GuestCartService.GuestCartItem> guestCart = guestCartService.getGuestCartItems(session);
                for (GuestCartService.GuestCartItem item : guestCart) {
                    if (item.getSoLuong() != null && item.getSoLuong() > 0) {
                        contextItems.add(CheckoutItemContext.builder()
                                .cartItemId(null)
                                .idSanPhamChiTiet(item.getIdSanPhamChiTiet())
                                .soLuong(item.getSoLuong())
                                .fromCart(true)
                                .build());
                    }
                }
            }
            context = checkoutContextService.createCartContext(session, idNguoiDung, contextItems);
        }

        if (!context.tryClaim()) {
            response.put("trangThai", "loi");
            response.put("message", "Yêu cầu thanh toán đang được xử lý hoặc đã hoàn tất. Vui lòng không gửi lại.");
            return ResponseEntity.ok(response);
        }



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

                session.setAttribute("nguoiDungDangNhap", tk.getUsername());
                session.setAttribute("idNguoiDung", tk.getId());
                session.setAttribute("vaiTro", "KH");
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                session.setAttribute("guestCheckoutEmail", tk.getUsername());

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
            if (idNguoiDung != null) {
                com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
                if (kh != null) {
                    guestCartService.transferGuestCartToDb(session, kh.getId());
                }
            }
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
        // Force Giao Hàng Nhanh (GHN) carrier for online orders
        DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream()
                .filter(c -> c.getTenDonVi() != null && (
                        c.getTenDonVi().toLowerCase().contains("giao hàng nhanh") ||
                        c.getTenDonVi().toLowerCase().contains("ghn")
                ))
                .findFirst()
                .orElse(null);
        if (dvvc == null) {
            dvvc = donViVanChuyenDAO.findById(idDonViVanChuyen).orElse(null);
        }
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
        Integer idDonViVanChuyenResolved = dvvc.getId();

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
                long existingCount = soDiaChiRepository.findByKhachHang_Id(khachHang.getId()).size();
                if (currentTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST
                        || startedAsAnonymousGuest
                        || existingCount == 0
                        || Boolean.TRUE.equals(saveAddress)) {
                    // Guest account / Auto-registered guest / First address: automatically save to SoDiaChi
                    shouldSave = true;
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
            OrderCreationResult orderResult;
            boolean isCod = "COD".equalsIgnoreCase(phuongThucThanhToan);

            if (isCod) {
                orderResult = gioHangService.submitCodOrder(
                        idNguoiDung, context, session,
                        hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyenResolved,
                        ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                        resolvedDiaChiLuuId, voucherCode);
            } else {
                orderResult = gioHangService.createSepayPendingOrder(
                        idNguoiDung, context,
                        hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyenResolved,
                        ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                        resolvedDiaChiLuuId, voucherCode);

                HoaDon hdPending = orderResult.getHoaDon();
                CheckoutExecutionSnapshot snapshot = CheckoutExecutionSnapshot.builder()
                        .orderId(hdPending.getId())
                        .maDonHang(hdPending.getMaDonHang())
                        .source(context.getSource())
                        .status(PendingCheckoutStatus.READY)
                        .customerId(idNguoiDung)
                        .sessionId(session.getId())
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .items(orderResult.getPurchasedItems())
                        .build();

                pendingCheckoutRegistry.registerSnapshot(snapshot);
            }

            context.consume();
            HoaDon hd = orderResult.getHoaDon();
            long endOrder = System.currentTimeMillis();
            log.info("[GuestCheckout] Create order: {}ms - SUCCESS", (endOrder - startOrder));


            TaiKhoan checkoutTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
            boolean isGuestCheckout = checkoutTk != null && checkoutTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST;

            // Guest checkout may create a GUEST account in-session; grant access only to the new order.
            if (startedAsAnonymousGuest || isGuestCheckout) {
                synchronized (session) {
                    Object attr = session.getAttribute("allowedGuestOrderAccesses");
                    List<GuestOrderAccess> allowedAccesses = new java.util.ArrayList<>();
                    if (attr instanceof java.util.List<?>) {
                        for (Object o : (java.util.List<?>) attr) {
                            if (o instanceof GuestOrderAccess) {
                                allowedAccesses.add((GuestOrderAccess) o);
                            }
                        }
                    }
                    allowedAccesses.add(new GuestOrderAccess(hd.getId(), java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES)));
                    session.setAttribute("allowedGuestOrderAccesses", allowedAccesses);
                }
            }

            if (isCod) {
                guestCheckoutService.incrementPurchaseCount(idNguoiDung);
            }


            TaiKhoan tk = (checkoutTk != null) ? checkoutTk : taiKhoanRepository.findById(idNguoiDung).orElse(null);
            if (tk != null) {
                response.put("isGuest", tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST);
                response.put("soLanMuaThanhCong", tk.getSoLanMuaThanhCong());

                if (isCod && "NEW".equals(emailStatus)) {
                    long startEmail = System.currentTimeMillis();
                    try {
                        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
                        String contactEmail = tk.getUsername();
                        if (contactEmail != null && contactEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                            guestCheckoutService.sendOrderAndAccountNotification(contactEmail, activationToken, appUrl);
                        }
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

            // Trigger order confirmation email for COD orders (SePay orders trigger on payment confirmation)
            if (isCod) {
                String userEmail = null;
                if (tk != null && tk.getUsername() != null && tk.getUsername().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                    userEmail = tk.getUsername();
                } else if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                    String un = hd.getKhachHang().getTaiKhoan().getUsername();
                    if (un != null && un.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                        userEmail = un;
                    }
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
            context.release();
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
    public ResponseEntity<Map<String, String>> checkEmail(@RequestParam("email") String email, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String status = guestCheckoutService.checkEmailStatus(email);
        if ("GUEST_EXPIRED".equals(status) || "GUEST_VALID".equals(status)) {
            if (email != null && !email.trim().isEmpty()) {
                session.setAttribute("guestCheckoutEmail", email.trim());
            }
        }
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/api/verify-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "checkoutToken", required = false) String checkoutToken,
            HttpSession session,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            com.smashvn.shop.entity.TaiKhoan tk = userDangNhapService.kiemTraDangNhap(email, password);

            HttpSession oldSession = session;
            request.changeSessionId();
            HttpSession newSession = request.getSession(true);

            newSession.setAttribute("nguoiDungDangNhap", tk.getUsername());
            newSession.setAttribute("idNguoiDung", tk.getId());
            newSession.setAttribute("vaiTro", "KH");

            if (oldSession != null) {
                oldSession.setAttribute("nguoiDungDangNhap", tk.getUsername());
                oldSession.setAttribute("idNguoiDung", tk.getId());
                oldSession.setAttribute("vaiTro", "KH");
            }

            com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                newSession.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                guestCartService.transferGuestCartToDb(newSession, kh.getId());
            } else {
                newSession.setAttribute("tenHienThi", "Khách hàng");
            }

            if (checkoutToken != null && !checkoutToken.isBlank()) {
                checkoutContextService.promoteGuestContextToAuthenticatedUser(
                        checkoutToken, oldSession, newSession, tk.getId());
                response.put("redirectUrl", "/checkout?token=" + java.net.URLEncoder.encode(checkoutToken.trim(), java.nio.charset.StandardCharsets.UTF_8));
            } else {
                response.put("redirectUrl", "/checkout");
            }

            response.put("trangThai", "ok");
            response.put("authenticated", true);
            response.put("reloadAddresses", true);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("trangThai", "loi");
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
            @RequestParam(value = "checkoutToken", required = false) String checkoutToken,
            HttpSession session,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        HttpSession oldSession = session;
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);
        if (activeAccount) {
            response.put("trangThai", "loi");
            response.put("success", false);
            response.put("message", "Phiên khách vãng lai không hợp lệ. Vui lòng dùng liên kết thiết lập mật khẩu trong email.");
            return ResponseEntity.ok(response);
        }
        if (idNguoiDung == null) {
            String sessionEmail = (String) session.getAttribute("guestCheckoutEmail");
            String targetEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : sessionEmail;

            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("success", false);
                response.put("message", "Chưa đăng nhập và không xác định được email đặt hàng.");
                return ResponseEntity.ok(response);
            }

            TaiKhoan tk = taiKhoanRepository.findByUsername(targetEmail.trim());
            if (tk == null) {
                response.put("trangThai", "loi");
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản");
                return ResponseEntity.ok(response);
            }

            if (tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.GUEST) {
                response.put("trangThai", "loi");
                response.put("success", false);
                response.put("message", "Tài khoản đã được kích hoạt trước đó, vui lòng đăng nhập bằng mật khẩu");
                return ResponseEntity.ok(response);
            }

            idNguoiDung = tk.getId();
        }

        TaiKhoan sessionTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        String sessionEmail = (String) session.getAttribute("guestCheckoutEmail");
        String effectiveEmail = (sessionEmail != null && !sessionEmail.isBlank()) ? sessionEmail : email;
        if (sessionTk == null
                || sessionTk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.GUEST
                || effectiveEmail == null
                || !effectiveEmail.equalsIgnoreCase(sessionTk.getUsername())
                || (email != null && !email.trim().isEmpty() && !email.trim().equalsIgnoreCase(sessionTk.getUsername()))) {

            response.put("trangThai", "loi");
            response.put("success", false);
            response.put("message", "Phiên khách vãng lai không hợp lệ hoặc không khớp email đặt hàng.");
            return ResponseEntity.ok(response);
        }

        try {
            guestCheckoutService.setPasswordForGuest(idNguoiDung, password);

            TaiKhoan activatedTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);

            @SuppressWarnings("unchecked")
            Map<String, com.smashvn.shop.dto.order.CheckoutContext> oldContextsMap =
                    oldSession != null ? (Map<String, com.smashvn.shop.dto.order.CheckoutContext>) oldSession.getAttribute(CheckoutContextService.SESSION_CONTEXTS_KEY) : null;

            if (oldSession != null) {
                oldSession.removeAttribute("guestCheckoutEmail");
                oldSession.removeAttribute("allowedGuestOrderAccesses");
                try {
                    oldSession.invalidate();
                } catch (Exception e) {
                    // Ignore
                }
            }

            HttpSession newSession = request.getSession(true);
            if (oldContextsMap != null) {
                Map<String, com.smashvn.shop.dto.order.CheckoutContext> newMap = new HashMap<>(oldContextsMap);
                newSession.setAttribute(CheckoutContextService.SESSION_CONTEXTS_KEY, newMap);
            }

            if (activatedTk != null) {
                newSession.setAttribute("nguoiDungDangNhap", activatedTk.getUsername());
                newSession.setAttribute("idNguoiDung", activatedTk.getId());
                newSession.setAttribute("vaiTro", "KH");

                com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(activatedTk.getId());
                if (kh != null) {
                    newSession.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                    guestCartService.transferGuestCartToDb(newSession, kh.getId());
                } else {
                    newSession.setAttribute("tenHienThi", "Khách hàng");
                }
            }

            if (checkoutToken != null && !checkoutToken.isBlank()) {
                checkoutContextService.promoteGuestContextToAuthenticatedUser(
                        checkoutToken, oldSession, newSession, activatedTk != null ? activatedTk.getId() : idNguoiDung);
                response.put("redirectUrl", "/checkout?token=" + java.net.URLEncoder.encode(checkoutToken.trim(), java.nio.charset.StandardCharsets.UTF_8));
            } else {
                response.put("redirectUrl", "/checkout");
            }

            response.put("trangThai", "ok");
            response.put("authenticated", true);
            response.put("reloadAddresses", true);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("trangThai", "loi");
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
