SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
USE SMDB_FINAL;
GO

-- 1. Table Blog
-- Add danh_muc, the, and updated_by
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'danh_muc')
    ALTER TABLE Blog ADD danh_muc NVARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'the')
    ALTER TABLE Blog ADD the NVARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'updated_by')
    ALTER TABLE Blog ADD updated_by VARCHAR(255) NULL;

-- Change ngay_dang to DATE
DROP INDEX IF EXISTS IX_BLOG_PUBLISH_DATE ON Blog;
ALTER TABLE Blog ALTER COLUMN ngay_dang DATE NULL;
CREATE NONCLUSTERED INDEX IX_BLOG_PUBLISH_DATE ON Blog(ngay_dang);
GO

-- 2. Table CommentViolationLog
-- Drop foreign key and recreate table CommentViolationLog
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_CommentViolationLog_DanhGia')
    ALTER TABLE CommentViolationLog DROP CONSTRAINT FK_CommentViolationLog_DanhGia;
DROP TABLE IF EXISTS CommentViolationLog;

CREATE TABLE CommentViolationLog (
    id INT IDENTITY(1,1) CONSTRAINT PK_CommentViolationLog PRIMARY KEY,
    id_tai_khoan INT NOT NULL,
    id_danh_gia INT NULL,
    id_san_pham INT NOT NULL,
    noi_dung_goc NVARCHAR(MAX) NOT NULL,
    noi_dung_da_loc NVARCHAR(MAX) NOT NULL,
    muc_do_vi_pham NVARCHAR(50) NOT NULL,
    so_lan_vi_pham INT NOT NULL,
    thoi_han_khoa NVARCHAR(100) NULL,
    ngay_vi_pham DATETIME NOT NULL CONSTRAINT DF_CommentViolationLog_NgayVP DEFAULT GETDATE(),
    ngay_tao DATETIME NOT NULL CONSTRAINT DF_CommentViolationLog_NgayTao DEFAULT GETDATE()
);

ALTER TABLE CommentViolationLog ADD CONSTRAINT FK_CommentViolationLog_DanhGia FOREIGN KEY (id_danh_gia) REFERENCES DanhGia(id) ON DELETE SET NULL;
GO

-- 3. Table ChatConversation
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversation') AND name = 'tieu_de')
    ALTER TABLE ChatConversation ADD tieu_de NVARCHAR(255) NULL;
GO

-- 4 & 5. ChatMessage & ChatFeedback Recreations
-- Drop FKs
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_ChatFeedback_Message')
    ALTER TABLE ChatFeedback DROP CONSTRAINT FK_ChatFeedback_Message;
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_ChatMessage_Conversation')
    ALTER TABLE ChatMessage DROP CONSTRAINT FK_ChatMessage_Conversation;

DROP TABLE IF EXISTS ChatFeedback;
DROP TABLE IF EXISTS ChatMessage;

CREATE TABLE ChatMessage (
    id BIGINT IDENTITY(1,1) CONSTRAINT PK_ChatMessage PRIMARY KEY,
    id_cuoc_tro_chuyen INT NOT NULL,
    loai_nguoi_gui VARCHAR(10) NOT NULL,
    noi_dung NVARCHAR(MAX) NOT NULL,
    thoi_gian DATETIME NOT NULL CONSTRAINT DF_ChatMessage_ThoiGian DEFAULT GETDATE()
);

CREATE TABLE ChatFeedback (
    id INT IDENTITY(1,1) CONSTRAINT PK_ChatFeedback PRIMARY KEY,
    id_tin_nhan BIGINT NOT NULL,
    diem_danh_gia BIT NOT NULL,
    noi_dung NVARCHAR(500) NULL,
    ngay_tao DATETIME NOT NULL CONSTRAINT DF_ChatFeedback_NgayTao DEFAULT GETDATE()
);

ALTER TABLE ChatMessage ADD CONSTRAINT FK_ChatMessage_Conversation FOREIGN KEY (id_cuoc_tro_chuyen) REFERENCES ChatConversation(id);
ALTER TABLE ChatFeedback ADD CONSTRAINT FK_ChatFeedback_Message FOREIGN KEY (id_tin_nhan) REFERENCES ChatMessage(id);
GO

