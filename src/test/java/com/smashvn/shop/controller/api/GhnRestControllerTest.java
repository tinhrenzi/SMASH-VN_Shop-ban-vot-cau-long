package com.smashvn.shop.controller.api;

import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GhnRestControllerTest {

    @Mock
    private GhnService ghnService;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Mock
    private OrderViewService orderViewService;

    @Mock
    private GhnConfig ghnConfig;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        GhnRestController controller = new GhnRestController(
                ghnService,
                hoaDonRepository,
                hoaDonChiTietRepository,
                orderViewService,
                ghnConfig,
                taiKhoanRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void activeOwnerCanTrackOwnOrder() throws Exception {
        TaiKhoan owner = activeCustomer(1);
        HoaDon order = orderOwnedBy(10, owner, "GHN123");
        when(taiKhoanRepository.findById(1)).thenReturn(Optional.of(owner));
        when(hoaDonRepository.findById(10)).thenReturn(Optional.of(order));
        when(ghnService.trackOrder("GHN123")).thenReturn(Map.of("status", "delivering"));

        mockMvc.perform(get("/api/ghn/track/order/10").sessionAttr("idNguoiDung", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.ghnOrderCode").value("GHN123"))
                .andExpect(jsonPath("$.data.status").value("delivering"));

        verify(ghnService).trackOrder("GHN123");
    }

    @Test
    void activeNonOwnerCannotTrackAnotherUsersOrder() throws Exception {
        TaiKhoan requester = activeCustomer(2);
        TaiKhoan owner = activeCustomer(1);
        HoaDon order = orderOwnedBy(10, owner, "GHN123");
        when(taiKhoanRepository.findById(2)).thenReturn(Optional.of(requester));
        when(hoaDonRepository.findById(10)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/ghn/track/order/10").sessionAttr("idNguoiDung", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"));

        verify(ghnService, never()).trackOrder("GHN123");
    }

    @Test
    void guestCannotTrackAnyOrder() throws Exception {
        TaiKhoan guest = activeCustomer(3);
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        when(taiKhoanRepository.findById(3)).thenReturn(Optional.of(guest));

        mockMvc.perform(get("/api/ghn/track/order/10").sessionAttr("idNguoiDung", 3))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"));

        verifyNoInteractions(hoaDonRepository);
        verifyNoInteractions(ghnService);
    }

    @Test
    void unauthenticatedSessionCannotTrackAnyOrder() throws Exception {
        mockMvc.perform(get("/api/ghn/track/order/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"));

        verifyNoInteractions(taiKhoanRepository);
        verifyNoInteractions(hoaDonRepository);
        verifyNoInteractions(ghnService);
    }

    @Test
    void activeOwnerGetsNotFoundWhenOrderDoesNotExist() throws Exception {
        TaiKhoan owner = activeCustomer(1);
        when(taiKhoanRepository.findById(1)).thenReturn(Optional.of(owner));
        when(hoaDonRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ghn/track/order/999").sessionAttr("idNguoiDung", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));

        verifyNoInteractions(ghnService);
    }

    @Test
    void inactiveCustomerCannotTrackAnyOrder() throws Exception {
        TaiKhoan inactive = activeCustomer(4);
        inactive.setTrangThai("khoa");
        inactive.setTrangThaiTaiKhoan(AccountStatus.LOCKED);
        when(taiKhoanRepository.findById(4)).thenReturn(Optional.of(inactive));

        mockMvc.perform(get("/api/ghn/track/order/10").sessionAttr("idNguoiDung", 4))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"));

        verifyNoInteractions(hoaDonRepository);
        verifyNoInteractions(ghnService);
    }

    private TaiKhoan activeCustomer(Integer id) {
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setId(id);
        taiKhoan.setUsername("customer" + id + "@example.com");
        taiKhoan.setVaiTro("KH");
        taiKhoan.setTrangThai("hoat_dong");
        taiKhoan.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        return taiKhoan;
    }

    private HoaDon orderOwnedBy(Integer orderId, TaiKhoan owner, String ghnOrderCode) {
        KhachHang khachHang = new KhachHang();
        khachHang.setId(owner.getId() + 100);
        khachHang.setTaiKhoan(owner);

        HoaDon order = new HoaDon();
        order.setId(orderId);
        order.setKhachHang(khachHang);
        order.setGhnOrderCode(ghnOrderCode);
        order.setTrangThaiDonHang("dang_giao");
        return order;
    }
}
