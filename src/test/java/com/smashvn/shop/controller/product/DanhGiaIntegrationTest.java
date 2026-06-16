package com.smashvn.shop.controller.product;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.*;
import com.smashvn.shop.service.product.DanhGiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class DanhGiaIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DanhGiaDAO danhGiaDAO;

    @Autowired
    private DanhGiaService danhGiaService;

    private MockMvc mockMvc;

    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private TaiKhoan testAdmin;

    private SanPham activeProduct;
    private SanPham inactiveProduct;
    private SanPhamChiTiet activeSpct;
    private SanPhamChiTiet inactiveSpct;

    private DonViVanChuyen testDvvc;
    private PhuongThucThanhToan testPttt;

    // A valid 1x1 transparent PNG byte array
    private static final byte[] TINY_PNG = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
        (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01,
        0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
        0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Seed customer user
        testUser = new TaiKhoan();
        testUser.setEmail("buyer_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");
        testUser.setLaKhachHang(true);
        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Nguyen");
        testKhachHang.setTenKh("Van A");
        testKhachHang.setSoDienThoaiKh("0987654321");
        testKhachHang = khachHangRepository.save(testKhachHang);

        // Seed admin user
        testAdmin = new TaiKhoan();
        testAdmin.setEmail("admin_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
        testAdmin.setMatKhau("adminpass");
        testAdmin.setVaiTro("QL");
        testAdmin.setTrangThai("hoat_dong");
        testAdmin.setLaQuanLy(true);
        testAdmin = taiKhoanRepository.save(testAdmin);

        // Seed Catalog & Brand
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Vợt Cầu Lông");
            return danhMucRepository.save(newDm);
        });

        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Yonex");
            return thuongHieuRepository.save(newTh);
        });

        // Seed Staff
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan nvUser = new TaiKhoan();
            nvUser.setEmail("staff_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
            nvUser.setMatKhau("pass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");
            nvUser.setLaNhanVien(true);
            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff");
            newNv.setChucVu("Nhân viên");
            newNv.setSoDienThoaiNv("0981112223");
            return nhanVienRepository.save(newNv);
        });

        // Seed Active Product
        activeProduct = new SanPham();
        activeProduct.setTenSanPham("Yonex Astrox 88D Pro");
        activeProduct.setTrangThai("dang_ban");
        activeProduct.setDanhMuc(dm);
        activeProduct.setThuongHieu(th);
        activeProduct.setNhanVien(nv);
        activeProduct.setSoDanhGia(0);
        activeProduct.setDiemTrungBinh(0.0);
        activeProduct = sanPhamRepository.save(activeProduct);

        activeSpct = new SanPhamChiTiet();
        activeSpct.setSanPham(activeProduct);
        activeSpct.setMauSac("Đỏ");
        activeSpct.setTrongLuong("4U");
        activeSpct.setMucCang("28 lbs");
        activeSpct.setSoLuongTon(50);
        activeSpct.setGiaBan(new BigDecimal("3500000"));
        activeSpct = sanPhamChiTietRepository.save(activeSpct);

        // Seed Inactive Product
        inactiveProduct = new SanPham();
        inactiveProduct.setTenSanPham("Yonex Nanoflare 800 (Ngừng bán)");
        inactiveProduct.setTrangThai("ngung_kinh_doanh");
        inactiveProduct.setDanhMuc(dm);
        inactiveProduct.setThuongHieu(th);
        inactiveProduct.setNhanVien(nv);
        inactiveProduct.setSoDanhGia(0);
        inactiveProduct.setDiemTrungBinh(0.0);
        inactiveProduct = sanPhamRepository.save(inactiveProduct);

        inactiveSpct = new SanPhamChiTiet();
        inactiveSpct.setSanPham(inactiveProduct);
        inactiveSpct.setMauSac("Xanh");
        inactiveSpct.setTrongLuong("3U");
        inactiveSpct.setMucCang("26 lbs");
        inactiveSpct.setSoLuongTon(10);
        inactiveSpct.setGiaBan(new BigDecimal("4000000"));
        inactiveSpct = sanPhamChiTietRepository.save(inactiveSpct);

        // Seed Carrier & Payment Method
        testDvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            DonViVanChuyen dv = new DonViVanChuyen();
            dv.setTenDonVi("Giao Hàng Nhanh");
            return donViVanChuyenDAO.save(dv);
        });

        testPttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan pt = new PhuongThucThanhToan();
            pt.setTenPhuongThuc("COD");
            return phuongThucThanhToanDAO.save(pt);
        });
    }

    private HoaDon createOrder(String orderStatus) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setTongTien(new BigDecimal("3500000"));
        hd.setPaymentStatus("PAID");
        hd.setTrangThaiDonHang(orderStatus);
        hd.setDiaChiNhan("123 Ha Noi");
        hd.setSdtNhan("0987654321");
        hd.setMaDonHang("HD_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setDonViVanChuyen(testDvvc);
        hd.setPhuongThucThanhToan(testPttt);
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(activeSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("3500000"));
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    void testReviewInactiveProduct_ShouldFail() throws Exception {
        mockMvc.perform(multipart("/san-pham/" + inactiveProduct.getId() + "/danh-gia")
                        .param("rating", "5")
                        .param("comment", "Sản phẩm tệ quá!")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Sản phẩm này hiện không còn hỗ trợ đánh giá."));
    }

    @Test
    void testReviewWithoutPurchase_ShouldFail() throws Exception {
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .param("rating", "5")
                        .param("comment", "Vợt ngon nha!")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công."));
    }

    @Test
    void testReviewWithUncompletedOrderStatus_ShouldFail() throws Exception {
        // Order with pending status (cho_xac_nhan)
        createOrder("cho_xac_nhan");

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .param("rating", "5")
                        .param("comment", "Vợt ngon nha!")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công."));
    }

    @Test
    void testReviewSuccessAndUniquenessPolicy() throws Exception {
        // Create completed order (da_giao)
        createOrder("da_giao");

        // First review submission
        MockMultipartFile image1 = new MockMultipartFile("fileAnh", "img1.png", "image/png", TINY_PNG);

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .file(image1)
                        .param("rating", "5")
                        .param("comment", "Vợt đánh rất đầm tay!")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Gửi đánh giá sản phẩm thành công!"));

        // Verify Database
        List<DanhGia> activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(1, activeReviews.size());
        DanhGia dg = activeReviews.get(0);
        assertEquals(5, dg.getSoSao());
        assertEquals("Vợt đánh rất đầm tay!", dg.getBinhLuan());
        assertEquals(1, dg.getDanhSachAnh().size());

        // Verify Cache Stats on SanPham
        SanPham updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(1, updatedSp.getSoDanhGia());
        assertEquals(5.0, updatedSp.getDiemTrungBinh());

        // --- SECOND SUBMISSION (Edit / Overwrite existing review) ---
        // We simulate a delay by changing the system rate limit parameters or wait, the rate limit would reject it!
        // To bypass the 30-second rate limiter during this test, we can manually change the last review's ngayDanhGia to 1 minute ago.
        dg.setNgayDanhGia(LocalDateTime.now().minusMinutes(1));
        dg.setNgayCapNhat(LocalDateTime.now().minusMinutes(1));
        danhGiaDAO.save(dg);

        MockMultipartFile image2 = new MockMultipartFile("fileAnh", "img2.png", "image/png", TINY_PNG);

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .file(image2)
                        .param("rating", "4")
                        .param("comment", "Cập nhật: Dùng lâu thấy hơi mỏi vai.")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Gửi đánh giá sản phẩm thành công!"));

        // Verify database still has only 1 review
        activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(1, activeReviews.size());
        DanhGia updatedDg = activeReviews.get(0);
        assertEquals(4, updatedDg.getSoSao());
        assertEquals("Cập nhật: Dùng lâu thấy hơi mỏi vai.", updatedDg.getBinhLuan());
        // Verify new image replaced the old one
        assertEquals(1, updatedDg.getDanhSachAnh().size());
        assertNotNull(updatedDg.getNgayCapNhat());

        // Verify Cache Stats on SanPham is updated
        updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(1, updatedSp.getSoDanhGia());
        assertEquals(4.0, updatedSp.getDiemTrungBinh());
    }

    @Test
    void testRateLimiting_ShouldBlock() throws Exception {
        createOrder("hoan_thanh");

        // Submit first review
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .param("rating", "5")
                        .param("comment", "Review 1")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Gửi đánh giá sản phẩm thành công!"));

        // Submit second review immediately (should fail because seconds < 30)
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .param("rating", "4")
                        .param("comment", "Review 2")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Bạn gửi yêu cầu quá nhanh! Vui lòng đợi ít nhất 30 giây giữa các lần đánh giá."));
    }

    @Test
    void testSoftDeleteReviewAndStatsCacheUpdate() throws Exception {
        createOrder("da_giao");

        // Submit a review
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .param("rating", "5")
                        .param("comment", "Đánh giá tốt")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        List<DanhGia> activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(1, activeReviews.size());
        DanhGia dg = activeReviews.get(0);

        // Perform Soft Delete by Admin
        mockMvc.perform(post("/admin/danh-gia/xoa/" + dg.getId())
                        .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Đã xóa mềm đánh giá thành công."));

        // Verify is soft deleted (daXoa = true)
        DanhGia softDeletedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(softDeletedDg.getDaXoa());
        assertNotNull(softDeletedDg.getNgayXoa());
        assertEquals(testAdmin.getId(), softDeletedDg.getNguoiXoa().getId());

        // Verify active reviews list is empty
        activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(0, activeReviews.size());

        // Verify cache rating on SanPham is reset to 0
        SanPham updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(0, updatedSp.getSoDanhGia());
        assertEquals(0.0, updatedSp.getDiemTrungBinh());
    }

    @Test
    void testSoftModeration_IndependentCommentAndImageHide() throws Exception {
        createOrder("da_giao");

        // Submit review with images
        MockMultipartFile image = new MockMultipartFile("fileAnh", "img.jpg", "image/jpeg", TINY_PNG);
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                        .file(image)
                        .param("rating", "5")
                        .param("comment", "Bình luận bậy bạ...")
                        .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        DanhGia dg = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);

        // 1. Hide Comment Only
        mockMvc.perform(post("/admin/danh-gia/an-binh-luan/" + dg.getId())
                        .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Đã ẩn nội dung bình luận thành công."));

        DanhGia updatedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(updatedDg.getAnBinhLuan());
        assertFalse(updatedDg.getAnHinhAnh()); // Independent image is still visible
        assertEquals(testAdmin.getId(), updatedDg.getNguoiAnBinhLuan().getId());
        assertNotNull(updatedDg.getNgayAnBinhLuan());

        // Rating Stats cache should NOT be updated by moderation action
        SanPham updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(1, updatedSp.getSoDanhGia());
        assertEquals(5.0, updatedSp.getDiemTrungBinh());

        // 2. Hide Image Only
        mockMvc.perform(post("/admin/danh-gia/an-hinh-anh/" + dg.getId())
                        .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Đã ẩn hình ảnh đánh giá thành công."));

        updatedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(updatedDg.getAnBinhLuan());
        assertTrue(updatedDg.getAnHinhAnh());
        assertEquals(testAdmin.getId(), updatedDg.getNguoiAnHinhAnh().getId());
        assertNotNull(updatedDg.getNgayAnHinhAnh());
    }
}
