package com.smashvn.shop.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.user.UserAddressService;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CheckoutControllerTest {

    @Mock
    private GioHangService gioHangService;

    @Mock
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private HttpSession session;

    @Mock
    private com.smashvn.shop.config.SepayConfig sepayConfig;

    @Mock
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Mock
    private com.smashvn.shop.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Mock
    private com.smashvn.shop.service.product.PricingService pricingService;

    @Mock
    private com.smashvn.shop.service.order.GuestCartService guestCartService;

    @Mock
    private com.smashvn.shop.service.order.GuestCheckoutService guestCheckoutService;

    @Mock
    private com.smashvn.shop.service.user.UserDangNhapService userDangNhapService;

    @Mock
    private com.smashvn.shop.repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    @Mock
    private com.smashvn.shop.repository.TaiKhoanRepository taiKhoanRepository;

    @Mock
    private com.smashvn.shop.repository.TokenKhoiPhucRepository tokenKhoiPhucRepository;

    @Mock
    private com.smashvn.shop.repository.SoDiaChiRepository soDiaChiRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CheckoutController checkoutController;

    private List<GioHangChiTiet> mockCartItems;
    private List<DonViVanChuyen> mockDvvcs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        checkoutController = new CheckoutController(
                gioHangService, 
                donViVanChuyenDAO, 
                userAddressService, 
                sepayConfig, 
                khachHangRepository, 
                phieuGiamGiaRepository, 
                pricingService,
                guestCartService,
                guestCheckoutService,
                userDangNhapService,
                sanPhamChiTietRepository,
                taiKhoanRepository,
                tokenKhoiPhucRepository,
                soDiaChiRepository
        );

        when(pricingService.calculateCurrentSellingPrice(any())).thenAnswer(invocation -> {
            SanPhamChiTiet arg = invocation.getArgument(0);
            return arg != null && arg.getGiaBan() != null ? arg.getGiaBan() : BigDecimal.ZERO;
        });

        com.smashvn.shop.entity.KhachHang kh = new com.smashvn.shop.entity.KhachHang();
        kh.setId(123);
        when(khachHangRepository.findByTaiKhoan_Id(123)).thenReturn(kh);

        when(session.getAttribute("idNguoiDung")).thenReturn(123);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(123);
        tk.setUsername("active@example.com");
        tk.setMatKhau("password123");
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        when(taiKhoanRepository.findById(123)).thenReturn(java.util.Optional.of(tk));

        mockCartItems = new ArrayList<>();
        GioHangChiTiet item = new GioHangChiTiet();
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSoLuongTon(10);
        spct.setGiaBan(new BigDecimal("100000"));
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Yonex");
        sp.setTrangThai("dang_ban");
        spct.setSanPham(sp);
        item.setSanPhamChiTiet(spct);
        item.setSoLuong(2);
        mockCartItems.add(item);

        mockDvvcs = new ArrayList<>();
        DonViVanChuyen dvvc = new DonViVanChuyen();
        dvvc.setId(1);
        dvvc.setTenDonVi("Giao Hàng Nhanh");
        mockDvvcs.add(dvvc);

        when(gioHangService.layDanhSachSanPhamTrongGio(123)).thenReturn(mockCartItems);
        when(donViVanChuyenDAO.findAll()).thenReturn(mockDvvcs);
    }

    @Test
    void testViewCheckout_NoSavedAddresses() {
        when(userAddressService.layDanhSachDiaChi(123)).thenReturn(new ArrayList<>());

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout(session, model);

        assertEquals("checkout", view);
        assertTrue(model.containsAttribute("listDiaChi"));
        assertEquals(false, model.getAttribute("hasDefaultAddress"));
        assertEquals("{}", model.getAttribute("addressMapJson"));

        List<?> listDiaChi = (List<?>) model.getAttribute("listDiaChi");
        assertTrue(listDiaChi.isEmpty());
    }

    @Test
    void testViewCheckout_OneSavedAddress_NoDefault() throws Exception {
        List<SoDiaChi> addresses = new ArrayList<>();
        SoDiaChi dc = new SoDiaChi();
        dc.setId(10);
        dc.setHoNguoiNhan("Nguyen");
        dc.setTenNguoiNhan("An");
        dc.setSdtNguoiNhan("0987654321");
        dc.setDiaChiCuThe("123 Duong ABC");
        dc.setTinhThanh("Ha Noi");
        dc.setQuocGia("Viet Nam");
        dc.setDefaultShipping(false);
        addresses.add(dc);

        when(userAddressService.layDanhSachDiaChi(123)).thenReturn(addresses);

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout(session, model);

        assertEquals("checkout", view);
        assertEquals(false, model.getAttribute("hasDefaultAddress"));

        String jsonStr = (String) model.getAttribute("addressMapJson");
        Map<?, ?> addressMap = objectMapper.readValue(jsonStr, Map.class);
        assertEquals(1, addressMap.size());
        
        Map<?, ?> details = (Map<?, ?>) addressMap.get("10");
        assertNotNull(details);
        assertEquals("Nguyen An", details.get("hoTen"));
        assertEquals("0987654321", details.get("sdt"));
        assertEquals("123 Duong ABC, Ha Noi, Viet Nam", details.get("diaChi"));
    }

    @Test
    void testViewCheckout_MultipleSavedAddresses_WithDefault() throws Exception {
        List<SoDiaChi> addresses = new ArrayList<>();
        
        SoDiaChi dc1 = new SoDiaChi();
        dc1.setId(10);
        dc1.setHoNguoiNhan("Nguyen");
        dc1.setTenNguoiNhan("An");
        dc1.setSdtNguoiNhan("0987654321");
        dc1.setDiaChiCuThe("123 Duong ABC");
        dc1.setTinhThanh("Ha Noi");
        dc1.setQuocGia("Viet Nam");
        dc1.setDefaultShipping(true);
        addresses.add(dc1);

        SoDiaChi dc2 = new SoDiaChi();
        dc2.setId(11);
        dc2.setHoNguoiNhan("Tran");
        dc2.setTenNguoiNhan("Binh");
        dc2.setSdtNguoiNhan("0912345678");
        dc2.setDiaChiCuThe("456 Duong XYZ");
        dc2.setTinhThanh("HCM");
        dc2.setQuocGia("Viet Nam");
        dc2.setDefaultShipping(false);
        addresses.add(dc2);

        when(userAddressService.layDanhSachDiaChi(123)).thenReturn(addresses);

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout(session, model);

        assertEquals("checkout", view);
        assertEquals(true, model.getAttribute("hasDefaultAddress"));

        String jsonStr = (String) model.getAttribute("addressMapJson");
        Map<?, ?> addressMap = objectMapper.readValue(jsonStr, Map.class);
        assertEquals(2, addressMap.size());

        Map<?, ?> details1 = (Map<?, ?>) addressMap.get("10");
        assertEquals("Nguyen An", details1.get("hoTen"));

        Map<?, ?> details2 = (Map<?, ?>) addressMap.get("11");
        assertEquals("Tran Binh", details2.get("hoTen"));
    }

    @Test
    void guestCheckoutPageDoesNotLoadSavedAddresses() {
        Integer guestAccountId = 456;
        when(session.getAttribute("idNguoiDung")).thenReturn(guestAccountId);

        TaiKhoan guest = new TaiKhoan();
        guest.setId(guestAccountId);
        guest.setUsername("guest@example.com");
        guest.setVaiTro("KH");
        guest.setTrangThai("hoat_dong");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        when(taiKhoanRepository.findById(guestAccountId)).thenReturn(java.util.Optional.of(guest));

        com.smashvn.shop.service.order.GuestCartService.GuestCartItem guestItem =
                new com.smashvn.shop.service.order.GuestCartService.GuestCartItem(99, 1);
        when(guestCartService.getGuestCartItems(session)).thenReturn(java.util.List.of(guestItem));

        SanPham sp = new SanPham();
        sp.setTenSanPham("Guest Product");
        sp.setTrangThai("dang_ban");
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(99);
        spct.setSoLuongTon(5);
        spct.setGiaBan(new BigDecimal("120000"));
        spct.setSanPham(sp);
        when(sanPhamChiTietRepository.findById(99)).thenReturn(java.util.Optional.of(spct));

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout(session, model);

        assertEquals("checkout", view);
        assertEquals(true, model.getAttribute("isGuest"));
        assertTrue(((List<?>) model.getAttribute("listDiaChi")).isEmpty());
        assertEquals("{}", model.getAttribute("addressMapJson"));
        verify(userAddressService, never()).layDanhSachDiaChi(anyInt());
        verify(gioHangService, never()).layDanhSachSanPhamTrongGio(guestAccountId);
        verify(gioHangService, never()).cleanPendingOrders(guestAccountId);
    }
}
