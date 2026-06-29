USE DBSM1;
GO

-- Definition for table Blog
CREATE TABLE [Blog] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NULL,
    [tieu_de] [nvarchar](255) NOT NULL,
    [duong_dan] [varchar](255) NULL,
    [tom_tat] [nvarchar](500) NULL,
    [noi_dung] [nvarchar](MAX) NULL,
    [hinh_anh] [varchar](MAX) NULL,
    [ngay_dang] [date] NULL,
    [trang_thai] [varchar](20) NULL DEFAULT ('DRAFT'),
    [da_xoa] [bit] NULL DEFAULT ((0)),
    [ngay_xoa] [datetime] NULL,
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [ngay_cap_nhat] [datetime] NULL DEFAULT (getdate()),
    [danh_muc] [nvarchar](255) NULL,
    [the] [nvarchar](255) NULL,
    [updated_by] [varchar](255) NULL,
    CONSTRAINT [PK_Blog] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [Blog] ADD CONSTRAINT [FK_Blog_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
CREATE NONCLUSTERED INDEX [IX_BLOG_PUBLISH_DATE] ON [Blog] ([ngay_dang]);
GO

-- Definition for table BlogComment
CREATE TABLE [BlogComment] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_blog] [int] NULL,
    [id_tai_khoan] [int] NULL,
    [id_binh_luan_cha] [int] NULL,
    [noi_dung] [nvarchar](1000) NULL,
    [da_xoa] [bit] NULL DEFAULT ((0)),
    [ly_do_xoa] [nvarchar](255) NULL,
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [ngay_xoa] [datetime] NULL,
    CONSTRAINT [PK_BlogComment] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [BlogComment] ADD CONSTRAINT [FK_BlogComment_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
ALTER TABLE [BlogComment] ADD CONSTRAINT [FK_BlogComment_Blog] FOREIGN KEY ([id_blog]) REFERENCES [Blog] ([id]);
ALTER TABLE [BlogComment] ADD CONSTRAINT [FK_BlogComment_Parent] FOREIGN KEY ([id_binh_luan_cha]) REFERENCES [BlogComment] ([id]);
GO

-- Definition for table ChatConversation
CREATE TABLE [ChatConversation] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_khach_hang] [int] NULL,
    [session_id] [varchar](100) NULL,
    [trang_thai] [varchar](20) NULL DEFAULT ('ACTIVE'),
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [tieu_de] [nvarchar](255) NULL,
    [ngay_cap_nhat] [datetime] NULL,
    CONSTRAINT [PK_ChatConversation] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [ChatConversation] ADD CONSTRAINT [FK_ChatConversation_KhachHang] FOREIGN KEY ([id_khach_hang]) REFERENCES [KhachHang] ([id]);
CREATE UNIQUE NONCLUSTERED INDEX [IX_ChatConversation_Session_Unique] ON [ChatConversation] ([session_id]);
GO

-- Definition for table ChatFeedback
CREATE TABLE [ChatFeedback] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tin_nhan] [bigint] NOT NULL,
    [diem_danh_gia] [bit] NOT NULL,
    [noi_dung] [nvarchar](500) NULL,
    [ngay_tao] [datetime] NOT NULL DEFAULT (getdate()),
    CONSTRAINT [PK_ChatFeedback] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [ChatFeedback] ADD CONSTRAINT [FK_ChatFeedback_Message] FOREIGN KEY ([id_tin_nhan]) REFERENCES [ChatMessage] ([id]);
GO

-- Definition for table ChatMessage
CREATE TABLE [ChatMessage] (
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [id_cuoc_tro_chuyen] [int] NOT NULL,
    [loai_nguoi_gui] [varchar](10) NOT NULL,
    [noi_dung] [nvarchar](MAX) NOT NULL,
    [thoi_gian] [datetime] NOT NULL DEFAULT (getdate()),
    CONSTRAINT [PK_ChatMessage] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [ChatMessage] ADD CONSTRAINT [FK_ChatMessage_Conversation] FOREIGN KEY ([id_cuoc_tro_chuyen]) REFERENCES [ChatConversation] ([id]);
GO

-- Definition for table CommentModerationKeyword
CREATE TABLE [CommentModerationKeyword] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [tu_khoa] [nvarchar](100) NOT NULL,
    [kich_hoat] [bit] NULL DEFAULT ((1)),
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_CommentModerationKeyword] PRIMARY KEY CLUSTERED ([id])
);
GO

