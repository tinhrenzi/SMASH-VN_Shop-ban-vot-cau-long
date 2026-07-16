package com.smashvn.shop.controller.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.product.PricingService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GioHangControllerTest {

    @Mock
    private GioHangService gioHangService;

    @Mock
    private PricingService pricingService;

    @Mock
    private GuestCartService guestCartService;

    @Mock
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private HttpSession session;

    private GioHangController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new GioHangController(gioHangService, pricingService, guestCartService, sanPhamChiTietRepository, taiKhoanRepository);
    }

    @Test
    void guestDoesNotUseDbBackedCartWhenAddingItem() {
        TaiKhoan guest = account(10, AccountStatus.GUEST, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(10);
        when(taiKhoanRepository.findById(10)).thenReturn(Optional.of(guest));

        SanPham sp = new SanPham();
        sp.setTenSanPham("Guest Product");
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(55);
        spct.setSanPham(sp);
        spct.setGiaBan(new BigDecimal("100000"));
        when(sanPhamChiTietRepository.findById(55)).thenReturn(Optional.of(spct));
        when(pricingService.calculateCurrentSellingPrice(spct)).thenReturn(new BigDecimal("100000"));

        ResponseEntity<?> response = controller.xuLyThemVaoGio(55, 2, session);

        assertEquals(200, response.getStatusCode().value());
        verify(guestCartService).addToGuestCart(session, 55, 2);
        verify(gioHangService, never()).themVaoGio(any(), any(), any());
    }

    @Test
    void activeAccountStillUsesDbBackedCartWhenAddingItem() {
        TaiKhoan active = account(11, AccountStatus.ACTIVE, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(11);
        when(taiKhoanRepository.findById(11)).thenReturn(Optional.of(active));
        when(gioHangService.themVaoGio(11, 55, 2)).thenReturn(new java.util.HashMap<>());

        ResponseEntity<?> response = controller.xuLyThemVaoGio(55, 2, session);

        assertEquals(200, response.getStatusCode().value());
        verify(gioHangService).themVaoGio(11, 55, 2);
        verify(guestCartService, never()).addToGuestCart(session, 55, 2);
    }

    @Test
    void guestMiniCartUsesSessionCart() {
        TaiKhoan guest = account(12, AccountStatus.GUEST, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(12);
        when(taiKhoanRepository.findById(12)).thenReturn(Optional.of(guest));
        when(guestCartService.layDuLieuMiniCart(session)).thenReturn(Map.of("tongSoLuong", 1));

        ResponseEntity<Map<String, Object>> response = controller.layDuLieuMiniCart(session);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().get("tongSoLuong"));
        verify(guestCartService).layDuLieuMiniCart(session);
        verify(gioHangService, never()).layDuLieuMiniCart(12);
    }

    private TaiKhoan account(Integer id, AccountStatus status, String trangThai) {
        TaiKhoan tk = new TaiKhoan();
        tk.setId(id);
        tk.setUsername("user" + id + "@example.com");
        tk.setVaiTro("KH");
        tk.setTrangThai(trangThai);
        tk.setTrangThaiTaiKhoan(status);
        return tk;
    }
}
