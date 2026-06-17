-- V1.22__add_promotion_validation_constraints.sql
-- Enforces check constraints on DotGiamGia and PhieuGiamGia tables.
-- Fails migration explicitly if invalid data is present.

-- 1. Check for invalid DotGiamGia records
IF EXISTS (SELECT 1 FROM DotGiamGia WHERE phan_tram_giam < 1 OR phan_tram_giam > 40)
BEGIN
    RAISERROR('Migration failed: Invalid historical DotGiamGia records found (phan_tram_giam out of 1-40 range). Please clean data before applying constraints.', 16, 1);
    RETURN;
END

-- 2. Check for invalid PhieuGiamGia records
IF EXISTS (
    SELECT 1 FROM PhieuGiamGia 
    WHERE so_luong_con_lai < 0 OR so_luong_con_lai > 1000000
       OR gia_tri <= 0
       OR (don_vi = '%' AND (gia_tri < 1 OR gia_tri > 100))
       OR (don_vi = 'VND' AND (gia_tri < 1 OR gia_tri > 100000000))
       OR gia_tri_don_hang_toi_thieu < 0 OR gia_tri_don_hang_toi_thieu > 100000000
       OR (don_vi = '%' AND (gia_tri_giam_toi_da IS NULL OR gia_tri_giam_toi_da < 1 OR gia_tri_giam_toi_da > 100000000))
       OR (don_vi = 'VND' AND gia_tri_giam_toi_da IS NOT NULL)
)
BEGIN
    RAISERROR('Migration failed: Invalid historical PhieuGiamGia records found. Please clean data before applying constraints.', 16, 1);
    RETURN;
END

-- 3. Add CHECK constraints idempotently if they do not exist
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_DotGiamGia_PhanTramGiam')
BEGIN
    ALTER TABLE DotGiamGia ADD CONSTRAINT CHK_DotGiamGia_PhanTramGiam 
        CHECK (phan_tram_giam >= 1 AND phan_tram_giam <= 40);
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_PhieuGiamGia_SoLuongConLai')
BEGIN
    ALTER TABLE PhieuGiamGia ADD CONSTRAINT CHK_PhieuGiamGia_SoLuongConLai 
        CHECK (so_luong_con_lai >= 0 AND so_luong_con_lai <= 1000000);
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_PhieuGiamGia_DonHangToiThieu')
BEGIN
    ALTER TABLE PhieuGiamGia ADD CONSTRAINT CHK_PhieuGiamGia_DonHangToiThieu 
        CHECK (gia_tri_don_hang_toi_thieu >= 0 AND gia_tri_don_hang_toi_thieu <= 100000000);
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_PhieuGiamGia_BusinessRules')
BEGIN
    ALTER TABLE PhieuGiamGia ADD CONSTRAINT CHK_PhieuGiamGia_BusinessRules 
        CHECK (
            (don_vi = '%' AND gia_tri >= 1 AND gia_tri <= 100 AND gia_tri_giam_toi_da >= 1 AND gia_tri_giam_toi_da <= 100000000)
            OR 
            (don_vi = 'VND' AND gia_tri >= 1 AND gia_tri <= 100000000 AND gia_tri_giam_toi_da IS NULL)
        );
END
