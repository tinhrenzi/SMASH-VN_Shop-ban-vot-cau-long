-- Flyway migration V1.25
-- Verify target Gmail accounts do not already exist for OTHER accounts (Email Uniqueness Validation)
IF EXISTS (
    SELECT 1 FROM TaiKhoan 
    WHERE email IN ('tinhluc02@gmail.com', 'luonghiep334@gmail.com')
      AND email NOT IN ('tinhadmin@smash.vn.com', 'hiepadmin@smash.vn.com')
)
BEGIN
    THROW 50000, 'Migration failed: Target Gmail accounts already exist in TaiKhoan table. Manual resolution required.', 1;
END;
GO

-- Update legacy emails
UPDATE TaiKhoan SET email = 'tinhluc02@gmail.com' WHERE email = 'tinhadmin@smash.vn.com';
UPDATE TaiKhoan SET email = 'luonghiep334@gmail.com' WHERE email = 'hiepadmin@smash.vn.com';
GO

-- Add uploader column if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'id_tai_khoan_nguoi_dang')
BEGIN
    ALTER TABLE Blog ADD id_tai_khoan_nguoi_dang INT NULL;
END;
GO

-- Add foreign key with ON DELETE NO ACTION if not exists
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_Blog_TaiKhoan')
BEGIN
    ALTER TABLE Blog ADD CONSTRAINT FK_Blog_TaiKhoan FOREIGN KEY (id_tai_khoan_nguoi_dang) REFERENCES TaiKhoan(id) ON DELETE NO ACTION;
END;
GO

-- Conditional Blog Mapping
DECLARE @adminId INT;
SELECT @adminId = id FROM TaiKhoan WHERE email = 'tinhluc02@gmail.com';

IF @adminId IS NOT NULL
BEGIN
    UPDATE Blog SET id_tai_khoan_nguoi_dang = @adminId WHERE id_tai_khoan_nguoi_dang IS NULL;
END
ELSE
BEGIN
    PRINT 'WARNING: Default admin account tinhluc02@gmail.com not found. Existing blog records will remain NULL.';
END;
GO