-- Definition for table CommentViolationLog
CREATE TABLE [CommentViolationLog] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NOT NULL,
    [id_danh_gia] [int] NULL,
    [id_san_pham] [int] NOT NULL,
    [noi_dung_goc] [nvarchar](MAX) NOT NULL,
    [noi_dung_da_loc] [nvarchar](MAX) NOT NULL,
    [muc_do_vi_pham] [nvarchar](50) NOT NULL,
    [so_lan_vi_pham] [int] NOT NULL,
    [thoi_han_khoa] [nvarchar](100) NULL,
    [ngay_vi_pham] [datetime] NOT NULL DEFAULT (getdate()),
    [ngay_tao] [datetime] NOT NULL DEFAULT (getdate()),
    CONSTRAINT [PK_CommentViolationLog] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [CommentViolationLog] ADD CONSTRAINT [FK_CommentViolationLog_DanhGia] FOREIGN KEY ([id_danh_gia]) REFERENCES [DanhGia] ([id]);
GO

-- Definition for table DanhGia
CREATE TABLE [DanhGia] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_san_pham] [int] NULL,
    [id_khach_hang] [int] NULL,
    [so_sao] [float] NOT NULL,
    [noi_dung] [nvarchar](MAX) NULL,
    [an_binh_luan] [bit] NULL DEFAULT ((0)),
    [an_hinh_anh] [bit] NULL DEFAULT ((0)),
    [da_xoa] [bit] NULL DEFAULT ((0)),
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [ngay_cap_nhat] [datetime] NULL DEFAULT (getdate()),
    [ngay_xoa] [datetime] NULL,
    [ngay_an_binh_luan] [datetime] NULL,
    [ngay_hien_binh_luan] [datetime] NULL,
    [ngay_an_hinh_anh] [datetime] NULL,
    [ngay_hien_hinh_anh] [datetime] NULL,
    [id_nguoi_xoa] [int] NULL,
    [id_nguoi_an_binh_luan] [int] NULL,
    [id_nguoi_hien_binh_luan] [int] NULL,
    [id_nguoi_an_hinh_anh] [int] NULL,
    [id_nguoi_hien_hinh_anh] [int] NULL,
    CONSTRAINT [PK_DanhGia] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_SanPham] FOREIGN KEY ([id_san_pham]) REFERENCES [SanPham] ([id]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_TaiKhoan_Xoa] FOREIGN KEY ([id_nguoi_xoa]) REFERENCES [TaiKhoan] ([id]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_TaiKhoan_AnBL] FOREIGN KEY ([id_nguoi_an_binh_luan]) REFERENCES [TaiKhoan] ([id]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_TaiKhoan_HienBL] FOREIGN KEY ([id_nguoi_hien_binh_luan]) REFERENCES [TaiKhoan] ([id]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_TaiKhoan_AnHA] FOREIGN KEY ([id_nguoi_an_hinh_anh]) REFERENCES [TaiKhoan] ([id]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_TaiKhoan_HienHA] FOREIGN KEY ([id_nguoi_hien_hinh_anh]) REFERENCES [TaiKhoan] ([id]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_KhachHang] FOREIGN KEY ([id_khach_hang]) REFERENCES [KhachHang] ([id]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_DanhGia_KH_SP_Active] ON [DanhGia] ([id_khach_hang]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_DanhGia_KH_SP_Active] ON [DanhGia] ([id_san_pham]);
GO

-- Definition for table DanhGiaAnh
CREATE TABLE [DanhGiaAnh] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_danh_gia] [int] NULL,
    [duong_dan] [varchar](MAX) NOT NULL,
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_DanhGiaAnh] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [DanhGiaAnh] ADD CONSTRAINT [FK_DanhGiaAnh_DanhGia] FOREIGN KEY ([id_danh_gia]) REFERENCES [DanhGia] ([id]);
GO

-- Definition for table DanhMuc
CREATE TABLE [DanhMuc] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ten_danh_muc] [nvarchar](100) NOT NULL,
    [mo_ta] [nvarchar](500) NULL,
    [trang_thai] [bit] NULL DEFAULT ((1)),
    CONSTRAINT [PK_DanhMuc] PRIMARY KEY CLUSTERED ([id])
);
GO