-- 6. Table DotGiamGia
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DotGiamGia') AND name = 'ten_chien_dich')
    ALTER TABLE DotGiamGia ADD ten_chien_dich NVARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DotGiamGia') AND name = 'loai_giam_gia')
    ALTER TABLE DotGiamGia ADD loai_giam_gia NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DotGiamGia') AND name = 'kich_hoat')
    ALTER TABLE DotGiamGia ADD kich_hoat BIT NULL CONSTRAINT DF_DotGiamGia_KichHoat DEFAULT 1;
GO

-- Populate default values
UPDATE DotGiamGia SET ten_chien_dich = ten_dot WHERE ten_chien_dich IS NULL;
UPDATE DotGiamGia SET loai_giam_gia = N'Theo Phần Trăm' WHERE loai_giam_gia IS NULL;
UPDATE DotGiamGia SET kich_hoat = CASE WHEN trang_thai = 'ACTIVE' THEN 1 ELSE 0 END WHERE kich_hoat IS NULL;
ALTER TABLE DotGiamGia ALTER COLUMN ten_dot NVARCHAR(150) NULL;
GO

-- 7. Table PhieuGiamGia
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('PhieuGiamGia') AND name = 'loai_giam_gia')
    ALTER TABLE PhieuGiamGia ADD loai_giam_gia NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('PhieuGiamGia') AND name = 'kich_hoat')
    ALTER TABLE PhieuGiamGia ADD kich_hoat BIT NULL CONSTRAINT DF_PhieuGiamGia_KichHoat DEFAULT 1;
GO

UPDATE PhieuGiamGia SET loai_giam_gia = CASE WHEN don_vi = '%' THEN N'Giảm phần trăm' ELSE N'Giảm trực tiếp' END;
UPDATE PhieuGiamGia SET kich_hoat = CASE WHEN trang_thai = 'ACTIVE' THEN 1 ELSE 0 END;
ALTER TABLE PhieuGiamGia ALTER COLUMN ten_phieu NVARCHAR(100) NULL;
GO

-- 8. Table TaiKhoan
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'token_xac_thuc_khoa')
    ALTER TABLE TaiKhoan ADD token_xac_thuc_khoa VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'ngay_khoa_binh_luan_den')
    ALTER TABLE TaiKhoan ADD ngay_khoa_binh_luan_den DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'ngay_vi_pham_gan_nhat')
    ALTER TABLE TaiKhoan ADD ngay_vi_pham_gan_nhat DATETIME NULL;
GO

-- 9. Table MaKhoiPhuc
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('MaKhoiPhuc') AND name = 'ma_xac_nhan')
    ALTER TABLE MaKhoiPhuc ADD ma_xac_nhan VARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('MaKhoiPhuc') AND name = 'thoi_gian_het_han')
    ALTER TABLE MaKhoiPhuc ADD thoi_gian_het_han DATETIME NULL;
GO

UPDATE MaKhoiPhuc SET ma_xac_nhan = token, thoi_gian_het_han = ngay_het_han;
ALTER TABLE MaKhoiPhuc ALTER COLUMN ma_xac_nhan VARCHAR(255) NOT NULL;
ALTER TABLE MaKhoiPhuc ALTER COLUMN thoi_gian_het_han DATETIME NOT NULL;
GO

-- 10. Table SanPham
-- Dynamically drop default constraint on diem_trung_binh
DECLARE @ConstraintName nvarchar(200);
SELECT @ConstraintName = Name FROM sys.default_constraints
WHERE parent_object_id = object_id('SanPham')
AND parent_column_id = (SELECT column_id FROM sys.columns WHERE object_id = object_id('SanPham') AND name = 'diem_trung_binh');
IF @ConstraintName IS NOT NULL
    EXEC('ALTER TABLE SanPham DROP CONSTRAINT ' + @ConstraintName);

ALTER TABLE SanPham ALTER COLUMN diem_trung_binh FLOAT NOT NULL;
ALTER TABLE SanPham ADD CONSTRAINT DF_SanPham_DiemTB DEFAULT 0.0 FOR diem_trung_binh;
GO

-- 11. Table SanPhamChiTiet
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SanPhamChiTiet') AND name = 'muc_cang')
    ALTER TABLE SanPhamChiTiet ADD muc_cang NVARCHAR(20) NULL;

-- Make sure mau_sac is present (it is in SMDB3 but check anyway)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SanPhamChiTiet') AND name = 'mau_sac')
    ALTER TABLE SanPhamChiTiet ADD mau_sac NVARCHAR(50) NULL;
