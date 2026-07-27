-- Migration V4: Create ThuocTinh and DanhMucThuocTinh tables for Category Attribute Type Configuration
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='DanhMuc' AND COLUMN_NAME='thuoc_tinh')
BEGIN
    ALTER TABLE DanhMuc DROP COLUMN thuoc_tinh;
END;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='ThuocTinh')
BEGIN
    CREATE TABLE ThuocTinh (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ten_thuoc_tinh NVARCHAR(100) NOT NULL UNIQUE,
        trang_thai BIT NOT NULL DEFAULT 1
    );
END;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='DanhMucThuocTinh')
BEGIN
    CREATE TABLE DanhMucThuocTinh (
        id INT IDENTITY(1,1) PRIMARY KEY,
        id_danh_muc INT NOT NULL CONSTRAINT FK_DM_TT_DanhMuc REFERENCES DanhMuc(id) ON DELETE CASCADE,
        id_thuoc_tinh INT NOT NULL CONSTRAINT FK_DM_TT_ThuocTinh REFERENCES ThuocTinh(id) ON DELETE CASCADE,
        trang_thai BIT NOT NULL DEFAULT 1,
        CONSTRAINT UQ_DanhMuc_ThuocTinh UNIQUE (id_danh_muc, id_thuoc_tinh)
    );
END;

IF NOT EXISTS (SELECT 1 FROM ThuocTinh WHERE ten_thuoc_tinh = N'Màu sắc') INSERT INTO ThuocTinh (ten_thuoc_tinh) VALUES (N'Màu sắc');
IF NOT EXISTS (SELECT 1 FROM ThuocTinh WHERE ten_thuoc_tinh = N'Size') INSERT INTO ThuocTinh (ten_thuoc_tinh) VALUES (N'Size');
IF NOT EXISTS (SELECT 1 FROM ThuocTinh WHERE ten_thuoc_tinh = N'Trọng lượng') INSERT INTO ThuocTinh (ten_thuoc_tinh) VALUES (N'Trọng lượng');
IF NOT EXISTS (SELECT 1 FROM ThuocTinh WHERE ten_thuoc_tinh = N'Sức căng') INSERT INTO ThuocTinh (ten_thuoc_tinh) VALUES (N'Sức căng');
IF NOT EXISTS (SELECT 1 FROM ThuocTinh WHERE ten_thuoc_tinh = N'Chất liệu') INSERT INTO ThuocTinh (ten_thuoc_tinh) VALUES (N'Chất liệu');
IF NOT EXISTS (SELECT 1 FROM ThuocTinh WHERE ten_thuoc_tinh = N'Kiểu dáng') INSERT INTO ThuocTinh (ten_thuoc_tinh) VALUES (N'Kiểu dáng');

-- Seed default category-attribute mappings
DECLARE @idMauSac INT = (SELECT id FROM ThuocTinh WHERE ten_thuoc_tinh = N'Màu sắc');
DECLARE @idSize INT = (SELECT id FROM ThuocTinh WHERE ten_thuoc_tinh = N'Size');
DECLARE @idTrongLuong INT = (SELECT id FROM ThuocTinh WHERE ten_thuoc_tinh = N'Trọng lượng');
DECLARE @idSucCang INT = (SELECT id FROM ThuocTinh WHERE ten_thuoc_tinh = N'Sức căng');
DECLARE @idChatLieu INT = (SELECT id FROM ThuocTinh WHERE ten_thuoc_tinh = N'Chất liệu');
DECLARE @idKieuDang INT = (SELECT id FROM ThuocTinh WHERE ten_thuoc_tinh = N'Kiểu dáng');

-- Vợt (id=42)
IF EXISTS (SELECT 1 FROM DanhMuc WHERE id = 42)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 42 AND id_thuoc_tinh = @idMauSac) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (42, @idMauSac);
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 42 AND id_thuoc_tinh = @idTrongLuong) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (42, @idTrongLuong);
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 42 AND id_thuoc_tinh = @idSucCang) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (42, @idSucCang);
END;

-- Giày (id=648)
IF EXISTS (SELECT 1 FROM DanhMuc WHERE id = 648)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 648 AND id_thuoc_tinh = @idMauSac) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (648, @idMauSac);
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 648 AND id_thuoc_tinh = @idSize) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (648, @idSize);
END;

-- Trang Phục (id=467)
IF EXISTS (SELECT 1 FROM DanhMuc WHERE id = 467)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 467 AND id_thuoc_tinh = @idMauSac) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (467, @idMauSac);
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 467 AND id_thuoc_tinh = @idSize) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (467, @idSize);
END;

-- Balo (id=43)
IF EXISTS (SELECT 1 FROM DanhMuc WHERE id = 43)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 43 AND id_thuoc_tinh = @idMauSac) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (43, @idMauSac);
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 43 AND id_thuoc_tinh = @idKieuDang) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (43, @idKieuDang);
    IF NOT EXISTS (SELECT 1 FROM DanhMucThuocTinh WHERE id_danh_muc = 43 AND id_thuoc_tinh = @idChatLieu) INSERT INTO DanhMucThuocTinh (id_danh_muc, id_thuoc_tinh) VALUES (43, @idChatLieu);
END;