-- Definition for table DonViVanChuyen
CREATE TABLE [DonViVanChuyen] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ma_don_vi] [varchar](50) NULL,
    [ten_don_vi] [nvarchar](100) NULL,
    [phien_ban] [bigint] NULL DEFAULT ((0)),
    [so_hotline] [varchar](20) NULL,
    [trang_web] [varchar](100) NULL,
    [ma_token] [varchar](255) NULL,
    [ma_client] [varchar](100) NULL,
    [dia_chi_kho] [nvarchar](500) NULL,
    [phi_noi_dia] [decimal](18,2) NULL,
    [phi_toan_quoc] [decimal](18,2) NULL,
    CONSTRAINT [PK_DonViVanChuyen] PRIMARY KEY CLUSTERED ([id])
);
GO

-- Definition for table DotGiamGia
CREATE TABLE [DotGiamGia] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ten_dot] [nvarchar](150) NULL,
    [id_nhan_vien] [int] NULL,
    [phan_tram_giam] [int] NULL,
    [ngay_bat_dau] [datetime] NOT NULL,
    [ngay_ket_thuc] [datetime] NOT NULL,
    [trang_thai] [varchar](50) NULL DEFAULT ('ACTIVE'),
    [ten_chien_dich] [nvarchar](255) NULL,
    [loai_giam_gia] [nvarchar](100) NULL,
    [kich_hoat] [bit] NULL DEFAULT ((1)),
    CONSTRAINT [PK_DotGiamGia] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [DotGiamGia] ADD CONSTRAINT [FK_DotGiamGia_NhanVien] FOREIGN KEY ([id_nhan_vien]) REFERENCES [NhanVien] ([id]);
GO

-- Definition for table EditLog
CREATE TABLE [EditLog] (
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NULL,
    [ten_bang] [varchar](100) NOT NULL,
    [id_ban_ghi] [bigint] NOT NULL,
    [hanh_dong] [varchar](20) NOT NULL,
    [gia_tri_cu] [nvarchar](MAX) NULL,
    [gia_tri_moi] [nvarchar](MAX) NULL,
    [thoi_gian] [datetime] NOT NULL DEFAULT (getdate()),
    [dia_chi_ip] [varchar](50) NULL,
    [ghi_chu] [varchar](500) NULL,
    [vai_tro_thuc_hien] [varchar](20) NULL,
    CONSTRAINT [PK_EditLog] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [EditLog] ADD CONSTRAINT [FK_EditLog_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
GO

-- Definition for table flyway_schema_history
CREATE TABLE [flyway_schema_history] (
    [installed_rank] [int] NOT NULL,
    [version] [nvarchar](50) NULL,
    [description] [nvarchar](200) NULL,
    [type] [nvarchar](20) NOT NULL,
    [script] [nvarchar](1000) NOT NULL,
    [checksum] [int] NULL,
    [installed_by] [nvarchar](100) NOT NULL,
    [installed_on] [datetime] NOT NULL DEFAULT (getdate()),
    [execution_time] [int] NOT NULL,
    [success] [bit] NOT NULL,
    CONSTRAINT [PK_flyway_schema_history] PRIMARY KEY CLUSTERED ([installed_rank])
);
CREATE NONCLUSTERED INDEX [flyway_schema_history_s_idx] ON [flyway_schema_history] ([success]);
GO

-- Definition for table GiaoDichThanhToan
CREATE TABLE [GiaoDichThanhToan] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_hoa_don] [int] NULL,
    [ma_giao_dich] [varchar](100) NULL,
    [gateway] [varchar](50) NULL,
    [so_tien] [decimal](18,2) NULL,
    [status] [varchar](50) NULL,
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [cong_thanh_toan] [varchar](50) NOT NULL,
    [trang_thai] [varchar](50) NOT NULL,
    [du_lieu_tho] [nvarchar](MAX) NULL,
    CONSTRAINT [PK_GiaoDichThanhToan] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [GiaoDichThanhToan] ADD CONSTRAINT [FK_GiaoDichThanhToan_HoaDon] FOREIGN KEY ([id_hoa_don]) REFERENCES [HoaDon] ([id]);
CREATE NONCLUSTERED INDEX [IX_GiaoDichThanhToan_HoaDon] ON [GiaoDichThanhToan] ([id_hoa_don]);
CREATE NONCLUSTERED INDEX [IX_GiaoDichThanhToan_MaGiaoDich] ON [GiaoDichThanhToan] ([ma_giao_dich]);
GO

-- Definition for table GioHang
CREATE TABLE [GioHang] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_khach_hang] [int] NULL,
    [session_id] [varchar](100) NULL,
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [ngay_cap_nhat] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_GioHang] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [GioHang] ADD CONSTRAINT [FK_GioHang_KhachHang] FOREIGN KEY ([id_khach_hang]) REFERENCES [KhachHang] ([id]);
GO

