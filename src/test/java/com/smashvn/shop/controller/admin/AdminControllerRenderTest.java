package com.smashvn.shop.controller.admin;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class AdminControllerRenderTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Test
    public void testDonHangRender() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");
        
        mockMvc.perform(get("/admin/don-hang")
                .requestAttr("_csrf", csrfToken)
                .sessionAttr("vaiTro", "QL")
                .sessionAttr("laKhachHang", false)
                .sessionAttr("laNhanVien", false)
                .sessionAttr("laQuanLy", true))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void testGetOrderDetailJson_Unauthorized() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvc.perform(get("/admin/don-hang/detail-json").param("id", "999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetOrderDetailJson_ForbiddenForCustomer() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TaiKhoan customer = new TaiKhoan();
        customer.setEmail("customer_test@smashvn.com");
        customer.setMatKhau("pass123");
        customer.setVaiTro("KH");
        customer.setLaKhachHang(true);
        customer = taiKhoanRepository.save(customer);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                .param("id", "999")
                .sessionAttr("idNguoiDung", customer.getId())
                .sessionAttr("vaiTro", "KH"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetOrderDetailJson_NotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TaiKhoan staff = new TaiKhoan();
        staff.setEmail("staff_test@smashvn.com");
        staff.setMatKhau("pass123");
        staff.setVaiTro("NV");
        staff.setLaNhanVien(true);
        staff = taiKhoanRepository.save(staff);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                .param("id", "999999")
                .sessionAttr("idNguoiDung", staff.getId())
                .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetOrderDetailJson_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Seed staff
        TaiKhoan staff = new TaiKhoan();
        staff.setEmail("staff_test_ok@smashvn.com");
        staff.setMatKhau("pass123");
        staff.setVaiTro("NV");
        staff.setLaNhanVien(true);
        staff = taiKhoanRepository.save(staff);

        // Seed associations
        DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("Mua tại quầy");
            d.setHotline("000000");
            return donViVanChuyenDAO.save(d);
        });

        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setTenPhuongThuc("Tiền mặt");
            return phuongThucThanhToanDAO.save(p);
        });

        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan guestTk = taiKhoanRepository.findByEmail("guest@smashvn.com");
            if (guestTk == null) {
                TaiKhoan tk = new TaiKhoan();
                tk.setEmail("guest@smashvn.com");
                tk.setMatKhau("pass123");
                tk.setVaiTro("KH");
                tk.setTrangThai("hoat_dong");
                guestTk = taiKhoanRepository.save(tk);
            }
            KhachHang customer = new KhachHang();
            customer.setTaiKhoan(guestTk);
            customer.setHoKh("Khách");
            customer.setTenKh("Lẻ");
            customer.setSoDienThoaiKh("0000000000");
            customer.setNhanBanTin(false);
            customer.setLaTaiKhoanNoiBo(false);
            return khachHangRepository.save(customer);
        });

        // Seed POS order
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HDSVN20260619-T100");
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(new BigDecimal("300000"));
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setNguoiXacNhanThanhToan("Staff Tester");
        hd.setThoiGianXacNhan(LocalDateTime.now());
        hd.setDiaChiNhan("Bán tại quầy");
        hd.setSdtNhan("0000000000");
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setTrangThaiDonHang("da_giao");
        hd.setDonViVanChuyen(dvvc);
        hd.setPhuongThucThanhToan(pttt);
        hd.setKhachHang(kh);
        hd = hoaDonRepository.save(hd);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                .param("id", String.valueOf(hd.getId()))
                .sessionAttr("idNguoiDung", staff.getId())
                .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hd.getId()))
                .andExpect(jsonPath("$.maDonHang").value("HDSVN20260619-T100"))
                .andExpect(jsonPath("$.nguoiXacNhan").value("Staff Tester"))
                .andExpect(jsonPath("$.thoiGianXacNhan").isNotEmpty());
    }
}
