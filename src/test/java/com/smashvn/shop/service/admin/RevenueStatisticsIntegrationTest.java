package com.smashvn.shop.service.admin;
import com.smashvn.shop.service.admin.AdminThongKeService;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RevenueStatisticsIntegrationTest {

    @Autowired
    private AdminThongKeService adminThongKeService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private KhachHang testKhachHang;
    private PhuongThucThanhToan ptttCOD;
    private PhuongThucThanhToan ptttOnline;
    private DonViVanChuyen testDvvc;
    private SanPhamChiTiet testSpct;

    @BeforeEach
    void setUp() {
        // Find / Create customer
        List<KhachHang> khs = khachHangRepository.findAll();
        if (khs.isEmpty()) {
            TaiKhoan customerUser = new TaiKhoan();
            customerUser.setUsername("customer-stats-test-" + System.nanoTime() + "@smashvn.com");
            customerUser.setMatKhau("123");
            customerUser.setVaiTro("KH");

            customerUser = taiKhoanRepository.save(customerUser);

            KhachHang newKh = new KhachHang();
            newKh.setHoKh("Test");
            newKh.setTenKh("Customer");
            newKh.setSoDienThoaiKh("0912345678");
            newKh.setLaTaiKhoanNoiBo(false);
            newKh.setTaiKhoan(customerUser);
            testKhachHang = khachHangRepository.save(newKh);
        } else {
            testKhachHang = khs.get(0);
        }

        // Find / Create payment methods
        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        ptttCOD = ptts.stream()
                .filter(p -> "COD".equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan p = new PhuongThucThanhToan();
                    p.setTenPhuongThuc("COD");
                    return phuongThucThanhToanDAO.save(p);
                });

        ptttOnline = ptts.stream()
                .filter(p -> !"COD".equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan p = new PhuongThucThanhToan();
                    p.setTenPhuongThuc("SEPAY");
                    return phuongThucThanhToanDAO.save(p);
                });

        // Find / Create shipping carrier
        List<DonViVanChuyen> dvvcs = donViVanChuyenDAO.findAll();
        if (dvvcs.isEmpty()) {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("GHTK");
            d.setHotline("19001000");
            d.setWebsite("ghtk.vn");
            testDvvc = donViVanChuyenDAO.save(d);
        } else {
            testDvvc = dvvcs.get(0);
        }

        // Seed NhanVien for product
        TaiKhoan nvUser = new TaiKhoan();
        nvUser.setUsername("staff-stats-" + System.nanoTime() + "@smashvn.com");
        nvUser.setMatKhau("123");
        nvUser.setVaiTro("NV");

        nvUser = taiKhoanRepository.save(nvUser);
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(nvUser);
        nv.setHoTenNv("Stats Staff");
        nv.setChucVu("Nhân viên");
        nv.setSoDienThoaiNv("0888999111");
        nv = nhanVienRepository.save(nv);

        // Seed DanhMuc / ThuongHieu / SanPham / SanPhamChiTiet
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc d = new DanhMuc();
            d.setTenDanhMuc("Mặc định");
            return danhMucRepository.save(d);
        });
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu t = new ThuongHieu();
            t.setTenThuongHieu("Mặc định");
            return thuongHieuRepository.save(t);
        });
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Stats " + System.nanoTime());
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);
        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Đen");
        testSpct.setSoLuongTon(50);
        testSpct.setGiaBan(BigDecimal.valueOf(300000));
        testSpct = sanPhamChiTietRepository.save(testSpct);

        // Clear statistics cache to ensure clean test environment
        if (cacheManager != null && cacheManager.getCache("thongke") != null) {
            cacheManager.getCache("thongke").clear();
        }
    }

    private HoaDon createTestOrder(String status, String paymentMethod, String paymentStatus, PhuongThucThanhToan pttt, BigDecimal amount) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(testDvvc);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(amount);
        hd.setMaDonHang("TEST-STATS-" + System.nanoTime());
        hd.setTrangThaiDonHang(status);
        hd.setPaymentMethod(paymentMethod);
        hd.setPaymentStatus(paymentStatus);
        
        String tStatus = "CHO_THANH_TOAN";
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            tStatus = "DA_THANH_TOAN";
        } else if ("refunded".equalsIgnoreCase(paymentStatus)) {
            tStatus = "REFUNDED";
        } else if ("cancelled".equalsIgnoreCase(paymentStatus)) {
            tStatus = "HUY";
        }
        hd.setTrangThaiThanhToan(tStatus);
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(amount);
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    void testRevenueClassificationRules() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(2);
        LocalDateTime end = LocalDateTime.now().plusMinutes(2);

        // Fetch initial statistics to isolate from database pollution
        Map<String, Object> initialStats = adminThongKeService.getStatisticsData(start, end);
        BigDecimal initialActualRevenue = (BigDecimal) initialStats.get("actualRevenue");
        BigDecimal initialExpectedRevenue = (BigDecimal) initialStats.get("expectedRevenue");
        BigDecimal initialRefundedRevenue = (BigDecimal) initialStats.get("refundedRevenue");

        // 1. Projected Revenue Inclusions
        // PAID + cho_xac_nhan => Projected Revenue (Only for COD. Non-COD is excluded)
        createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(100000));
        // PAID + da_xac_nhan => Projected Revenue (Only for COD. Non-COD is excluded)
        createTestOrder("da_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(110000));
        // PAID + dang_lay_hang => Projected Revenue (Only for COD. Non-COD is excluded)
        createTestOrder("dang_lay_hang", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(120000));
        // PAID + dang_giao => Projected Revenue
        createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(130000));

        // COD + active states => Projected Revenue (COD orders are counted for all active states)
        createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, BigDecimal.valueOf(50000));
        createTestOrder("da_xac_nhan", "COD", "pending", ptttCOD, BigDecimal.valueOf(60000));
        createTestOrder("dang_lay_hang", "COD", "pending", ptttCOD, BigDecimal.valueOf(70000));
        createTestOrder("dang_giao", "COD", "pending", ptttCOD, BigDecimal.valueOf(80000));

        // 2. Actual Revenue Inclusions
        // DA_GIAO => Actual Revenue
        createTestOrder("da_giao", "COD", "paid", ptttCOD, BigDecimal.valueOf(200000));
        // HOAN_THANH => Actual Revenue
        createTestOrder("hoan_thanh", "COD", "paid", ptttCOD, BigDecimal.valueOf(250000));

        // 3. Cancel before delivery
        // PAID + DA_HUY => removed from Projected Revenue, does not affect Actual Revenue
        createTestOrder("da_huy", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(300000));

        // 4. Delivered then Refunded
        // DA_GIAO + REFUNDED => negative Actual Revenue contribution (reversal)
        HoaDon refundedOrder = createTestOrder("da_giao", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(400000));
        refundedOrder.setRefundStatus(RefundStatus.COMPLETED);
        hoaDonRepository.save(refundedOrder);

        // Clear statistics cache before second fetch to bypass @Cacheable
        if (cacheManager != null && cacheManager.getCache("thongke") != null) {
            cacheManager.getCache("thongke").clear();
        }

        // Calculate statistics
        Map<String, Object> stats = adminThongKeService.getStatisticsData(start, end);

        BigDecimal actualRevenue = (BigDecimal) stats.get("actualRevenue");
        BigDecimal expectedRevenue = (BigDecimal) stats.get("expectedRevenue");
        BigDecimal refundedRevenue = (BigDecimal) stats.get("refundedRevenue");

        BigDecimal diffActual = actualRevenue.subtract(initialActualRevenue != null ? initialActualRevenue : BigDecimal.ZERO);
        BigDecimal diffExpected = expectedRevenue.subtract(initialExpectedRevenue != null ? initialExpectedRevenue : BigDecimal.ZERO);
        BigDecimal diffRefunded = refundedRevenue.subtract(initialRefundedRevenue != null ? initialRefundedRevenue : BigDecimal.ZERO);

        // Projected Revenue should sum the dang_giao PAID undelivered order (130k) + active COD orders (50k + 60k + 70k + 80k = 260k) = 390k
        assertEquals(0, BigDecimal.valueOf(390000).compareTo(diffExpected), "Projected revenue does not match expected");

        // Actual Revenue should sum delivered orders (200k + 250k = 450k) minus refunded delivered order (400k) = 50k
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(diffActual), "Actual revenue does not match expected");

        // Refunded Revenue should be 400k
        assertEquals(0, BigDecimal.valueOf(400000).compareTo(diffRefunded), "Refunded revenue does not match expected");

        // Verify chart values sum matches Actual Revenue
        List<BigDecimal> chartValues = (List<BigDecimal>) stats.get("chartValues");
        BigDecimal chartSum = chartValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, actualRevenue.compareTo(chartSum), "Chart sum does not match actual revenue");
    }
}