-- Definition for table GioHangChiTiet
CREATE TABLE [GioHangChiTiet] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_gio_hang] [int] NULL,
    [id_san_pham_chi_tiet] [int] NULL,
    [id_trang_thai] [int] NULL,
    [so_luong] [int] NOT NULL,
    [ngay_them] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_GioHangChiTiet] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [GioHangChiTiet] ADD CONSTRAINT [FK_GioHangChiTiet_SPCT] FOREIGN KEY ([id_san_pham_chi_tiet]) REFERENCES [SanPhamChiTiet] ([id]);
ALTER TABLE [GioHangChiTiet] ADD CONSTRAINT [FK_GioHangChiTiet_TrangThai] FOREIGN KEY ([id_trang_thai]) REFERENCES [TrangThaiGioHang] ([id]);
ALTER TABLE [GioHangChiTiet] ADD CONSTRAINT [FK_GioHangChiTiet_GioHang] FOREIGN KEY ([id_gio_hang]) REFERENCES [GioHang] ([id]);
GO

-- Definition for table HinhAnhSanPham
CREATE TABLE [HinhAnhSanPham] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_san_pham_chi_tiet] [int] NULL,
    [duong_dan] [varchar](MAX) NOT NULL,
    [la_anh_chinh] [bit] NULL DEFAULT ((0)),
    [mau_sac] [nvarchar](50) NULL,
    CONSTRAINT [PK_HinhAnhSanPham] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [HinhAnhSanPham] ADD CONSTRAINT [FK_HinhAnhSanPham_SPCT] FOREIGN KEY ([id_san_pham_chi_tiet]) REFERENCES [SanPhamChiTiet] ([id]);
GO

-- Definition for table HoaDon
CREATE TABLE [HoaDon] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ma_don_hang] [varchar](50) NULL,
    [id_khach_hang] [int] NULL,
    [id_nhan_vien] [int] NULL,
    [id_phuong_thuc_thanh_toan] [int] NULL,
    [id_don_vi_van_chuyen] [int] NULL,
    [id_dia_chi] [int] NULL,
    [id_phieu_giam_gia] [int] NULL,
    [trang_thai_don_hang] [nvarchar](50) NULL DEFAULT (N'cho_xac_nhan'),
    [trang_thai_thanh_toan] [varchar](50) NULL DEFAULT ('CHO_THANH_TOAN'),
    [tong_tien] [decimal](18,2) NULL DEFAULT ((0)),
    [so_tien_giam_gia] [decimal](18,2) NULL DEFAULT ((0)),
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [phi_van_chuyen] [decimal](18,2) NULL DEFAULT ((0)),
    [ten_nguoi_nhan] [nvarchar](100) NULL,
    [sdt_nhan] [varchar](15) NULL,
    [dia_chi_nhan] [nvarchar](255) NULL,
    [ghi_chu] [nvarchar](500) NULL,
    [ma_giao_dich] [nvarchar](100) NULL,
    [nguoi_xac_nhan_thanh_toan] [nvarchar](100) NULL,
    [thoi_gian_xac_nhan] [datetime] NULL,
    [phuong_thuc_thanh_toan] [varchar](50) NULL,
    [phan_hoi_cong_tt] [nvarchar](MAX) NULL,
    [ngay_thanh_toan] [datetime] NULL,
    [ma_giao_dich_ung_dung] [varchar](100) NULL,
    [ghn_order_code] [varchar](50) NULL,
    [ghn_status] [varchar](100) NULL,
    [ghn_to_district_id] [int] NULL,
    [ghn_to_ward_code] [varchar](20) NULL,
    [trang_thai_hoan_hang] [varchar](50) NULL,
    [ngay_xac_nhan_hoan_hang] [datetime] NULL,
    [ma_giam_gia_ap_dung] [varchar](50) NULL,
    [ten_giam_gia_ap_dung] [varchar](255) NULL,
    [mo_ta_giam_gia_snapshot] [varchar](500) NULL,
    [id_nhan_vien_xac_nhan] [int] NULL,
    [id_nhan_vien_xac_nhan_hoan_tien] [int] NULL,
    [loai_don_hang] [varchar](30) NULL,
    [tong_tien_hang] [decimal](18,2) NULL,
    [so_tien_giam_voucher] [decimal](18,2) NULL,
    [ngay_cap_nhat] [datetime] NULL,
    CONSTRAINT [PK_HoaDon] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_NhanVien] FOREIGN KEY ([id_nhan_vien]) REFERENCES [NhanVien] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_NhanVien_XacNhan] FOREIGN KEY ([id_nhan_vien_xac_nhan]) REFERENCES [NhanVien] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_NhanVien_XacNhanHoanTien] FOREIGN KEY ([id_nhan_vien_xac_nhan_hoan_tien]) REFERENCES [NhanVien] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_PhieuGiamGia] FOREIGN KEY ([id_phieu_giam_gia]) REFERENCES [PhieuGiamGia] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_PTTT] FOREIGN KEY ([id_phuong_thuc_thanh_toan]) REFERENCES [PhuongThucThanhToan] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_DiaChi] FOREIGN KEY ([id_dia_chi]) REFERENCES [SoDiaChi] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_DVVC] FOREIGN KEY ([id_don_vi_van_chuyen]) REFERENCES [DonViVanChuyen] ([id]);
