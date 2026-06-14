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
            KhachHang newKh = new KhachHang();
            newKh.setHoKh("Test");
            newKh.setTenKh("Customer");
            newKh.setSoDienThoaiKh("0912345678");
            newKh.setLaTaiKhoanNoiBo(false);
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

        // Fetch / Create variant
        List<SanPhamChiTiet> spcts = sanPhamChiTietRepository.findAll();
        assertFalse(spcts.isEmpty(), "SanPhamChiTiet table must not be empty for integration tests");
        testSpct = spcts.get(0);
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

        // 1. Projected Revenue Inclusions
        // PAID + cho_xac_nhan => Projected Revenue
        createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(100000));
        // PAID + da_xac_nhan => Projected Revenue
        createTestOrder("da_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(110000));
        // PAID + dang_lay_hang => Projected Revenue
        createTestOrder("dang_lay_hang", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(120000));
        // PAID + dang_giao => Projected Revenue
        createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(130000));

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

        // Calculate statistics
        Map<String, Object> stats = adminThongKeService.getStatisticsData(start, end);

        BigDecimal actualRevenue = (BigDecimal) stats.get("actualRevenue");
        BigDecimal expectedRevenue = (BigDecimal) stats.get("expectedRevenue");
        BigDecimal refundedRevenue = (BigDecimal) stats.get("refundedRevenue");

        // Projected Revenue should sum the four PAID undelivered orders (100k + 110k + 120k + 130k = 460k)
        assertEquals(0, BigDecimal.valueOf(460000).compareTo(expectedRevenue), "Projected revenue does not match expected");

        // Actual Revenue should sum delivered orders (200k + 250k = 450k) minus refunded delivered order (400k) = 50k
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(actualRevenue), "Actual revenue does not match expected");

        // Refunded Revenue should be 400k
        assertEquals(0, BigDecimal.valueOf(400000).compareTo(refundedRevenue), "Refunded revenue does not match expected");

        // Verify chart values sum matches Actual Revenue
        List<BigDecimal> chartValues = (List<BigDecimal>) stats.get("chartValues");
        BigDecimal chartSum = chartValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, actualRevenue.compareTo(chartSum), "Chart sum does not match actual revenue");
    }
}
