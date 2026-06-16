package com.smashvn.shop.util;

import com.smashvn.shop.entity.PhieuGiamGia;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized voucher discount calculator.
 * <p>
 * All discount computations across CheckoutController, GioHangService, and AdminPosService
 * must delegate to this class to guarantee consistent results.
 * </p>
 *
 * <h3>Calculation Rules:</h3>
 * <ul>
 *   <li>Returns BigDecimal.ZERO when inputs are null / orderTotal ≤ 0.</li>
 *   <li>Returns BigDecimal.ZERO when orderTotal &lt; minimum required order value.</li>
 *   <li>For percentage vouchers: discount = (orderTotal × rate%) rounded to 0 decimal places (HALF_UP).</li>
 *   <li>For percentage vouchers with a cap: discount is capped at {@code giaTriGiamToiDa} (if set).</li>
 *   <li>For fixed-amount vouchers: discount equals the voucher face value; cap field is ignored.</li>
 *   <li>Final discount is never negative and never exceeds orderTotal.</li>
 * </ul>
 */
public final class VoucherCalculator {

    private VoucherCalculator() {
        // Utility class – no instantiation
    }

    /**
     * Calculates the discount amount to be deducted from {@code orderTotal}.
     *
     * @param orderTotal the pre-discount order subtotal (must be &gt; 0)
     * @param voucher    the applied voucher entity (must not be null)
     * @return the discount amount, always ≥ 0 and ≤ orderTotal
     */
    public static BigDecimal calculateVoucherDiscount(BigDecimal orderTotal, PhieuGiamGia voucher) {
        if (voucher == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Minimum order validation
        BigDecimal minOrder = voucher.getGiaTriDonHangToiThieu();
        if (minOrder != null && orderTotal.compareTo(minOrder) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        boolean isPercentage = "%".equals(voucher.getDonVi())
                || "Giảm phần trăm".equalsIgnoreCase(voucher.getLoaiGiamGia());

        if (isPercentage) {
            // Percentage discount with HALF_UP rounding, scale 0 (VNĐ is integer currency)
            discount = orderTotal
                    .multiply(voucher.getGiaTri())
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);

            // Apply maximum discount cap (only meaningful for percentage vouchers)
            BigDecimal cap = voucher.getGiaTriGiamToiDa();
            if (cap != null && cap.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(cap) > 0) {
                discount = cap;
            }
        } else {
            // Fixed-amount discount – cap field is ignored
            discount = voucher.getGiaTri();
        }

        // Guard: never negative
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        // Guard: never exceed order total
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }
}
