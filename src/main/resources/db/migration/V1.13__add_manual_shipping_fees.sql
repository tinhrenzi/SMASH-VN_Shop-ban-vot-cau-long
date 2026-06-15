IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'phi_local')
BEGIN
    ALTER TABLE DonViVanChuyen ADD phi_local DECIMAL(18,2) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'phi_nationwide')
BEGIN
    ALTER TABLE DonViVanChuyen ADD phi_nationwide DECIMAL(18,2) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'version')
BEGIN
    ALTER TABLE DonViVanChuyen ADD [version] BIGINT NOT NULL DEFAULT 0;
END;

-- Backfill GHTK
EXEC('UPDATE DonViVanChuyen SET phi_local = 22000.00, phi_nationwide = 30000.00 WHERE (ten_don_vi LIKE ''%GHTK%'' OR ten_don_vi LIKE N''%Tiết Kiệm%'' OR ten_don_vi LIKE ''%Tiet Kiem%'') AND phi_local IS NULL');

-- Backfill GHN
EXEC('UPDATE DonViVanChuyen SET phi_local = 25000.00, phi_nationwide = 38000.00 WHERE (ten_don_vi LIKE ''%GHN%'' OR ten_don_vi LIKE N''%Giao Hàng Nhanh%'' OR ten_don_vi LIKE ''%Giao Hang Nhanh%'') AND phi_local IS NULL');

-- Backfill others or default values
EXEC('UPDATE DonViVanChuyen SET phi_local = 30000.00, phi_nationwide = 30000.00 WHERE phi_local IS NULL');
