package com.smashvn.shop.controller.user;

import com.smashvn.shop.dto.user.UserProfileEditDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.OrderViewService;
import com.smashvn.shop.service.user.UserDashboardService;
import com.smashvn.shop.repository.NewsletterSubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDashboardControllerTest {

    @Mock
    private UserDashboardService dashboardService;

    @Mock
    private OrderViewService orderViewService;

    @Mock
    private SanPhamYeuThichRepository wishlistRepository;

    @Mock
    private ThongBaoRepository thongBaoRepository;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    private UserDashboardController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UserDashboardController(dashboardService, orderViewService, wishlistRepository, thongBaoRepository, hoaDonRepository, newsletterSubscriberRepository);
    }

    @Test
    void testXuLySuaHoSo_Success() {
        UserProfileEditDto dto = new UserProfileEditDto("Nguyen", "Van A", "0912345678");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("test@gmail.com");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        when(bindingResult.hasErrors()).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = controller.xuLySuaHoSo(session, dto, bindingResult, model);

        assertEquals("redirect:/user/profile?capNhatThanhCong", view);
        verify(dashboardService).capNhatHoSo(1, dto);
    }

    @Test
    void testXuLySuaHoSo_BindingErrors() {
        UserProfileEditDto dto = new UserProfileEditDto("", "", "invalid-phone");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(bindingResult.hasErrors()).thenReturn(true);
        
        ObjectError error = new FieldError("profileDto", "ho", "Họ không được để trống!");
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(error));

        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("test@gmail.com");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = controller.xuLySuaHoSo(session, dto, bindingResult, model);

        assertEquals("dash-edit-profile", view);
        assertEquals("Họ không được để trống!", model.getAttribute("loi"));
        verify(dashboardService, never()).capNhatHoSo(any(), any());
    }

    @Test
    void testXuLySuaHoSo_ServiceException() {
        UserProfileEditDto dto = new UserProfileEditDto("Nguyen", "Van A", "0912345678");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("test@gmail.com");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalArgumentException("Số điện thoại này đã được đăng ký bởi tài khoản khác!"))
                .when(dashboardService).capNhatHoSo(1, dto);

        Model model = new ConcurrentModel();
        String view = controller.xuLySuaHoSo(session, dto, bindingResult, model);

        assertEquals("dash-edit-profile", view);
        assertEquals("Số điện thoại này đã được đăng ký bởi tài khoản khác!", model.getAttribute("loi"));
    }

    @Test
    void testViewInvoice_Success() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        kh.setId(10);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(100);
        hd.setKhachHang(kh);
        
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.of(hd));
        
        java.util.Map<String, Object> mockDetails = new java.util.HashMap<>();
        mockDetails.put("order", hd);
        when(orderViewService.layChiTietOrder(100, 10)).thenReturn(mockDetails);

        Model model = new ConcurrentModel();
        String view = controller.viewInvoice(100, session, model);

        assertEquals("invoice-print", view);
        assertNotNull(model.getAttribute("order"));
    }

    @Test
    void testViewInvoice_AccessDenied() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        kh.setId(10);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        KhachHang otherKh = new KhachHang();
        otherKh.setId(99);

        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(100);
        hd.setKhachHang(otherKh);
        
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.of(hd));

        Model model = new ConcurrentModel();
        String view = controller.viewInvoice(100, session, model);

        assertEquals("redirect:/user/dang-nhap", view);
    }

    @Test
    void testViewInvoice_NotFound() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.empty());

        Model model = new ConcurrentModel();
        String view = controller.viewInvoice(100, session, model);

        assertEquals("redirect:/user/my-order?loi=donhangkhongton", view);
    }
}
