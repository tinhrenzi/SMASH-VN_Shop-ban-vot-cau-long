-- V1.18: Add gia_tri_giam_toi_da to PhieuGiamGia table
-- This column stores the maximum discount cap for percentage vouchers.
-- NULL = unlimited (backward-compatible with existing vouchers).
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'PhieuGiamGia'
      AND COLUMN_NAME = 'gia_tri_giam_toi_da'
)
BEGIN
    ALTER TABLE PhieuGiamGia
        ADD gia_tri_giam_toi_da DECIMAL(18, 2) NULL;
END
