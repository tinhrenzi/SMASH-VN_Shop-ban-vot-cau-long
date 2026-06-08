package com.smashvn.shop.service;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.ShippingZone;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ShippingIntegrationTest {

    @Autowired
    private ShippingZoneResolver zoneResolver;

    @Autowired
    private ShippingFeeCalculator feeCalculator;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private AdminShippingService adminShippingService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Test
    void testAddressNormalizationAndZoneResolution() {
        // Local cases
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Hà Nội"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("ha noi"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("   HA NOI   "));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Ha-Noi"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Thái Nguyên"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("thai nguyen"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Thái-Nguyên, Việt Nam."));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Hà Nội, Cầu Giấy"));

        // Nationwide cases
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone("Hồ Chí Minh"));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone("Đà Nẵng"));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone("Hai Phong"));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone(""));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone(null));
    }

    @Test
    void testCarrierFeeMatrixAndFallbacks() {
        // Mock / Fetch GHTK and GHN carrier references
        DonViVanChuyen ghtk = new DonViVanChuyen();
        ghtk.setTenDonVi("Giao hàng tiết kiệm (GHTK)");

        DonViVanChuyen ghn = new DonViVanChuyen();
        ghn.setTenDonVi("Giao Hàng Nhanh (GHN)");

        DonViVanChuyen other = new DonViVanChuyen();
        other.setTenDonVi("Viettel Post");

        // GHTK fees
        assertEquals(0, BigDecimal.valueOf(22000).compareTo(feeCalculator.calculateFee(ghtk, "Thái Nguyên")));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee(ghtk, "Hồ Chí Minh")));

        // GHN fees
        assertEquals(0, BigDecimal.valueOf(25000).compareTo(feeCalculator.calculateFee(ghn, "Hà Nội")));
        assertEquals(0, BigDecimal.valueOf(38000).compareTo(feeCalculator.calculateFee(ghn, "Đà Nẵng")));

        // Other carrier
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee(other, "Hà Nội")));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee(other, "Đà Nẵng")));

        // Null / Empty cases & fallback
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee((DonViVanChuyen) null, "Hà Nội")));
        assertEquals(0, BigDecimal.valueOf(38000).compareTo(feeCalculator.calculateFee(ghn, "")));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee((DonViVanChuyen) null, (String) null)));
    }

    @Test
    void testHistoricalOrderDisplayIntegrity() {
        // Create an order
        HoaDon hd = new HoaDon();
        
        List<KhachHang> khs = khachHangRepository.findAll();
        assertFalse(khs.isEmpty(), "KhachHang table must not be empty for this test");
        hd.setKhachHang(khs.get(0));

        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        assertFalse(ptts.isEmpty(), "PhuongThucThanhToan table must not be empty for this test");
        hd.setPhuongThucThanhToan(ptts.get(0));

        List<DonViVanChuyen> dvvcs = donViVanChuyenDAO.findAll();
        assertFalse(dvvcs.isEmpty(), "DonViVanChuyen table must not be empty for this test");
        hd.setDonViVanChuyen(dvvcs.get(0));

        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd.setTongTien(new BigDecimal("1000000"));
        
        // Persist shipping fee as 22000
        BigDecimal persistedFee = BigDecimal.valueOf(22000);
        hd.setPhiVanChuyen(persistedFee);

        hd = hoaDonRepository.save(hd);
        assertNotNull(hd.getId());

        // Now verify it's persisted and loaded correctly
        HoaDon loaded = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(loaded);
        assertEquals(0, persistedFee.compareTo(loaded.getPhiVanChuyen()));

        // Even if we query fee calculation or change the address logic, the persisted order's fee remains 22000
        assertEquals(0, persistedFee.compareTo(loaded.getPhiVanChuyen()));
    }

    @Test
    void testNegativeFeeRejected() {
        // Create acting QL user
        TaiKhoan manager = new TaiKhoan();
        manager.setEmail("manager-ship-test-" + System.nanoTime() + "@smashvn.com");
        manager.setMatKhau("123");
        manager.setVaiTro("QL");
        manager.setLaQuanLy(true);
        manager = taiKhoanRepository.save(manager);

        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        assertFalse(carriers.isEmpty());
        DonViVanChuyen carrier = carriers.get(0);

        final Integer cid = carrier.getId();
        final Long ver = carrier.getVersion();
        final Integer mid = manager.getId();

        assertThrows(IllegalArgumentException.class, () -> {
            adminShippingService.updateShippingFee(cid, BigDecimal.valueOf(-1000), BigDecimal.valueOf(30000), ver, mid, "127.0.0.1");
        });
    }

    @Test
    void testNullFeeRejected() {
        TaiKhoan manager = new TaiKhoan();
        manager.setEmail("manager-ship-test-" + System.nanoTime() + "@smashvn.com");
        manager.setMatKhau("123");
        manager.setVaiTro("QL");
        manager.setLaQuanLy(true);
        manager = taiKhoanRepository.save(manager);

        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        assertFalse(carriers.isEmpty());
        DonViVanChuyen carrier = carriers.get(0);

        final Integer cid = carrier.getId();
        final Long ver = carrier.getVersion();
        final Integer mid = manager.getId();

        assertThrows(IllegalArgumentException.class, () -> {
            adminShippingService.updateShippingFee(cid, null, BigDecimal.valueOf(30000), ver, mid, "127.0.0.1");
        });
    }

    @Test
    void testServiceAuthorizationEnforced() {
        // Create NV user (should be rejected)
        TaiKhoan employee = new TaiKhoan();
        employee.setEmail("emp-ship-test-" + System.nanoTime() + "@smashvn.com");
        employee.setMatKhau("123");
        employee.setVaiTro("NV");
        employee.setLaNhanVien(true);
        employee = taiKhoanRepository.save(employee);

        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        assertFalse(carriers.isEmpty());
        DonViVanChuyen carrier = carriers.get(0);

        final Integer cid = carrier.getId();
        final Long ver = carrier.getVersion();
        final Integer eid = employee.getId();

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            adminShippingService.updateShippingFee(cid, BigDecimal.valueOf(20000), BigDecimal.valueOf(30000), ver, eid, "127.0.0.1");
        });
    }

    @Test
    void testOptimisticLockingConflict() {
        TaiKhoan manager = new TaiKhoan();
        manager.setEmail("manager-ship-test-" + System.nanoTime() + "@smashvn.com");
        manager.setMatKhau("123");
        manager.setVaiTro("QL");
        manager.setLaQuanLy(true);
        manager = taiKhoanRepository.save(manager);

        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        assertFalse(carriers.isEmpty());
        DonViVanChuyen carrier = carriers.get(0);

        final Integer cid = carrier.getId();
        final Long correctVer = carrier.getVersion();
        final Long staleVer = correctVer - 1; // Stale version
        final Integer mid = manager.getId();

        // Stale update must fail with Optimistic Locking exception
        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
            adminShippingService.updateShippingFee(cid, BigDecimal.valueOf(25000), BigDecimal.valueOf(35000), staleVer, mid, "127.0.0.1");
        });
    }

    @Test
    void testCacheEvictedAfterUpdate() {
        TaiKhoan manager = new TaiKhoan();
        manager.setEmail("manager-ship-test-" + System.nanoTime() + "@smashvn.com");
        manager.setMatKhau("123");
        manager.setVaiTro("QL");
        manager.setLaQuanLy(true);
        manager = taiKhoanRepository.save(manager);

        List<DonViVanChuyen> list1 = adminShippingService.getAllCarriers();
        assertFalse(list1.isEmpty());
        DonViVanChuyen carrier = list1.get(0);

        // Update fee
        BigDecimal newLocal = BigDecimal.valueOf(31415);
        adminShippingService.updateShippingFee(carrier.getId(), newLocal, BigDecimal.valueOf(40000), carrier.getVersion(), manager.getId(), "127.0.0.1");

        // Next lookup should yield the new fee (cache is evicted and refreshed)
        List<DonViVanChuyen> list2 = adminShippingService.getAllCarriers();
        DonViVanChuyen updated = list2.stream().filter(c -> c.getId().equals(carrier.getId())).findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals(0, newLocal.compareTo(updated.getPhiLocal()));
    }

    @Test
    void testHistoricalOrderFeeUnaffected() {
        // Create an order with fee 22000
        HoaDon hd = new HoaDon();
        List<KhachHang> khs = khachHangRepository.findAll();
        hd.setKhachHang(khs.get(0));
        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        hd.setPhuongThucThanhToan(ptts.get(0));
        
        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        DonViVanChuyen carrier = carriers.get(0);
        hd.setDonViVanChuyen(carrier);
        
        hd.setDiaChiNhan("Thái Nguyên");
        hd.setSdtNhan("0912345678");
        hd.setTongTien(BigDecimal.valueOf(100000));
        hd.setPhiVanChuyen(BigDecimal.valueOf(22000));
        hd = hoaDonRepository.save(hd);

        // Update carrier fee to 99000 in database
        TaiKhoan manager = new TaiKhoan();
        manager.setEmail("manager-ship-test-" + System.nanoTime() + "@smashvn.com");
        manager.setMatKhau("123");
        manager.setVaiTro("QL");
        manager.setLaQuanLy(true);
        manager = taiKhoanRepository.save(manager);

        adminShippingService.updateShippingFee(carrier.getId(), BigDecimal.valueOf(99000), BigDecimal.valueOf(99000), carrier.getVersion(), manager.getId(), "127.0.0.1");

        // Reload the order, fee must remain 22000
        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals(0, BigDecimal.valueOf(22000).compareTo(reloaded.getPhiVanChuyen()));
    }
}
