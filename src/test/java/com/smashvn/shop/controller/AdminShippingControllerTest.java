package com.smashvn.shop.controller;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AdminShippingService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminShippingControllerTest {

    @Mock
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private AdminShippingService adminShippingService;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    private AdminShippingController adminShippingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminShippingController = new AdminShippingController(donViVanChuyenDAO, taiKhoanRepository, adminShippingService);
    }

    @Test
    void testViewConfig_GHNExists() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setLaQuanLy(true);
        when(taiKhoanRepository.findById(1)).thenReturn(Optional.of(tk));

        List<DonViVanChuyen> carriers = new ArrayList<>();
        DonViVanChuyen ghn = new DonViVanChuyen();
        ghn.setId(1);
        ghn.setTenDonVi("Giao Hàng Nhanh (GHN)");
        ghn.setHotline("1900636677");
        ghn.setWebsite("https://ghn.vn");
        ghn.setToken("mock-token");
        ghn.setClientId("mock-client-id");
        ghn.setDiaChiKho("Thai Nguyen");
        carriers.add(ghn);

        when(adminShippingService.getAllCarriers()).thenReturn(carriers);

        Model model = new ConcurrentModel();
        String view = adminShippingController.viewConfig(model, session);

        assertEquals("admin/shipping-config", view);
        assertTrue(model.containsAttribute("ghn"));
        assertTrue(model.containsAttribute("isManager"));
        assertEquals(true, model.getAttribute("isManager"));
        
        DonViVanChuyen modelGhn = (DonViVanChuyen) model.getAttribute("ghn");
        assertEquals(1, modelGhn.getId());
        assertEquals("mock-token", modelGhn.getToken());
        assertEquals("mock-client-id", modelGhn.getClientId());
        assertEquals("Thai Nguyen", modelGhn.getDiaChiKho());
    }

    @Test
    void testViewConfig_GHNMissing_CreatesFallback() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setLaQuanLy(true);
        when(taiKhoanRepository.findById(1)).thenReturn(Optional.of(tk));

        when(adminShippingService.getAllCarriers()).thenReturn(new ArrayList<>());
        
        // Mock save returning the same entity
        when(donViVanChuyenDAO.save(any(DonViVanChuyen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Model model = new ConcurrentModel();
        String view = adminShippingController.viewConfig(model, session);

        assertEquals("admin/shipping-config", view);
        verify(donViVanChuyenDAO).save(any(DonViVanChuyen.class));
        
        DonViVanChuyen modelGhn = (DonViVanChuyen) model.getAttribute("ghn");
        assertNotNull(modelGhn);
        assertEquals("Giao Hàng Nhanh (GHN)", modelGhn.getTenDonVi());
    }

    @Test
    void testSaveConfig() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminShippingController.saveConfig(1, "new-token", "new-client", "new-warehouse", session, request, redirectAttributes);

        assertEquals("redirect:/admin/shipping-config", view);
        verify(adminShippingService).updateGhnConfig(1, "new-token", "new-client", "new-warehouse", 1, "127.0.0.1");
        assertEquals("Cập nhật kết nối GHN thành công!", redirectAttributes.getFlashAttributes().get("successMsg"));
    }

    @Test
    void testSaveFees() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminShippingController.saveFees(1, BigDecimal.valueOf(15000), BigDecimal.valueOf(35000), 0L, session, request, redirectAttributes);

        assertEquals("redirect:/admin/shipping-config", view);
        verify(adminShippingService).updateShippingFee(eq(1), eq(BigDecimal.valueOf(15000)), eq(BigDecimal.valueOf(35000)), eq(0L), eq(1), eq("127.0.0.1"));
        assertEquals("Cập nhật phí vận chuyển thủ công thành công!", redirectAttributes.getFlashAttributes().get("successMsg"));
    }
}
