-- Flyway Migration V1.7: Add ZaloPay payment fields to HoaDon table
-- Database: SQL Server

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'payment_method')
BEGIN
    ALTER TABLE HoaDon ADD payment_method VARCHAR(50) NULL;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'payment_status')
BEGIN
    ALTER TABLE HoaDon ADD payment_status VARCHAR(50) NULL;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'transaction_id')
BEGIN
    ALTER TABLE HoaDon ADD transaction_id VARCHAR(100) NULL;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'gateway_response')
BEGIN
    ALTER TABLE HoaDon ADD gateway_response NVARCHAR(MAX) NULL;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'paid_at')
BEGIN
    ALTER TABLE HoaDon ADD paid_at DATETIME NULL;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'app_trans_id')
BEGIN
    ALTER TABLE HoaDon ADD app_trans_id VARCHAR(100) NULL;
END

-- Update existing records for data mapping consistency using dynamic SQL to prevent SQL Server compile-time column checks
EXEC('UPDATE HoaDon SET payment_status = ''PAID'' WHERE trang_thai_thanh_toan = ''DA_THANH_TOAN'' AND payment_status IS NULL');
EXEC('UPDATE HoaDon SET payment_status = ''PENDING'' WHERE trang_thai_thanh_toan = ''CHO_THANH_TOAN'' AND payment_status IS NULL');
EXEC('UPDATE HoaDon SET payment_method = ''CASH'' WHERE id_phuong_thuc_thanh_toan = 1 AND payment_method IS NULL');
EXEC('UPDATE HoaDon SET payment_method = ''BANK_TRANSFER'' WHERE id_phuong_thuc_thanh_toan = 2 AND payment_method IS NULL');