ALTER TABLE [HoaDon] ADD CONSTRAINT [FK_HoaDon_KhachHang] FOREIGN KEY ([id_khach_hang]) REFERENCES [KhachHang] ([id]);
CREATE NONCLUSTERED INDEX [IX_HoaDon_KhachHang_NgayTao] ON [HoaDon] ([id_khach_hang]);
CREATE NONCLUSTERED INDEX [IX_HoaDon_KhachHang_NgayTao] ON [HoaDon] ([ngay_tao]);
CREATE NONCLUSTERED INDEX [IX_HoaDon_LoaiDonHang_NgayTao] ON [HoaDon] ([loai_don_hang]);
CREATE NONCLUSTERED INDEX [IX_HoaDon_LoaiDonHang_NgayTao] ON [HoaDon] ([ngay_tao]);
GO

-- Definition for table HoaDonChiTiet
CREATE TABLE [HoaDonChiTiet] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_hoa_don] [int] NULL,
    [id_san_pham_chi_tiet] [int] NULL,
    [so_luong] [int] NOT NULL,
    [don_gia] [decimal](18,2) NOT NULL,
    [thanh_tien] [decimal](18,2) NULL,
    [gia_niem_yet] [decimal](18,2) NULL,
    [phan_tram_giam] [decimal](18,2) NULL,
    [so_tien_giam_san_pham] [decimal](18,2) NULL,
    [ten_dot_giam_gia] [nvarchar](100) NULL,
    [id_dot_giam_gia] [int] NULL,
    [ten_san_pham_snapshot] [nvarchar](255) NULL,
    [ma_hang_snapshot] [varchar](100) NULL,
    [thuoc_tinh_snapshot] [nvarchar](500) NULL,
    [thuong_hieu_snapshot] [nvarchar](100) NULL,
    [danh_muc_snapshot] [nvarchar](100) NULL,
    CONSTRAINT [PK_HoaDonChiTiet] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [HoaDonChiTiet] ADD CONSTRAINT [FK_HoaDonChiTiet_SPCT] FOREIGN KEY ([id_san_pham_chi_tiet]) REFERENCES [SanPhamChiTiet] ([id]);
ALTER TABLE [HoaDonChiTiet] ADD CONSTRAINT [FK_HoaDonChiTiet_HoaDon] FOREIGN KEY ([id_hoa_don]) REFERENCES [HoaDon] ([id]);
CREATE NONCLUSTERED INDEX [IX_HoaDonChiTiet_HoaDon] ON [HoaDonChiTiet] ([id_hoa_don]);
GO

-- Definition for table KhachHang
CREATE TABLE [KhachHang] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NULL,
    [ho_kh] [nvarchar](50) NULL,
    [ten_kh] [nvarchar](50) NULL,
    [sdt] [varchar](15) NULL,
    [nhan_ban_tin] [bit] NULL DEFAULT ((0)),
    [la_tai_khoan_noi_bo] [bit] NULL DEFAULT ((0)),
    [so_dien_thoai_kh] [varchar](15) NOT NULL,
    [loai_khach_hang] [varchar](30) NULL,
    [nguon_tao] [varchar](50) NULL,
    [ngay_tao] [datetime] NULL,
    [ngay_cap_nhat] [datetime] NULL,
    CONSTRAINT [PK_KhachHang] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [KhachHang] ADD CONSTRAINT [FK_KhachHang_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_KhachHang_SoDienThoaiKh] ON [KhachHang] ([so_dien_thoai_kh]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_KhachHang_TaiKhoan] ON [KhachHang] ([id_tai_khoan]);
GO

