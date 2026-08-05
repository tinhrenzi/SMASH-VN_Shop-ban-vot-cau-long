USE [master]
GO
/****** Object:  Database [BadmintonShopDB1]    Script Date: 31/07/2026 08:39:33 ******/
DROP DATABASE [BadmintonShopDB1]
GO
/****** Object:  Database [BadmintonShopDB1]    Script Date: 31/07/2026 08:39:33 ******/
CREATE DATABASE [BadmintonShopDB1]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'BadmintonShopDB1', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA\BadmintonShopDB1.mdf' , SIZE = 73728KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'BadmintonShopDB1_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA\BadmintonShopDB1_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [BadmintonShopDB1] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [BadmintonShopDB1].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [BadmintonShopDB1] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET ARITHABORT OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET AUTO_CLOSE ON 
GO
ALTER DATABASE [BadmintonShopDB1] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [BadmintonShopDB1] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [BadmintonShopDB1] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET  ENABLE_BROKER 
GO
ALTER DATABASE [BadmintonShopDB1] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [BadmintonShopDB1] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [BadmintonShopDB1] SET  MULTI_USER 
GO
ALTER DATABASE [BadmintonShopDB1] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [BadmintonShopDB1] SET DB_CHAINING OFF 
GO
ALTER DATABASE [BadmintonShopDB1] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [BadmintonShopDB1] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [BadmintonShopDB1] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [BadmintonShopDB1] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
ALTER DATABASE [BadmintonShopDB1] SET QUERY_STORE = ON
GO
ALTER DATABASE [BadmintonShopDB1] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [BadmintonShopDB1]
GO
/****** Object:  Table [dbo].[Blog]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Blog](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NULL,
	[danh_muc] [nvarchar](255) NULL,
	[noi_dung] [nvarchar](max) NULL,
	[hinh_anh] [nvarchar](255) NULL,
	[ngay_dang] [date] NULL,
	[duong_dan] [varchar](255) NOT NULL,
	[tom_tat] [nvarchar](1000) NULL,
	[the] [nvarchar](255) NULL,
	[tieu_de] [nvarchar](255) NOT NULL,
	[trang_thai] [varchar](20) NOT NULL,
	[da_xoa] [bit] NOT NULL,
	[ngay_xoa] [datetime] NULL,
	[ngay_tao] [datetime] NOT NULL,
	[updated_by] [varchar](255) NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BlogComment]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BlogComment](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_blog] [int] NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[noi_dung] [nvarchar](1000) NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[da_xoa] [bit] NOT NULL,
	[ngay_xoa] [datetime] NULL,
	[ly_do_xoa] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatConversation]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatConversation](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NULL,
	[session_id] [nvarchar](100) NULL,
	[tieu_de] [nvarchar](255) NULL,
	[trang_thai] [nvarchar](20) NOT NULL,
	[ngay_tao] [datetime2](0) NOT NULL,
	[ngay_cap_nhat] [datetime2](0) NULL,
 CONSTRAINT [PK_ChatConversation] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatFeedback]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatFeedback](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_tin_nhan] [bigint] NOT NULL,
	[id_tai_khoan] [int] NULL,
	[session_id] [nvarchar](100) NULL,
	[danh_gia] [smallint] NOT NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[ngay_tao] [datetime2](0) NOT NULL,
	[ngay_cap_nhat] [datetime2](0) NULL,
 CONSTRAINT [PK_ChatFeedback] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatMessage]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatMessage](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_cuoc_tro_chuyen] [bigint] NOT NULL,
	[vai_tro] [nvarchar](20) NOT NULL,
	[noi_dung] [nvarchar](max) NOT NULL,
	[ten_model] [nvarchar](100) NULL,
	[trang_thai] [nvarchar](20) NOT NULL,
	[so_token_dau_vao] [int] NULL,
	[so_token_dau_ra] [int] NULL,
	[thoi_gian_xu_ly_ms] [bigint] NULL,
	[ma_loi] [nvarchar](100) NULL,
	[noi_dung_loi] [nvarchar](1000) NULL,
	[ngay_tao] [datetime2](0) NOT NULL,
 CONSTRAINT [PK_ChatMessage] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CommentModerationKeyword]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CommentModerationKeyword](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[tu_khoa] [nvarchar](255) NOT NULL,
	[kich_hoat] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CommentViolationLog]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CommentViolationLog](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[id_danh_gia] [int] NULL,
	[id_san_pham] [int] NOT NULL,
	[noi_dung_goc] [nvarchar](max) NOT NULL,
	[noi_dung_da_loc] [nvarchar](max) NOT NULL,
	[muc_do_vi_pham] [nvarchar](50) NOT NULL,
	[so_lan_vi_pham] [int] NOT NULL,
	[thoi_han_khoa] [nvarchar](100) NULL,
	[ngay_vi_pham] [datetime] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DanhGia]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DanhGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[id_san_pham] [int] NOT NULL,
	[so_sao] [float] NOT NULL,
	[noi_dung] [nvarchar](max) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[binh_luan_an] [bit] NOT NULL,
	[hinh_anh_an] [bit] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
	[id_nhan_vien] [int] NULL,
	[ngay_an_binh_luan] [datetime] NULL,
	[ngay_hien_binh_luan] [datetime] NULL,
	[ngay_an_hinh_anh] [datetime] NULL,
	[ngay_hien_hinh_anh] [datetime] NULL,
	[da_xoa] [bit] NOT NULL,
	[ngay_xoa] [datetime] NULL,
	[id_nguoi_xoa] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DanhMuc]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DanhMuc](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_danh_muc] [nvarchar](255) NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DanhMucThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DanhMucThuocTinh](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_danh_muc] [int] NOT NULL,
	[id_thuoc_tinh] [int] NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DonViVanChuyen]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DonViVanChuyen](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_don_vi] [nvarchar](50) NULL,
	[ten_don_vi] [nvarchar](100) NULL,
	[so_hotline] [nvarchar](20) NULL,
	[web_url] [nvarchar](100) NULL,
	[ma_token] [nvarchar](255) NULL,
	[ma_client] [nvarchar](100) NULL,
	[dia_chi_kho] [nvarchar](500) NULL,
	[phi_noi_dia] [decimal](18, 2) NULL,
	[phi_toan_quoc] [decimal](18, 2) NULL,
	[phien_ban] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DotGiamGia]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DotGiamGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_chien_dich] [nvarchar](255) NOT NULL,
	[ngay_bat_dau] [datetime] NOT NULL,
	[ngay_ket_thuc] [datetime] NOT NULL,
	[phan_tram_giam] [int] NULL,
	[gia_tri_giam] [decimal](18, 2) NULL,
	[gia_tu] [decimal](18, 2) NULL,
	[gia_den] [decimal](18, 2) NULL,
	[loai_giam_gia] [nvarchar](100) NOT NULL,
	[id_nhan_vien] [int] NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[EditLog]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[EditLog](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NULL,
	[ten_bang] [nvarchar](100) NOT NULL,
	[id_ban_ghi] [int] NOT NULL,
	[hanh_dong] [nvarchar](20) NOT NULL,
	[gia_tri_cu] [nvarchar](max) NULL,
	[gia_tri_moi] [nvarchar](max) NULL,
	[thoi_gian] [datetime] NOT NULL,
	[dia_chi_ip] [varchar](50) NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[vai_tro_thuc_hien] [nvarchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[flyway_schema_history]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[flyway_schema_history](
	[installed_rank] [int] NOT NULL,
	[version] [nvarchar](50) NULL,
	[description] [nvarchar](200) NULL,
	[type] [nvarchar](20) NOT NULL,
	[script] [nvarchar](1000) NOT NULL,
	[checksum] [int] NULL,
	[installed_by] [nvarchar](100) NOT NULL,
	[installed_on] [datetime] NOT NULL,
	[execution_time] [int] NOT NULL,
	[success] [bit] NOT NULL,
 CONSTRAINT [flyway_schema_history_pk] PRIMARY KEY CLUSTERED 
(
	[installed_rank] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[GiaoDichThanhToan]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GiaoDichThanhToan](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_giao_dich] [nvarchar](100) NOT NULL,
	[id_hoa_don] [int] NULL,
	[so_tien] [decimal](18, 2) NOT NULL,
	[cong_thanh_toan] [nvarchar](50) NOT NULL,
	[trang_thai] [nvarchar](50) NOT NULL,
	[du_lieu_tho] [nvarchar](max) NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[GioHang]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GioHang](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[GioHangChiTiet]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GioHangChiTiet](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_gio_hang] [int] NOT NULL,
	[id_trang_thai] [int] NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[so_luong] [int] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HinhAnhDanhGia]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HinhAnhDanhGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_danh_gia] [int] NOT NULL,
	[url_hinh_anh] [nvarchar](max) NOT NULL,
	[thu_tu] [int] NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HinhAnhSanPham]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HinhAnhSanPham](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[url_hinh_anh] [nvarchar](max) NOT NULL,
	[mau_sac] [nvarchar](50) NULL,
	[la_anh_chinh] [bit] NOT NULL,
	[thu_tu] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HoaDon]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HoaDon](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[id_nhan_vien] [int] NULL,
	[id_phuong_thuc_thanh_toan] [int] NOT NULL,
	[id_phieu_giam_gia] [int] NULL,
	[id_don_vi_van_chuyen] [int] NULL,
	[id_dia_chi] [int] NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_thanh_toan] [datetime] NULL,
	[tien_hang] [decimal](18, 2) NOT NULL,
	[phi_van_chuyen] [decimal](18, 2) NOT NULL,
	[so_tien_giam_gia] [decimal](18, 2) NOT NULL,
	[tong_tien] [decimal](18, 2) NOT NULL,
	[trang_thai_don_hang] [nvarchar](50) NOT NULL,
	[trang_thai_thanh_toan] [nvarchar](50) NOT NULL,
	[ten_nguoi_nhan] [nvarchar](100) NULL,
	[sdt_nhan] [nvarchar](15) NOT NULL,
	[dia_chi_nhan] [nvarchar](500) NOT NULL,
	[ly_do_huy] [nvarchar](500) NULL,
	[ly_do_hoan_tien] [nvarchar](500) NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[ma_giao_dich] [nvarchar](100) NULL,
	[nguoi_xac_nhan_thanh_toan] [nvarchar](100) NULL,
	[thoi_gian_xac_nhan] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HoaDonChiTiet]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HoaDonChiTiet](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_hoa_don] [int] NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[so_luong] [int] NOT NULL,
	[don_gia] [decimal](18, 2) NOT NULL,
	[gia_goc] [decimal](18, 2) NULL,
	[gia_sau_giam] [decimal](18, 2) NULL,
	[ten_san_pham_snapshot] [nvarchar](255) NULL,
	[sku_snapshot] [nvarchar](100) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ten_dot_giam_gia_snapshot] [nvarchar](255) NULL,
	[thuoc_tinh_snapshot] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[KhachHang]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[KhachHang](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[ho_ten_kh] [nvarchar](100) NULL,
	[so_dien_thoai_kh] [varchar](15) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[LichSuTrangThaiDonHang]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LichSuTrangThaiDonHang](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_hoa_don] [int] NOT NULL,
	[id_nhan_vien] [int] NULL,
	[hanh_dong] [nvarchar](100) NOT NULL,
	[trang_thai_cu] [nvarchar](50) NULL,
	[trang_thai_moi] [nvarchar](50) NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[thoi_gian] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MaKhoiPhuc]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MaKhoiPhuc](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[ma_xac_nhan] [nvarchar](100) NOT NULL,
	[loai_xac_nhan] [nvarchar](20) NOT NULL,
	[thoi_gian_het_han] [datetime] NOT NULL,
	[da_su_dung] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NewsletterSubscriber]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NewsletterSubscriber](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[email] [nvarchar](255) NOT NULL,
	[trang_thai] [nvarchar](30) NOT NULL,
	[token_huy_dang_ky] [nvarchar](255) NULL,
	[ngay_dang_ky] [datetime] NOT NULL,
	[ngay_huy] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NhanVien]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NhanVien](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[ho_ten] [nvarchar](100) NOT NULL,
	[chuc_vu] [nvarchar](100) NOT NULL,
	[so_dien_thoai_nv] [nvarchar](15) NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PhieuGiamGia]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PhieuGiamGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_phieu] [nvarchar](50) NOT NULL,
	[gia_tri] [decimal](18, 2) NOT NULL,
	[gia_tri_giam_toi_da] [decimal](18, 2) NULL,
	[don_vi] [nvarchar](10) NOT NULL,
	[ngay_bat_dau] [datetime] NOT NULL,
	[ngay_ket_thuc] [datetime] NOT NULL,
	[so_luong_con_lai] [int] NOT NULL,
	[loai_giam_gia] [nvarchar](100) NOT NULL,
	[id_nhan_vien] [int] NOT NULL,
	[gia_tri_don_hang_toi_thieu] [decimal](18, 2) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PhuongThucThanhToan]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PhuongThucThanhToan](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_phuong_thuc] [nvarchar](50) NULL,
	[ten_phuong_thuc] [nvarchar](100) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPham]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPham](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_danh_muc] [int] NOT NULL,
	[id_thuong_hieu] [int] NOT NULL,
	[id_nhan_vien] [int] NOT NULL,
	[ten_san_pham] [nvarchar](255) NOT NULL,
	[mo_ta] [nvarchar](max) NULL,
	[trang_thai] [bit] NOT NULL,
	[so_luot_danh_gia] [int] NOT NULL,
	[diem_trung_binh] [float] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPham_DotGiamGia]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPham_DotGiamGia](
	[id_san_pham] [int] NOT NULL,
	[id_dot_giam_gia] [int] NOT NULL,
 CONSTRAINT [PK_SanPham_DotGiamGia] PRIMARY KEY CLUSTERED 
(
	[id_san_pham] ASC,
	[id_dot_giam_gia] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPhamChiTiet]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPhamChiTiet](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_san_pham] [int] NOT NULL,
	[gia_nhap] [decimal](18, 2) NULL,
	[gia_ban] [decimal](18, 2) NOT NULL,
	[so_luong_ton] [int] NOT NULL,
	[trang_thai] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPhamChiTietThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPhamChiTietThuocTinh](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[id_thuoc_tinh] [int] NOT NULL,
	[gia_tri] [nvarchar](500) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPhamYeuThich]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPhamYeuThich](
	[id_khach_hang] [int] NOT NULL,
	[id_san_pham] [int] NOT NULL,
	[ngay_them] [datetime] NOT NULL,
 CONSTRAINT [PK_SanPhamYeuThich] PRIMARY KEY CLUSTERED 
(
	[id_khach_hang] ASC,
	[id_san_pham] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SoDiaChi]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SoDiaChi](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[ho_va_ten_nguoi_nhan] [nvarchar](100) NULL,
	[sdt_nguoi_nhan] [nvarchar](15) NOT NULL,
	[dia_chi_cu_the] [nvarchar](255) NOT NULL,
	[tinh_thanh] [nvarchar](100) NULL,
	[quan_huyen] [nvarchar](100) NULL,
	[phuong_xa] [nvarchar](100) NULL,
	[ghn_province_id] [int] NULL,
	[ghn_district_id] [int] NULL,
	[ghn_ward_code] [nvarchar](50) NULL,
	[dia_chi_mac_dinh] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TaiKhoan]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TaiKhoan](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[username] [nvarchar](255) NOT NULL,
	[mat_khau] [nvarchar](255) NULL,
	[vai_tro] [nvarchar](10) NOT NULL,
	[trang_thai_tai_khoan] [varchar](30) NOT NULL,
	[so_lan_mua_thanh_cong] [int] NOT NULL,
	[so_lan_nhac_nho_vi_pham] [int] NOT NULL,
	[ngay_vi_pham_gan_nhat] [datetime] NULL,
	[thoi_han_mo_khoa] [datetime] NULL,
	[ma_xac_thuc_khoa] [nvarchar](100) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ThongBao]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ThongBao](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[tieu_de] [nvarchar](255) NOT NULL,
	[noi_dung] [nvarchar](max) NOT NULL,
	[loai_thong_bao] [nvarchar](50) NULL,
	[da_doc] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ThuocTinh](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_thuoc_tinh] [nvarchar](100) NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ThuongHieu]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ThuongHieu](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_thuong_hieu] [nvarchar](255) NOT NULL,
	[logo] [nvarchar](255) NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TichHopVanChuyen]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TichHopVanChuyen](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_hoa_don] [int] NOT NULL,
	[nha_cung_cap] [nvarchar](50) NOT NULL,
	[ma_don_hang_ngoai] [nvarchar](100) NULL,
	[ma_van_don] [nvarchar](100) NULL,
	[trang_thai] [nvarchar](100) NULL,
	[du_lieu_yeu_cau] [nvarchar](max) NULL,
	[du_lieu_phan_hoi] [nvarchar](max) NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TrangThaiGioHang]    Script Date: 31/07/2026 08:39:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TrangThaiGioHang](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_trang_thai] [nvarchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Blog__BEF221E836D6382F]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[Blog] ADD UNIQUE NONCLUSTERED 
(
	[duong_dan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IDX_ChatConversation_Session]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatConversation_Session] ON [dbo].[ChatConversation]
(
	[session_id] ASC,
	[ngay_tao] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_ChatConversation_TaiKhoan]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatConversation_TaiKhoan] ON [dbo].[ChatConversation]
(
	[id_tai_khoan] ASC,
	[ngay_tao] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_ChatFeedback_TinNhan]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatFeedback_TinNhan] ON [dbo].[ChatFeedback]
(
	[id_tin_nhan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_ChatFeedback_TinNhan_Session]    Script Date: 31/07/2026 08:39:33 ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_ChatFeedback_TinNhan_Session] ON [dbo].[ChatFeedback]
(
	[id_tin_nhan] ASC,
	[session_id] ASC
)
WHERE ([session_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_ChatFeedback_TinNhan_TaiKhoan]    Script Date: 31/07/2026 08:39:33 ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_ChatFeedback_TinNhan_TaiKhoan] ON [dbo].[ChatFeedback]
(
	[id_tin_nhan] ASC,
	[id_tai_khoan] ASC
)
WHERE ([id_tai_khoan] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_ChatMessage_CuocTroChuyen]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatMessage_CuocTroChuyen] ON [dbo].[ChatMessage]
(
	[id_cuoc_tro_chuyen] ASC,
	[ngay_tao] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__CommentM__841D7448EA497485]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[CommentModerationKeyword] ADD UNIQUE NONCLUSTERED 
(
	[tu_khoa] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_KhachHang_SanPham]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[DanhGia] ADD  CONSTRAINT [UQ_KhachHang_SanPham] UNIQUE NONCLUSTERED 
(
	[id_khach_hang] ASC,
	[id_san_pham] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_DANHGIA_SANPHAM]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_DANHGIA_SANPHAM] ON [dbo].[DanhGia]
(
	[id_san_pham] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_DanhMucThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[DanhMucThuocTinh] ADD  CONSTRAINT [UQ_DanhMucThuocTinh] UNIQUE NONCLUSTERED 
(
	[id_danh_muc] ASC,
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_DanhMucThuocTinh_DanhMuc]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_DanhMucThuocTinh_DanhMuc] ON [dbo].[DanhMucThuocTinh]
(
	[id_danh_muc] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_DanhMucThuocTinh_ThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_DanhMucThuocTinh_ThuocTinh] ON [dbo].[DanhMucThuocTinh]
(
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_EDITLOG_TIME]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_EDITLOG_TIME] ON [dbo].[EditLog]
(
	[thoi_gian] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [flyway_schema_history_s_idx]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [flyway_schema_history_s_idx] ON [dbo].[flyway_schema_history]
(
	[success] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__GiaoDich__FB80ED33A71CA1EF]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[GiaoDichThanhToan] ADD UNIQUE NONCLUSTERED 
(
	[ma_giao_dich] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_GIAODICH_HOADON]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_GIAODICH_HOADON] ON [dbo].[GiaoDichThanhToan]
(
	[id_hoa_don] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_GIOHANG_KHACHHANG]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_GIOHANG_KHACHHANG] ON [dbo].[GioHang]
(
	[id_khach_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_GIOHANGCHITIET_GIOHANG]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_GIOHANGCHITIET_GIOHANG] ON [dbo].[GioHangChiTiet]
(
	[id_gio_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_HOADON_KHACHHANG]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_HOADON_KHACHHANG] ON [dbo].[HoaDon]
(
	[id_khach_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_HOADON_NGAYTAO]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_HOADON_NGAYTAO] ON [dbo].[HoaDon]
(
	[ngay_tao] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_HOADONCHITIET_HOADON]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_HOADONCHITIET_HOADON] ON [dbo].[HoaDonChiTiet]
(
	[id_hoa_don] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ__KhachHan__519100D3830BF1E0]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[KhachHang] ADD UNIQUE NONCLUSTERED 
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_KHACHHANG_TAIKHOAN]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_KHACHHANG_TAIKHOAN] ON [dbo].[KhachHang]
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Newslett__AB6E6164F9660214]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[NewsletterSubscriber] ADD UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ__NhanVien__519100D37D39DE79]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[NhanVien] ADD UNIQUE NONCLUSTERED 
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_NHANVIEN_TAIKHOAN]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_NHANVIEN_TAIKHOAN] ON [dbo].[NhanVien]
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__PhieuGia__11D0F07B39CDC6A3]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[PhieuGiamGia] ADD UNIQUE NONCLUSTERED 
(
	[ma_phieu] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_SPCT_ThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] ADD  CONSTRAINT [UQ_SPCT_ThuocTinh] UNIQUE NONCLUSTERED 
(
	[id_san_pham_chi_tiet] ASC,
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_SPCTTT_SPCT]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_SPCTTT_SPCT] ON [dbo].[SanPhamChiTietThuocTinh]
(
	[id_san_pham_chi_tiet] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_SPCTTT_ThuocTinh]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_SPCTTT_ThuocTinh] ON [dbo].[SanPhamChiTietThuocTinh]
(
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_SODIACHI_KHACHHANG]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_SODIACHI_KHACHHANG] ON [dbo].[SoDiaChi]
(
	[id_khach_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__TaiKhoan__F3DBC5729B9E1E3B]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[TaiKhoan] ADD UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__ThuocTin__B85BFDA0B2A1710C]    Script Date: 31/07/2026 08:39:33 ******/
