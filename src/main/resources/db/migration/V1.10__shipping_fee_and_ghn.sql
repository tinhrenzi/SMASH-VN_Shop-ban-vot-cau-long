-- Flyway Migration V1.10: Add phi_van_chuyen to HoaDon and Insert GHN carrier
-- Database: SQL Server

-- 1. Add phi_van_chuyen to HoaDon if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phi_van_chuyen')
BEGIN
    ALTER TABLE HoaDon ADD phi_van_chuyen DECIMAL(18,2) NULL;
END;

-- 2. Backfill existing orders with default 30000.00
EXEC('
UPDATE HoaDon 
SET phi_van_chuyen = 30000.00 
WHERE phi_van_chuyen IS NULL;
');

-- 3. Alter column to be NOT NULL if it exists and is nullable
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phi_van_chuyen' AND is_nullable = 1)
BEGIN
    ALTER TABLE HoaDon ALTER COLUMN phi_van_chuyen DECIMAL(18,2) NOT NULL;
END;

-- 4. Insert Giao Hàng Nhanh (GHN) carrier if it does not exist
IF NOT EXISTS (SELECT * FROM DonViVanChuyen WHERE ten_don_vi LIKE N'%Giao Hàng Nhanh%' OR ten_don_vi LIKE N'%GHN%')
BEGIN
    INSERT INTO DonViVanChuyen (ten_don_vi, hotline, website)
    VALUES (N'Giao Hàng Nhanh (GHN)', '1900 636677', 'https://ghn.vn');
END;
