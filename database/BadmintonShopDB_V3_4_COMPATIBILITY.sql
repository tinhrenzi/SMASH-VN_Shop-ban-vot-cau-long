/*
================================================================================
 SMASH VN - CLEAN DATABASE SCHEMA V3.4 (COMPATIBILITY MODE)
 Target DBMS : Microsoft SQL Server 2019/2022+
 Database    : SMDB_FINAL_V3_TEST
 Purpose     : Rebuild database based on BadmintonShopDB_CLEAN_FIXED_ANNOTATED.sql
               while keeping all columns required by current JPA/Hibernate entities.

 WARNING     : SAFE TEST VERSION. This script DROPS and RECREATES database [SMDB_FINAL_V3_TEST], not [SMDB_FINAL].
================================================================================
*/

USE [master];
GO

IF DB_ID(N'SMDB_FINAL_V3_TEST') IS NOT NULL
BEGIN
    ALTER DATABASE [SMDB_FINAL_V3_TEST] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [SMDB_FINAL_V3_TEST];
END
GO

CREATE DATABASE [SMDB_FINAL_V3_TEST];
GO

ALTER DATABASE [SMDB_FINAL_V3_TEST] SET AUTO_CLOSE OFF;
ALTER DATABASE [SMDB_FINAL_V3_TEST] SET AUTO_SHRINK OFF;
ALTER DATABASE [SMDB_FINAL_V3_TEST] SET READ_COMMITTED_SNAPSHOT ON;
ALTER DATABASE [SMDB_FINAL_V3_TEST] SET ALLOW_SNAPSHOT_ISOLATION ON;
ALTER DATABASE [SMDB_FINAL_V3_TEST] SET QUERY_STORE = ON;
GO

USE [SMDB_FINAL_V3_TEST];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
GO

/*===============================================================================
  01. ACCOUNT / PEOPLE
===============================================================================*/

CREATE TABLE dbo.TaiKhoan (
    id                         INT IDENTITY(1,1) NOT NULL,
    email                      VARCHAR(255) NOT NULL,
    mat_khau                   NVARCHAR(255) NULL,
    trang_thai_tai_khoan       VARCHAR(20) NOT NULL CONSTRAINT DF_TaiKhoan_TrangThaiTaiKhoan DEFAULT ('ACTIVE'),
    so_lan_mua_thanh_cong      INT NOT NULL CONSTRAINT DF_TaiKhoan_MuaThanhCong DEFAULT (0),
    vai_tro                    VARCHAR(10) NOT NULL CONSTRAINT DF_TaiKhoan_VaiTro DEFAULT ('KH'),
    trang_thai                 NVARCHAR(50) NOT NULL CONSTRAINT DF_TaiKhoan_TrangThai DEFAULT (N'hoat_dong'),
    token_xac_thuc_khoa        VARCHAR(100) NULL, -- compatibility column
    so_lan_nhac_nho_vi_pham    INT NOT NULL CONSTRAINT DF_TaiKhoan_NhacNho DEFAULT (0),
    ngay_khoa_binh_luan_den    DATETIME2(0) NULL, -- compatibility column
    ngay_vi_pham_gan_nhat      DATETIME2(0) NULL,
    ngay_tao                   DATETIME2(0) NOT NULL CONSTRAINT DF_TaiKhoan_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat              DATETIME2(0) NOT NULL CONSTRAINT DF_TaiKhoan_NgayCapNhat DEFAULT (SYSDATETIME()),
    -- New columns from annotated DDL
    thoi_han_mo_khoa           DATETIME NULL,
    ma_xac_thuc_khoa           NVARCHAR(100) NULL,
    CONSTRAINT PK_TaiKhoan PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_TaiKhoan_Email UNIQUE (email),
    CONSTRAINT CK_TaiKhoan_Email CHECK (email LIKE '%_@_%._%'),
    CONSTRAINT CK_TaiKhoan_VaiTro CHECK (vai_tro IN ('KH','NV','QL','CUSTOMER','STAFF','MANAGER','ADMIN')),
    CONSTRAINT CK_TaiKhoan_TrangThai CHECK (trang_thai IN (N'hoat_dong', N'cho_khoa')),
    CONSTRAINT CK_TaiKhoan_TrangThaiTaiKhoan CHECK (trang_thai_tai_khoan IN ('ACTIVE','GUEST','LOCKED','DISABLED')),
    CONSTRAINT CK_TaiKhoan_Counter CHECK (so_lan_mua_thanh_cong >= 0 AND so_lan_nhac_nho_vi_pham >= 0)
);
GO

CREATE TABLE dbo.KhachHang (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_tai_khoan         INT NOT NULL,
    ho_kh                NVARCHAR(50) NULL, -- compatibility column
    ten_kh               NVARCHAR(50) NULL, -- compatibility column
    so_dien_thoai_kh     VARCHAR(15) NULL,
    nhan_ban_tin         BIT NOT NULL CONSTRAINT DF_KhachHang_NhanBanTin DEFAULT (0), -- compatibility column
    la_tai_khoan_noi_bo  BIT NOT NULL CONSTRAINT DF_KhachHang_LaNoiBo DEFAULT (0), -- compatibility column
    loai_khach_hang      VARCHAR(30) NULL CONSTRAINT DF_KhachHang_Loai DEFAULT ('REGISTERED'),
    nguon_tao            VARCHAR(50) NULL CONSTRAINT DF_KhachHang_Nguon DEFAULT ('SYSTEM'),
    ngay_tao             DATETIME2(0) NULL CONSTRAINT DF_KhachHang_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat        DATETIME2(0) NULL CONSTRAINT DF_KhachHang_NgayCapNhat DEFAULT (SYSDATETIME()),
    -- New columns from annotated DDL
    ho_ten_kh            NVARCHAR(100) NULL,
    la_thanh_vien        BIT NOT NULL CONSTRAINT DF_KhachHang_LaThanhVien DEFAULT (0),
    CONSTRAINT PK_KhachHang PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_KhachHang_TaiKhoan UNIQUE (id_tai_khoan),
    CONSTRAINT FK_KhachHang_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id)
);
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_KhachHang_SoDienThoai ON dbo.KhachHang(so_dien_thoai_kh) WHERE so_dien_thoai_kh IS NOT NULL;
GO

CREATE TABLE dbo.NhanVien (
    id              INT IDENTITY(1,1) NOT NULL,
    id_tai_khoan     INT NOT NULL,
    ho_ten           NVARCHAR(100) NULL, -- compatibility column (Java maps ho_ten)
    chuc_vu          NVARCHAR(100) NOT NULL,
    so_dien_thoai    VARCHAR(15) NULL, -- compatibility column (Java maps so_dien_thoai)
    ngay_tao         DATETIME2(0) NOT NULL CONSTRAINT DF_NhanVien_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat    DATETIME2(0) NOT NULL CONSTRAINT DF_NhanVien_NgayCapNhat DEFAULT (SYSDATETIME()),
    -- New columns from annotated DDL
    ho_ten_nv        NVARCHAR(100) NULL,
    so_dien_thoai_nv NVARCHAR(15) NULL,
    CONSTRAINT PK_NhanVien PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_NhanVien_TaiKhoan UNIQUE (id_tai_khoan),
    CONSTRAINT FK_NhanVien_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id)
);
GO

CREATE TABLE dbo.MaKhoiPhuc (
    id              INT IDENTITY(1,1) NOT NULL,
    id_tai_khoan     INT NOT NULL,
    ma_xac_nhan      VARCHAR(255) NULL, -- compatibility column (Java maps ma_xac_nhan)
    loai_xac_nhan    VARCHAR(50) NOT NULL,
    thoi_gian_het_han DATETIME2(0) NOT NULL,
    da_su_dung       BIT NOT NULL CONSTRAINT DF_MKP_DaSuDung DEFAULT (0),
    ngay_tao         DATETIME2(0) NOT NULL CONSTRAINT DF_MKP_NgayTao DEFAULT (SYSDATETIME()),
    -- New columns from annotated DDL
    ma_khoi_phuc     NVARCHAR(255) NULL,
    CONSTRAINT PK_MaKhoiPhuc PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_MaKhoiPhuc_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id)
);
GO

