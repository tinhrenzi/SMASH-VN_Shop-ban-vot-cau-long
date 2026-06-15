-- Add token_xac_thuc_khoa column for manager email lock approvals
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'token_xac_thuc_khoa')
BEGIN
    ALTER TABLE TaiKhoan ADD token_xac_thuc_khoa VARCHAR(100) NULL;
END;
GO