-- Definition for table LichSuTrangThaiDonHang
CREATE TABLE [LichSuTrangThaiDonHang] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_hoa_don] [int] NULL,
    [trang_thai_cu] [nvarchar](50) NULL,
    [trang_thai_moi] [nvarchar](50) NOT NULL,
    [ghi_chu] [nvarchar](255) NULL,
    [nguoi_thuc_hien] [varchar](100) NULL,
    [thoi_gian] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_LichSuTrangThaiDonHang] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [LichSuTrangThaiDonHang] ADD CONSTRAINT [FK_LichSu_HoaDon] FOREIGN KEY ([id_hoa_don]) REFERENCES [HoaDon] ([id]);
GO

-- Definition for table MaKhoiPhuc
CREATE TABLE [MaKhoiPhuc] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NULL,
    [token] [varchar](255) NULL,
    [loai_xac_nhan] [varchar](10) NULL,
    [da_su_dung] [bit] NULL DEFAULT ((0)),
    [ngay_het_han] [datetime] NULL,
    [ma_xac_nhan] [varchar](255) NOT NULL,
    [thoi_gian_het_han] [datetime] NOT NULL,
    CONSTRAINT [PK_MaKhoiPhuc] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [MaKhoiPhuc] ADD CONSTRAINT [FK_MaKhoiPhuc_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
CREATE UNIQUE NONCLUSTERED INDEX [IX_MaKhoiPhuc_Token_Unique] ON [MaKhoiPhuc] ([token]);
GO

-- Definition for table NhanVien
CREATE TABLE [NhanVien] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NULL,
    [ho_ten_nv] [nvarchar](100) NULL,
    [ho_ten] [nvarchar](100) NOT NULL,
    [chuc_vu] [nvarchar](100) NOT NULL,
    [so_dien_thoai] [varchar](15) NOT NULL,
    CONSTRAINT [PK_NhanVien] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [NhanVien] ADD CONSTRAINT [FK_NhanVien_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_NhanVien_SoDienThoai] ON [NhanVien] ([so_dien_thoai]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_NhanVien_TaiKhoan] ON [NhanVien] ([id_tai_khoan]);
GO

-- Definition for table PhieuGiamGia
CREATE TABLE [PhieuGiamGia] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ma_phieu] [varchar](50) NOT NULL,
    [ten_phieu] [nvarchar](100) NULL,
    [id_nhan_vien] [int] NULL,
    [don_vi] [varchar](10) NULL,
    [gia_tri] [decimal](18,2) NOT NULL,
    [gia_tri_giam_toi_da] [decimal](18,2) NULL,
    [gia_tri_don_hang_toi_thieu] [decimal](18,2) NULL DEFAULT ((0)),
    [so_luong_con_lai] [int] NULL DEFAULT ((0)),
    [ngay_bat_dau] [datetime] NOT NULL,
    [ngay_ket_thuc] [datetime] NOT NULL,
    [trang_thai] [varchar](50) NULL DEFAULT ('ACTIVE'),
    [loai_giam_gia] [nvarchar](100) NULL,
    [kich_hoat] [bit] NULL DEFAULT ((1)),
    CONSTRAINT [PK_PhieuGiamGia] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [PhieuGiamGia] ADD CONSTRAINT [FK_PhieuGiamGia_NhanVien] FOREIGN KEY ([id_nhan_vien]) REFERENCES [NhanVien] ([id]);
CREATE NONCLUSTERED INDEX [IX_PhieuGiamGia_MaPhieu] ON [PhieuGiamGia] ([ma_phieu]);
GO

-- Definition for table PhuongThucThanhToan
CREATE TABLE [PhuongThucThanhToan] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ma_phuong_thuc] [varchar](50) NULL,
    [ten_phuong_thuc] [nvarchar](100) NULL,
    CONSTRAINT [PK_PhuongThucThanhToan] PRIMARY KEY CLUSTERED ([id])
);
CREATE UNIQUE NONCLUSTERED INDEX [IX_PTTT_Ma_Unique] ON [PhuongThucThanhToan] ([ma_phuong_thuc]);
GO