ALTER TABLE [dbo].[ThuocTinh] ADD UNIQUE NONCLUSTERED 
(
	[ten_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_TICHHOPVC_HOADON]    Script Date: 31/07/2026 08:39:33 ******/
CREATE NONCLUSTERED INDEX [IDX_TICHHOPVC_HOADON] ON [dbo].[TichHopVanChuyen]
(
	[id_hoa_don] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Blog] ADD  DEFAULT ((0)) FOR [da_xoa]
GO
ALTER TABLE [dbo].[Blog] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[BlogComment] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[BlogComment] ADD  DEFAULT ((0)) FOR [da_xoa]
GO
ALTER TABLE [dbo].[ChatConversation] ADD  CONSTRAINT [DF_ChatConversation_TrangThai]  DEFAULT (N'ACTIVE') FOR [trang_thai]
GO
ALTER TABLE [dbo].[ChatConversation] ADD  CONSTRAINT [DF_ChatConversation_NgayTao]  DEFAULT (sysdatetime()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ChatFeedback] ADD  CONSTRAINT [DF_ChatFeedback_NgayTao]  DEFAULT (sysdatetime()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ChatMessage] ADD  CONSTRAINT [DF_ChatMessage_TrangThai]  DEFAULT (N'SUCCESS') FOR [trang_thai]
GO
ALTER TABLE [dbo].[ChatMessage] ADD  CONSTRAINT [DF_ChatMessage_NgayTao]  DEFAULT (sysdatetime()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[CommentModerationKeyword] ADD  DEFAULT ((1)) FOR [kich_hoat]
GO
ALTER TABLE [dbo].[CommentModerationKeyword] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[CommentViolationLog] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT ((0)) FOR [binh_luan_an]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT ((0)) FOR [hinh_anh_an]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT ((0)) FOR [da_xoa]
GO
ALTER TABLE [dbo].[DanhMuc] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[DanhMucThuocTinh] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[DotGiamGia] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[EditLog] ADD  DEFAULT (getdate()) FOR [thoi_gian]
GO
ALTER TABLE [dbo].[flyway_schema_history] ADD  DEFAULT (getdate()) FOR [installed_on]
GO
ALTER TABLE [dbo].[GiaoDichThanhToan] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[GioHang] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[GioHangChiTiet] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[HinhAnhDanhGia] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[HinhAnhSanPham] ADD  DEFAULT ((0)) FOR [la_anh_chinh]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT ((0)) FOR [tien_hang]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT ((0)) FOR [phi_van_chuyen]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT ((0)) FOR [so_tien_giam_gia]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT (N'CHO_XAC_NHAN') FOR [trang_thai_don_hang]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT (N'CHO_THANH_TOAN') FOR [trang_thai_thanh_toan]
GO
ALTER TABLE [dbo].[HoaDonChiTiet] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[KhachHang] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] ADD  DEFAULT (getdate()) FOR [thoi_gian]
GO
ALTER TABLE [dbo].[MaKhoiPhuc] ADD  DEFAULT ((0)) FOR [da_su_dung]
GO
ALTER TABLE [dbo].[MaKhoiPhuc] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[NewsletterSubscriber] ADD  DEFAULT ('ACTIVE') FOR [trang_thai]
GO
ALTER TABLE [dbo].[NewsletterSubscriber] ADD  DEFAULT (getdate()) FOR [ngay_dang_ky]
GO
ALTER TABLE [dbo].[NhanVien] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT ((0)) FOR [so_luot_danh_gia]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT ((0.0)) FOR [diem_trung_binh]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT ((0)) FOR [so_luong_ton]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[SanPhamYeuThich] ADD  DEFAULT (getdate()) FOR [ngay_them]
GO
ALTER TABLE [dbo].[SoDiaChi] ADD  DEFAULT ((0)) FOR [dia_chi_mac_dinh]
GO
ALTER TABLE [dbo].[SoDiaChi] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT ('ACTIVE') FOR [trang_thai_tai_khoan]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT ((0)) FOR [so_lan_mua_thanh_cong]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT ((0)) FOR [so_lan_nhac_nho_vi_pham]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ThongBao] ADD  DEFAULT ((0)) FOR [da_doc]
GO
ALTER TABLE [dbo].[ThongBao] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ThuocTinh] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[ThuongHieu] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[TichHopVanChuyen] ADD  DEFAULT ('GHN') FOR [nha_cung_cap]
GO
ALTER TABLE [dbo].[TichHopVanChuyen] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[Blog]  WITH CHECK ADD  CONSTRAINT [FK_Blog_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[Blog] CHECK CONSTRAINT [FK_Blog_TaiKhoan]
GO
ALTER TABLE [dbo].[BlogComment]  WITH CHECK ADD  CONSTRAINT [FK_BlogComment_Blog] FOREIGN KEY([id_blog])
REFERENCES [dbo].[Blog] ([id])
GO
ALTER TABLE [dbo].[BlogComment] CHECK CONSTRAINT [FK_BlogComment_Blog]
GO
ALTER TABLE [dbo].[BlogComment]  WITH CHECK ADD  CONSTRAINT [FK_BlogComment_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[BlogComment] CHECK CONSTRAINT [FK_BlogComment_TaiKhoan]
GO
ALTER TABLE [dbo].[ChatConversation]  WITH CHECK ADD  CONSTRAINT [FK_ChatConversation_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[ChatConversation] CHECK CONSTRAINT [FK_ChatConversation_TaiKhoan]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [FK_ChatFeedback_ChatMessage] FOREIGN KEY([id_tin_nhan])
REFERENCES [dbo].[ChatMessage] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [FK_ChatFeedback_ChatMessage]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [FK_ChatFeedback_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [FK_ChatFeedback_TaiKhoan]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [FK_ChatMessage_ChatConversation] FOREIGN KEY([id_cuoc_tro_chuyen])
REFERENCES [dbo].[ChatConversation] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [FK_ChatMessage_ChatConversation]
GO
ALTER TABLE [dbo].[CommentViolationLog]  WITH CHECK ADD  CONSTRAINT [FK_CommentViolationLog_DanhGia] FOREIGN KEY([id_danh_gia])
REFERENCES [dbo].[DanhGia] ([id])
GO
ALTER TABLE [dbo].[CommentViolationLog] CHECK CONSTRAINT [FK_CommentViolationLog_DanhGia]
GO
ALTER TABLE [dbo].[CommentViolationLog]  WITH CHECK ADD  CONSTRAINT [FK_CommentViolationLog_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[CommentViolationLog] CHECK CONSTRAINT [FK_CommentViolationLog_SanPham]
GO
ALTER TABLE [dbo].[CommentViolationLog]  WITH CHECK ADD  CONSTRAINT [FK_CommentViolationLog_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[CommentViolationLog] CHECK CONSTRAINT [FK_CommentViolationLog_TaiKhoan]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_KhachHang]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_NguoiHienHA] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_NguoiHienHA]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_NguoiXoa] FOREIGN KEY([id_nguoi_xoa])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_NguoiXoa]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_SanPham]
GO
ALTER TABLE [dbo].[DanhMucThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_DanhMucThuocTinh_DanhMuc] FOREIGN KEY([id_danh_muc])
REFERENCES [dbo].[DanhMuc] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[DanhMucThuocTinh] CHECK CONSTRAINT [FK_DanhMucThuocTinh_DanhMuc]
GO
ALTER TABLE [dbo].[DanhMucThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_DanhMucThuocTinh_ThuocTinh] FOREIGN KEY([id_thuoc_tinh])
REFERENCES [dbo].[ThuocTinh] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[DanhMucThuocTinh] CHECK CONSTRAINT [FK_DanhMucThuocTinh_ThuocTinh]
GO
ALTER TABLE [dbo].[DotGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_DotGiamGia_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[DotGiamGia] CHECK CONSTRAINT [FK_DotGiamGia_NhanVien]
GO
ALTER TABLE [dbo].[EditLog]  WITH CHECK ADD  CONSTRAINT [FK_EditLog_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[EditLog] CHECK CONSTRAINT [FK_EditLog_TaiKhoan]
GO
ALTER TABLE [dbo].[GiaoDichThanhToan]  WITH CHECK ADD  CONSTRAINT [FK_GiaoDichThanhToan_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[GiaoDichThanhToan] CHECK CONSTRAINT [FK_GiaoDichThanhToan_HoaDon]
GO
ALTER TABLE [dbo].[GioHang]  WITH CHECK ADD  CONSTRAINT [FK_GioHang_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[GioHang] CHECK CONSTRAINT [FK_GioHang_KhachHang]
GO
ALTER TABLE [dbo].[GioHangChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_GioHangChiTiet_GioHang] FOREIGN KEY([id_gio_hang])
REFERENCES [dbo].[GioHang] ([id])
GO
ALTER TABLE [dbo].[GioHangChiTiet] CHECK CONSTRAINT [FK_GioHangChiTiet_GioHang]
GO
ALTER TABLE [dbo].[GioHangChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_GioHangChiTiet_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
GO
ALTER TABLE [dbo].[GioHangChiTiet] CHECK CONSTRAINT [FK_GioHangChiTiet_SPCT]
GO
ALTER TABLE [dbo].[GioHangChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_GioHangChiTiet_TrangThai] FOREIGN KEY([id_trang_thai])
REFERENCES [dbo].[TrangThaiGioHang] ([id])
GO
ALTER TABLE [dbo].[GioHangChiTiet] CHECK CONSTRAINT [FK_GioHangChiTiet_TrangThai]
GO
ALTER TABLE [dbo].[HinhAnhDanhGia]  WITH CHECK ADD  CONSTRAINT [FK_HinhAnhDanhGia_DanhGia] FOREIGN KEY([id_danh_gia])
REFERENCES [dbo].[DanhGia] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[HinhAnhDanhGia] CHECK CONSTRAINT [FK_HinhAnhDanhGia_DanhGia]
GO
ALTER TABLE [dbo].[HinhAnhSanPham]  WITH CHECK ADD  CONSTRAINT [FK_HinhAnhSanPham_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
GO
ALTER TABLE [dbo].[HinhAnhSanPham] CHECK CONSTRAINT [FK_HinhAnhSanPham_SPCT]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_DiaChi] FOREIGN KEY([id_dia_chi])
REFERENCES [dbo].[SoDiaChi] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_DiaChi]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_DVVC] FOREIGN KEY([id_don_vi_van_chuyen])
REFERENCES [dbo].[DonViVanChuyen] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_DVVC]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_KhachHang]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_NhanVien]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_PhieuGiamGia] FOREIGN KEY([id_phieu_giam_gia])
REFERENCES [dbo].[PhieuGiamGia] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_PhieuGiamGia]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_PTTT] FOREIGN KEY([id_phuong_thuc_thanh_toan])
REFERENCES [dbo].[PhuongThucThanhToan] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_PTTT]
GO
ALTER TABLE [dbo].[HoaDonChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_HoaDonChiTiet_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[HoaDonChiTiet] CHECK CONSTRAINT [FK_HoaDonChiTiet_HoaDon]
GO
ALTER TABLE [dbo].[HoaDonChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_HoaDonChiTiet_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
GO
ALTER TABLE [dbo].[HoaDonChiTiet] CHECK CONSTRAINT [FK_HoaDonChiTiet_SPCT]
GO
ALTER TABLE [dbo].[KhachHang]  WITH CHECK ADD  CONSTRAINT [FK_KhachHang_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[KhachHang] CHECK CONSTRAINT [FK_KhachHang_TaiKhoan]
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang]  WITH CHECK ADD  CONSTRAINT [FK_LichSu_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] CHECK CONSTRAINT [FK_LichSu_HoaDon]
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang]  WITH CHECK ADD  CONSTRAINT [FK_LichSu_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] CHECK CONSTRAINT [FK_LichSu_NhanVien]
GO
ALTER TABLE [dbo].[MaKhoiPhuc]  WITH CHECK ADD  CONSTRAINT [FK_MaKhoiPhuc_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[MaKhoiPhuc] CHECK CONSTRAINT [FK_MaKhoiPhuc_TaiKhoan]
GO
ALTER TABLE [dbo].[NhanVien]  WITH CHECK ADD  CONSTRAINT [FK_NhanVien_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[NhanVien] CHECK CONSTRAINT [FK_NhanVien_TaiKhoan]
GO
ALTER TABLE [dbo].[PhieuGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_PhieuGiamGia_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[PhieuGiamGia] CHECK CONSTRAINT [FK_PhieuGiamGia_NhanVien]
GO
ALTER TABLE [dbo].[SanPham]  WITH CHECK ADD  CONSTRAINT [FK_SanPham_DanhMuc] FOREIGN KEY([id_danh_muc])
REFERENCES [dbo].[DanhMuc] ([id])
GO
ALTER TABLE [dbo].[SanPham] CHECK CONSTRAINT [FK_SanPham_DanhMuc]
GO
ALTER TABLE [dbo].[SanPham]  WITH CHECK ADD  CONSTRAINT [FK_SanPham_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[SanPham] CHECK CONSTRAINT [FK_SanPham_NhanVien]
GO
ALTER TABLE [dbo].[SanPham]  WITH CHECK ADD  CONSTRAINT [FK_SanPham_ThuongHieu] FOREIGN KEY([id_thuong_hieu])
REFERENCES [dbo].[ThuongHieu] ([id])
GO
ALTER TABLE [dbo].[SanPham] CHECK CONSTRAINT [FK_SanPham_ThuongHieu]
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_SPDGG_DotGiamGia] FOREIGN KEY([id_dot_giam_gia])
REFERENCES [dbo].[DotGiamGia] ([id])
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia] CHECK CONSTRAINT [FK_SPDGG_DotGiamGia]
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_SPDGG_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia] CHECK CONSTRAINT [FK_SPDGG_SanPham]
GO
ALTER TABLE [dbo].[SanPhamChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_SanPhamChiTiet_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[SanPhamChiTiet] CHECK CONSTRAINT [FK_SanPhamChiTiet_SanPham]
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_SPCTTT_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] CHECK CONSTRAINT [FK_SPCTTT_SPCT]
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_SPCTTT_ThuocTinh] FOREIGN KEY([id_thuoc_tinh])
REFERENCES [dbo].[ThuocTinh] ([id])
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] CHECK CONSTRAINT [FK_SPCTTT_ThuocTinh]
GO
ALTER TABLE [dbo].[SanPhamYeuThich]  WITH CHECK ADD  CONSTRAINT [FK_SanPhamYeuThich_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[SanPhamYeuThich] CHECK CONSTRAINT [FK_SanPhamYeuThich_KhachHang]
GO
ALTER TABLE [dbo].[SanPhamYeuThich]  WITH CHECK ADD  CONSTRAINT [FK_SanPhamYeuThich_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[SanPhamYeuThich] CHECK CONSTRAINT [FK_SanPhamYeuThich_SanPham]
GO
ALTER TABLE [dbo].[SoDiaChi]  WITH CHECK ADD  CONSTRAINT [FK_SoDiaChi_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[SoDiaChi] CHECK CONSTRAINT [FK_SoDiaChi_KhachHang]
GO
ALTER TABLE [dbo].[ThongBao]  WITH CHECK ADD  CONSTRAINT [FK_ThongBao_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ThongBao] CHECK CONSTRAINT [FK_ThongBao_TaiKhoan]
GO
ALTER TABLE [dbo].[TichHopVanChuyen]  WITH CHECK ADD  CONSTRAINT [FK_TichHopVanChuyen_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[TichHopVanChuyen] CHECK CONSTRAINT [FK_TichHopVanChuyen_HoaDon]
GO
ALTER TABLE [dbo].[Blog]  WITH CHECK ADD CHECK  (([trang_thai]='PUBLISHED' OR [trang_thai]='DRAFT'))
GO
ALTER TABLE [dbo].[BlogComment]  WITH CHECK ADD  CONSTRAINT [CK_BlogComment_Content_Length] CHECK  ((len(ltrim(rtrim([noi_dung])))>=(1) AND len(ltrim(rtrim([noi_dung])))<=(1000)))
GO
ALTER TABLE [dbo].[BlogComment] CHECK CONSTRAINT [CK_BlogComment_Content_Length]
GO
ALTER TABLE [dbo].[ChatConversation]  WITH CHECK ADD  CONSTRAINT [CK_ChatConversation_NguoiDung] CHECK  (([id_tai_khoan] IS NOT NULL OR nullif(ltrim(rtrim([session_id])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[ChatConversation] CHECK CONSTRAINT [CK_ChatConversation_NguoiDung]
GO
ALTER TABLE [dbo].[ChatConversation]  WITH CHECK ADD  CONSTRAINT [CK_ChatConversation_TrangThai] CHECK  (([trang_thai]=N'ARCHIVED' OR [trang_thai]=N'CLOSED' OR [trang_thai]=N'ACTIVE'))
GO
ALTER TABLE [dbo].[ChatConversation] CHECK CONSTRAINT [CK_ChatConversation_TrangThai]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [CK_ChatFeedback_DanhGia] CHECK  (([danh_gia]=(1) OR [danh_gia]=(-1)))
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [CK_ChatFeedback_DanhGia]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [CK_ChatFeedback_NguoiDanhGia] CHECK  (([id_tai_khoan] IS NOT NULL OR nullif(ltrim(rtrim([session_id])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [CK_ChatFeedback_NguoiDanhGia]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_NoiDung] CHECK  ((nullif(ltrim(rtrim([noi_dung])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_NoiDung]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_ThoiGianXuLy] CHECK  (([thoi_gian_xu_ly_ms] IS NULL OR [thoi_gian_xu_ly_ms]>=(0)))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_ThoiGianXuLy]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_TokenDauRa] CHECK  (([so_token_dau_ra] IS NULL OR [so_token_dau_ra]>=(0)))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_TokenDauRa]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_TokenDauVao] CHECK  (([so_token_dau_vao] IS NULL OR [so_token_dau_vao]>=(0)))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_TokenDauVao]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_TrangThai] CHECK  (([trang_thai]=N'BLOCKED' OR [trang_thai]=N'FAILED' OR [trang_thai]=N'SUCCESS' OR [trang_thai]=N'PENDING'))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_TrangThai]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_VaiTro] CHECK  (([vai_tro]=N'TOOL' OR [vai_tro]=N'SYSTEM' OR [vai_tro]=N'ASSISTANT' OR [vai_tro]=N'USER'))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_VaiTro]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD CHECK  (([so_sao]>=(1.0) AND [so_sao]<=(5.0)))
GO
ALTER TABLE [dbo].[EditLog]  WITH CHECK ADD CHECK  (([hanh_dong]='DELETE' OR [hanh_dong]='UPDATE' OR [hanh_dong]='INSERT'))
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang]  WITH CHECK ADD  CONSTRAINT [CK_LichSuTrangThaiDonHang_HanhDong] CHECK  (([hanh_dong]=N'GHI_CHU' OR [hanh_dong]=N'CAP_NHAT_MA_VAN_DON' OR [hanh_dong]=N'CAP_NHAT_TRANG_THAI' OR [hanh_dong]=N'XAC_NHAN_HOAN_TIEN' OR [hanh_dong]=N'XAC_NHAN_THANH_TOAN' OR [hanh_dong]=N'GIAO_HANG' OR [hanh_dong]=N'DONG_GOI' OR [hanh_dong]=N'HUY_DON' OR [hanh_dong]=N'XAC_NHAN_DON' OR [hanh_dong]=N'TAO_DON'))
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] CHECK CONSTRAINT [CK_LichSuTrangThaiDonHang_HanhDong]
GO
ALTER TABLE [dbo].[MaKhoiPhuc]  WITH CHECK ADD CHECK  (([loai_xac_nhan]='SMS' OR [loai_xac_nhan]='EMAIL'))
GO
ALTER TABLE [dbo].[PhieuGiamGia]  WITH CHECK ADD CHECK  (([don_vi]='%' OR [don_vi]='VND'))
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh]  WITH CHECK ADD  CONSTRAINT [CK_SPCTTT_GiaTri] CHECK  ((nullif(ltrim(rtrim([gia_tri])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] CHECK CONSTRAINT [CK_SPCTTT_GiaTri]
GO
ALTER TABLE [dbo].[TaiKhoan]  WITH CHECK ADD CHECK  (([vai_tro]='QL' OR [vai_tro]='NV' OR [vai_tro]='KH'))
GO
USE [master]
GO
ALTER DATABASE [BadmintonShopDB1] SET  READ_WRITE 
GO

-- =============================================================================
-- SEED DATA FOR BADMINTON SHOP
-- Generated on: 2026-08-05
-- =============================================================================

USE [BadmintonShopDB1]
GO

SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. TaiKhoan (Mật khẩu mặc định: 123456 -> BCrypt)
INSERT INTO [dbo].[TaiKhoan] ([username],[mat_khau],[vai_tro],[trang_thai_tai_khoan],[so_lan_mua_thanh_cong],[so_lan_nhac_nho_vi_pham],[ngay_tao]) VALUES
(N'admin', N'$2a$10$Z9VSbcL8RWNqcDTKi1cRO.QVey7xEfRfGDHu0rW82wcZ3dnTzOX96', N'QL', N'ACTIVE', 0, 0, GETDATE()),
(N'nhanvien1@smashvn.com', N'$2a$10$Z9VSbcL8RWNqcDTKi1cRO.QVey7xEfRfGDHu0rW82wcZ3dnTzOX96', N'NV', N'ACTIVE', 0, 0, GETDATE()),
(N'khachhang1@gmail.com', N'$2a$10$Z9VSbcL8RWNqcDTKi1cRO.QVey7xEfRfGDHu0rW82wcZ3dnTzOX96', N'KH', N'ACTIVE', 5, 0, GETDATE()),
(N'khachhang2@gmail.com', N'$2a$10$Z9VSbcL8RWNqcDTKi1cRO.QVey7xEfRfGDHu0rW82wcZ3dnTzOX96', N'KH', N'ACTIVE', 2, 0, GETDATE()),
(N'khachhang3@gmail.com', N'$2a$10$Z9VSbcL8RWNqcDTKi1cRO.QVey7xEfRfGDHu0rW82wcZ3dnTzOX96', N'KH', N'ACTIVE', 0, 0, GETDATE());
GO

-- 2. NhanVien
INSERT INTO [dbo].[NhanVien] ([id_tai_khoan],[ho_ten],[chuc_vu],[so_dien_thoai_nv],[ngay_tao]) VALUES
(1, N'Nguyễn Văn Quản Lý', N'Quản lý cửa hàng', N'0901234567', GETDATE()),
(2, N'Trần Thị Nhân Viên', N'Nhân viên bán hàng', N'0912345678', GETDATE());
GO

-- 3. KhachHang
INSERT INTO [dbo].[KhachHang] ([id_tai_khoan],[ho_ten_kh],[so_dien_thoai_kh],[ngay_tao]) VALUES
(3, N'Lê Văn Khách', N'0923456789', GETDATE()),
(4, N'Phạm Thị Hương', N'0934567890', GETDATE()),
(5, N'Nguyễn Minh Tuấn', N'0945678901', GETDATE());
GO

-- 4. DanhMuc
-- 1: Vợt cầu lông, 2: Giày cầu lông, 3: Áo cầu lông, 4: Quần cầu lông, 5: Balo cầu lông, 6: Túi cầu lông, 7: Dây cước, 8: Quấn cán
INSERT INTO [dbo].[DanhMuc] ([ten_danh_muc],[trang_thai]) VALUES
(N'Vợt cầu lông', 1),
(N'Giày cầu lông', 1),
(N'Áo cầu lông', 1),
(N'Quần cầu lông', 1),
(N'Balo cầu lông', 1),
(N'Túi cầu lông', 1),
(N'Dây cước', 1),
(N'Quấn cán', 1);
GO

-- 5. ThuongHieu
-- 1: Yonex, 2: Li-Ning, 3: Victor, 4: Mizuno, 5: GOSEN, 6: Kizuna
INSERT INTO [dbo].[ThuongHieu] ([ten_thuong_hieu],[logo],[trang_thai]) VALUES
(N'Yonex', NULL, 1),
(N'Li-Ning', NULL, 1),
(N'Victor', NULL, 1),
(N'Mizuno', NULL, 1),
(N'GOSEN', NULL, 1),
(N'Kizuna', NULL, 1);
GO

-- 6. ThuocTinh
-- 1: Màu sắc, 2: Độ cứng, 3: Trọng lượng, 4: Điểm cân bằng, 5: Loại người chơi, 6: Kích thước, 7: Sức căng
INSERT INTO [dbo].[ThuocTinh] ([ten_thuoc_tinh],[trang_thai]) VALUES
(N'Màu sắc', 1),
(N'Độ cứng', 1),
(N'Trọng lượng', 1),
(N'Điểm cân bằng', 1),
(N'Loại người chơi', 1),
(N'Kích thước', 1),
(N'Sức căng', 1);
GO

-- 7. DanhMucThuocTinh
-- Vợt (1): Màu sắc, Độ cứng, Trọng lượng, Điểm cân bằng, Loại người chơi, Sức căng
-- Giày (2): Màu sắc, Kích thước
-- Áo (3): Màu sắc, Kích thước
-- Quần (4): Màu sắc, Kích thước
-- Balo (5), Túi (6), Cước (7), Quấn cán (8): Màu sắc
INSERT INTO [dbo].[DanhMucThuocTinh] ([id_danh_muc],[id_thuoc_tinh],[trang_thai]) VALUES
(1,1,1), (1,2,1), (1,3,1), (1,4,1), (1,5,1), (1,7,1),
(2,1,1), (2,6,1),
(3,1,1), (3,6,1),
(4,1,1), (4,6,1),
(5,1,1),
(6,1,1),
(7,1,1),
(8,1,1);
GO

-- 8. PhuongThucThanhToan
INSERT INTO [dbo].[PhuongThucThanhToan] ([ma_phuong_thuc],[ten_phuong_thuc]) VALUES
(N'COD', N'Thanh toán khi nhận hàng'),
(N'VNPAY', N'VNPay'),
(N'MOMO', N'Ví MoMo');
GO

-- 9. DonViVanChuyen
INSERT INTO [dbo].[DonViVanChuyen] ([ma_don_vi],[ten_don_vi],[so_hotline],[web_url],[phi_noi_dia],[phi_toan_quoc]) VALUES
(N'GHN', N'Giao Hàng Nhanh', N'1900636677', N'https://ghn.vn', 25000, 35000);
GO

-- 10. TrangThaiGioHang
INSERT INTO [dbo].[TrangThaiGioHang] ([ten_trang_thai]) VALUES (N'Đang chờ'), (N'Đã chọn');
GO

-- 11. SanPham
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Lining Bladex 800 Speed 2026', N'Sản phẩm Lining Bladex 800 Speed 2026 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Axforce 100 Gen 2', N'Sản phẩm Vợt cầu lông Lining Axforce 100 Gen 2 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025', N'Sản phẩm Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Axforce BigBang new', N'Sản phẩm Vợt cầu lông Lining Axforce BigBang new chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Bladex Assassin', N'Sản phẩm Vợt cầu lông Lining Bladex Assassin chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Halbertec 1000 chính hãng', N'Sản phẩm Vợt cầu lông Lining Halbertec 1000 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt Cầu Lông Lining Halbertec Motor', N'Sản phẩm Vợt Cầu Lông Lining Halbertec Motor chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt Cầu Lông Mizuno Altair T327', N'Sản phẩm Vợt Cầu Lông Mizuno Altair T327 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt cầu lông Mizuno BDSS Altius Sonic', N'Sản phẩm Vợt cầu lông Mizuno BDSS Altius Sonic chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt cầu lông Mizuno Fortius 55 Strive', N'Sản phẩm Vợt cầu lông Mizuno Fortius 55 Strive chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng', N'Sản phẩm Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Auraspeed 99 J 2026', N'Sản phẩm Vợt cầu lông Victor Auraspeed 99 J 2026 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor AuraSpeed A', N'Sản phẩm Vợt cầu lông Victor AuraSpeed A chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Auraspeed FANTOME F HYQ', N'Sản phẩm Vợt cầu lông Victor Auraspeed FANTOME F HYQ chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt Cầu Lông Victor AuraSpeed LYC', N'Sản phẩm Vợt Cầu Lông Victor AuraSpeed LYC chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor DriveX 12 WT25', N'Sản phẩm Vợt cầu lông Victor DriveX 12 WT25 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Jetspeed S12 II R', N'Sản phẩm Vợt cầu lông Victor Jetspeed S12 II R chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Thruster Hammer Light', N'Sản phẩm Vợt cầu lông Victor Thruster Hammer Light chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 10', N'Sản phẩm Vợt cầu lông Yonex Astrox 10 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 100 Tour VA', N'Sản phẩm Vợt cầu lông Yonex Astrox 100 Tour VA chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng', N'Sản phẩm Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng', N'Sản phẩm Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 99 Pro 2025', N'Sản phẩm Vợt cầu lông Yonex Astrox 99 Pro 2025 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 99 Tour 2025', N'Sản phẩm Vợt cầu lông Yonex Astrox 99 Tour 2025 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox Lite 43i', N'Sản phẩm Vợt cầu lông Yonex Astrox Lite 43i chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Nanoflare 1000Z', N'Sản phẩm Vợt cầu lông Yonex Nanoflare 1000Z chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', N'Sản phẩm Vợt cầu lông Yonex Nanoflare 700 Pro 2024 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 2, 1, N'Giày cầu lông Lining AYZW007-3 chính hãng', N'Sản phẩm Giày cầu lông Lining AYZW007-3 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 3, 1, N'Giày cầu lông Victor A531 WAG chính hãng', N'Sản phẩm Giày cầu lông Victor A531 WAG chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 3, 1, N'Giày cầu lông Victor A970 cADVAM - Trắng chính hãng', N'Sản phẩm Giày cầu lông Victor A970 cADVAM - Trắng chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 1, 1, N'Giày cầu lông Yonex Eclipsion X3', N'Sản phẩm Giày cầu lông Yonex Eclipsion X3 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 1, 1, N'Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng', N'Sản phẩm Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 1, 1, N'Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng', N'Sản phẩm Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 2, 1, N'Áo cầu lông Lining P-APLUA47-1 nam chính hãng', N'Sản phẩm Áo cầu lông Lining P-APLUA47-1 nam chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 1, 1, N'Áo cầu lông Yonex RM3216 - Poinciana chính hãng', N'Sản phẩm Áo cầu lông Yonex RM3216 - Poinciana chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 1, 1, N'Áo cầu lông Yonex RM3232 - White chính hãng', N'Sản phẩm Áo cầu lông Yonex RM3232 - White chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 3, 1, N'Áo hoodie lót bông Victor Vic07 - Đỏ', N'Sản phẩm Áo hoodie lót bông Victor Vic07 - Đỏ chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần cầu lông Lining 92001 - Đen trắng', N'Sản phẩm Quần cầu lông Lining 92001 - Đen trắng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần cầu lông Lining 9682 - Đen xanh ngọc', N'Sản phẩm Quần cầu lông Lining 9682 - Đen xanh ngọc chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần cầu lông lining nữ đen - mã 081', N'Sản phẩm Quần cầu lông lining nữ đen - mã 081 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần Cầu Lông Lining training trắng', N'Sản phẩm Quần Cầu Lông Lining training trắng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 1, 1, N'Quần cầu lông Yonex Q3 nữ - Đen trắng', N'Sản phẩm Quần cầu lông Yonex Q3 nữ - Đen trắng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 1, 1, N'Quần cầu lông Yonex TSM3117 - Lion chính hãng', N'Sản phẩm Quần cầu lông Yonex TSM3117 - Lion chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 2, 1, N'Balo cầu lông Lining P-ABSV133-3 chính hãng', N'Sản phẩm Balo cầu lông Lining P-ABSV133-3 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 3, 1, N'Balo cầu lông Victor BR5042 EXA - Trắng đỏ chính hãng', N'Sản phẩm Balo cầu lông Victor BR5042 EXA - Trắng đỏ chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 3, 1, N'Balo cầu lông Victor BR5051 CNY - Trắng đỏ chính hãng', N'Sản phẩm Balo cầu lông Victor BR5051 CNY - Trắng đỏ chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 1, 1, N'Balo cầu lông Yonex BAG525B1212Z', N'Sản phẩm Balo cầu lông Yonex BAG525B1212Z chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 2, 1, N'Túi cầu lông Lining ABJU013-2 chính hãng', N'Sản phẩm Túi cầu lông Lining ABJU013-2 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 2, 1, N'Túi cầu lông Lining P-ABLV029-3 chính hãng', N'Sản phẩm Túi cầu lông Lining P-ABLV029-3 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 3, 1, N'Túi cầu lông Victor BR5651CNY - Trắng đỏ chính hãng', N'Sản phẩm Túi cầu lông Victor BR5651CNY - Trắng đỏ chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 1, 1, N'Túi cầu lông Yonex BA92026EX xám chính hãng', N'Sản phẩm Túi cầu lông Yonex BA92026EX xám chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 1, 1, N'Túi Xách Cầu Lông Yonex 3D 2241R (BKDBL)', N'Sản phẩm Túi Xách Cầu Lông Yonex 3D 2241R (BKDBL) chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 5, 1, N'Dây cước căng vợt GOSEN Ryzonic 62', N'Sản phẩm Dây cước căng vợt GOSEN Ryzonic 62 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 6, 1, N'Dây cước căng vợt Kizuna Z65X', N'Sản phẩm Dây cước căng vợt Kizuna Z65X chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 1, 1, N'Dây cước căng vợt Yonex BG 66 Ultimax', N'Sản phẩm Dây cước căng vợt Yonex BG 66 Ultimax chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 1, 1, N'Dây cước căng vợt Yonex BG SKY', N'Sản phẩm Dây cước căng vợt Yonex BG SKY chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 1, 1, N'Dây cước căng vợt Yonex Nanogy BG 95', N'Sản phẩm Dây cước căng vợt Yonex Nanogy BG 95 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (8, 1, 1, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', N'Sản phẩm Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn) chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());

GO

-- 12. SanPhamChiTiet
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (1, 2500000, 3200000, 30, 1, GETDATE()); -- spct_1
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (2, 2500000, 3200000, 30, 1, GETDATE()); -- spct_2
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (3, 2500000, 3200000, 30, 1, GETDATE()); -- spct_3
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (4, 2500000, 3200000, 30, 1, GETDATE()); -- spct_4
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (5, 2500000, 3200000, 30, 1, GETDATE()); -- spct_5
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (6, 2500000, 3200000, 30, 1, GETDATE()); -- spct_6
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (7, 2500000, 3200000, 30, 1, GETDATE()); -- spct_7
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (8, 2500000, 3200000, 30, 1, GETDATE()); -- spct_8
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (9, 2500000, 3200000, 30, 1, GETDATE()); -- spct_9
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (10, 2500000, 3200000, 30, 1, GETDATE()); -- spct_10
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (11, 2500000, 3200000, 30, 1, GETDATE()); -- spct_11
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (12, 2500000, 3200000, 30, 1, GETDATE()); -- spct_12
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (13, 2500000, 3200000, 30, 1, GETDATE()); -- spct_13
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (14, 2500000, 3200000, 30, 1, GETDATE()); -- spct_14
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (15, 2500000, 3200000, 30, 1, GETDATE()); -- spct_15
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (16, 2500000, 3200000, 30, 1, GETDATE()); -- spct_16
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (17, 2500000, 3200000, 30, 1, GETDATE()); -- spct_17
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (18, 2500000, 3200000, 30, 1, GETDATE()); -- spct_18
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (19, 2500000, 3200000, 30, 1, GETDATE()); -- spct_19
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (20, 2500000, 3200000, 30, 1, GETDATE()); -- spct_20
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (21, 2500000, 3200000, 30, 1, GETDATE()); -- spct_21
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (22, 2500000, 3200000, 30, 1, GETDATE()); -- spct_22
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (23, 2500000, 3200000, 30, 1, GETDATE()); -- spct_23
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (24, 2500000, 3200000, 30, 1, GETDATE()); -- spct_24
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (25, 2500000, 3200000, 20, 1, GETDATE()); -- spct_25
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (25, 2500000, 3200000, 20, 1, GETDATE()); -- spct_26
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (26, 2500000, 3200000, 30, 1, GETDATE()); -- spct_27
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (27, 2500000, 3200000, 30, 1, GETDATE()); -- spct_28
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2100000, 20, 1, GETDATE()); -- spct_29
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2100000, 20, 1, GETDATE()); -- spct_30
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2100000, 20, 1, GETDATE()); -- spct_31
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2100000, 20, 1, GETDATE()); -- spct_32
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 2100000, 20, 1, GETDATE()); -- spct_33
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 2100000, 20, 1, GETDATE()); -- spct_34
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 2100000, 20, 1, GETDATE()); -- spct_35
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 2100000, 20, 1, GETDATE()); -- spct_36
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_37
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_38
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_39
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_40
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_41
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_42
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_43
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_44
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_45
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_46
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_47
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_48
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_49
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_50
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_51
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_52
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_53
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_54
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_55
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_56
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_57
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_58
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_59
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_60
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 320000, 25, 1, GETDATE()); -- spct_61
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 320000, 25, 1, GETDATE()); -- spct_62
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 320000, 25, 1, GETDATE()); -- spct_63
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 320000, 25, 1, GETDATE()); -- spct_64
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_65
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_66
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_67
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_68
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_69
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_70
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_71
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_72
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_73
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_74
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_75
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_76
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_77
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_78
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_79
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_80
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_81
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_82
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_83
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_84
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_85
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_86
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_87
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_88
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_89
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_90
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_91
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_92
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_93
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_94
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_95
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_96
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 320000, 25, 1, GETDATE()); -- spct_97
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 320000, 25, 1, GETDATE()); -- spct_98
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 320000, 25, 1, GETDATE()); -- spct_99
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 320000, 25, 1, GETDATE()); -- spct_100
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (44, 500000, 790000, 30, 1, GETDATE()); -- spct_101
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (45, 500000, 790000, 30, 1, GETDATE()); -- spct_102
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (46, 500000, 790000, 30, 1, GETDATE()); -- spct_103
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (47, 200000, 350000, 20, 1, GETDATE()); -- spct_104
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (47, 200000, 350000, 20, 1, GETDATE()); -- spct_105
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (47, 200000, 350000, 20, 1, GETDATE()); -- spct_106
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (48, 500000, 790000, 30, 1, GETDATE()); -- spct_107
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (49, 500000, 790000, 30, 1, GETDATE()); -- spct_108
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (50, 500000, 790000, 30, 1, GETDATE()); -- spct_109
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (51, 500000, 790000, 30, 1, GETDATE()); -- spct_110
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (52, 500000, 790000, 30, 1, GETDATE()); -- spct_111
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (53, 80000, 150000, 30, 1, GETDATE()); -- spct_112
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (54, 80000, 150000, 30, 1, GETDATE()); -- spct_113
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (55, 80000, 150000, 30, 1, GETDATE()); -- spct_114
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (56, 80000, 150000, 30, 1, GETDATE()); -- spct_115
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (57, 80000, 150000, 30, 1, GETDATE()); -- spct_116
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (58, 80000, 150000, 30, 1, GETDATE()); -- spct_117