GO

UPDATE SanPhamChiTiet SET muc_cang = N'10.5kg' WHERE muc_cang IS NULL;
UPDATE SanPhamChiTiet SET mau_sac = N'Đỏ' WHERE mau_sac IS NULL;

ALTER TABLE SanPhamChiTiet ALTER COLUMN muc_cang NVARCHAR(20) NOT NULL;
ALTER TABLE SanPhamChiTiet ALTER COLUMN mau_sac NVARCHAR(50) NOT NULL;

IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_SanPhamChiTiet_UniqueAttrs' AND type = 'UQ')
    ALTER TABLE SanPhamChiTiet ADD CONSTRAINT UQ_SanPhamChiTiet_UniqueAttrs UNIQUE (id_san_pham, mau_sac, trong_luong, muc_cang);

IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_SPCT_SKU' AND parent_object_id = OBJECT_ID('SanPhamChiTiet'))
    ALTER TABLE SanPhamChiTiet DROP CONSTRAINT UQ_SPCT_SKU;

IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_SPCT_Barcode' AND parent_object_id = OBJECT_ID('SanPhamChiTiet'))
    ALTER TABLE SanPhamChiTiet DROP CONSTRAINT UQ_SPCT_Barcode;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_SPCT_SKU_Unique' AND object_id = OBJECT_ID('SanPhamChiTiet'))
    CREATE UNIQUE NONCLUSTERED INDEX IX_SPCT_SKU_Unique ON SanPhamChiTiet(SKU) WHERE SKU IS NOT NULL;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_SPCT_Barcode_Unique' AND object_id = OBJECT_ID('SanPhamChiTiet'))
    CREATE UNIQUE NONCLUSTERED INDEX IX_SPCT_Barcode_Unique ON SanPhamChiTiet(barcode) WHERE barcode IS NOT NULL;
GO

-- 12. Table HoaDon
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghi_chu')
    ALTER TABLE HoaDon ADD ghi_chu NVARCHAR(500) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ma_giao_dich')
    ALTER TABLE HoaDon ADD ma_giao_dich NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'nguoi_xac_nhan_thanh_toan')
    ALTER TABLE HoaDon ADD nguoi_xac_nhan_thanh_toan NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'thoi_gian_xac_nhan')
    ALTER TABLE HoaDon ADD thoi_gian_xac_nhan DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phuong_thuc_thanh_toan')
    ALTER TABLE HoaDon ADD phuong_thuc_thanh_toan VARCHAR(50) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phan_hoi_cong_tt')
    ALTER TABLE HoaDon ADD phan_hoi_cong_tt NVARCHAR(MAX) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ngay_thanh_toan')
    ALTER TABLE HoaDon ADD ngay_thanh_toan DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ma_giao_dich_ung_dung')
    ALTER TABLE HoaDon ADD ma_giao_dich_ung_dung VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghn_order_code')
    ALTER TABLE HoaDon ADD ghn_order_code VARCHAR(50) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghn_status')
    ALTER TABLE HoaDon ADD ghn_status VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghn_to_district_id')
    ALTER TABLE HoaDon ADD ghn_to_district_id INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghn_to_ward_code')
    ALTER TABLE HoaDon ADD ghn_to_ward_code VARCHAR(20) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'trang_thai_hoan_hang')
    ALTER TABLE HoaDon ADD trang_thai_hoan_hang VARCHAR(50) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ngay_xac_nhan_hoan_hang')
    ALTER TABLE HoaDon ADD ngay_xac_nhan_hoan_hang DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ma_giam_gia_ap_dung')
    ALTER TABLE HoaDon ADD ma_giam_gia_ap_dung VARCHAR(50) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ten_giam_gia_ap_dung')
    ALTER TABLE HoaDon ADD ten_giam_gia_ap_dung VARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'mo_ta_giam_gia_snapshot')
    ALTER TABLE HoaDon ADD mo_ta_giam_gia_snapshot VARCHAR(500) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'id_nhan_vien_xac_nhan')
    ALTER TABLE HoaDon ADD id_nhan_vien_xac_nhan INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'id_nhan_vien_xac_nhan_hoan_tien')
    ALTER TABLE HoaDon ADD id_nhan_vien_xac_nhan_hoan_tien INT NULL;

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_HoaDon_NhanVien_XacNhan')
    ALTER TABLE HoaDon ADD CONSTRAINT FK_HoaDon_NhanVien_XacNhan FOREIGN KEY (id_nhan_vien_xac_nhan) REFERENCES NhanVien(id);

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_HoaDon_NhanVien_XacNhanHoanTien')
    ALTER TABLE HoaDon ADD CONSTRAINT FK_HoaDon_NhanVien_XacNhanHoanTien FOREIGN KEY (id_nhan_vien_xac_nhan_hoan_tien) REFERENCES NhanVien(id);
