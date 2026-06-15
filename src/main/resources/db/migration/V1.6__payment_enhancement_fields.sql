-- V1.6: POS Payment Enhancement Schema Adjustments

-- 1. Drop the redundant phuong_thuc_pos column if it exists to avoid duplicate sources of truth
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phuong_thuc_pos')
BEGIN
    ALTER TABLE HoaDon DROP COLUMN phuong_thuc_pos;
END;
GO

-- 2. Add optional transaction reference for bank transfers
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ma_giao_dich')
BEGIN
    ALTER TABLE HoaDon ADD ma_giao_dich NVARCHAR(100) NULL;
END;
GO

-- 3. Add payment confirmation audit fields
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'nguoi_xac_nhan_thanh_toan')
BEGIN
    ALTER TABLE HoaDon ADD nguoi_xac_nhan_thanh_toan NVARCHAR(100) NULL;
END;
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'thoi_gian_xac_nhan')
BEGIN
    ALTER TABLE HoaDon ADD thoi_gian_xac_nhan DATETIME NULL;
END;
GO

-- 4. Update existing payment status values
UPDATE HoaDon SET trang_thai_thanh_toan = 'CHO_THANH_TOAN' WHERE trang_thai_thanh_toan = 'chua_thanh_toan' OR trang_thai_thanh_toan IS NULL;
UPDATE HoaDon SET trang_thai_thanh_toan = 'DA_THANH_TOAN' WHERE trang_thai_thanh_toan = 'da_thanh_toan';
GO

-- 5. Drop existing default constraint on trang_thai_thanh_toan if any exists to align defaults
DECLARE @ConstraintName NVARCHAR(256)
SELECT @ConstraintName = d.name
FROM sys.default_constraints d
INNER JOIN sys.columns c ON d.parent_column_id = c.column_id AND d.parent_object_id = c.object_id
WHERE d.parent_object_id = OBJECT_ID('HoaDon') AND c.name = 'trang_thai_thanh_toan';

IF @ConstraintName IS NOT NULL
BEGIN
    EXEC('ALTER TABLE HoaDon DROP CONSTRAINT ' + @ConstraintName);
END;
GO

-- 6. Add default constraint to trang_thai_thanh_toan
ALTER TABLE HoaDon ADD CONSTRAINT DF_HoaDon_trang_thai_thanh_toan DEFAULT 'CHO_THANH_TOAN' FOR trang_thai_thanh_toan;
GO
