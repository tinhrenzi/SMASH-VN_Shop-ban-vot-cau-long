-- V1.4__remove_vai_tro_hien_tai.sql
-- Remove vai_tro_hien_tai column as active role is stored only in session state if it exists
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'vai_tro_hien_tai')
BEGIN
    ALTER TABLE TaiKhoan DROP COLUMN vai_tro_hien_tai;
END;
GO