CREATE TABLE dbo.ThongBao (
    id              INT IDENTITY(1,1) NOT NULL,
    id_tai_khoan     INT NULL,
    tieu_de          NVARCHAR(255) NOT NULL,
    noi_dung         NVARCHAR(MAX) NOT NULL,
    da_doc           BIT NOT NULL CONSTRAINT DF_ThongBao_DaDoc DEFAULT (0),
    loai_thong_bao   VARCHAR(50) NULL,
    ngay_tao         DATETIME2(0) NOT NULL CONSTRAINT DF_ThongBao_NgayTao DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_ThongBao PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_ThongBao_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id) ON DELETE CASCADE
);
GO

/*===============================================================================
  02. CUSTOMER ADDRESS / NEWSLETTER / WISHLIST
===============================================================================*/

CREATE TABLE dbo.SoDiaChi (
    id                         INT IDENTITY(1,1) NOT NULL,
    id_khach_hang               INT NOT NULL,
    ho_nguoi_nhan               NVARCHAR(50) NULL, -- compatibility column
    ten_nguoi_nhan              NVARCHAR(50) NULL, -- compatibility column
    sdt_nguoi_nhan              VARCHAR(15) NOT NULL,
    dia_chi_cu_the              NVARCHAR(255) NOT NULL,
    tinh_thanh                  NVARCHAR(100) NULL,
    quoc_gia                    NVARCHAR(100) NOT NULL CONSTRAINT DF_SoDiaChi_QuocGia DEFAULT (N'Việt Nam'),
    latitude                    FLOAT NULL, -- compatibility column
    longitude                   FLOAT NULL, -- compatibility column
    la_mac_dinh_giao_hang       BIT NOT NULL CONSTRAINT DF_SoDiaChi_DefaultShip DEFAULT (0), -- compatibility column
    la_mac_dinh_thanh_toan      BIT NOT NULL CONSTRAINT DF_SoDiaChi_DefaultPay DEFAULT (0), -- compatibility column
    thanh_pho                   NVARCHAR(100) NULL, -- compatibility column
    vi_do                       FLOAT NULL, -- compatibility column
    kinh_do                     FLOAT NULL, -- compatibility column
    province_id                 INT NULL, -- compatibility column
    district_id                 INT NULL, -- compatibility column
    ward_code                   VARCHAR(20) NULL, -- compatibility column
    province_name               NVARCHAR(100) NULL, -- compatibility column
    district_name               NVARCHAR(100) NULL, -- compatibility column
    ward_name                   NVARCHAR(100) NULL, -- compatibility column
    ngay_tao                    DATETIME2(0) NOT NULL CONSTRAINT DF_SoDiaChi_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat               DATETIME2(0) NULL,
    -- New columns from annotated DDL
    ho_va_ten_nguoi_nhan        NVARCHAR(100) NULL,
    quan_huyen                  NVARCHAR(100) NULL,
    phuong_xa                   NVARCHAR(100) NULL,
    ghn_province_id             INT NULL,
    ghn_district_id             INT NULL,
    ghn_ward_code               NVARCHAR(50) NULL,
    la_mac_dinh                 BIT NOT NULL DEFAULT 0,
    CONSTRAINT PK_SoDiaChi PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_SoDiaChi_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES dbo.KhachHang(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.NewsletterSubscriber (
    id                  INT IDENTITY(1,1) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    gioi_tinh           VARCHAR(10) NULL,
    ngay_dang_ky        DATETIME2(0) NOT NULL CONSTRAINT DF_Newsletter_NgayDangKy DEFAULT (SYSDATETIME()),
    ngay_huy            DATETIME2(0) NULL,
    trang_thai          VARCHAR(50) NOT NULL CONSTRAINT DF_Newsletter_TrangThai DEFAULT ('ACTIVE'),
    token_huy           VARCHAR(100) NULL,
    -- New columns from annotated DDL
    token_huy_dang_ky   NVARCHAR(255) NULL,
    CONSTRAINT PK_NewsletterSubscriber PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_Newsletter_Email UNIQUE (email)
);
GO

/*===============================================================================
  03. PRODUCT CATALOG
===============================================================================*/

CREATE TABLE dbo.DanhMuc (
    id              INT IDENTITY(1,1) NOT NULL,
    ten_danh_muc     NVARCHAR(255) NOT NULL,
    mo_ta            NVARCHAR(500) NULL,
    trang_thai       BIT NOT NULL CONSTRAINT DF_DanhMuc_TrangThai DEFAULT (1),
    CONSTRAINT PK_DanhMuc PRIMARY KEY CLUSTERED (id)
);
GO

CREATE TABLE dbo.ThuongHieu (
    id                INT IDENTITY(1,1) NOT NULL,
    ten_thuong_hieu    NVARCHAR(255) NOT NULL,
    mo_ta              NVARCHAR(500) NULL,
    trang_thai         BIT NOT NULL CONSTRAINT DF_ThuongHieu_TrangThai DEFAULT (1),
    logo               NVARCHAR(255) NULL,
    CONSTRAINT PK_ThuongHieu PRIMARY KEY CLUSTERED (id)
);
GO

CREATE TABLE dbo.SanPham (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_danh_muc         INT NULL,
    id_thuong_hieu      INT NULL,
    id_nhan_vien        INT NULL,
    ten_san_pham        NVARCHAR(255) NOT NULL,
    mo_ta               NVARCHAR(MAX) NULL,
    trang_thai          NVARCHAR(50) NOT NULL CONSTRAINT DF_SanPham_TrangThai DEFAULT (N'dang_ban'), -- String compatibility column
    so_danh_gia         INT NOT NULL CONSTRAINT DF_SanPham_SoDanhGia DEFAULT (0), -- compatibility column
    diem_trung_binh     FLOAT NOT NULL CONSTRAINT DF_SanPham_DiemTB DEFAULT (0.0),
    ngay_tao            DATETIME2(0) NOT NULL CONSTRAINT DF_SanPham_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat       DATETIME2(0) NULL,
    -- New columns from annotated DDL
    so_luot_danh_gia    INT NOT NULL DEFAULT 0,
    CONSTRAINT PK_SanPham PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_SanPham_DanhMuc FOREIGN KEY (id_danh_muc) REFERENCES dbo.DanhMuc(id),
    CONSTRAINT FK_SanPham_ThuongHieu FOREIGN KEY (id_thuong_hieu) REFERENCES dbo.ThuongHieu(id),
    CONSTRAINT FK_SanPham_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES dbo.NhanVien(id)
);
GO

CREATE TABLE dbo.SanPhamChiTiet (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_san_pham         INT NULL,
    mau_sac             NVARCHAR(50) NOT NULL,
    muc_cang            NVARCHAR(50) NULL, -- compatibility column
    trong_luong         NVARCHAR(20) NULL,
    gia_ban             DECIMAL(18,2) NOT NULL,
    so_luong_ton        INT NULL CONSTRAINT DF_SPCT_TonKho DEFAULT (0),
    barcode             VARCHAR(100) NULL,
    chat_lieu           NVARCHAR(100) NULL,
    gia_nhap            DECIMAL(18,2) NULL,
    kich_thuoc          NVARCHAR(50) NULL,
    SKU                 VARCHAR(100) NULL, -- compatibility column
    trang_thai          NVARCHAR(50) NOT NULL CONSTRAINT DF_SPCT_TrangThai DEFAULT (N'dang_ban'), -- String compatibility column
    ngay_tao            DATETIME2(0) NOT NULL CONSTRAINT DF_SPCT_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat       DATETIME2(0) NULL,
    -- New columns from annotated DDL
    suc_cang            NVARCHAR(50) NULL,
    CONSTRAINT PK_SanPhamChiTiet PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_SanPhamChiTiet_SanPham FOREIGN KEY (id_san_pham) REFERENCES dbo.SanPham(id),
    CONSTRAINT UQ_SANPHAM_VARIANT UNIQUE (id_san_pham, mau_sac, muc_cang, trong_luong)
);
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_SanPhamChiTiet_SKU ON dbo.SanPhamChiTiet(SKU) WHERE SKU IS NOT NULL;
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_SanPhamChiTiet_Barcode ON dbo.SanPhamChiTiet(barcode) WHERE barcode IS NOT NULL;
GO

CREATE TABLE dbo.HinhAnhSanPham (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_san_pham_chi_tiet INT NOT NULL,
    duong_dan           NVARCHAR(MAX) NULL, -- compatibility column
    mau_sac             NVARCHAR(50) NULL,
    la_anh_chinh        BIT NOT NULL CONSTRAINT DF_HASP_LaAnhChinh DEFAULT (0),
    -- New columns from annotated DDL
    url_hinh_anh        NVARCHAR(MAX) NULL,
    thu_tu              INT NULL,
    CONSTRAINT PK_HinhAnhSanPham PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_HinhAnhSanPham_SPCT FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES dbo.SanPhamChiTiet(id)
);
GO

CREATE TABLE dbo.SanPhamYeuThich (
    id_khach_hang       INT NOT NULL,
    id_san_pham         INT NOT NULL,
    ngay_them           DATETIME2(0) NOT NULL CONSTRAINT DF_SPYT_NgayThem DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_SanPhamYeuThich PRIMARY KEY CLUSTERED (id_khach_hang, id_san_pham),
    CONSTRAINT FK_SPYT_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES dbo.KhachHang(id) ON DELETE CASCADE,
    CONSTRAINT FK_SPYT_SanPham FOREIGN KEY (id_san_pham) REFERENCES dbo.SanPham(id) ON DELETE CASCADE
);
GO

/*===============================================================================
  04. CART / ORDER / PAYMENT / SHIPPING
===============================================================================*/

CREATE TABLE dbo.PhuongThucThanhToan (
    id                  INT IDENTITY(1,1) NOT NULL,
    ma_phuong_thuc      NVARCHAR(50) NULL,
    ten_phuong_thuc     NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_PhuongThucThanhToan PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_PhuongThucThanhToan_Ma UNIQUE (ma_phuong_thuc)
);
GO

CREATE TABLE dbo.DonViVanChuyen (
    id                  INT IDENTITY(1,1) NOT NULL,
    ma_don_vi            VARCHAR(50) NULL,
    ten_don_vi           NVARCHAR(100) NULL,
    so_hotline           VARCHAR(20) NULL,
    trang_web            VARCHAR(100) NULL,
    ma_token             VARCHAR(255) NULL,
    ma_client            VARCHAR(100) NULL,
    dia_chi_kho          NVARCHAR(500) NULL,
    phi_noi_dia          DECIMAL(18,2) NULL,
    phi_toan_quoc        DECIMAL(18,2) NULL,
    phien_ban            BIGINT NOT NULL CONSTRAINT DF_DVVC_PhienBan DEFAULT (0), -- compatibility column
    kich_hoat            BIT NOT NULL CONSTRAINT DF_DVVC_KichHoat DEFAULT (1),
    CONSTRAINT PK_DonViVanChuyen PRIMARY KEY CLUSTERED (id)
);
GO

CREATE TABLE dbo.TrangThaiGioHang (
    id                  INT IDENTITY(1,1) NOT NULL,
    ma_trang_thai       VARCHAR(50) NULL,
    ten_trang_thai      NVARCHAR(50) NULL,
    CONSTRAINT PK_TrangThaiGioHang PRIMARY KEY CLUSTERED (id)
);
GO

CREATE TABLE dbo.GioHang (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_khach_hang       INT NULL,
    session_id          VARCHAR(100) NULL,
    ngay_tao            DATETIME2(0) NOT NULL CONSTRAINT DF_GioHang_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat       DATETIME2(0) NOT NULL CONSTRAINT DF_GioHang_NgayCapNhat DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_GioHang PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_GioHang_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES dbo.KhachHang(id)
);
GO

CREATE TABLE dbo.GioHangChiTiet (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_gio_hang         INT NULL,
    id_san_pham_chi_tiet INT NULL,
    id_trang_thai       INT NULL,
    so_luong            INT NOT NULL,
    ngay_them           DATETIME2(0) NOT NULL CONSTRAINT DF_GHCT_NgayThem DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_GioHangChiTiet PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_GHCT_GioHang FOREIGN KEY (id_gio_hang) REFERENCES dbo.GioHang(id) ON DELETE CASCADE,
    CONSTRAINT FK_GHCT_SPCT FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES dbo.SanPhamChiTiet(id),
    CONSTRAINT FK_GHCT_TrangThai FOREIGN KEY (id_trang_thai) REFERENCES dbo.TrangThaiGioHang(id)
);
GO

CREATE TABLE dbo.PhieuGiamGia (
    id                         INT IDENTITY(1,1) NOT NULL,
    ma_phieu                   VARCHAR(50) NOT NULL,
    ten_phieu                  NVARCHAR(100) NULL,
    id_nhan_vien               INT NULL,
    don_vi                     VARCHAR(10) NULL,
    gia_tri                    DECIMAL(18,2) NOT NULL,
    gia_tri_giam_toi_da        DECIMAL(18,2) NULL,
    gia_tri_don_hang_toi_thieu DECIMAL(18,2) NULL,
    so_luong_con_lai           INT NULL,
    ngay_bat_dau               DATETIME2(0) NOT NULL,
    ngay_ket_thuc              DATETIME2(0) NOT NULL,
    trang_thai                 VARCHAR(50) NULL,
    loai_giam_gia              NVARCHAR(100) NULL,
    kich_hoat                  BIT NULL CONSTRAINT DF_PGG_KichHoat DEFAULT (1),
    CONSTRAINT PK_PhieuGiamGia PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_PhieuGiamGia_Ma UNIQUE (ma_phieu),
    CONSTRAINT FK_PhieuGiamGia_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES dbo.NhanVien(id)
);
GO

CREATE TABLE dbo.DotGiamGia (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_nhan_vien        INT NULL,
    phan_tram_giam      INT NULL,
    ngay_bat_dau        DATETIME2(0) NOT NULL,
    ngay_ket_thuc       DATETIME2(0) NOT NULL,
    trang_thai          VARCHAR(50) NULL,
    ten_chien_dich      NVARCHAR(255) NULL,
    loai_giam_gia       NVARCHAR(100) NULL,
    kich_hoat           BIT NULL CONSTRAINT DF_DGG_KichHoat DEFAULT (1),
    ngay_tao            DATETIME2(0) NOT NULL CONSTRAINT DF_DGG_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat       DATETIME2(0) NULL,
    CONSTRAINT PK_DotGiamGia PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_DotGiamGia_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES dbo.NhanVien(id)
);
GO

CREATE TABLE dbo.SanPham_DotGiamGia (
    id_dot_giam_gia     INT NOT NULL,
    id_san_pham         INT NOT NULL,
    CONSTRAINT PK_SanPham_DotGiamGia PRIMARY KEY CLUSTERED (id_dot_giam_gia, id_san_pham),
    CONSTRAINT FK_SP_DGG_DGG FOREIGN KEY (id_dot_giam_gia) REFERENCES dbo.DotGiamGia(id) ON DELETE CASCADE,
    CONSTRAINT FK_SP_DGG_SP FOREIGN KEY (id_san_pham) REFERENCES dbo.SanPham(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.HoaDon (
    id                                  INT IDENTITY(1,1) NOT NULL,
    id_khach_hang                        INT NULL,
    id_nhan_vien                         INT NULL,
    id_phuong_thuc_thanh_toan            INT NULL,
    id_phieu_giam_gia                    INT NULL,
    id_don_vi_van_chuyen                 INT NULL,
    id_dia_chi                           INT NULL,
    ngay_tao                             DATETIME2(0) NULL CONSTRAINT DF_HoaDon_NgayTao DEFAULT (SYSDATETIME()),
    tong_tien                            DECIMAL(18,2) NULL,
    trang_thai_don_hang                  NVARCHAR(50) NULL CONSTRAINT DF_HoaDon_TrangThaiDon DEFAULT (N'cho_xac_nhan'),
    trang_thai_thanh_toan                VARCHAR(50) NULL CONSTRAINT DF_HoaDon_TrangThaiTT DEFAULT ('CHO_THANH_TOAN'),
    dia_chi_nhan                         NVARCHAR(500) NULL,
    sdt_nhan                             VARCHAR(15) NULL,
    ghi_chu                              NVARCHAR(500) NULL,
    ma_giao_dich                         VARCHAR(100) NULL,
    nguoi_xac_nhan_thanh_toan            NVARCHAR(100) NULL,
    thoi_gian_xac_nhan                   DATETIME2(0) NULL,
    ma_don_hang                          VARCHAR(50) NULL,
    phi_van_chuyen                       DECIMAL(18,2) NULL CONSTRAINT DF_HoaDon_PhiShip DEFAULT (0),
    phuong_thuc_thanh_toan               VARCHAR(50) NULL, -- compatibility column
    phan_hoi_cong_tt                     NVARCHAR(MAX) NULL, -- compatibility column
    ngay_thanh_toan                      DATETIME2(0) NULL,
    ma_giao_dich_ung_dung                VARCHAR(100) NULL, -- compatibility column
    ghn_order_code                       VARCHAR(50) NULL, -- compatibility column
    ghn_status                           VARCHAR(100) NULL, -- compatibility column
    ghn_to_district_id                   INT NULL, -- compatibility column
    ghn_to_ward_code                     VARCHAR(20) NULL, -- compatibility column
    trang_thai_hoan_hang                 VARCHAR(50) NULL, -- compatibility column
    ngay_xac_nhan_hoan_hang              DATETIME2(0) NULL, -- compatibility column
    id_nhan_vien_xac_nhan                INT NULL, -- compatibility column
    id_nhan_vien_xac_nhan_hoan_tien      INT NULL, -- compatibility column
    so_tien_giam_voucher                 DECIMAL(18,2) NULL CONSTRAINT DF_HoaDon_GiamVoucher DEFAULT (0), -- compatibility column
    loai_don_hang                        VARCHAR(30) NULL CONSTRAINT DF_HoaDon_Loai DEFAULT ('ONLINE'),
    ngay_cap_nhat                        DATETIME2(0) NULL CONSTRAINT DF_HoaDon_NgayCapNhat DEFAULT (SYSDATETIME()),
    ten_nguoi_nhan                       NVARCHAR(100) NULL,
    tong_tien_hang                       DECIMAL(18,2) NULL CONSTRAINT DF_HoaDon_TienHang DEFAULT (0), -- compatibility column
    so_tien_giam_gia                     DECIMAL(18,2) NULL CONSTRAINT DF_HoaDon_GiamGia DEFAULT (0),
    ma_giam_gia_ap_dung                  VARCHAR(50) NULL,
    ten_giam_gia_ap_dung                 VARCHAR(255) NULL, -- compatibility column
    mo_ta_giam_gia_snapshot              VARCHAR(500) NULL, -- compatibility column
    -- New columns from annotated DDL
    tien_hang                            DECIMAL(18,2) NULL,
    ly_do_huy                            NVARCHAR(500) NULL,
    ly_do_hoan_tien                      NVARCHAR(500) NULL,
    CONSTRAINT PK_HoaDon PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES dbo.KhachHang(id),
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES dbo.NhanVien(id),
    CONSTRAINT FK_HoaDon_PTTT FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES dbo.PhuongThucThanhToan(id),
    CONSTRAINT FK_HoaDon_PGG FOREIGN KEY (id_phieu_giam_gia) REFERENCES dbo.PhieuGiamGia(id),
    CONSTRAINT FK_HoaDon_DVVC FOREIGN KEY (id_don_vi_van_chuyen) REFERENCES dbo.DonViVanChuyen(id),
    CONSTRAINT FK_HoaDon_SoDiaChi FOREIGN KEY (id_dia_chi) REFERENCES dbo.SoDiaChi(id),
    CONSTRAINT FK_HoaDon_NVXacNhan FOREIGN KEY (id_nhan_vien_xac_nhan) REFERENCES dbo.NhanVien(id),
    CONSTRAINT FK_HoaDon_NVHoanTien FOREIGN KEY (id_nhan_vien_xac_nhan_hoan_tien) REFERENCES dbo.NhanVien(id)
);
GO
CREATE UNIQUE NONCLUSTERED INDEX UQ_HoaDon_MaDonHang ON dbo.HoaDon(ma_don_hang) WHERE ma_don_hang IS NOT NULL;
GO

CREATE TABLE dbo.HoaDonChiTiet (
    id                         INT IDENTITY(1,1) NOT NULL,
    id_hoa_don                  INT NOT NULL,
    id_san_pham_chi_tiet        INT NOT NULL,
    so_luong                    INT NOT NULL,
    don_gia                     DECIMAL(18,2) NOT NULL,
    thanh_tien                  DECIMAL(18,2) NULL,
    gia_niem_yet                DECIMAL(18,2) NULL,
    phan_tram_giam              DECIMAL(18,2) NULL CONSTRAINT DF_HDCT_PhanTramGiam DEFAULT (0),
    so_tien_giam_san_pham       DECIMAL(18,2) NULL CONSTRAINT DF_HDCT_GiamSP DEFAULT (0),
    ten_dot_giam_gia            NVARCHAR(100) NULL,
    id_dot_giam_gia             INT NULL,
    ten_san_pham_snapshot       NVARCHAR(255) NULL,
    ma_hang_snapshot            VARCHAR(100) NULL, -- compatibility column
    thuoc_tinh_snapshot         NVARCHAR(500) NULL,
    thuong_hieu_snapshot        NVARCHAR(100) NULL,
    danh_muc_snapshot           NVARCHAR(100) NULL,
    -- New columns from annotated DDL
    gia_goc                     DECIMAL(18,2) NULL,
    gia_sau_giam                DECIMAL(18,2) NULL,
    sku_snapshot                NVARCHAR(100) NULL,
    ngay_tao                    DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_HoaDonChiTiet PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_HDCT_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES dbo.HoaDon(id) ON DELETE CASCADE,
    CONSTRAINT FK_HDCT_SPCT FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES dbo.SanPhamChiTiet(id),
    CONSTRAINT FK_HDCT_DotGiamGia FOREIGN KEY (id_dot_giam_gia) REFERENCES dbo.DotGiamGia(id)
);
GO

CREATE TABLE dbo.GiaoDichThanhToan (
    id                   INT IDENTITY(1,1) NOT NULL,
    ma_giao_dich          VARCHAR(100) NOT NULL,
    id_hoa_don            INT NULL,
    so_tien               DECIMAL(18,2) NOT NULL,
    cong_thanh_toan       VARCHAR(50) NOT NULL,
    trang_thai            VARCHAR(50) NOT NULL CONSTRAINT DF_GDTT_TrangThai DEFAULT ('PENDING'),
    du_lieu_tho           NVARCHAR(MAX) NULL,
    ngay_tao              DATETIME2(0) NOT NULL CONSTRAINT DF_GDTT_NgayTao DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_GiaoDichThanhToan PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_GiaoDichThanhToan_Ma UNIQUE (ma_giao_dich),
    CONSTRAINT FK_GDTT_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES dbo.HoaDon(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.TichHopVanChuyen (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_hoa_don           INT NOT NULL,
    nha_cung_cap         NVARCHAR(50) NOT NULL DEFAULT 'GHN',
    ma_don_hang_ngoai    NVARCHAR(100) NULL,
    ma_van_don           NVARCHAR(100) NULL,
    trang_thai           NVARCHAR(100) NULL,
    du_lieu_yeu_cau      NVARCHAR(MAX) NULL,
    du_lieu_phan_hoi     NVARCHAR(MAX) NULL,
    ngay_tao             DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_TichHopVanChuyen PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_TichHopVanChuyen_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES dbo.HoaDon(id)
);
GO

CREATE TABLE dbo.LichSuTrangThaiDonHang (
    id                  BIGINT IDENTITY(1,1) NOT NULL,
    id_hoa_don           INT NOT NULL,
    id_nhan_vien         INT NULL,
    hanh_dong            NVARCHAR(100) NOT NULL,
    trang_thai_cu        NVARCHAR(50) NULL,
    trang_thai_moi        NVARCHAR(50) NULL,
    ghi_chu              NVARCHAR(500) NULL,
    thoi_gian            DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_LichSuTrangThaiDonHang PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_LichSu_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES dbo.HoaDon(id) ON DELETE CASCADE,
    CONSTRAINT FK_LichSu_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES dbo.NhanVien(id)
);
GO

/*===============================================================================
  05. REVIEW / MODERATION
===============================================================================*/

CREATE TABLE dbo.DanhGia (
    id                           INT IDENTITY(1,1) NOT NULL,
    id_khach_hang                 INT NOT NULL,
    id_san_pham                   INT NOT NULL,
    so_sao                        FLOAT NOT NULL,
    noi_dung                      NVARCHAR(MAX) NULL,
    ngay_tao                      DATETIME2(0) NOT NULL CONSTRAINT DF_DanhGia_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat                 DATETIME2(0) NULL,
    an_binh_luan                  BIT NOT NULL CONSTRAINT DF_DanhGia_AnBL DEFAULT (0),
    an_hinh_anh                   BIT NOT NULL CONSTRAINT DF_DanhGia_AnHA DEFAULT (0),
    da_xoa                        BIT NOT NULL CONSTRAINT DF_DanhGia_DaXoa DEFAULT (0),
    ngay_xoa                      DATETIME2(0) NULL,
    id_nguoi_xoa                  INT NULL,
    id_nguoi_an_binh_luan         INT NULL,
    ngay_an_binh_luan             DATETIME2(0) NULL,
    id_nguoi_hien_binh_luan       INT NULL,
    ngay_hien_binh_luan           DATETIME2(0) NULL,
    id_nguoi_an_hinh_anh          INT NULL,
    ngay_an_hinh_anh              DATETIME2(0) NULL,
    id_nguoi_hien_hinh_anh        INT NULL,
    ngay_hien_hinh_anh            DATETIME2(0) NULL,
    CONSTRAINT PK_DanhGia PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_KhachHang_SanPham UNIQUE (id_khach_hang, id_san_pham),
    CONSTRAINT FK_DanhGia_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES dbo.KhachHang(id),
    CONSTRAINT FK_DanhGia_SanPham FOREIGN KEY (id_san_pham) REFERENCES dbo.SanPham(id),
    CONSTRAINT FK_DanhGia_NguoiXoa FOREIGN KEY (id_nguoi_xoa) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT FK_DanhGia_NguoiAnBL FOREIGN KEY (id_nguoi_an_binh_luan) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT FK_DanhGia_NguoiHienBL FOREIGN KEY (id_nguoi_hien_binh_luan) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT FK_DanhGia_NguoiAnHA FOREIGN KEY (id_nguoi_an_hinh_anh) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT FK_DanhGia_NguoiHienHA FOREIGN KEY (id_nguoi_hien_hinh_anh) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT CK_DanhGia_SoSao CHECK (so_sao BETWEEN 1 AND 5)
);
GO

-- Renamed to match Java Entity name (DanhGiaAnh)
CREATE TABLE dbo.DanhGiaAnh (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_danh_gia          INT NOT NULL,
    duong_dan            VARCHAR(255) NOT NULL, -- compatibility column
    ngay_tao             DATETIME2(0) NOT NULL CONSTRAINT DF_DanhGiaAnh_NgayTao DEFAULT (SYSDATETIME()),
    -- New columns from annotated DDL
    url_hinh_anh         NVARCHAR(MAX) NULL,
    thu_tu               INT NULL,
    CONSTRAINT PK_DanhGiaAnh PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_DanhGiaAnh_DanhGia FOREIGN KEY (id_danh_gia) REFERENCES dbo.DanhGia(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.CommentModerationKeyword (
    id                  INT IDENTITY(1,1) NOT NULL,
    tu_khoa              NVARCHAR(100) NOT NULL,
    kich_hoat            BIT NOT NULL CONSTRAINT DF_CMK_KichHoat DEFAULT (1),
    ngay_tao             DATETIME2(0) NOT NULL CONSTRAINT DF_CMK_NgayTao DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_CommentModerationKeyword PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_CMK_Keyword UNIQUE (tu_khoa)
);
GO

CREATE TABLE dbo.CommentViolationLog (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_tai_khoan         INT NOT NULL,
    id_danh_gia          INT NULL,
    id_san_pham          INT NOT NULL,
    noi_dung_goc         NVARCHAR(MAX) NOT NULL,
    noi_dung_da_loc      NVARCHAR(MAX) NOT NULL,
    muc_do_vi_pham       NVARCHAR(50) NOT NULL,
    so_lan_vi_pham       INT NOT NULL,
    thoi_han_khoa        NVARCHAR(100) NULL,
    ngay_vi_pham         DATETIME NOT NULL,
    ngay_tao             DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_CommentViolationLog PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_CommentViolationLog_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT FK_CommentViolationLog_DanhGia FOREIGN KEY (id_danh_gia) REFERENCES dbo.DanhGia(id),
    CONSTRAINT FK_CommentViolationLog_SanPham FOREIGN KEY (id_san_pham) REFERENCES dbo.SanPham(id)
);
GO

/*===============================================================================
  06. BLOG / CHAT / AUDIT
===============================================================================*/

CREATE TABLE dbo.Blog (
    id                INT IDENTITY(1,1) NOT NULL,
    tieu_de            NVARCHAR(255) NOT NULL,
    duong_dan          VARCHAR(255) NOT NULL,
    tom_tat            NVARCHAR(1000) NULL,
    noi_dung           TEXT NOT NULL, -- Keep TEXT definition matching V1
    hinh_anh           VARCHAR(500) NULL,
    ngay_dang          DATE NULL,
    danh_muc           NVARCHAR(255) NULL,
    the                NVARCHAR(255) NULL,
    trang_thai         VARCHAR(30) NOT NULL CONSTRAINT DF_Blog_TrangThai DEFAULT ('DRAFT'),
    da_xoa             BIT NOT NULL CONSTRAINT DF_Blog_DaXoa DEFAULT (0),
    ngay_xoa           DATETIME2(0) NULL,
    id_tai_khoan       INT NULL,
    ngay_tao           DATETIME2(0) NOT NULL CONSTRAINT DF_Blog_NgayTao DEFAULT (SYSDATETIME()),
    updated_by         VARCHAR(255) NULL, -- compatibility column
    ngay_cap_nhat      DATETIME2(0) NULL,
    CONSTRAINT PK_Blog PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_Blog_DuongDan UNIQUE (duong_dan),
    CONSTRAINT FK_Blog_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT CK_Blog_TrangThai CHECK (trang_thai IN ('DRAFT','PUBLISHED'))
);
GO

CREATE TABLE dbo.BlogComment (
    id                    INT IDENTITY(1,1) NOT NULL,
    id_blog                INT NOT NULL,
    id_tai_khoan           INT NOT NULL,
    noi_dung               NVARCHAR(1000) NOT NULL,
    ngay_tao               DATETIME2(0) NOT NULL CONSTRAINT DF_BlogComment_NgayTao DEFAULT (SYSDATETIME()),
    da_xoa                 BIT NOT NULL CONSTRAINT DF_BlogComment_DaXoa DEFAULT (0),
    ngay_xoa               DATETIME2(0) NULL,
    ly_do_xoa              NVARCHAR(500) NULL,
    id_binh_luan_cha       INT NULL,
    CONSTRAINT PK_BlogComment PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_BlogComment_Blog FOREIGN KEY (id_blog) REFERENCES dbo.Blog(id) ON DELETE CASCADE,
    CONSTRAINT FK_BlogComment_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id),
    CONSTRAINT FK_BlogComment_Cha FOREIGN KEY (id_binh_luan_cha) REFERENCES dbo.BlogComment(id)
);
GO

CREATE TABLE dbo.ChatConversation (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_khach_hang        INT NOT NULL,
    tieu_de              NVARCHAR(255) NULL,
    ngay_tao             DATETIME2(0) NOT NULL CONSTRAINT DF_ChatConv_NgayTao DEFAULT (SYSDATETIME()),
    ngay_cap_nhat        DATETIME2(0) NOT NULL CONSTRAINT DF_ChatConv_NgayCapNhat DEFAULT (SYSDATETIME()),
    session_id           VARCHAR(100) NULL, -- compatibility column
    trang_thai           NVARCHAR(20) NULL,
    CONSTRAINT PK_ChatConversation PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_ChatConversation_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES dbo.KhachHang(id)
);
GO

CREATE TABLE dbo.ChatMessage (
    id                  BIGINT IDENTITY(1,1) NOT NULL,
    id_cuoc_tro_chuyen   INT NOT NULL,
    loai_nguoi_gui       NVARCHAR(10) NULL,
    noi_dung             NVARCHAR(MAX) NOT NULL,
    thoi_gian            DATETIME2(0) NOT NULL CONSTRAINT DF_ChatMessage_ThoiGian DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_ChatMessage PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_ChatMessage_Conversation FOREIGN KEY (id_cuoc_tro_chuyen) REFERENCES dbo.ChatConversation(id)
);
GO

CREATE TABLE dbo.ChatFeedback (
    id                  INT IDENTITY(1,1) NOT NULL,
    id_tin_nhan          BIGINT NOT NULL,
    diem_danh_gia        BIT NOT NULL, -- mapped in Java
    ghi_chu              NVARCHAR(500) NULL,
    thoi_gian            DATETIME2(0) NOT NULL CONSTRAINT DF_ChatFB_ThoiGian DEFAULT (SYSDATETIME()),
    noi_dung             NVARCHAR(500) NULL, -- compatibility column
    ngay_tao             DATETIME2(0) NULL CONSTRAINT DF_ChatFB_NgayTao DEFAULT (SYSDATETIME()), -- compatibility column
    CONSTRAINT PK_ChatFeedback PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_ChatFeedback_Message FOREIGN KEY (id_tin_nhan) REFERENCES dbo.ChatMessage(id)
);
GO

CREATE TABLE dbo.EditLog (
    id                  BIGINT IDENTITY(1,1) NOT NULL,
    id_tai_khoan         INT NULL, -- Make nullable for system action logs
    ten_bang             NVARCHAR(100) NOT NULL,
    id_ban_ghi           BIGINT NOT NULL,
    hanh_dong            NVARCHAR(20) NOT NULL,
    gia_tri_cu           NVARCHAR(MAX) NULL,
    gia_tri_moi           NVARCHAR(MAX) NULL,
    thoi_gian            DATETIME2(0) NOT NULL CONSTRAINT DF_EditLog_ThoiGian DEFAULT (SYSDATETIME()),
    dia_chi_ip           VARCHAR(50) NULL,
    ghi_chu              NVARCHAR(500) NULL,
    vai_tro_thuc_hien    NVARCHAR(20) NULL,
    CONSTRAINT PK_EditLog PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_EditLog_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES dbo.TaiKhoan(id)
);
GO

/*===============================================================================
  07. PERFORMANCE & HARDENING INDEXES
===============================================================================*/

CREATE INDEX IDX_GIOHANG_KHACHHANG ON dbo.GioHang(id_khach_hang);
CREATE INDEX IDX_GIOHANGCHITIET_GIOHANG ON dbo.GioHangChiTiet(id_gio_hang);
CREATE INDEX IDX_HOADON_KHACHHANG ON dbo.HoaDon(id_khach_hang);
CREATE INDEX IDX_HOADON_NGAYTAO ON dbo.HoaDon(ngay_tao);
CREATE INDEX IDX_HOADONCHITIET_HOADON ON dbo.HoaDonChiTiet(id_hoa_don);
CREATE INDEX IDX_DANHGIA_SANPHAM ON dbo.DanhGia(id_san_pham);
CREATE INDEX IDX_EDITLOG_TIME ON dbo.EditLog(thoi_gian);
CREATE INDEX IDX_KHACHHANG_TAIKHOAN ON dbo.KhachHang(id_tai_khoan);
CREATE INDEX IDX_NHANVIEN_TAIKHOAN ON dbo.NhanVien(id_tai_khoan);
CREATE INDEX IDX_SODIACHI_KHACHHANG ON dbo.SoDiaChi(id_khach_hang);
CREATE INDEX IDX_GIAODICH_HOADON ON dbo.GiaoDichThanhToan(id_hoa_don);
CREATE INDEX IDX_TICHHOPVC_HOADON ON dbo.TichHopVanChuyen(id_hoa_don);
GO

/*===============================================================================
  08. SEED DATA FOR TESTING
===============================================================================*/

INSERT INTO dbo.TaiKhoan(email, mat_khau, vai_tro, trang_thai_tai_khoan, trang_thai)
VALUES
('admin@smash.vn', N'$2a$10$7qNfP49H3X2hKk1zU/yY7.2qHlJ78vKxI1wGZ3K0vQ9n2h8gXgRKi', 'QL', 'ACTIVE', N'hoat_dong'),
('staff@smash.vn', N'$2a$10$7qNfP49H3X2hKk1zU/yY7.2qHlJ78vKxI1wGZ3K0vQ9n2h8gXgRKi', 'NV', 'ACTIVE', N'hoat_dong'),
('customer@smash.vn', N'$2a$10$7qNfP49H3X2hKk1zU/yY7.2qHlJ78vKxI1wGZ3K0vQ9n2h8gXgRKi', 'KH', 'ACTIVE', N'hoat_dong');
GO

INSERT INTO dbo.NhanVien(id_tai_khoan, ho_ten, ho_ten_nv, chuc_vu, so_dien_thoai)
SELECT id, N'Quản trị SMASH VN', N'Quản trị SMASH VN', N'Administrator', '0900000001' FROM dbo.TaiKhoan WHERE email = 'admin@smash.vn'
UNION ALL
SELECT id, N'Nhân viên bán hàng', N'Nhân viên bán hàng', N'Sales Staff', '0900000002' FROM dbo.TaiKhoan WHERE email = 'staff@smash.vn';
GO

INSERT INTO dbo.KhachHang(id_tai_khoan, ho_kh, ten_kh, so_dien_thoai_kh, nhan_ban_tin, loai_khach_hang, nguon_tao, ho_ten_kh)
SELECT id, N'Khách', N'Hàng mẫu', '0911222333', 1, 'REGISTERED', 'SEED', N'Khách Hàng mẫu' FROM dbo.TaiKhoan WHERE email = 'customer@smash.vn';
GO

INSERT INTO dbo.DanhMuc(ten_danh_muc, mo_ta, trang_thai)
VALUES
(N'Vợt cầu lông', N'Các dòng vợt cầu lông chính hãng', 1),
(N'Giày cầu lông', N'Giày thi đấu và luyện tập', 1),
(N'Áo cầu lông', N'Trang phục cầu lông', 1),
(N'Phụ kiện', N'Balo, dây cước, quấn cán và phụ kiện khác', 1);
GO

INSERT INTO dbo.ThuongHieu(ten_thuong_hieu, mo_ta, trang_thai)
VALUES
(N'Yonex', N'Thương hiệu cầu lông Nhật Bản', 1),
(N'Li-Ning', N'Thương hiệu thể thao Trung Quốc', 1),
(N'Victor', N'Thương hiệu cầu lông Đài Loan', 1),
(N'Mizuno', N'Thương hiệu thể thao Nhật Bản', 1),
(N'Kawasaki', N'Thương hiệu cầu lông phổ thông', 1);
GO

INSERT INTO dbo.SanPham(id_danh_muc, id_thuong_hieu, id_nhan_vien, ten_san_pham, mo_ta, trang_thai)
SELECT dm.id, th.id, nv.id, N'Yonex Astrox 88D Pro', N'Vợt thiên công, phù hợp đánh đôi phía sau.', 'dang_ban'
FROM dbo.DanhMuc dm
CROSS JOIN dbo.ThuongHieu th
CROSS JOIN dbo.NhanVien nv
WHERE dm.ten_danh_muc = N'Vợt cầu lông'
  AND th.ten_thuong_hieu = N'Yonex'
  AND nv.so_dien_thoai = '0900000001';
GO

INSERT INTO dbo.SanPhamChiTiet(id_san_pham, mau_sac, muc_cang, trong_luong, gia_ban, so_luong_ton, barcode, chat_lieu, gia_nhap, kich_thuoc, SKU, trang_thai, suc_cang)
SELECT id, N'Đen/Bạc', N'20-28 lbs', N'4U', 3990000, 20, '893000000001', N'HM Graphite', 2800000, N'4U G5', 'SM-YON-AX88D-4U-G5', 'dang_ban', N'20-28 lbs'
FROM dbo.SanPham WHERE ten_san_pham = N'Yonex Astrox 88D Pro';
GO

INSERT INTO dbo.HinhAnhSanPham(id_san_pham_chi_tiet, duong_dan, url_hinh_anh, la_anh_chinh, mau_sac)
SELECT id, N'/images/products/yonex-astrox-88d-pro.jpg', N'/images/products/yonex-astrox-88d-pro.jpg', 1, N'Đen/Bạc'
FROM dbo.SanPhamChiTiet WHERE SKU = 'SM-YON-AX88D-4U-G5';
GO

INSERT INTO dbo.PhuongThucThanhToan(ma_phuong_thuc, ten_phuong_thuc)
VALUES
('COD', N'Thanh toán khi nhận hàng'),
('SEPAY', N'Chuyển khoản SePay'),
('VNPAY', N'Thanh toán VNPay'),
('CASH', N'Tiền mặt tại quầy');
GO

INSERT INTO dbo.DonViVanChuyen(ma_don_vi, ten_don_vi, so_hotline, trang_web, ma_token, ma_client, dia_chi_kho, phi_noi_dia, phi_toan_quoc)
VALUES
('GHN', N'Giao Hàng Nhanh', '1900636677', 'https://ghn.vn', 'ENV:GHN_TOKEN', 'ENV:GHN_CLIENT_ID', N'Kho SMASH VN', 25000, 35000),
('PICKUP', N'Nhận tại cửa hàng', NULL, NULL, NULL, NULL, N'Cửa hàng SMASH VN', 0, 0);
GO

INSERT INTO dbo.TrangThaiGioHang(ma_trang_thai, ten_trang_thai)
VALUES
('ACTIVE', N'Đang chọn'),
('SAVED', N'Lưu sau'),
('REMOVED', N'Đã xóa');
GO

INSERT INTO dbo.PhieuGiamGia(ma_phieu, ten_phieu, id_nhan_vien, don_vi, gia_tri, gia_tri_giam_toi_da, gia_tri_don_hang_toi_thieu, so_luong_con_lai, ngay_bat_dau, ngay_ket_thuc, trang_thai, loai_giam_gia, kich_hoat)
SELECT 'SMASH50K', N'Giảm 50K đơn từ 1 triệu', nv.id, 'VND', 50000, NULL, 1000000, 100, DATEADD(DAY,-1,SYSDATETIME()), DATEADD(DAY,30,SYSDATETIME()), 'ACTIVE', N'Giảm trực tiếp', 1
FROM dbo.NhanVien nv WHERE nv.so_dien_thoai = '0900000001';
GO

INSERT INTO dbo.DotGiamGia(ten_chien_dich, id_nhan_vien, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc, trang_thai, loai_giam_gia, kich_hoat)
SELECT N'Flash Sale vợt cao cấp', nv.id, 10, DATEADD(DAY,-1,SYSDATETIME()), DATEADD(DAY,14,SYSDATETIME()), 'ACTIVE', N'Theo Phần Trăm', 1
FROM dbo.NhanVien nv WHERE nv.so_dien_thoai = '0900000001';
GO

INSERT INTO dbo.SanPham_DotGiamGia(id_dot_giam_gia, id_san_pham)
SELECT dgg.id, sp.id
FROM dbo.DotGiamGia dgg
CROSS JOIN dbo.SanPham sp
WHERE dgg.ten_chien_dich = N'Flash Sale vợt cao cấp'
  AND sp.ten_san_pham = N'Yonex Astrox 88D Pro';
GO

INSERT INTO dbo.SoDiaChi(id_khach_hang, ho_nguoi_nhan, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_cu_the, tinh_thanh, quoc_gia, thanh_pho, vi_do, kinh_do, province_id, district_id, ward_code, province_name, district_name, ward_name, la_mac_dinh_giao_hang, la_mac_dinh_thanh_toan, ho_va_ten_nguoi_nhan)
SELECT kh.id, kh.ho_kh, kh.ten_kh, kh.so_dien_thoai_kh, N'123 Nguyễn Trãi', N'Hà Nội', N'Việt Nam', N'Hà Nội', NULL, NULL, 201, 3440, '13010', N'Hà Nội', N'Thanh Xuân', N'Phường Thượng Đình', 1, 1, N'Khách Hàng mẫu'
FROM dbo.KhachHang kh WHERE kh.so_dien_thoai_kh = '0911222333';
GO

INSERT INTO dbo.GioHang(id_khach_hang, session_id)
SELECT id, 'seed-session-customer' FROM dbo.KhachHang WHERE so_dien_thoai_kh = '0911222333';
GO

INSERT INTO dbo.GioHangChiTiet(id_gio_hang, id_san_pham_chi_tiet, id_trang_thai, so_luong)
SELECT gh.id, spct.id, tt.id, 1
FROM dbo.GioHang gh
CROSS JOIN dbo.SanPhamChiTiet spct
CROSS JOIN dbo.TrangThaiGioHang tt
WHERE gh.session_id = 'seed-session-customer'
  AND spct.SKU = 'SM-YON-AX88D-4U-G5'
  AND tt.ma_trang_thai = 'ACTIVE';
GO

DECLARE @CustomerId INT = (SELECT id FROM dbo.KhachHang WHERE so_dien_thoai_kh = '0911222333');
DECLARE @StaffId INT = (SELECT id FROM dbo.NhanVien WHERE so_dien_thoai = '0900000002');
DECLARE @PaymentId INT = (SELECT id FROM dbo.PhuongThucThanhToan WHERE ma_phuong_thuc = 'COD');
DECLARE @ShipId INT = (SELECT id FROM dbo.DonViVanChuyen WHERE ma_don_vi = 'GHN');
DECLARE @AddressId INT = (SELECT TOP 1 id FROM dbo.SoDiaChi WHERE id_khach_hang = @CustomerId);
DECLARE @VoucherId INT = (SELECT id FROM dbo.PhieuGiamGia WHERE ma_phieu = 'SMASH50K');
DECLARE @Price DECIMAL(18,2) = (SELECT gia_ban FROM dbo.SanPhamChiTiet WHERE SKU = 'SM-YON-AX88D-4U-G5');

INSERT INTO dbo.HoaDon(ma_don_hang, id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, id_don_vi_van_chuyen, id_dia_chi, id_phieu_giam_gia, trang_thai_don_hang, trang_thai_thanh_toan, tong_tien, tong_tien_hang, so_tien_giam_gia, so_tien_giam_voucher, phi_van_chuyen, ten_nguoi_nhan, sdt_nhan, dia_chi_nhan, phuong_thuc_thanh_toan, loai_don_hang, ma_giam_gia_ap_dung, ten_giam_gia_ap_dung, mo_ta_giam_gia_snapshot, tien_hang)
VALUES ('HD-SEED-000001', @CustomerId, @StaffId, @PaymentId, @ShipId, @AddressId, @VoucherId, N'da_giao', 'DA_THANH_TOAN', @Price - (@Price * 0.10) - 50000 + 25000, @Price, @Price * 0.10, 50000, 25000, N'Khách Hàng mẫu', '0911222333', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'COD', 'ONLINE', 'SMASH50K', 'Giảm 50K đơn từ 1 triệu', 'VND 50000', @Price);
GO

INSERT INTO dbo.HoaDonChiTiet(id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, gia_niem_yet, phan_tram_giam, so_tien_giam_san_pham, ten_dot_giam_gia, id_dot_giam_gia, ten_san_pham_snapshot, ma_hang_snapshot, thuoc_tinh_snapshot, thuong_hieu_snapshot, danh_muc_snapshot, gia_goc, gia_sau_giam, sku_snapshot)
SELECT hd.id, spct.id, 1, spct.gia_ban * 0.90, spct.gia_ban * 0.90, spct.gia_ban, 10, spct.gia_ban * 0.10, dgg.ten_chien_dich, dgg.id, sp.ten_san_pham, spct.SKU, CONCAT(spct.mau_sac, N' / ', spct.trong_luong, N' / ', spct.muc_cang), th.ten_thuong_hieu, dm.ten_danh_muc, spct.gia_ban, spct.gia_ban * 0.90, spct.SKU
FROM dbo.HoaDon hd
CROSS JOIN dbo.SanPhamChiTiet spct
INNER JOIN dbo.SanPham sp ON spct.id_san_pham = sp.id
INNER JOIN dbo.ThuongHieu th ON sp.id_thuong_hieu = th.id
INNER JOIN dbo.DanhMuc dm ON sp.id_danh_muc = dm.id
LEFT JOIN dbo.DotGiamGia dgg ON dgg.ten_chien_dich = N'Flash Sale vợt cao cấp'
WHERE hd.ma_don_hang = 'HD-SEED-000001'
  AND spct.SKU = 'SM-YON-AX88D-4U-G5';
GO

INSERT INTO dbo.GiaoDichThanhToan(ma_giao_dich, id_hoa_don, so_tien, cong_thanh_toan, trang_thai, du_lieu_tho)
SELECT 'GD-SEED-000001', id, tong_tien, 'COD', 'SUCCESS', N'{"seed":true}'
FROM dbo.HoaDon WHERE ma_don_hang = 'HD-SEED-000001';
GO

INSERT INTO dbo.DanhGia(id_khach_hang, id_san_pham, so_sao, noi_dung)
SELECT kh.id, sp.id, 5.0, N'Sản phẩm mẫu tốt, dùng để seed dữ liệu.'
FROM dbo.KhachHang kh CROSS JOIN dbo.SanPham sp
WHERE kh.so_dien_thoai_kh = '0911222333' AND sp.ten_san_pham = N'Yonex Astrox 88D Pro';
GO

INSERT INTO dbo.Blog(id_tai_khoan, tieu_de, duong_dan, tom_tat, noi_dung, danh_muc, the, trang_thai, da_xoa, ngay_dang, updated_by)
SELECT tk.id, N'Hướng dẫn chọn vợt cầu lông cho người mới', 'huong-dan-chon-vot-cau-long', N'Các tiêu chí cơ bản khi chọn vợt.', 'Seed blog content', N'Hướng dẫn', N'vợt,cầu lông', 'PUBLISHED', 0, CAST(SYSDATETIME() AS DATE), 'seed'
FROM dbo.TaiKhoan tk WHERE tk.email = 'admin@smash.vn';
GO

INSERT INTO dbo.ChatConversation(id_khach_hang, tieu_de, trang_thai)
SELECT id, N'Tư vấn sản phẩm mẫu', 'OPEN'
FROM dbo.KhachHang WHERE so_dien_thoai_kh = '0911222333';
GO

INSERT INTO dbo.ChatMessage(id_cuoc_tro_chuyen, loai_nguoi_gui, noi_dung)
SELECT id, 'USER', N'Tôi muốn tư vấn chọn vợt.' FROM dbo.ChatConversation WHERE tieu_de = N'Tư vấn sản phẩm mẫu';
GO

INSERT INTO dbo.CommentModerationKeyword(tu_khoa)
VALUES (N'dm'), (N'đm'), (N'cmm'), (N'lừa đảo'), (N'fake');
GO

INSERT INTO dbo.NewsletterSubscriber(email, token_huy, ngay_dang_ky, trang_thai)
VALUES (N'newsletter.seed@smash.vn', N'seed-unsubscribe-token', SYSDATETIME(), 'ACTIVE');
GO

INSERT INTO dbo.SanPhamYeuThich(id_khach_hang, id_san_pham)
SELECT kh.id, sp.id
FROM dbo.KhachHang kh CROSS JOIN dbo.SanPham sp
WHERE kh.so_dien_thoai_kh = '0911222333' AND sp.ten_san_pham = N'Yonex Astrox 88D Pro';
GO

/*===============================================================================
  09. TRIGGERS
===============================================================================*/

CREATE OR ALTER TRIGGER dbo.TRG_TaiKhoan_SetUpdatedAt ON dbo.TaiKhoan AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.TaiKhoan t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_KhachHang_SetUpdatedAt ON dbo.KhachHang AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.KhachHang t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_NhanVien_SetUpdatedAt ON dbo.NhanVien AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.NhanVien t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_SanPham_SetUpdatedAt ON dbo.SanPham AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.SanPham t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_SanPhamChiTiet_SetUpdatedAt ON dbo.SanPhamChiTiet AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.SanPhamChiTiet t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_DotGiamGia_SetUpdatedAt ON dbo.DotGiamGia AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.DotGiamGia t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_SoDiaChi_SetUpdatedAt ON dbo.SoDiaChi AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.SoDiaChi t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_SoDiaChi_SingleDefault ON dbo.SoDiaChi AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE sdc
       SET la_mac_dinh_giao_hang = 0, la_mac_dinh = 0
    FROM dbo.SoDiaChi sdc
    INNER JOIN inserted i ON sdc.id_khach_hang = i.id_khach_hang
    WHERE i.la_mac_dinh_giao_hang = 1 AND sdc.id <> i.id;

    UPDATE sdc
       SET la_mac_dinh_thanh_toan = 0
    FROM dbo.SoDiaChi sdc
    INNER JOIN inserted i ON sdc.id_khach_hang = i.id_khach_hang
    WHERE i.la_mac_dinh_thanh_toan = 1 AND sdc.id <> i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_GioHangChiTiet_UpdateCartTime ON dbo.GioHangChiTiet AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    ;WITH changed AS (
        SELECT id_gio_hang FROM inserted
        UNION
        SELECT id_gio_hang FROM deleted
    )
    UPDATE gh SET ngay_cap_nhat = SYSDATETIME()
    FROM dbo.GioHang gh INNER JOIN changed c ON gh.id = c.id_gio_hang;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_HoaDon_LogStatusChange ON dbo.HoaDon AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.LichSuTrangThaiDonHang(id_hoa_don, trang_thai_cu, trang_thai_moi, ghi_chu, hanh_dong)
    SELECT i.id,
           CONVERT(VARCHAR(50), d.trang_thai_don_hang),
           CONVERT(VARCHAR(50), i.trang_thai_don_hang),
           CASE WHEN d.id IS NULL THEN N'Tạo đơn hàng' ELSE N'Cập nhật trạng thái đơn hàng' END,
           CASE WHEN d.id IS NULL THEN N'TAO_DON' ELSE N'CAP_NHAT_TRANG_THAI' END
    FROM inserted i
    LEFT JOIN deleted d ON i.id = d.id
    WHERE d.id IS NULL OR ISNULL(d.trang_thai_don_hang, N'') <> ISNULL(i.trang_thai_don_hang, N'');
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_HoaDon_SetUpdatedAt ON dbo.HoaDon AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF TRIGGER_NESTLEVEL() > 1 RETURN;
    UPDATE t SET ngay_cap_nhat = SYSDATETIME() FROM dbo.HoaDon t INNER JOIN inserted i ON t.id = i.id;
END;
GO

CREATE OR ALTER TRIGGER dbo.TRG_DanhGia_RecalculateProductRating ON dbo.DanhGia AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    ;WITH changed AS (
        SELECT id_san_pham FROM inserted
        UNION
        SELECT id_san_pham FROM deleted
    ), ratings AS (
        SELECT sp.id AS id_san_pham,
               COUNT(dg.id) AS so_danh_gia,
               COALESCE(AVG(CAST(dg.so_sao AS FLOAT)), 0) AS diem_trung_binh
        FROM dbo.SanPham sp
        INNER JOIN changed c ON sp.id = c.id_san_pham
        LEFT JOIN dbo.DanhGia dg ON dg.id_san_pham = sp.id AND dg.da_xoa = 0
        GROUP BY sp.id
    )
    UPDATE sp
       SET so_danh_gia = r.so_danh_gia,
           diem_trung_binh = r.diem_trung_binh,
           ngay_cap_nhat = SYSDATETIME()
    FROM dbo.SanPham sp
    INNER JOIN ratings r ON sp.id = r.id_san_pham;
END;
GO

/*===============================================================================
  10. VIEWS
===============================================================================*/

CREATE OR ALTER VIEW dbo.v_SanPhamDangBan
AS
SELECT
    sp.id,
    sp.ten_san_pham,
    dm.ten_danh_muc,
    th.ten_thuong_hieu,
    sp.diem_trung_binh,
    sp.so_danh_gia,
    MIN(spct.gia_ban) AS gia_thap_nhat,
    MAX(spct.gia_ban) AS gia_cao_nhat,
    SUM(spct.so_luong_ton) AS tong_ton_kho
FROM dbo.SanPham sp
INNER JOIN dbo.DanhMuc dm ON sp.id_danh_muc = dm.id
INNER JOIN dbo.ThuongHieu th ON sp.id_thuong_hieu = th.id
INNER JOIN dbo.SanPhamChiTiet spct ON sp.id = spct.id_san_pham
WHERE sp.trang_thai = 'dang_ban'
  AND spct.trang_thai = 'dang_ban'
  AND dm.trang_thai = 1
  AND th.trang_thai = 1
GROUP BY sp.id, sp.ten_san_pham, dm.ten_danh_muc, th.ten_thuong_hieu, sp.diem_trung_binh, sp.so_danh_gia;
GO

CREATE OR ALTER VIEW dbo.v_DoanhThuNgay
AS
SELECT
    CAST(ngay_tao AS DATE) AS ngay,
    COUNT(id) AS so_don,
    SUM(tong_tien) AS doanh_thu,
    SUM(tong_tien_hang) AS tong_tien_hang,
    SUM(so_tien_giam_gia) AS tong_giam_gia,
    SUM(phi_van_chuyen) AS tong_phi_van_chuyen
FROM dbo.HoaDon
WHERE trang_thai_don_hang = N'da_giao'
GROUP BY CAST(ngay_tao AS DATE);
GO
