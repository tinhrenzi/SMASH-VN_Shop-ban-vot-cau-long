-- Thêm các cột trạng thái kiểm duyệt độc lập vào bảng DanhGia
ALTER TABLE DanhGia ADD an_binh_luan BIT NOT NULL DEFAULT 0;
ALTER TABLE DanhGia ADD an_hinh_anh BIT NOT NULL DEFAULT 0;

-- Thêm cột ngày chỉnh sửa vào bảng DanhGia
ALTER TABLE DanhGia ADD ngay_cap_nhat DATETIME NULL;

-- Thêm các cột audit quản trị cho Bình luận độc lập vào bảng DanhGia
ALTER TABLE DanhGia ADD id_nguoi_an_binh_luan INT NULL;
ALTER TABLE DanhGia ADD ngay_an_binh_luan DATETIME NULL;
ALTER TABLE DanhGia ADD id_nguoi_hien_binh_luan INT NULL;
ALTER TABLE DanhGia ADD ngay_hien_binh_luan DATETIME NULL;

-- Thêm các cột audit quản trị cho Hình ảnh độc lập vào bảng DanhGia
ALTER TABLE DanhGia ADD id_nguoi_an_hinh_anh INT NULL;
ALTER TABLE DanhGia ADD ngay_an_hinh_anh DATETIME NULL;
ALTER TABLE DanhGia ADD id_nguoi_hien_hinh_anh INT NULL;
ALTER TABLE DanhGia ADD ngay_hien_hinh_anh DATETIME NULL;

-- Thêm cột cờ xóa mềm (Soft Delete) vào bảng DanhGia
ALTER TABLE DanhGia ADD da_xoa BIT NOT NULL DEFAULT 0;
ALTER TABLE DanhGia ADD ngay_xoa DATETIME NULL;
ALTER TABLE DanhGia ADD id_nguoi_xoa INT NULL;

-- Tạo khóa ngoại cho audit và soft delete fields
ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_NguoiAnBL FOREIGN KEY (id_nguoi_an_binh_luan) REFERENCES TaiKhoan(id);
ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_NguoiHienBL FOREIGN KEY (id_nguoi_hien_binh_luan) REFERENCES TaiKhoan(id);
ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_NguoiAnHA FOREIGN KEY (id_nguoi_an_hinh_anh) REFERENCES TaiKhoan(id);
ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_NguoiHienHA FOREIGN KEY (id_nguoi_hien_hinh_anh) REFERENCES TaiKhoan(id);
ALTER TABLE DanhGia ADD CONSTRAINT FK_DanhGia_NguoiXoa FOREIGN KEY (id_nguoi_xoa) REFERENCES TaiKhoan(id);

-- Tạo ràng buộc duy nhất (Unique Constraint) cho cặp (id_khach_hang, id_san_pham) để tránh trùng lặp
ALTER TABLE DanhGia ADD CONSTRAINT UQ_KhachHang_SanPham UNIQUE(id_khach_hang, id_san_pham);

-- Thêm các cột thống kê cache đánh giá vào bảng SanPham
ALTER TABLE SanPham ADD so_danh_gia INT NOT NULL DEFAULT 0;
ALTER TABLE SanPham ADD diem_trung_binh FLOAT NOT NULL DEFAULT 0.0;

-- Tạo bảng ảnh đánh giá (DanhGiaAnh) quan hệ 1-N với DanhGia
CREATE TABLE DanhGiaAnh (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_danh_gia INT NOT NULL,
    duong_dan NVARCHAR(255) NOT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_DanhGiaAnh_DanhGia FOREIGN KEY (id_danh_gia) REFERENCES DanhGia(id) ON DELETE CASCADE
);

-- Tạo các chỉ mục tối ưu hóa truy vấn
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_DANHGIA_SANPHAM' AND object_id = OBJECT_ID('DanhGia'))
BEGIN
    CREATE INDEX IDX_DANHGIA_SANPHAM ON DanhGia(id_san_pham);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_DANHGIA_NGAY_DANHGIA' AND object_id = OBJECT_ID('DanhGia'))
BEGIN
    CREATE INDEX IDX_DANHGIA_NGAY_DANHGIA ON DanhGia(ngay_danh_gia DESC);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_DANHGIA_AN_BINH_LUAN' AND object_id = OBJECT_ID('DanhGia'))
BEGIN
    CREATE INDEX IDX_DANHGIA_AN_BINH_LUAN ON DanhGia(an_binh_luan);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_DANHGIA_AN_HINH_ANH' AND object_id = OBJECT_ID('DanhGia'))
BEGIN
    CREATE INDEX IDX_DANHGIA_AN_HINH_ANH ON DanhGia(an_hinh_anh);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_DANHGIAANH_DANHGIA' AND object_id = OBJECT_ID('DanhGiaAnh'))
BEGIN
    CREATE INDEX IDX_DANHGIAANH_DANHGIA ON DanhGiaAnh(id_danh_gia);
END;