-- Definition for table SanPham
CREATE TABLE [SanPham] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_danh_muc] [int] NULL,
    [id_thuong_hieu] [int] NULL,
    [id_nhan_vien] [int] NULL,
    [ten_san_pham] [nvarchar](255) NOT NULL,
    [mo_ta] [nvarchar](MAX) NULL,
    [diem_trung_binh] [float] NOT NULL DEFAULT ((0.0)),
    [so_danh_gia] [int] NULL DEFAULT ((0)),
    [trang_thai] [varchar](50) NULL DEFAULT ('dang_ban'),
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    [ngay_cap_nhat] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_SanPham] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [SanPham] ADD CONSTRAINT [FK_SanPham_NhanVien] FOREIGN KEY ([id_nhan_vien]) REFERENCES [NhanVien] ([id]);
ALTER TABLE [SanPham] ADD CONSTRAINT [FK_SanPham_ThuongHieu] FOREIGN KEY ([id_thuong_hieu]) REFERENCES [ThuongHieu] ([id]);
ALTER TABLE [SanPham] ADD CONSTRAINT [FK_SanPham_DanhMuc] FOREIGN KEY ([id_danh_muc]) REFERENCES [DanhMuc] ([id]);
GO

-- Definition for table SanPham_DotGiamGia
CREATE TABLE [SanPham_DotGiamGia] (
    [id_san_pham] [int] NOT NULL,
    [id_dot_giam_gia] [int] NOT NULL,
    CONSTRAINT [PK_SanPham_DotGiamGia] PRIMARY KEY CLUSTERED ([id_san_pham])
);
ALTER TABLE [SanPham_DotGiamGia] ADD CONSTRAINT [FK_SanPham_DotGiamGia_SP] FOREIGN KEY ([id_san_pham]) REFERENCES [SanPham] ([id]);
ALTER TABLE [SanPham_DotGiamGia] ADD CONSTRAINT [FK_SanPham_DotGiamGia_DGG] FOREIGN KEY ([id_dot_giam_gia]) REFERENCES [DotGiamGia] ([id]);
GO

-- Definition for table SanPhamChiTiet
CREATE TABLE [SanPhamChiTiet] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_san_pham] [int] NULL,
    [SKU] [varchar](100) NULL,
    [barcode] [varchar](100) NULL,
    [mau_sac] [nvarchar](50) NOT NULL,
    [kich_thuoc] [nvarchar](50) NULL,
    [chat_lieu] [nvarchar](50) NULL,
    [trong_luong] [varchar](50) NULL,
    [gia_nhap] [decimal](18,2) NULL DEFAULT ((0)),
    [gia_ban] [decimal](18,2) NOT NULL,
    [so_luong_ton] [int] NULL DEFAULT ((0)),
    [trang_thai] [varchar](50) NULL DEFAULT ('dang_ban'),
    [muc_cang] [nvarchar](20) NOT NULL,
    [ngay_tao] [datetime] NULL,
    [ngay_cap_nhat] [datetime] NULL,
    CONSTRAINT [PK_SanPhamChiTiet] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [SanPhamChiTiet] ADD CONSTRAINT [FK_SanPhamChiTiet_SanPham] FOREIGN KEY ([id_san_pham]) REFERENCES [SanPham] ([id]);
CREATE NONCLUSTERED INDEX [IX_SanPhamChiTiet_SanPham] ON [SanPhamChiTiet] ([id_san_pham]);
CREATE UNIQUE NONCLUSTERED INDEX [IX_SPCT_Barcode_Unique] ON [SanPhamChiTiet] ([barcode]);
CREATE UNIQUE NONCLUSTERED INDEX [IX_SPCT_SKU_Unique] ON [SanPhamChiTiet] ([SKU]);
GO

-- Definition for table SanPhamYeuThich
CREATE TABLE [SanPhamYeuThich] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_khach_hang] [int] NULL,
    [id_san_pham] [int] NULL,
    [ngay_them] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_SanPhamYeuThich] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [SanPhamYeuThich] ADD CONSTRAINT [FK_SanPhamYeuThich_SanPham] FOREIGN KEY ([id_san_pham]) REFERENCES [SanPham] ([id]);
ALTER TABLE [SanPhamYeuThich] ADD CONSTRAINT [FK_SanPhamYeuThich_KhachHang] FOREIGN KEY ([id_khach_hang]) REFERENCES [KhachHang] ([id]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_SanPhamYeuThich_KH_SP] ON [SanPhamYeuThich] ([id_khach_hang]);
CREATE UNIQUE NONCLUSTERED INDEX [UX_SanPhamYeuThich_KH_SP] ON [SanPhamYeuThich] ([id_san_pham]);
GO