GO

-- 13. Table DanhGia
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'ngay_an_binh_luan')
    ALTER TABLE DanhGia ADD ngay_an_binh_luan DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'ngay_hien_binh_luan')
    ALTER TABLE DanhGia ADD ngay_hien_binh_luan DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'ngay_an_hinh_anh')
    ALTER TABLE DanhGia ADD ngay_an_hinh_anh DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'ngay_hien_hinh_anh')
    ALTER TABLE DanhGia ADD ngay_hien_hinh_anh DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'id_nguoi_xoa')
    ALTER TABLE DanhGia ADD id_nguoi_xoa INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'id_nguoi_an_binh_luan')
    ALTER TABLE DanhGia ADD id_nguoi_an_binh_luan INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'id_nguoi_hien_binh_luan')
    ALTER TABLE DanhGia ADD id_nguoi_hien_binh_luan INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'id_nguoi_an_hinh_anh')
    ALTER TABLE DanhGia ADD id_nguoi_an_hinh_anh INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DanhGia') AND name = 'id_nguoi_hien_hinh_anh')
    ALTER TABLE DanhGia ADD id_nguoi_hien_hinh_anh INT NULL;

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_DanhGia_TaiKhoan_Xoa')
    ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_TaiKhoan_Xoa FOREIGN KEY (id_nguoi_xoa) REFERENCES TaiKhoan(id);

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_DanhGia_TaiKhoan_AnBL')
    ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_TaiKhoan_AnBL FOREIGN KEY (id_nguoi_an_binh_luan) REFERENCES TaiKhoan(id);

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_DanhGia_TaiKhoan_HienBL')
    ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_TaiKhoan_HienBL FOREIGN KEY (id_nguoi_hien_binh_luan) REFERENCES TaiKhoan(id);

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_DanhGia_TaiKhoan_AnHA')
    ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_TaiKhoan_AnHA FOREIGN KEY (id_nguoi_an_hinh_anh) REFERENCES TaiKhoan(id);

IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_DanhGia_TaiKhoan_HienHA')
    ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_TaiKhoan_HienHA FOREIGN KEY (id_nguoi_hien_hinh_anh) REFERENCES TaiKhoan(id);

-- Dynamically drop check constraint on so_sao if exists, alter type, and recreate constraint
IF EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CK_DanhGia_SoSao')
    ALTER TABLE DanhGia DROP CONSTRAINT CK_DanhGia_SoSao;
ALTER TABLE DanhGia ALTER COLUMN so_sao FLOAT NOT NULL;
ALTER TABLE DanhGia ADD CONSTRAINT CK_DanhGia_SoSao CHECK (so_sao >= 1.0 AND so_sao <= 5.0);
GO

-- 14. Table DonViVanChuyen
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'so_hotline')
    ALTER TABLE DonViVanChuyen ADD so_hotline VARCHAR(20) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'trang_web')
    ALTER TABLE DonViVanChuyen ADD trang_web VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'ma_token')
    ALTER TABLE DonViVanChuyen ADD ma_token VARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'ma_client')
    ALTER TABLE DonViVanChuyen ADD ma_client VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'dia_chi_kho')
    ALTER TABLE DonViVanChuyen ADD dia_chi_kho NVARCHAR(500) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'phi_noi_dia')
    ALTER TABLE DonViVanChuyen ADD phi_noi_dia DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DonViVanChuyen') AND name = 'phi_toan_quoc')
    ALTER TABLE DonViVanChuyen ADD phi_toan_quoc DECIMAL(18,2) NULL;
GO