GO

-- 13. SanPhamChiTietThuocTinh
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (1, 1, N'Màu mặc định'), (1, 2, N'Cứng (Stiff)'), (1, 3, N'4U'), (1, 4, N'Đầu nặng'), (1, 5, N'Tấn công'), (1, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (2, 1, N'Màu mặc định'), (2, 2, N'Cứng (Stiff)'), (2, 3, N'4U'), (2, 4, N'Đầu nặng'), (2, 5, N'Tấn công'), (2, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (3, 1, N'Loh Kean Yew 2025'), (3, 2, N'Cứng (Stiff)'), (3, 3, N'4U'), (3, 4, N'Đầu nặng'), (3, 5, N'Tấn công'), (3, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (4, 1, N'Màu mặc định'), (4, 2, N'Cứng (Stiff)'), (4, 3, N'4U'), (4, 4, N'Đầu nặng'), (4, 5, N'Tấn công'), (4, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (5, 1, N'Màu mặc định'), (5, 2, N'Cứng (Stiff)'), (5, 3, N'4U'), (5, 4, N'Đầu nặng'), (5, 5, N'Tấn công'), (5, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (6, 1, N'Màu mặc định'), (6, 2, N'Cứng (Stiff)'), (6, 3, N'4U'), (6, 4, N'Đầu nặng'), (6, 5, N'Tấn công'), (6, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (7, 1, N'Màu mặc định'), (7, 2, N'Cứng (Stiff)'), (7, 3, N'4U'), (7, 4, N'Đầu nặng'), (7, 5, N'Tấn công'), (7, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (8, 1, N'Màu mặc định'), (8, 2, N'Cứng (Stiff)'), (8, 3, N'4U'), (8, 4, N'Đầu nặng'), (8, 5, N'Tấn công'), (8, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (9, 1, N'Màu mặc định'), (9, 2, N'Cứng (Stiff)'), (9, 3, N'4U'), (9, 4, N'Đầu nặng'), (9, 5, N'Tấn công'), (9, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (10, 1, N'Màu mặc định'), (10, 2, N'Cứng (Stiff)'), (10, 3, N'4U'), (10, 4, N'Đầu nặng'), (10, 5, N'Tấn công'), (10, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (11, 1, N'Đen xanh cam chính hãng'), (11, 2, N'Cứng (Stiff)'), (11, 3, N'4U'), (11, 4, N'Đầu nặng'), (11, 5, N'Tấn công'), (11, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (12, 1, N'Màu mặc định'), (12, 2, N'Cứng (Stiff)'), (12, 3, N'4U'), (12, 4, N'Đầu nặng'), (12, 5, N'Tấn công'), (12, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (13, 1, N'Màu mặc định'), (13, 2, N'Cứng (Stiff)'), (13, 3, N'4U'), (13, 4, N'Đầu nặng'), (13, 5, N'Tấn công'), (13, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (14, 1, N'Màu mặc định'), (14, 2, N'Cứng (Stiff)'), (14, 3, N'4U'), (14, 4, N'Đầu nặng'), (14, 5, N'Tấn công'), (14, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (15, 1, N'Màu mặc định'), (15, 2, N'Cứng (Stiff)'), (15, 3, N'4U'), (15, 4, N'Đầu nặng'), (15, 5, N'Tấn công'), (15, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (16, 1, N'Màu mặc định'), (16, 2, N'Cứng (Stiff)'), (16, 3, N'4U'), (16, 4, N'Đầu nặng'), (16, 5, N'Tấn công'), (16, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (17, 1, N'Màu mặc định'), (17, 2, N'Cứng (Stiff)'), (17, 3, N'4U'), (17, 4, N'Đầu nặng'), (17, 5, N'Tấn công'), (17, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (18, 1, N'Màu mặc định'), (18, 2, N'Cứng (Stiff)'), (18, 3, N'4U'), (18, 4, N'Đầu nặng'), (18, 5, N'Tấn công'), (18, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (19, 1, N'Màu mặc định'), (19, 2, N'Cứng (Stiff)'), (19, 3, N'4U'), (19, 4, N'Đầu nặng'), (19, 5, N'Tấn công'), (19, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (20, 1, N'Màu mặc định'), (20, 2, N'Cứng (Stiff)'), (20, 3, N'4U'), (20, 4, N'Đầu nặng'), (20, 5, N'Tấn công'), (20, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (21, 1, N'Màu mặc định'), (21, 2, N'Cứng (Stiff)'), (21, 3, N'4U'), (21, 4, N'Đầu nặng'), (21, 5, N'Tấn công'), (21, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (22, 1, N'Light Beige chính hãng'), (22, 2, N'Cứng (Stiff)'), (22, 3, N'4U'), (22, 4, N'Đầu nặng'), (22, 5, N'Tấn công'), (22, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (23, 1, N'Màu mặc định'), (23, 2, N'Cứng (Stiff)'), (23, 3, N'4U'), (23, 4, N'Đầu nặng'), (23, 5, N'Tấn công'), (23, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (24, 1, N'Màu mặc định'), (24, 2, N'Cứng (Stiff)'), (24, 3, N'4U'), (24, 4, N'Đầu nặng'), (24, 5, N'Tấn công'), (24, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (25, 1, N'Aqua blue'), (25, 2, N'Cứng vừa (Stiff)'), (25, 3, N'4U'), (25, 4, N'Trung bình'), (25, 5, N'Toàn diện'), (25, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (26, 1, N'Green'), (26, 2, N'Cứng vừa (Stiff)'), (26, 3, N'4U'), (26, 4, N'Trung bình'), (26, 5, N'Toàn diện'), (26, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (27, 1, N'Màu mặc định'), (27, 2, N'Cứng (Stiff)'), (27, 3, N'4U'), (27, 4, N'Đầu nặng'), (27, 5, N'Tấn công'), (27, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (28, 1, N'Màu mặc định'), (28, 2, N'Cứng (Stiff)'), (28, 3, N'4U'), (28, 4, N'Đầu nặng'), (28, 5, N'Tấn công'), (28, 7, N'20 - 28 lbs');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (29, 1, N'3 chính hãng'), (29, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (30, 1, N'3 chính hãng'), (30, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (31, 1, N'3 chính hãng'), (31, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (32, 1, N'3 chính hãng'), (32, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (33, 1, N'Màu mặc định'), (33, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (34, 1, N'Màu mặc định'), (34, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (35, 1, N'Màu mặc định'), (35, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (36, 1, N'Màu mặc định'), (36, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (37, 1, N'Trắng chính hãng'), (37, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (38, 1, N'Trắng chính hãng'), (38, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (39, 1, N'Trắng chính hãng'), (39, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (40, 1, N'Trắng chính hãng'), (40, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (41, 1, N'Trắng'), (41, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (42, 1, N'Trắng'), (42, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (43, 1, N'Trắng'), (43, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (44, 1, N'Trắng'), (44, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (45, 1, N'Trắng đen'), (45, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (46, 1, N'Trắng đen'), (46, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (47, 1, N'Trắng đen'), (47, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (48, 1, N'Trắng đen'), (48, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (49, 1, N'Xanh NaVy'), (49, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (50, 1, N'Xanh NaVy'), (50, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (51, 1, N'Xanh NaVy'), (51, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (52, 1, N'Xanh NaVy'), (52, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (53, 1, N'Grayish Beige chính hãng'), (53, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (54, 1, N'Grayish Beige chính hãng'), (54, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (55, 1, N'Grayish Beige chính hãng'), (55, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (56, 1, N'Grayish Beige chính hãng'), (56, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (57, 1, N'Crystal teal chính hãng'), (57, 6, N'39');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (58, 1, N'Crystal teal chính hãng'), (58, 6, N'40');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (59, 1, N'Crystal teal chính hãng'), (59, 6, N'41');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (60, 1, N'Crystal teal chính hãng'), (60, 6, N'42');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (61, 1, N'1 nam chính hãng'), (61, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (62, 1, N'1 nam chính hãng'), (62, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (63, 1, N'1 nam chính hãng'), (63, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (64, 1, N'1 nam chính hãng'), (64, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (65, 1, N'Poinciana chính hãng'), (65, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (66, 1, N'Poinciana chính hãng'), (66, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (67, 1, N'Poinciana chính hãng'), (67, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (68, 1, N'Poinciana chính hãng'), (68, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (69, 1, N'White chính hãng'), (69, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (70, 1, N'White chính hãng'), (70, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (71, 1, N'White chính hãng'), (71, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (72, 1, N'White chính hãng'), (72, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (73, 1, N'Đỏ'), (73, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (74, 1, N'Đỏ'), (74, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (75, 1, N'Đỏ'), (75, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (76, 1, N'Đỏ'), (76, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (77, 1, N'Đen trắng'), (77, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (78, 1, N'Đen trắng'), (78, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (79, 1, N'Đen trắng'), (79, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (80, 1, N'Đen trắng'), (80, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (81, 1, N'Đen xanh ngọc'), (81, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (82, 1, N'Đen xanh ngọc'), (82, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (83, 1, N'Đen xanh ngọc'), (83, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (84, 1, N'Đen xanh ngọc'), (84, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (85, 1, N'mã 081'), (85, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (86, 1, N'mã 081'), (86, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (87, 1, N'mã 081'), (87, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (88, 1, N'mã 081'), (88, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (89, 1, N'Màu mặc định'), (89, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (90, 1, N'Màu mặc định'), (90, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (91, 1, N'Màu mặc định'), (91, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (92, 1, N'Màu mặc định'), (92, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (93, 1, N'Đen trắng'), (93, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (94, 1, N'Đen trắng'), (94, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (95, 1, N'Đen trắng'), (95, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (96, 1, N'Đen trắng'), (96, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (97, 1, N'Lion chính hãng'), (97, 6, N'S');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (98, 1, N'Lion chính hãng'), (98, 6, N'M');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (99, 1, N'Lion chính hãng'), (99, 6, N'L');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (100, 1, N'Lion chính hãng'), (100, 6, N'XL');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (101, 1, N'3 chính hãng');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (102, 1, N'Trắng đỏ chính hãng');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (103, 1, N'Trắng đỏ chính hãng');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (104, 1, N'Bright White');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (105, 1, N'Jet Black');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (106, 1, N'Riviera');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (107, 1, N'2 chính hãng');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (108, 1, N'3 chính hãng');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (109, 1, N'Trắng đỏ chính hãng');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (110, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (111, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (112, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (113, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (114, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (115, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (116, 1, N'Màu mặc định');
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (117, 1, N'30 EX (Túi 2 cuộn)');

GO

-- 14. HinhAnhSanPham
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (1, N'Vợt cầu lông/Li-Ning/Lining Bladex 800 Speed 2026/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (1, N'Vợt cầu lông/Li-Ning/Lining Bladex 800 Speed 2026/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (1, N'Vợt cầu lông/Li-Ning/Lining Bladex 800 Speed 2026/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (2, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 100 Gen 2/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (2, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 100 Gen 2/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (2, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 100 Gen 2/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (3, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025/anh1.png', N'Loh Kean Yew 2025', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (3, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025/anh2.png', N'Loh Kean Yew 2025', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (3, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025/anh3.png', N'Loh Kean Yew 2025', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (4, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce BigBang new/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (4, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce BigBang new/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (4, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce BigBang new/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (5, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Bladex Assassin/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (5, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Bladex Assassin/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (5, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Bladex Assassin/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (6, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Halbertec 1000 chính hãng/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (6, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Halbertec 1000 chính hãng/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (6, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Halbertec 1000 chính hãng/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (7, N'Vợt cầu lông/Li-Ning/Vợt Cầu Lông Lining Halbertec Motor/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (7, N'Vợt cầu lông/Li-Ning/Vợt Cầu Lông Lining Halbertec Motor/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (7, N'Vợt cầu lông/Li-Ning/Vợt Cầu Lông Lining Halbertec Motor/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (8, N'Vợt cầu lông/Mizuno/Vợt Cầu Lông Mizuno Altair T327/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (8, N'Vợt cầu lông/Mizuno/Vợt Cầu Lông Mizuno Altair T327/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (9, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno BDSS Altius Sonic/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (9, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno BDSS Altius Sonic/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (9, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno BDSS Altius Sonic/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (10, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno Fortius 55 Strive/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (10, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno Fortius 55 Strive/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (10, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno Fortius 55 Strive/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh.png', N'Đen xanh cam chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh1.png', N'Đen xanh cam chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh2.png', N'Đen xanh cam chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh3.png', N'Đen xanh cam chính hãng', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (12, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed 99 J 2026/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (12, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed 99 J 2026/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (12, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed 99 J 2026/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (13, N'Vợt cầu lông/Victor/Vợt cầu lông Victor AuraSpeed A/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (13, N'Vợt cầu lông/Victor/Vợt cầu lông Victor AuraSpeed A/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (13, N'Vợt cầu lông/Victor/Vợt cầu lông Victor AuraSpeed A/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (14, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed FANTOME F HYQ/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (14, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed FANTOME F HYQ/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (14, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed FANTOME F HYQ/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (15, N'Vợt cầu lông/Victor/Vợt Cầu Lông Victor AuraSpeed LYC/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (15, N'Vợt cầu lông/Victor/Vợt Cầu Lông Victor AuraSpeed LYC/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (15, N'Vợt cầu lông/Victor/Vợt Cầu Lông Victor AuraSpeed LYC/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (16, N'Vợt cầu lông/Victor/Vợt cầu lông Victor DriveX 12 WT25/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (16, N'Vợt cầu lông/Victor/Vợt cầu lông Victor DriveX 12 WT25/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (16, N'Vợt cầu lông/Victor/Vợt cầu lông Victor DriveX 12 WT25/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (17, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Jetspeed S12 II R/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (17, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Jetspeed S12 II R/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (17, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Jetspeed S12 II R/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (18, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Thruster Hammer Light/anh1.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (18, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Thruster Hammer Light/anh2.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (18, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Thruster Hammer Light/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (19, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 10/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (19, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 10/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh3.png', N'Màu mặc định', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (21, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (21, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (21, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng/anh3.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh.png', N'Light Beige chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh1.png', N'Light Beige chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh2.png', N'Light Beige chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh3.png', N'Light Beige chính hãng', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh3.png', N'Màu mặc định', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh3.png', N'Màu mặc định', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh.png', N'Aqua blue', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh1.png', N'Aqua blue', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh2.png', N'Aqua blue', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh3.png', N'Aqua blue', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (26, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Green/anh.png', N'Green', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (26, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Green/anh1.png', N'Green', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (26, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Green/anh2.png', N'Green', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (27, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 1000Z/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (27, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 1000Z/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (27, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 1000Z/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh3.png', N'Màu mặc định', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh.png', N'3 chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh1.png', N'3 chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh2.png', N'3 chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh3.png', N'3 chính hãng', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh4.png', N'3 chính hãng', 0, 5);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (33, N'Giày/Giày cầu lông Victor A531 WAG chính hãng/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (33, N'Giày/Giày cầu lông Victor A531 WAG chính hãng/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (33, N'Giày/Giày cầu lông Victor A531 WAG chính hãng/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (37, N'Giày/Giày cầu lông Victor A970 cADVAM - Trắng chính hãng/anh.png', N'Trắng chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (37, N'Giày/Giày cầu lông Victor A970 cADVAM - Trắng chính hãng/anh1.png', N'Trắng chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (37, N'Giày/Giày cầu lông Victor A970 cADVAM - Trắng chính hãng/anh2.png', N'Trắng chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (41, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng/anh.png', N'Trắng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (41, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng/anh1.png', N'Trắng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (41, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng/anh2.png', N'Trắng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (45, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng đen/anh.png', N'Trắng đen', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (45, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng đen/anh1.png', N'Trắng đen', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (45, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng đen/anh2.png', N'Trắng đen', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (49, N'Giày/Giày cầu lông Yonex Eclipsion X3/Xanh NaVy/anh.png', N'Xanh NaVy', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (49, N'Giày/Giày cầu lông Yonex Eclipsion X3/Xanh NaVy/anh1.png', N'Xanh NaVy', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh.png', N'Grayish Beige chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh1.png', N'Grayish Beige chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh2.png', N'Grayish Beige chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh3.png', N'Grayish Beige chính hãng', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh4.png', N'Grayish Beige chính hãng', 0, 5);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (57, N'Giày/Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng/anh.png', N'Crystal teal chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (57, N'Giày/Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng/anh1.png', N'Crystal teal chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (61, N'Áo/Áo cầu lông Lining P-APLUA47-1 nam chính hãng/anh.png', N'1 nam chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (61, N'Áo/Áo cầu lông Lining P-APLUA47-1 nam chính hãng/anh1.png', N'1 nam chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (61, N'Áo/Áo cầu lông Lining P-APLUA47-1 nam chính hãng/anh2.png', N'1 nam chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (65, N'Áo/Áo cầu lông Yonex RM3216 - Poinciana chính hãng/anh.png', N'Poinciana chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (69, N'Áo/Áo cầu lông Yonex RM3232 - White chính hãng/anh.png', N'White chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (73, N'Áo/Áo hoodie lót bông Victor Vic07 - Đỏ/anh.png', N'Đỏ', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (77, N'Quần/Quần cầu lông Lining 92001 - Đen trắng/anh.png', N'Đen trắng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (81, N'Quần/Quần cầu lông Lining 9682 - Đen xanh ngọc/anh.png', N'Đen xanh ngọc', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (85, N'Quần/Quần cầu lông lining nữ đen - mã 081/anh.png', N'mã 081', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (89, N'Quần/Quần Cầu Lông Lining training trắng/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (93, N'Quần/Quần cầu lông Yonex Q3 nữ - Đen trắng/anh.png', N'Đen trắng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (97, N'Quần/Quần cầu lông Yonex TSM3117 - Lion chính hãng/anh.png', N'Lion chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (97, N'Quần/Quần cầu lông Yonex TSM3117 - Lion chính hãng/anh1.png', N'Lion chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (97, N'Quần/Quần cầu lông Yonex TSM3117 - Lion chính hãng/anh3.png', N'Lion chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (101, N'Balo/Balo cầu lông Lining P-ABSV133-3 chính hãng/anh.png', N'3 chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (102, N'Balo/Balo cầu lông Victor BR5042 EXA - Trắng đỏ chính hãng/anh.png', N'Trắng đỏ chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (103, N'Balo/Balo cầu lông Victor BR5051 CNY - Trắng đỏ chính hãng/anh.png', N'Trắng đỏ chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (104, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Bright White/anh.png', N'Bright White', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (104, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Bright White/anh1.png', N'Bright White', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (105, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Jet Black/anh.png', N'Jet Black', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (105, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Jet Black/anh1.png', N'Jet Black', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (106, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Riviera/anh.png', N'Riviera', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (106, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Riviera/anh1.png', N'Riviera', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (107, N'Túi Xách/Túi cầu lông Lining ABJU013-2 chính hãng/anh.png', N'2 chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (107, N'Túi Xách/Túi cầu lông Lining ABJU013-2 chính hãng/anh1.png', N'2 chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (108, N'Túi Xách/Túi cầu lông Lining P-ABLV029-3 chính hãng/anh.png', N'3 chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (108, N'Túi Xách/Túi cầu lông Lining P-ABLV029-3 chính hãng/anh1.png', N'3 chính hãng', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (108, N'Túi Xách/Túi cầu lông Lining P-ABLV029-3 chính hãng/anh2.png', N'3 chính hãng', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (109, N'Túi Xách/Túi cầu lông Victor BR5651CNY - Trắng đỏ chính hãng/anh.png', N'Trắng đỏ chính hãng', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (110, N'Túi Xách/Túi cầu lông Yonex BA92026EX xám chính hãng/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (111, N'Túi Xách/Túi Xách Cầu Lông Yonex 3D 2241R (BKDBL)/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt GOSEN Ryzonic 62/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt GOSEN Ryzonic 62/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt GOSEN Ryzonic 62/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt GOSEN Ryzonic 62/anh3.png', N'Màu mặc định', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Kizuna Z65X/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Kizuna Z65X/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Kizuna Z65X/anh2.png', N'Màu mặc định', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Kizuna Z65X/anh3.png', N'Màu mặc định', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Kizuna Z65X/anh4.png', N'Màu mặc định', 0, 5);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Kizuna Z65X/anh5.png', N'Màu mặc định', 0, 6);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (114, N'Cước/Dây cước căng vợt Yonex BG 66 Ultimax/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (114, N'Cước/Dây cước căng vợt Yonex BG 66 Ultimax/anh1.png', N'Màu mặc định', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (115, N'Cước/Dây cước căng vợt Yonex BG SKY/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Cước/Dây cước căng vợt Yonex Nanogy BG 95/anh.png', N'Màu mặc định', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh.png', N'30 EX (Túi 2 cuộn)', 1, 1);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh1.png', N'30 EX (Túi 2 cuộn)', 0, 2);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh2.png', N'30 EX (Túi 2 cuộn)', 0, 3);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh3.png', N'30 EX (Túi 2 cuộn)', 0, 4);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh4.png', N'30 EX (Túi 2 cuộn)', 0, 5);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh5.png', N'30 EX (Túi 2 cuộn)', 0, 6);
INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (117, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh6.png', N'30 EX (Túi 2 cuộn)', 0, 7);

GO


-- 15. GioHang
INSERT INTO [dbo].[GioHang] ([id_khach_hang],[ngay_tao]) VALUES (1, GETDATE()), (2, GETDATE()), (3, GETDATE());
GO

PRINT N'=== THÀNH CÔNG: DỮ LIỆU CƠ SỞ DỮ LIỆU ĐÃ ĐƯỢC CHÈN HOÀN CHỈNH ===';
GO
