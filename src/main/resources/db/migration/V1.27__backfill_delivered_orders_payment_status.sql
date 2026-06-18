-- Flyway migration V1.27: Backfill payment status for historical delivered/completed orders.
-- Skips already paid or refunded orders using LOWER() to handle mixed-case stored values.
UPDATE HoaDon
SET payment_status = 'paid',
    trang_thai_thanh_toan = 'DA_THANH_TOAN',
    paid_at = COALESCE(paid_at, ngay_tao)
WHERE trang_thai_don_hang IN ('da_giao', 'hoan_thanh')
  AND LOWER(COALESCE(payment_status, '')) NOT IN ('paid', 'refunded');
