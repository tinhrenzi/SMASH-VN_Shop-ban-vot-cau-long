-- Thêm các cột quản lý vi phạm bình luận vào bảng TaiKhoan
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'so_lan_nhac_nho_vi_pham')
BEGIN
    ALTER TABLE TaiKhoan ADD so_lan_nhac_nho_vi_pham INT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'ngay_khoa_binh_luan_den')
BEGIN
    ALTER TABLE TaiKhoan ADD ngay_khoa_binh_luan_den DATETIME NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'ngay_vi_pham_gan_nhat')
BEGIN
    ALTER TABLE TaiKhoan ADD ngay_vi_pham_gan_nhat DATETIME NULL;
END;

-- Tạo bảng ThongBao
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('ThongBao') AND type = 'U')
BEGIN
    CREATE TABLE ThongBao (
        id INT IDENTITY(1,1) PRIMARY KEY,
        id_tai_khoan INT NOT NULL,
        tieu_de NVARCHAR(255) NOT NULL,
        noi_dung NVARCHAR(MAX) NOT NULL,
        da_doc BIT NOT NULL DEFAULT 0,
        ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_ThongBao_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES TaiKhoan(id) ON DELETE CASCADE
    );
END;

-- Tạo bảng CommentViolationLog
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('CommentViolationLog') AND type = 'U')
BEGIN
    CREATE TABLE CommentViolationLog (
        id INT IDENTITY(1,1) PRIMARY KEY,
        tai_khoan_id INT NOT NULL,
        danh_gia_id INT NULL,
        san_pham_id INT NOT NULL,
        noi_dung_goc NVARCHAR(MAX) NOT NULL,
        noi_dung_da_loc NVARCHAR(MAX) NOT NULL,
        muc_do_vi_pham VARCHAR(50) NOT NULL,
        so_lan_vi_pham INT NOT NULL,
        thoi_han_khoa VARCHAR(100) NULL,
        ngay_vi_pham DATETIME NOT NULL DEFAULT GETDATE(),
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_CommentViolationLog_TaiKhoan FOREIGN KEY (tai_khoan_id) REFERENCES TaiKhoan(id),
        CONSTRAINT FK_CommentViolationLog_DanhGia FOREIGN KEY (danh_gia_id) REFERENCES DanhGia(id) ON DELETE SET NULL,
        CONSTRAINT FK_CommentViolationLog_SanPham FOREIGN KEY (san_pham_id) REFERENCES SanPham(id)
    );
END;
GO