-- 15. Table SoDiaChi
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SoDiaChi') AND name = 'thanh_pho')
    ALTER TABLE SoDiaChi ADD thanh_pho NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SoDiaChi') AND name = 'vi_do')
    ALTER TABLE SoDiaChi ADD vi_do FLOAT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SoDiaChi') AND name = 'kinh_do')
    ALTER TABLE SoDiaChi ADD kinh_do FLOAT NULL;
GO

UPDATE SoDiaChi SET vi_do = latitude, kinh_do = longitude;
GO

-- 16. Table NhanVien
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('NhanVien') AND name = 'ho_ten')
    ALTER TABLE NhanVien ADD ho_ten NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('NhanVien') AND name = 'chuc_vu')
    ALTER TABLE NhanVien ADD chuc_vu NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('NhanVien') AND name = 'so_dien_thoai')
    ALTER TABLE NhanVien ADD so_dien_thoai VARCHAR(15) NULL;
GO

UPDATE NhanVien SET ho_ten = ho_ten_nv;
UPDATE NhanVien SET chuc_vu = N'Nhân Viên' WHERE chuc_vu IS NULL;
UPDATE NhanVien SET so_dien_thoai = '0123456789' WHERE so_dien_thoai IS NULL;

ALTER TABLE NhanVien ALTER COLUMN ho_ten NVARCHAR(100) NOT NULL;
ALTER TABLE NhanVien ALTER COLUMN chuc_vu NVARCHAR(100) NOT NULL;
ALTER TABLE NhanVien ALTER COLUMN so_dien_thoai VARCHAR(15) NOT NULL;

IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_NhanVien_Ma' AND type = 'UQ')
    ALTER TABLE NhanVien DROP CONSTRAINT UQ_NhanVien_Ma;
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('NhanVien') AND name = 'ma_nhan_vien')
    ALTER TABLE NhanVien DROP COLUMN ma_nhan_vien;
GO

-- 17. Table KhachHang
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('KhachHang') AND name = 'so_dien_thoai_kh')
    ALTER TABLE KhachHang ADD so_dien_thoai_kh VARCHAR(15) NULL;
GO

UPDATE KhachHang SET so_dien_thoai_kh = sdt;
UPDATE KhachHang SET so_dien_thoai_kh = '0123456789' WHERE so_dien_thoai_kh IS NULL;
ALTER TABLE KhachHang ALTER COLUMN so_dien_thoai_kh VARCHAR(15) NOT NULL;
GO

-- 18. Table EditLog
DROP TABLE IF EXISTS EditLog;
CREATE TABLE EditLog (
    id BIGINT IDENTITY(1,1) CONSTRAINT PK_EditLog PRIMARY KEY,
    id_tai_khoan INT NULL,
    ten_bang VARCHAR(100) NOT NULL,
    id_ban_ghi BIGINT NOT NULL,
    hanh_dong VARCHAR(20) NOT NULL,
    gia_tri_cu NVARCHAR(MAX) NULL,
    gia_tri_moi NVARCHAR(MAX) NULL,
    thoi_gian DATETIME NOT NULL CONSTRAINT DF_EditLog_ThoiGian DEFAULT GETDATE(),
    dia_chi_ip VARCHAR(50) NULL,
    ghi_chu VARCHAR(500) NULL,
    vai_tro_thuc_hien VARCHAR(20) NULL
);
ALTER TABLE EditLog ADD CONSTRAINT FK_EditLog_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES TaiKhoan(id);
GO

-- 19. Table GiaoDichThanhToan
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('GiaoDichThanhToan') AND name = 'cong_thanh_toan')
    ALTER TABLE GiaoDichThanhToan ADD cong_thanh_toan VARCHAR(50) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('GiaoDichThanhToan') AND name = 'trang_thai')
    ALTER TABLE GiaoDichThanhToan ADD trang_thai VARCHAR(50) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('GiaoDichThanhToan') AND name = 'du_lieu_tho')
    ALTER TABLE GiaoDichThanhToan ADD du_lieu_tho NVARCHAR(MAX) NULL;
GO

UPDATE GiaoDichThanhToan SET cong_thanh_toan = gateway, trang_thai = status;
ALTER TABLE GiaoDichThanhToan ALTER COLUMN cong_thanh_toan VARCHAR(50) NOT NULL;
ALTER TABLE GiaoDichThanhToan ALTER COLUMN trang_thai VARCHAR(50) NOT NULL;
GO

