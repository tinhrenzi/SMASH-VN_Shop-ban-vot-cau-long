-- V1.3__multi_role_support.sql
-- Add columns to support multi-role accounts if they do not exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'vai_tro_hien_tai')
BEGIN
    ALTER TABLE TaiKhoan ADD vai_tro_hien_tai NVARCHAR(20) NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'la_khach_hang')
BEGIN
    ALTER TABLE TaiKhoan ADD la_khach_hang BIT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'la_nhan_vien')
BEGIN
    ALTER TABLE TaiKhoan ADD la_nhan_vien BIT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'la_quan_ly')
BEGIN
    ALTER TABLE TaiKhoan ADD la_quan_ly BIT NULL;
END;

GO

-- Copy existing data to new columns
UPDATE TaiKhoan
SET vai_tro_hien_tai = COALESCE(vai_tro_hien_tai, vai_tro),
    la_khach_hang = COALESCE(la_khach_hang, CASE WHEN vai_tro = 'KH' THEN 1 ELSE 0 END),
    la_nhan_vien = COALESCE(la_nhan_vien, CASE WHEN vai_tro = 'NV' THEN 1 ELSE 0 END),
    la_quan_ly = COALESCE(la_quan_ly, CASE WHEN vai_tro = 'QL' THEN 1 ELSE 0 END);

GO

