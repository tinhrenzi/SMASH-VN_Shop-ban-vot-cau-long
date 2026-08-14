package com.smashvn.shop.service.payment;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayOrderPaymentServiceTest {

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SepayOrderPaymentService sepayOrderPaymentService;

    private HoaDon sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new HoaDon();
        sampleOrder.setId(100);
        sampleOrder.setMaDonHang("HD100");
        sampleOrder.setTrangThaiDonHang("YEU_CAU_HUY");
        sampleOrder.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        sampleOrder.setRefundStatus(RefundStatus.PENDING);
    }

    @Test
    @DisplayName("TC-B15.1 – Transaction SUCCESS bình thường phải bị chặn (throw IllegalStateException)")
    void testTC_B15_1_NormalSuccessTransactionBlocked() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        PaymentTransaction successTx = new PaymentTransaction();
        successTx.setStatus("SUCCESS");
        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(List.of(successTx));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);
        });

        assertEquals("Đơn hàng không thuộc trường hợp đã thanh toán nhưng thiếu tồn kho.", ex.getMessage());
        assertEquals("YEU_CAU_HUY", sampleOrder.getTrangThaiDonHang());
        assertNotEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        verify(hoaDonRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-B15.2 – PAID_INSUFFICIENT_STOCK hợp lệ -> Finalize refund thành công")
    void testTC_B15_2_PaidInsufficientStockSuccess() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        PaymentTransaction insufficientTx = new PaymentTransaction();
        insufficientTx.setStatus("PAID_INSUFFICIENT_STOCK");
        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(List.of(insufficientTx));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);

        assertEquals("DA_HUY", sampleOrder.getTrangThaiDonHang());
        assertEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, sampleOrder.getRefundStatus());
        verify(hoaDonRepository, times(1)).save(sampleOrder);
        verify(auditService, times(1)).log(eq(1), eq("HoaDon"), eq(100L), eq("FINALIZE_REFUND_NO_RESTOCK"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-B15.3 – Có cả SUCCESS và PAID_INSUFFICIENT_STOCK -> Cho phép refund dựa trên PAID_INSUFFICIENT_STOCK")
    void testTC_B15_3_BothSuccessAndPaidInsufficientStock() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        PaymentTransaction successTx = new PaymentTransaction();
        successTx.setStatus("SUCCESS");

        PaymentTransaction insufficientTx = new PaymentTransaction();
        insufficientTx.setStatus("PAID_INSUFFICIENT_STOCK");

        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(List.of(successTx, insufficientTx));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);

        assertEquals("DA_HUY", sampleOrder.getTrangThaiDonHang());
        assertEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, sampleOrder.getRefundStatus());
        verify(hoaDonRepository, times(1)).save(sampleOrder);
    }

    @Test
    @DisplayName("TC-B15.4 – Không có transaction (txs = empty) -> Reject an toàn, không NullPointerException")
    void testTC_B15_4_EmptyTransactionsRejectSafely() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));
        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);
        });

        assertEquals("Đơn hàng không thuộc trường hợp đã thanh toán nhưng thiếu tồn kho.", ex.getMessage());
        assertEquals("YEU_CAU_HUY", sampleOrder.getTrangThaiDonHang());
        verify(hoaDonRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-B15.5 – Retry sau khi đã refund -> Idempotent, không refund lần 2")
    void testTC_B15_5_RetryAfterRefundedIdempotent() {
        sampleOrder.setTrangThaiDonHang("DA_HUY");
        sampleOrder.setTrangThaiThanhToan("REFUNDED");
        sampleOrder.setRefundStatus(RefundStatus.COMPLETED);

        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);

        assertEquals("DA_HUY", sampleOrder.getTrangThaiDonHang());
        assertEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, sampleOrder.getRefundStatus());
        verify(paymentTransactionRepository, never()).findByOrder_Id(any());
        verify(hoaDonRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