-- 20. Table HinhAnhSanPham
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HinhAnhSanPham') AND name = 'mau_sac')
    ALTER TABLE HinhAnhSanPham ADD mau_sac NVARCHAR(50) NULL;
GO

-- 21. Table HoaDonChiTiet
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'gia_niem_yet')
    ALTER TABLE HoaDonChiTiet ADD gia_niem_yet DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'phan_tram_giam')
    ALTER TABLE HoaDonChiTiet ADD phan_tram_giam DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'so_tien_giam_san_pham')
    ALTER TABLE HoaDonChiTiet ADD so_tien_giam_san_pham DECIMAL(18,2) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'ten_dot_giam_gia')
    ALTER TABLE HoaDonChiTiet ADD ten_dot_giam_gia NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'id_dot_giam_gia')
    ALTER TABLE HoaDonChiTiet ADD id_dot_giam_gia INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'ten_san_pham_snapshot')
    ALTER TABLE HoaDonChiTiet ADD ten_san_pham_snapshot NVARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'ma_hang_snapshot')
    ALTER TABLE HoaDonChiTiet ADD ma_hang_snapshot VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'thuoc_tinh_snapshot')
    ALTER TABLE HoaDonChiTiet ADD thuoc_tinh_snapshot NVARCHAR(500) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'thuong_hieu_snapshot')
    ALTER TABLE HoaDonChiTiet ADD thuong_hieu_snapshot NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDonChiTiet') AND name = 'danh_muc_snapshot')
    ALTER TABLE HoaDonChiTiet ADD danh_muc_snapshot NVARCHAR(100) NULL;
GO

-- 23. Table BlogComment
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('BlogComment') AND name = 'ngay_xoa')
    ALTER TABLE BlogComment ADD ngay_xoa DATETIME NULL;
GO

-- 24. Table ChatConversation
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversation') AND name = 'ngay_cap_nhat')
    ALTER TABLE ChatConversation ADD ngay_cap_nhat DATETIME NULL;
GO

-- 25. Table PhuongThucThanhToan
-- Drop UQ_PhuongThucThanhToan_Ma constraint and replace with filtered index
IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_PhuongThucThanhToan_Ma' AND parent_object_id = OBJECT_ID('PhuongThucThanhToan'))
    ALTER TABLE PhuongThucThanhToan DROP CONSTRAINT UQ_PhuongThucThanhToan_Ma;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_PTTT_Ma_Unique' AND object_id = OBJECT_ID('PhuongThucThanhToan'))
    CREATE UNIQUE NONCLUSTERED INDEX IX_PTTT_Ma_Unique ON PhuongThucThanhToan(ma_phuong_thuc) WHERE ma_phuong_thuc IS NOT NULL;
GO

-- 26. Table ChatConversation (continued)
-- Drop UQ_ChatConversation_Session constraint and replace with filtered index
IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_ChatConversation_Session' AND parent_object_id = OBJECT_ID('ChatConversation'))
    ALTER TABLE ChatConversation DROP CONSTRAINT UQ_ChatConversation_Session;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ChatConversation_Session_Unique' AND object_id = OBJECT_ID('ChatConversation'))
    CREATE UNIQUE NONCLUSTERED INDEX IX_ChatConversation_Session_Unique ON ChatConversation(session_id) WHERE session_id IS NOT NULL;
GO

-- 27. Table MaKhoiPhuc
-- Drop index first if exists to prevent dependency errors during column alter
IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_MaKhoiPhuc_Token_Unique' AND object_id = OBJECT_ID('MaKhoiPhuc'))
    DROP INDEX IX_MaKhoiPhuc_Token_Unique ON MaKhoiPhuc;

IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ_MaKhoiPhuc_Token' AND parent_object_id = OBJECT_ID('MaKhoiPhuc'))
    ALTER TABLE MaKhoiPhuc DROP CONSTRAINT UQ_MaKhoiPhuc_Token;

ALTER TABLE MaKhoiPhuc ALTER COLUMN token VARCHAR(255) NULL;
ALTER TABLE MaKhoiPhuc ALTER COLUMN ngay_het_han DATETIME NULL;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_MaKhoiPhuc_Token_Unique' AND object_id = OBJECT_ID('MaKhoiPhuc'))
    CREATE UNIQUE NONCLUSTERED INDEX IX_MaKhoiPhuc_Token_Unique ON MaKhoiPhuc(token) WHERE token IS NOT NULL;
GO
