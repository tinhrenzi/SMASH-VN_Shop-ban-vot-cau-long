-- ============================================================
-- Script: Xóa tài khoản buyer giả và dọn dữ liệu liên quan
-- ============================================================
-- TaiKhoan IDs: 9975, 9997, 10019, 10041, 10063, 10085, 10107,
--               10129, 10151, 10209, 10382, 10468, 10489, 10532, 10761, 10990
-- KhachHang IDs: 2956, 2967, 2978, 2989, 3000, 3011, 3022, 3033,
--                3044, 3087, 3138, 3146, 3147, 3183, 3266, 3349
-- ============================================================

SET NOCOUNT ON;
BEGIN TRANSACTION;

-- 1. Danh sách IDs
DECLARE @TaiKhoanIDs TABLE (id INT);
INSERT INTO @TaiKhoanIDs VALUES
(9975),(9997),(10019),(10041),(10063),(10085),(10107),
(10129),(10151),(10209),(10382),(10468),(10489),(10532),(10761),(10990);

DECLARE @KhachHangIDs TABLE (id INT);
INSERT INTO @KhachHangIDs VALUES
(2956),(2967),(2978),(2989),(3000),(3011),(3022),(3033),
(3044),(3087),(3138),(3146),(3147),(3183),(3266),(3349);

-- HoaDon IDs của các buyer giả
DECLARE @HoaDonIDs TABLE (id INT);
INSERT INTO @HoaDonIDs
SELECT id FROM HoaDon WHERE id_khach_hang IN (SELECT id FROM @KhachHangIDs);

-- 2. Xóa dữ liệu liên quan đến đơn hàng giả
PRINT 'Xóa HoaDonChiTiet...';
DELETE FROM HoaDonChiTiet WHERE id_hoa_don IN (SELECT id FROM @HoaDonIDs);

PRINT 'Xóa LichSuTrangThaiDonHang...';
DELETE FROM LichSuTrangThaiDonHang WHERE id_hoa_don IN (SELECT id FROM @HoaDonIDs);

PRINT 'Xóa PaymentTransaction...';
DELETE FROM PaymentTransaction WHERE order_id IN (SELECT id FROM @HoaDonIDs);

PRINT 'Xóa HoaDon...';
DELETE FROM HoaDon WHERE id IN (SELECT id FROM @HoaDonIDs);

-- 3. Xóa các bảng con tham chiếu đến KhachHang
PRINT 'Xóa GioHangChiTiet...';
DELETE ghct FROM GioHangChiTiet ghct
INNER JOIN GioHang gh ON ghct.id_gio_hang = gh.id
WHERE gh.id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa GioHang...';
DELETE FROM GioHang WHERE id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa SanPhamYeuThich...';
DELETE FROM SanPhamYeuThich WHERE id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa SoDiaChi...';
DELETE FROM SoDiaChi WHERE id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa DanhGiaAnh (từ DanhGia của buyer)...';
DELETE dga FROM DanhGiaAnh dga
INNER JOIN DanhGia dg ON dga.id_danh_gia = dg.id
WHERE dg.id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa DanhGia...';
DELETE FROM DanhGia WHERE id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa ChatFeedback (từ ChatMessage của buyer)...';
DELETE cf FROM ChatFeedback cf
INNER JOIN ChatMessage cm ON cf.id_message = cm.id
INNER JOIN ChatConversation cc ON cm.id_conversation = cc.id
WHERE cc.id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa ChatMessage (từ ChatConversation của buyer)...';
DELETE cm FROM ChatMessage cm
INNER JOIN ChatConversation cc ON cm.id_conversation = cc.id
WHERE cc.id_khach_hang IN (SELECT id FROM @KhachHangIDs);

PRINT 'Xóa ChatConversation...';
DELETE FROM ChatConversation WHERE id_khach_hang IN (SELECT id FROM @KhachHangIDs);

-- 4. Xóa các bảng con tham chiếu đến TaiKhoan
PRINT 'Xóa ThongBao...';
DELETE FROM ThongBao WHERE id_tai_khoan IN (SELECT id FROM @TaiKhoanIDs);

PRINT 'Xóa TokenKhoiPhuc...';
DELETE FROM TokenKhoiPhuc WHERE id_tai_khoan IN (SELECT id FROM @TaiKhoanIDs);

PRINT 'Xóa BlogComment...';
DELETE FROM BlogComment WHERE id_tai_khoan IN (SELECT id FROM @TaiKhoanIDs);

PRINT 'Xóa EditLog...';
DELETE FROM EditLog WHERE id_tai_khoan IN (SELECT id FROM @TaiKhoanIDs);

PRINT 'Xóa CommentViolationLog...';
DELETE FROM CommentViolationLog WHERE tai_khoan_id IN (SELECT id FROM @TaiKhoanIDs);

-- 5. Xóa KhachHang
PRINT 'Xóa KhachHang...';
DELETE FROM KhachHang WHERE id IN (SELECT id FROM @KhachHangIDs);

-- 6. Xóa TaiKhoan
PRINT 'Xóa TaiKhoan...';
DELETE FROM TaiKhoan WHERE id IN (SELECT id FROM @TaiKhoanIDs);

PRINT 'Hoàn thành xóa. Đang commit...';
COMMIT TRANSACTION;

-- ============================================================
-- RESET IDENTITY về giá trị max hiện tại
-- ============================================================
PRINT 'Reseed IDENTITY cho TaiKhoan...';
DECLARE @maxTaiKhoan INT = (SELECT ISNULL(MAX(id), 0) FROM TaiKhoan);
DBCC CHECKIDENT ('TaiKhoan', RESEED, @maxTaiKhoan);

PRINT 'Reseed IDENTITY cho KhachHang...';
DECLARE @maxKhachHang INT = (SELECT ISNULL(MAX(id), 0) FROM KhachHang);
DBCC CHECKIDENT ('KhachHang', RESEED, @maxKhachHang);

PRINT 'Reseed IDENTITY cho HoaDon...';
DECLARE @maxHoaDon INT = (SELECT ISNULL(MAX(id), 0) FROM HoaDon);
DBCC CHECKIDENT ('HoaDon', RESEED, @maxHoaDon);

PRINT 'Reseed IDENTITY cho GioHang...';
DECLARE @maxGioHang INT = (SELECT ISNULL(MAX(id), 0) FROM GioHang);
DBCC CHECKIDENT ('GioHang', RESEED, @maxGioHang);

PRINT 'Reseed IDENTITY cho SoDiaChi...';
DECLARE @maxSoDiaChi INT = (SELECT ISNULL(MAX(id), 0) FROM SoDiaChi);
DBCC CHECKIDENT ('SoDiaChi', RESEED, @maxSoDiaChi);

PRINT '=== XONG! ==='
PRINT 'Các fake buyer và đơn hàng giả đã được xóa. IDENTITY đã được reset.';
GO