-- Definition for table SoDiaChi
CREATE TABLE [SoDiaChi] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_khach_hang] [int] NULL,
    [ho_nguoi_nhan] [nvarchar](50) NULL,
    [ten_nguoi_nhan] [nvarchar](50) NULL,
    [sdt_nguoi_nhan] [varchar](15) NULL,
    [dia_chi_cu_the] [nvarchar](255) NULL,
    [tinh_thanh] [nvarchar](100) NULL,
    [quoc_gia] [nvarchar](100) NULL,
    [latitude] [float] NULL,
    [longitude] [float] NULL,
    [la_mac_dinh_giao_hang] [bit] NULL DEFAULT ((0)),
    [la_mac_dinh_thanh_toan] [bit] NULL DEFAULT ((0)),
    [thanh_pho] [nvarchar](100) NULL,
    [vi_do] [float] NULL,
    [kinh_do] [float] NULL,
    [province_id] [int] NULL,
    [district_id] [int] NULL,
    [ward_code] [varchar](20) NULL,
    [province_name] [nvarchar](100) NULL,
    [district_name] [nvarchar](100) NULL,
    [ward_name] [nvarchar](100) NULL,
    CONSTRAINT [PK_SoDiaChi] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [SoDiaChi] ADD CONSTRAINT [FK_SoDiaChi_KhachHang] FOREIGN KEY ([id_khach_hang]) REFERENCES [KhachHang] ([id]);
CREATE NONCLUSTERED INDEX [IX_SoDiaChi_KhachHang] ON [SoDiaChi] ([id_khach_hang]);
GO

-- Definition for table TaiKhoan
CREATE TABLE [TaiKhoan] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [email] [varchar](255) NOT NULL,
    [mat_khau] [nvarchar](255) NULL,
    [vai_tro] [varchar](5) NULL,
    [trang_thai] [nvarchar](50) NOT NULL DEFAULT (N'hoat_dong'),
    [trang_thai_tai_khoan] [varchar](50) NOT NULL DEFAULT ('GUEST'),
    [so_lan_nhac_nho_vi_pham] [int] NULL DEFAULT ((0)),
    [so_lan_mua_thanh_cong] [int] NULL DEFAULT ((0)),
    [token_xac_thuc_khoa] [varchar](100) NULL,
    [ngay_khoa_binh_luan_den] [datetime] NULL,
    [ngay_vi_pham_gan_nhat] [datetime] NULL,
    [la_khach_hang] [bit] NULL,
    [la_nhan_vien] [bit] NULL,
    [la_quan_ly] [bit] NULL,
    CONSTRAINT [PK_TaiKhoan] PRIMARY KEY CLUSTERED ([id])
);
GO

-- Definition for table ThongBao
CREATE TABLE [ThongBao] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_tai_khoan] [int] NULL,
    [tieu_de] [nvarchar](255) NOT NULL,
    [noi_dung] [nvarchar](MAX) NOT NULL,
    [loai_thong_bao] [varchar](50) NULL,
    [da_doc] [bit] NULL DEFAULT ((0)),
    [ngay_tao] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_ThongBao] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [ThongBao] ADD CONSTRAINT [FK_ThongBao_TaiKhoan] FOREIGN KEY ([id_tai_khoan]) REFERENCES [TaiKhoan] ([id]);
GO

-- Definition for table ThuongHieu
CREATE TABLE [ThuongHieu] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ten_thuong_hieu] [nvarchar](100) NOT NULL,
    [mo_ta] [nvarchar](500) NULL,
    [trang_thai] [bit] NULL DEFAULT ((1)),
    CONSTRAINT [PK_ThuongHieu] PRIMARY KEY CLUSTERED ([id])
);
GO

-- Definition for table TichHopVanChuyen
CREATE TABLE [TichHopVanChuyen] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [id_hoa_don] [int] NULL,
    [ma_van_don] [varchar](100) NULL,
    [trang_thai_ghn] [varchar](50) NULL,
    [ngay_cap_nhat] [datetime] NULL DEFAULT (getdate()),
    CONSTRAINT [PK_TichHopVanChuyen] PRIMARY KEY CLUSTERED ([id])
);
ALTER TABLE [TichHopVanChuyen] ADD CONSTRAINT [FK_TichHopVanChuyen_HoaDon] FOREIGN KEY ([id_hoa_don]) REFERENCES [HoaDon] ([id]);
GO

-- Definition for table TrangThaiGioHang
CREATE TABLE [TrangThaiGioHang] (
    [id] [int] IDENTITY(1,1) NOT NULL,
    [ten_trang_thai] [nvarchar](50) NOT NULL,
    CONSTRAINT [PK_TrangThaiGioHang] PRIMARY KEY CLUSTERED ([id])
);
GO
