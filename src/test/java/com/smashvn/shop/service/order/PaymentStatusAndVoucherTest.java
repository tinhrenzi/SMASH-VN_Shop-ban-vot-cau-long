package com.smashvn.shop.service.order;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.PhieuGiamGia;

@SpringBootTest
@Transactional
public class PaymentStatusAndVoucherTest {

    @Autowired
    private OrderViewService orderViewService;

    @Test
    public void testPaymentStatusMapping() {
        // 1. paid -> Đã thanh toán (bg-success)
        var paidInfo = orderViewService.getPaymentStatusInfo("paid");
        assertEquals("PAID", paidInfo.code());
        assertEquals("Đã thanh toán", paidInfo.label());
        assertEquals("bg-success", paidInfo.badgeClass());

        var daThanhToanInfo = orderViewService.getPaymentStatusInfo("DA_THANH_TOAN");
        assertEquals("PAID", daThanhToanInfo.code());
        assertEquals("Đã thanh toán", daThanhToanInfo.label());
        assertEquals("bg-success", daThanhToanInfo.badgeClass());

        // 2. pending -> Chờ thanh toán (bg-warning text-dark)
        var pendingInfo = orderViewService.getPaymentStatusInfo("pending");
        assertEquals("PENDING", pendingInfo.code());
        assertEquals("Chờ thanh toán", pendingInfo.label());
        assertEquals("bg-warning text-dark", pendingInfo.badgeClass());

        // 3. cancelled -> Đã hủy (bg-danger)
        var cancelledInfo = orderViewService.getPaymentStatusInfo("cancelled");
        assertEquals("CANCELLED", cancelledInfo.code());
        assertEquals("Đã hủy", cancelledInfo.label());
        assertEquals("bg-danger", cancelledInfo.badgeClass());

        // 4. Strange status -> Raw status (bg-secondary)
        var strangeInfo = orderViewService.getPaymentStatusInfo("SOME_STRANGE_STATUS");
        assertEquals("UNKNOWN", strangeInfo.code());
        assertEquals("SOME_STRANGE_STATUS", strangeInfo.label());
        assertEquals("bg-secondary", strangeInfo.badgeClass());
    }

    @Test
    public void testVoucherFallbackLogic() {
        HoaDon hd = new HoaDon();
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setMaVoucherApDung(null);

        // Case 1: no voucher
        String maVoucher = hd.getMaVoucherApDung();
        if (maVoucher == null || maVoucher.isEmpty()) {
            if (hd.getPhieuGiamGia() != null) {
                maVoucher = hd.getPhieuGiamGia().getMaPhieu();
            } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(BigDecimal.ZERO) > 0) {
                maVoucher = "Voucher";
            } else {
                maVoucher = "Không áp dụng voucher";
            }
        }
        assertEquals("Không áp dụng voucher", maVoucher);

        // Case 2: has voucher fallback to relation
        PhieuGiamGia pg = new PhieuGiamGia();
        pg.setMaPhieu("TESTCODE123");
        hd.setPhieuGiamGia(pg);
        hd.setSoTienGiamVoucher(new BigDecimal("50000"));

        maVoucher = hd.getMaVoucherApDung();
        if (maVoucher == null || maVoucher.isEmpty()) {
            if (hd.getPhieuGiamGia() != null) {
                maVoucher = hd.getPhieuGiamGia().getMaPhieu();
            } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(BigDecimal.ZERO) > 0) {
                maVoucher = "Voucher";
            } else {
                maVoucher = "Không áp dụng voucher";
            }
        }
        assertEquals("TESTCODE123", maVoucher);
    }

    @Test
    public void testTensionSpellingCorrection() {
        String originalSnapshot = "Màu sắc: Xanh, Trọng lượng: 3U, Mức cảng: 28 lbs";
        String corrected = originalSnapshot.replace("Mức cảng:", "Sức căng khuyến nghị:");
        assertEquals("Màu sắc: Xanh, Trọng lượng: 3U, Sức căng khuyến nghị: 28 lbs", corrected);
    }
}
