package com.smashvn.shop.controller.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.smashvn.shop.entity.HoaDon;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smashvn.shop.dto.user.UserProfileEditDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.service.order.OrderViewService;
import com.smashvn.shop.service.user.UserDashboardService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserDashboardController {

    @Value("${app.upload.path:uploads}")
    private String uploadPathConfig;

    private final UserDashboardService dashboardService;
    private final OrderViewService orderViewService;
    private final SanPhamYeuThichRepository wishlistRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final HoaDonRepository hoaDonRepository;
    private final com.smashvn.shop.repository.NewsletterSubscriberRepository newsletterSubscriberRepository;

    private List<String> processUploadedEvidenceFiles(MultipartFile[] files, Integer orderId) throws Exception {
        List<String> uploadedPaths = new java.util.ArrayList<>();
        if (files == null || files.length == 0) {
            return uploadedPaths;
        }

        List<MultipartFile> validFiles = new java.util.ArrayList<>();
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty() && f.getSize() > 0) {
                validFiles.add(f);
            }
        }

        if (validFiles.isEmpty()) {
            return uploadedPaths;
        }

        if (validFiles.size() > 5) {
            throw new IllegalArgumentException("Chỉ được upload tối đa 5 file bằng chứng (ảnh/video).");
        }

        java.nio.file.Path baseUploadDir = java.nio.file.Paths.get(uploadPathConfig != null ? uploadPathConfig : "uploads").toAbsolutePath().normalize();
        java.nio.file.Path targetUploadDir = baseUploadDir.resolve("returns").resolve(String.valueOf(orderId)).normalize();
        if (!java.nio.file.Files.exists(targetUploadDir)) {
            java.nio.file.Files.createDirectories(targetUploadDir);
        }

        List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp", "mp4", "webm", "mov");
        List<String> allowedMimeTypes = List.of(
            "image/jpeg", "image/png", "image/webp",
            "video/mp4", "video/webm", "video/quicktime"
        );

        long maxFileSize = 10 * 1024 * 1024; // 10MB per file

        for (MultipartFile file : validFiles) {
            if (file.getSize() > maxFileSize) {
                throw new IllegalArgumentException("Dung lượng mỗi file bằng chứng không được vượt quá 10MB.");
            }

            String contentType = file.getContentType();
            if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Chỉ nhận ảnh (JPG, PNG, WEBP) hoặc video (MP4, WEBM, MOV).");
            }

            String origName = file.getOriginalFilename();
            String ext = "";
            if (origName != null && origName.contains(".")) {
                ext = origName.substring(origName.lastIndexOf(".") + 1).toLowerCase();
            }
            if (!allowedExtensions.contains(ext)) {
                throw new IllegalArgumentException("Đuôi file '." + ext + "' không hợp lệ.");
            }

            String safeFileName = java.util.UUID.randomUUID().toString() + "." + ext;
            java.nio.file.Path targetFilePath = targetUploadDir.resolve(safeFileName).normalize();

            if (!targetFilePath.startsWith(targetUploadDir)) {
                throw new SecurityException("Phát hiện hành vi Path Traversal không hợp lệ.");
            }

            file.transferTo(targetFilePath.toFile());
            uploadedPaths.add("/uploads/returns/" + orderId + "/" + safeFileName);
        }

        return uploadedPaths;
    }

    // Hàm dùng chung để kiểm tra đăng nhập và lấy KhachHang
    private KhachHang getLoggedInCustomer(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        KhachHang kh = dashboardService.layThongTinKhachHang(idTaiKhoan);
        if (kh == null || kh.getTaiKhoan() == null) {
            return null;
        }
        com.smashvn.shop.entity.AccountStatus status = kh.getTaiKhoan().getTrangThaiTaiKhoan();
        if (status == com.smashvn.shop.entity.AccountStatus.LOCKED || status == com.smashvn.shop.entity.AccountStatus.PENDING_LOCK) {
            return null;
        }
        return kh;
    }

    private String checkRoleAndRedirect(HttpSession session) {
        String vaiTro = (String) session.getAttribute("vaiTro");
        if ("QL".equals(vaiTro)) {
            return "redirect:/admin/all";
        }
        if ("NV".equals(vaiTro)) {
            return "redirect:/admin/don-hang";
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String hienThiDashboard(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        boolean nhanBanTin = false;
        if (kh.getTaiKhoan() != null && kh.getTaiKhoan().getUsername() != null) {
            String userEmail = kh.getTaiKhoan().getUsername().trim().toLowerCase();
            nhanBanTin = newsletterSubscriberRepository.findByEmail(userEmail)
                    .map(sub -> "hoat_dong".equalsIgnoreCase(sub.getTrangThai()))
                    .orElse(false);
        }
        model.addAttribute("nhanBanTin", nhanBanTin);

        // Lấy địa chỉ giao hàng và hóa đơn mặc định
        SoDiaChi defaultShipping = null;
        SoDiaChi defaultBilling = null;
        if (kh.getSoDiaChis() != null) {
            defaultShipping = kh.getSoDiaChis().stream()
                    .filter(SoDiaChi::isDefaultShipping)
                    .findFirst()
                    .orElse(null);
            defaultBilling = kh.getSoDiaChis().stream()
                    .filter(SoDiaChi::isDefaultBilling)
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("defaultShipping", defaultShipping);
        model.addAttribute("defaultBilling", defaultBilling);

        // Đơn hàng gần đây (giới hạn 5 đơn)
        List<Map<String, Object>> recentOrders = ordersList.stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("orders", recentOrders);

        return "dashboard"; // Trỏ đến dashboard.html
    }

    @GetMapping("/profile")
    public String hienThiHoSo(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-my-profile"; // Trỏ đến dash-my-profile.html
    }

    @GetMapping("/profile/edit")
    public String hienThiSuaHoSo(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);
        return "dash-edit-profile"; // Trỏ đến dash-edit-profile.html
    }

    @PostMapping("/profile/edit")
    public String xuLySuaHoSo(HttpSession session,
            @Valid @ModelAttribute("profileDto") UserProfileEditDto profileDto,
            BindingResult bindingResult,
            Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        Integer idTaiKhoan = kh.getTaiKhoan().getId();

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            kh.setHoKh(profileDto.getHo());
            kh.setTenKh(profileDto.getTen());
            kh.setSoDienThoaiKh(profileDto.getSdt());
            model.addAttribute("kh", kh);
            model.addAttribute("loi", errorMessage);
            return "dash-edit-profile";
        }

        try {
            dashboardService.capNhatHoSo(idTaiKhoan, profileDto);
            return "redirect:/user/profile?capNhatThanhCong";
        } catch (IllegalArgumentException e) {
            kh.setHoKh(profileDto.getHo());
            kh.setTenKh(profileDto.getTen());
            kh.setSoDienThoaiKh(profileDto.getSdt());
            model.addAttribute("kh", kh);
            model.addAttribute("loi", e.getMessage());
            return "dash-edit-profile";
        }
    }

    @GetMapping("/my-order")
    public String hienThiMyOrders(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        model.addAttribute("orders", ordersList);

        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);
        return "dash-my-order"; // Trỏ đến dash-my-order.html
    }

    @GetMapping("/cancellation")
    public String hienThiCancellation(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-cancellation";
    }

    @GetMapping("/payment-option")
    public String hienThiPaymentOption(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-payment-option";
    }

    @GetMapping("/track-order")
    public String hienThiTrackOrder(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            model.addAttribute("kh", null);
            model.addAttribute("orderPlaced", 0);
            model.addAttribute("cancelOrders", 0);
            model.addAttribute("wishlist", 0);
            return "dash-track-order";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-track-order";
    }

    @PostMapping("/track-order/submit")
    public String submitTrackOrder(
            @RequestParam("orderId") String orderIdStr,
            @RequestParam(value = "contactInfo", required = false) String contactInfo,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Mã đơn hàng không được để trống.");
            return "redirect:/user/track-order";
        }

        // Try lookup by maDonHang
        java.util.Optional<com.smashvn.shop.entity.HoaDon> hdOpt = hoaDonRepository.findByMaDonHang(orderIdStr.trim());
        if (hdOpt.isEmpty()) {
            String normalized = orderIdStr.trim().replace("-", "").replace("_", "");
            hdOpt = hoaDonRepository.findByMaDonHangOrNormalized(orderIdStr.trim(), normalized);
        }
        if (hdOpt.isEmpty() && orderIdStr.trim().matches("\\d+")) {
            hdOpt = hoaDonRepository.findById(Integer.parseInt(orderIdStr.trim()));
        }

        if (hdOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Không tìm thấy đơn hàng tương ứng với mã cung cấp.");
            return "redirect:/user/track-order";
        }

        com.smashvn.shop.entity.HoaDon hd = hdOpt.get();
        KhachHang loggedInKh = getLoggedInCustomer(session);

        if (loggedInKh != null) {
            // Logged in user: verify if the order belongs to them
            if (hd.getKhachHang() == null || !hd.getKhachHang().getId().equals(loggedInKh.getId())) {
                redirectAttributes.addFlashAttribute("loi", "Bạn không có quyền xem đơn hàng này.");
                return "redirect:/user/track-order";
            }
        } else {
            // Guest user: verify that contactInfo matches the order's email or phone number
            if (contactInfo == null || contactInfo.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("loi", "Email hoặc số điện thoại là bắt buộc đối với khách vãng lai.");
                return "redirect:/user/track-order";
            }

            String searchVal = contactInfo.trim().toLowerCase();
            String orderEmail = (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null)
                    ? hd.getKhachHang().getTaiKhoan().getUsername() : "";
            String orderPhone = hd.getSdtNhan() != null ? hd.getSdtNhan() : "";

            boolean matchesEmail = !orderEmail.isEmpty() && orderEmail.toLowerCase().equals(searchVal);
            boolean matchesPhone = !orderPhone.isEmpty() && (orderPhone.equals(searchVal) || com.smashvn.shop.util.PhoneUtils.normalize(orderPhone).equals(com.smashvn.shop.util.PhoneUtils.normalize(searchVal)));

            if (!matchesEmail && !matchesPhone) {
                redirectAttributes.addFlashAttribute("loi", "Thông tin email hoặc số điện thoại không khớp với đơn hàng.");
                return "redirect:/user/track-order";
            }

            // Successfully validated: grant guest access in session
            synchronized (session) {
                List<com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess> allowedAccesses =
                        (List<com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess>) session.getAttribute("allowedGuestOrderAccesses");
                if (allowedAccesses == null) {
                    allowedAccesses = new java.util.ArrayList<>();
                }
                allowedAccesses.add(new com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess(hd.getId(), java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES)));
                session.setAttribute("allowedGuestOrderAccesses", allowedAccesses);
                if (!orderEmail.isEmpty()) {
                    session.setAttribute("guestCheckoutEmail", orderEmail);
                }
            }
        }

        // Redirect to detail page
        return "redirect:/user/manage-order/" + hd.getId();
    }

    @GetMapping({"/manage-order/{id}", "/manage-order"})
    public String hienThiManageOrder(@PathVariable(value = "id", required = false) String pathId,
            @RequestParam(value = "id", required = false) String paramId,
            HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        String targetStr = (pathId != null) ? pathId : paramId;
        if (targetStr == null || targetStr.trim().isEmpty()) {
            return "redirect:/user/my-order";
        }

        // Try lookup by maDonHang first
        java.util.Optional<com.smashvn.shop.entity.HoaDon> hdOpt = hoaDonRepository.findByMaDonHang(targetStr.trim());
        if (hdOpt.isEmpty()) {
            // Try normalized maDonHang
            String normalized = targetStr.trim().replace("-", "").replace("_", "");
            hdOpt = hoaDonRepository.findByMaDonHangOrNormalized(targetStr.trim(), normalized);
        }
        if (hdOpt.isEmpty() && targetStr.trim().matches("\\d+")) {
            // Fallback: try by numeric ID
            hdOpt = hoaDonRepository.findById(Integer.parseInt(targetStr.trim()));
        }

        if (hdOpt.isEmpty()) {
            return "redirect:/user/my-order?loi=donhangkhongton";
        }

        com.smashvn.shop.entity.HoaDon hd = hdOpt.get();
        Integer targetId = hd.getId();

        KhachHang kh = getLoggedInCustomer(session);
        boolean isGuestView = false;
        if (kh != null && kh.getTaiKhoan() != null && kh.getTaiKhoan().getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST) {
            isGuestView = true;
        }

        if (kh == null) {
            // Guest check
            String guestEmail = (String) session.getAttribute("guestCheckoutEmail");
            Object allowedAccessesAttr = session.getAttribute("allowedGuestOrderAccesses");
            java.util.List<?> allowedAccessesRaw = (allowedAccessesAttr instanceof java.util.List<?>)
                    ? (java.util.List<?>) allowedAccessesAttr : null;

            boolean isAllowed = false;
            if (guestEmail != null && allowedAccessesRaw != null) {
                synchronized (session) {
                    java.util.Iterator<?> iterator = allowedAccessesRaw.iterator();
                    while (iterator.hasNext()) {
                        Object item = iterator.next();
                        if (item instanceof com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess access
                                && access.isExpired()) {
                            iterator.remove();
                        }
                    }
                    for (Object item : allowedAccessesRaw) {
                        if (item instanceof com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess access) {
                            if (access.getOrderId().equals(targetId)) {
                                isAllowed = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!isAllowed) {
                return "redirect:/user/dang-nhap";
            }

            // Cross-email validation to prevent IDOR
            String orderEmail = (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null)
                    ? hd.getKhachHang().getTaiKhoan().getUsername() : null;
            if (orderEmail == null || !orderEmail.equalsIgnoreCase(guestEmail)) {
                return "redirect:/user/dang-nhap";
            }

            kh = hd.getKhachHang();
            isGuestView = true;
        }

        try {
            Map<String, Object> details = orderViewService.layChiTietOrder(targetId, kh.getId());
            if (details == null) {
                return "redirect:/user/my-order?loi=donhangkhongton";
            }

            model.addAttribute("kh", kh);
            model.addAllAttributes(details);

            long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());
            model.addAttribute("wishlistCount", wishlistCount);
            model.addAttribute("isGuestView", isGuestView);

            return "dash-manage-order";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/my-order?loi=loihethong";
        }
    }

    @PostMapping("/manage-order/cancel/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyHuyDonHang(
            @PathVariable("id") Integer idHoaDon,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thực hiện thao tác này.");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        String ipAddress = request.getRemoteAddr();
        try {
            boolean success = orderViewService.huyDonHang(idHoaDon, kh.getId(), ipAddress, lyDoHuy);
            if (success) {
                response.put("success", true);
                response.put("message", "Hủy đơn hàng thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể hủy đơn hàng này. Đơn hàng có thể đã được giao hoặc đang được xử lý.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/manage-order/confirm-received/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyXacNhanDaNhanHang(
            @PathVariable("id") Integer idHoaDon,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thực hiện thao tác này.");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        try {
            boolean success = orderViewService.xacNhanDaNhanHang(idHoaDon, kh.getId(), request.getRemoteAddr());
            if (success) {
                response.put("success", true);
                response.put("message", "Cảm ơn bạn đã xác nhận đã nhận được hàng!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể xác nhận đơn hàng này. Vui lòng kiểm tra lại trạng thái đơn.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    private void cleanupUploadedFiles(List<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return;
        }
        java.nio.file.Path baseUploadDir = java.nio.file.Paths.get(uploadPathConfig != null ? uploadPathConfig : "uploads").toAbsolutePath().normalize();
        for (String relPath : relativePaths) {
            try {
                if (relPath != null && relPath.startsWith("/uploads/")) {
                    String subPath = relPath.substring("/uploads/".length());
                    java.nio.file.Path filePath = baseUploadDir.resolve(subPath).normalize();
                    if (filePath.startsWith(baseUploadDir)) {
                        java.nio.file.Files.deleteIfExists(filePath);
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể xóa file đính kèm rác {}: {}", relPath, e.getMessage());
            }
        }
    }

    @PostMapping("/manage-order/request-return/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyYeuCauTraHang(
            @PathVariable("id") Integer idHoaDon,
            @RequestParam(value = "loaiYeuCau", required = false) String loaiYeuCau,
            @RequestParam(value = "lyDo", required = false) String lyDo,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thực hiện thao tác này.");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        List<String> bangChungPaths = new java.util.ArrayList<>();
        try {
            bangChungPaths = processUploadedEvidenceFiles(files, idHoaDon);
            boolean success = orderViewService.yeuCauTraHang(idHoaDon, kh.getId(), loaiYeuCau, lyDo, bangChungPaths, request.getRemoteAddr());
            if (success) {
                response.put("success", true);
                response.put("message", "Yêu cầu Đổi/Trả hàng của bạn đã được gửi thành công! Shop sẽ sớm phản hồi.");
            } else {
                cleanupUploadedFiles(bangChungPaths);
                response.put("success", false);
                response.put("message", "Không thể gửi yêu cầu Đổi/Trả cho đơn hàng này.");
            }
        } catch (Exception e) {
            cleanupUploadedFiles(bangChungPaths);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @GetMapping("/notifications")
    public String hienThiThongBao(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<ThongBao> listThongBao = thongBaoRepository.findByTaiKhoan_IdOrderByNgayTaoDesc(kh.getTaiKhoan().getId());
        model.addAttribute("listThongBao", listThongBao);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-notifications"; // Trỏ đến dash-notifications.html
    }

    @PostMapping("/notifications/read/{id}")
    public String danhDauDaDoc(@PathVariable("id") Integer id, HttpSession session) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        thongBaoRepository.findById(id).ifPresent(tb -> {
            if (tb.getTaiKhoan().getId().equals(kh.getTaiKhoan().getId())) {
                tb.setDaDoc(true);
                thongBaoRepository.save(tb);
            }
        });
        return "redirect:/user/notifications";
    }

    @GetMapping("/order/invoice/{id}")
    public String viewInvoice(@PathVariable("id") Integer id, HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        Optional<HoaDon> hdOpt = hoaDonRepository.findById(id);
        if (hdOpt.isEmpty()) {
            return "redirect:/user/my-order?loi=donhangkhongton";
        }

        HoaDon hd = hdOpt.get();
        KhachHang kh = getLoggedInCustomer(session);

        boolean isAllowed = false;
        if (kh != null) {
            if (hd.getKhachHang() != null && hd.getKhachHang().getId().equals(kh.getId())) {
                isAllowed = true;
            }
        } else {
            String guestEmail = (String) session.getAttribute("guestCheckoutEmail");
            Object allowedAccessesAttr = session.getAttribute("allowedGuestOrderAccesses");
            java.util.List<?> allowedAccessesRaw = (allowedAccessesAttr instanceof java.util.List<?>)
                    ? (java.util.List<?>) allowedAccessesAttr : null;

            if (guestEmail != null && allowedAccessesRaw != null) {
                for (Object item : allowedAccessesRaw) {
                    if (item instanceof com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess access) {
                        if (access.getOrderId().equals(id) && !access.isExpired()) {
                            isAllowed = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!isAllowed) {
            return "redirect:/user/dang-nhap";
        }

        try {
            Map<String, Object> details = orderViewService.layChiTietOrder(id, hd.getKhachHang().getId());
            if (details == null) {
                return "redirect:/user/my-order?loi=donhangkhongton";
            }
            model.addAllAttributes(details);
            return "invoice-print";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/my-order?loi=loihethong";
        }
    }
}
