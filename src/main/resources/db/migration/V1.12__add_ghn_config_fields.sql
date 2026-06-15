-- Flyway Migration V1.12: Add configuration fields to DonViVanChuyen
-- Database: SQL Server

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'token')
BEGIN
    ALTER TABLE DonViVanChuyen ADD token NVARCHAR(255) NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'client_id')
BEGIN
    ALTER TABLE DonViVanChuyen ADD client_id NVARCHAR(100) NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'dia_chi_kho')
BEGIN
    ALTER TABLE DonViVanChuyen ADD dia_chi_kho NVARCHAR(500) NULL;
END;
