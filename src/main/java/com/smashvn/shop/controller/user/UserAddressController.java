package com.smashvn.shop.controller.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.dto.user.UserAddressDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.user.UserAddressService;
import com.smashvn.shop.service.user.UserDashboardService;
import com.smashvn.shop.service.order.OrderViewService;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;
    private final UserDashboardService dashboardService;
    private final OrderViewService orderViewService;
    private final SanPhamYeuThichRepository wishlistRepository;

    private KhachHang getLoggedInCustomer(HttpSession session) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        KhachHang kh = dashboardService.layThongTinKhachHang(idTaiKhoan);
        if (kh == null || kh.getTaiKhoan() == null || kh.getTaiKhoan().getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.ACTIVE) {
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

    private void populateUserStats(KhachHang kh, Model model) {
        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);
    }

    // 1. Trang danh sách
    @GetMapping
    public String hienThiSoDiaChi(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);
        populateUserStats(kh, model);
        model.addAttribute("danhSachDiaChi", addressService.layDanhSachDiaChi(kh.getId()));
        return "dash-address-book";
    }

    // 2. Form thêm mới
    @GetMapping("/add")
    public String hienThiThemDiaChi(HttpSession session, Model model,
            @RequestParam(value = "from", required = false) String from) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);
        populateUserStats(kh, model);
        model.addAttribute("fromPage", from); // Truyền trang nguồn vào view
        if (!model.containsAttribute("addressDto")) {
            model.addAttribute("addressDto", new UserAddressDto());
        }
        return "dash-address-add";
    }

    // 3. Xử lý thêm mới
    @PostMapping("/add")
    public String xuLyThemDiaChi(HttpSession session,
            @Valid @ModelAttribute("addressDto") UserAddressDto addressDto,
            BindingResult bindingResult,
            @RequestParam(value = "from", required = false) String from,
            Model model,
            RedirectAttributes redirectAttributes) {

        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        // Xác định trang đích sau khi thêm thành công
        String successRedirect = "checkout".equals(from) ? "redirect:/checkout" : "redirect:/user/address";

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("kh", kh);
            model.addAttribute("loi", errorMessage);
            model.addAttribute("fromPage", from);
            return "dash-address-add";
        }

        try {
            addressService.themDiaChiMoi(kh, addressDto);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Đã thêm địa chỉ mới thành công!");
            return successRedirect;
        } catch (IllegalArgumentException e) {
            model.addAttribute("kh", kh);
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("fromPage", from);
            return "dash-address-add";
        } catch (Exception e) {
            model.addAttribute("kh", kh);
            model.addAttribute("loi", "Có lỗi xảy ra khi thêm địa chỉ.");
            model.addAttribute("fromPage", from);
            return "dash-address-add";
        }
    }

    // 4. Form cập nhật
    @GetMapping("/edit/{id}")
    public String hienThiSuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        try {
            SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            model.addAttribute("dc", dc);

            if (!model.containsAttribute("addressDto")) {
                UserAddressDto addressDto = UserAddressDto.builder()
                        .id(dc.getId())
                        .hoNguoiNhan(dc.getHoNguoiNhan())
                        .tenNguoiNhan(dc.getTenNguoiNhan())
                        .sdtNguoiNhan(dc.getSdtNguoiNhan())
                        .diaChiCuThe(dc.getDiaChiCuThe())
                        .tinhThanh(dc.getTinhThanh())
                        .quocGia(dc.getQuocGia())
                        .latitude(dc.getLatitude())
                        .longitude(dc.getLongitude())
                        .defaultAddress(dc.isDefaultShipping())
                        .build();
                model.addAttribute("addressDto", addressDto);
            }
            return "dash-address-edit";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Địa chỉ không tồn tại hoặc bạn không có quyền truy cập.");
            return "redirect:/user/address";
        }
    }

    // 5. Xử lý cập nhật
    @PostMapping("/edit/{id}")
    public String xuLySuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session,
            @Valid @ModelAttribute("addressDto") UserAddressDto addressDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        // FIRST: Validate ownership of the address before handling binding result or running validation!
        try {
            addressService.layDiaChiTheoId(idDiaChi, kh.getId());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", e.getMessage());
            return "redirect:/user/address";
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("kh", kh);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", errorMessage);
            return "dash-address-edit";
        }

        try {
            addressService.capNhatDiaChi(idDiaChi, kh.getId(), addressDto);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Cập nhật địa chỉ thành công!");
            return "redirect:/user/address";
        } catch (IllegalArgumentException e) {
            model.addAttribute("kh", kh);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", e.getMessage());
            return "dash-address-edit";
        } catch (Exception e) {
            model.addAttribute("kh", kh);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", "Có lỗi xảy ra khi cập nhật địa chỉ.");
            return "dash-address-edit";
        }
    }

    // 6. Xử lý Đặt làm mặc định
    @GetMapping("/set-default/{id}")
    public String thietLapDiaChiMacDinh(@PathVariable("id") Integer idDiaChi, HttpSession session, RedirectAttributes redirectAttributes) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        try {
            addressService.datLamMacDinh(idDiaChi, kh.getId());
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Đã thay đổi địa chỉ giao hàng mặc định.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Lỗi: Không thể thay đổi địa chỉ mặc định.");
        }
        return "redirect:/user/address";
    }

    // 7. API Xóa địa chỉ bằng AJAX
    @GetMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> xoaDiaChiAjax(@PathVariable("id") Integer idDiaChi, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);

        if (kh == null) {
            response.put("trangThai", "chuadangnhap");
            return ResponseEntity.ok(response);
        }

        try {
            addressService.xoaDiaChi(idDiaChi, kh.getId());
            response.put("trangThai", "ok");
        } catch (RuntimeException e) {
            response.put("trangThai", "loi");
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
