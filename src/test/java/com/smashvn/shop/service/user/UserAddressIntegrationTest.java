package com.smashvn.shop.service.user;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class UserAddressIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private SoDiaChiRepository soDiaChiRepository;

    private MockMvc mockMvc;
    private TaiKhoan testUser;
    private KhachHang testKhachHang;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Seed customer user
        testUser = new TaiKhoan();
        testUser.setEmail("address_tester@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Address");
        testKhachHang.setTenKh("Tester");
        testKhachHang.setSoDienThoaiKh("0987654321");
        testKhachHang = khachHangRepository.save(testKhachHang);
    }

    @Test
    void testAddAddress_Success_And_Sanitization() throws Exception {
        // Post address containing XSS payload
        mockMvc.perform(post("/user/address/add")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "<script>alert(1)</script>Nguyễn")
                        .param("tenNguoiNhan", "<img src=x onerror=alert(2)>Đặng")
                        .param("sdtNguoiNhan", "0912345678")
                        .param("diaChiCuThe", "123 Đường Láng <iframe src=\"javascript:alert(3)\"></iframe>")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam")
                        .param("latitude", "21.0285")
                        .param("longitude", "105.8542")
                        .param("defaultAddress", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/address"))
                .andExpect(flash().attribute("thongBaoThanhCong", "Đã thêm địa chỉ mới thành công!"));

        List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(testKhachHang.getId());
        assertFalse(addresses.isEmpty());
        SoDiaChi saved = addresses.get(0);

        // Verify HTML tags are sanitized and pure texts are preserved
        assertEquals("Nguyễn", saved.getHoNguoiNhan());
        assertEquals("Đặng", saved.getTenNguoiNhan());
        assertEquals("0912345678", saved.getSdtNguoiNhan());
        assertEquals("123 Đường Láng", saved.getDiaChiCuThe());
        assertEquals("Hà Nội", saved.getTinhThanh());
        assertEquals("Việt Nam", saved.getQuocGia());
        assertEquals(21.0285, saved.getLatitude());
        assertEquals(105.8542, saved.getLongitude());
        assertTrue(saved.isDefaultShipping());
    }

    @Test
    void testAddAddress_EmptyInput_Rejected() throws Exception {
        // Empty Họ người nhận
        mockMvc.perform(post("/user/address/add")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "")
                        .param("tenNguoiNhan", "Đặng")
                        .param("sdtNguoiNhan", "0912345678")
                        .param("diaChiCuThe", "123 Đường Láng")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam"))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-address-add"))
                .andExpect(model().attributeExists("loi"));

        // All spaces Họ người nhận
        mockMvc.perform(post("/user/address/add")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "   ")
                        .param("tenNguoiNhan", "Đặng")
                        .param("sdtNguoiNhan", "0912345678")
                        .param("diaChiCuThe", "123 Đường Láng")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam"))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-address-add"))
                .andExpect(model().attributeExists("loi"));
    }

    @Test
    void testAddAddress_InvalidPhone_Rejected() throws Exception {
        // Missing digits
        mockMvc.perform(post("/user/address/add")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "Nguyễn")
                        .param("tenNguoiNhan", "Đặng")
                        .param("sdtNguoiNhan", "09123456")
                        .param("diaChiCuThe", "123 Đường Láng")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam"))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-address-add"))
                .andExpect(model().attributeExists("loi"));

        // Invalid prefix
        mockMvc.perform(post("/user/address/add")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "Nguyễn")
                        .param("tenNguoiNhan", "Đặng")
                        .param("sdtNguoiNhan", "1234567890")
                        .param("diaChiCuThe", "123 Đường Láng")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam"))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-address-add"))
                .andExpect(model().attributeExists("loi"));
    }

    @Test
    void testAddAddress_Overlength_Rejected() throws Exception {
        String tooLongName = "A".repeat(51);

        mockMvc.perform(post("/user/address/add")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", tooLongName)
                        .param("tenNguoiNhan", "Đặng")
                        .param("sdtNguoiNhan", "0912345678")
                        .param("diaChiCuThe", "123 Đường Láng")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam"))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-address-add"))
                .andExpect(model().attributeExists("loi"));
    }

    @Test
    void testEditAddress_Success() throws Exception {
        // Create an address first
        SoDiaChi dc = new SoDiaChi();
        dc.setKhachHang(testKhachHang);
        dc.setHoNguoiNhan("Trần");
        dc.setTenNguoiNhan("Nam");
        dc.setSdtNguoiNhan("0987654321");
        dc.setDiaChiCuThe("456 Cầu Giấy");
        dc.setTinhThanh("Hà Nội");
        dc.setThanhPho("Hà Nội");
        dc.setQuocGia("Việt Nam");
        dc.setMaBuuDien("700000");
        dc = soDiaChiRepository.save(dc);

        mockMvc.perform(post("/user/address/edit/" + dc.getId())
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "Lê")
                        .param("tenNguoiNhan", "Lợi")
                        .param("sdtNguoiNhan", "0987654322")
                        .param("diaChiCuThe", "789 Kim Mã")
                        .param("tinhThanh", "Hà Nội")
                        .param("quocGia", "Việt Nam")
                        .param("defaultAddress", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/address"))
                .andExpect(flash().attribute("thongBaoThanhCong", "Cập nhật địa chỉ thành công!"));

        SoDiaChi updated = soDiaChiRepository.findById(dc.getId()).orElseThrow();
        assertEquals("Lê", updated.getHoNguoiNhan());
        assertEquals("Lợi", updated.getTenNguoiNhan());
        assertEquals("0987654322", updated.getSdtNguoiNhan());
        assertEquals("789 Kim Mã", updated.getDiaChiCuThe());
    }

    @Test
    void testEditAddress_Unauthorized_Rejected() throws Exception {
        // Create another user
        TaiKhoan otherUser = new TaiKhoan();
        otherUser.setEmail("other@gmail.com");
        otherUser.setMatKhau("pass123");
        otherUser.setVaiTro("KH");
        otherUser.setTrangThai("hoat_dong");

        otherUser = taiKhoanRepository.save(otherUser);

        KhachHang otherKh = new KhachHang();
        otherKh.setTaiKhoan(otherUser);
        otherKh.setHoKh("Other");
        otherKh.setTenKh("User");
        otherKh.setSoDienThoaiKh("0911111111");
        otherKh = khachHangRepository.save(otherKh);

        // Address owned by other user
        SoDiaChi otherAddress = new SoDiaChi();
        otherAddress.setKhachHang(otherKh);
        otherAddress.setHoNguoiNhan("Stranger");
        otherAddress.setTenNguoiNhan("Danger");
        otherAddress.setSdtNguoiNhan("0911111111");
        otherAddress.setDiaChiCuThe("Stranger address");
        otherAddress.setTinhThanh("Hải Phòng");
        otherAddress.setThanhPho("Hải Phòng");
        otherAddress.setQuocGia("Việt Nam");
        otherAddress.setMaBuuDien("100000");
        otherAddress = soDiaChiRepository.save(otherAddress);

        // Try to edit otherAddress using testUser's session
        mockMvc.perform(post("/user/address/edit/" + otherAddress.getId())
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .param("hoNguoiNhan", "Hacker")
                        .param("tenNguoiNhan", "Target")
                        .param("sdtNguoiNhan", "0987654321")
                        .param("diaChiCuThe", "Malicious attempt")
                        .param("tinhThanh", "Hải Phòng")
                        .param("quocGia", "Việt Nam"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/address"))
                .andExpect(flash().attributeExists("thongBaoLoi"));

        // Verify address remains unchanged in DB
        SoDiaChi unchanged = soDiaChiRepository.findById(otherAddress.getId()).orElseThrow();
        assertEquals("Stranger", unchanged.getHoNguoiNhan());
    }
}

