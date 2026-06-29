-- =================================================================================
-- SQL SERVER SCHEMA CLEANUP SCRIPT WITH TRANSACTION & DRY-RUN SAFETY
-- Database target: SMDB_FINAL
-- =================================================================================

USE SMDB_FINAL;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

-- =================================================================================
-- BIẾN ĐIỀU KHIỂN CHÍNH (MASTER CONTROLLER FLAG)
-- =================================================================================
-- 0 = DRY-RUN MODE: Chỉ SELECT hiển thị thống kê dữ liệu, thông báo hành động (Mặc định).
-- 1 = EXECUTE MODE: Thực thi DROP trong TRANSACTION (Có commit/rollback).
-- =================================================================================
DECLARE @DoExecute BIT = 0;

PRINT '=================================================================================';
PRINT 'KỊCH BẢN DỌN DẸP SCHEMA CƠ SỞ DỮ LIỆU - PHẦN CỘT THỪA LEGACY (SMDB_FINAL)';
PRINT 'Trạng thái chạy thử nghiệm (DoExecute) = ' + CAST(@DoExecute AS VARCHAR(1));
PRINT '=================================================================================';

-- =================================================================================
-- 1. THỐNG KÊ CHI TIẾT CÁC CỘT TRƯỚC KHI CLEANUP
-- =================================================================================
PRINT '--- [1] THỐNG KÊ CÁC CỘT DỰ KIẾN DROP (DROP_CANDIDATE_AFTER_BACKUP) ---';

-- A. DotGiamGia.ten_dot
SELECT 'DotGiamGia.ten_dot' AS [ColumnName],
       COUNT(*) AS [TotalRows],
       COUNT(ten_dot) AS [NonNullCount],
       COUNT(*) - COUNT(ten_dot) AS [NullCount],
       COUNT(DISTINCT ten_dot) AS [DistinctCount]
FROM DotGiamGia;

-- B. GiaoDichThanhToan.gateway & status
SELECT 'GiaoDichThanhToan' AS [TableName],
       COUNT(*) AS [TotalRows],
       COUNT(gateway) AS [Gateway_NonNull],
       COUNT(status) AS [Status_NonNull],
       COUNT(*) - COUNT(gateway) AS [Gateway_Null],
       COUNT(*) - COUNT(status) AS [Status_Null]
FROM GiaoDichThanhToan;

-- C. KhachHang.sdt
SELECT 'KhachHang.sdt' AS [ColumnName],
       COUNT(*) AS [TotalRows],
       COUNT(sdt) AS [NonNullCount],
       COUNT(*) - COUNT(sdt) AS [NullCount]
FROM KhachHang;

-- D. NhanVien.ho_ten_nv
SELECT 'NhanVien.ho_ten_nv' AS [ColumnName],
       COUNT(*) AS [TotalRows],
       COUNT(ho_ten_nv) AS [NonNullCount],
       COUNT(*) - COUNT(ho_ten_nv) AS [NullCount]
FROM NhanVien;

-- E. SoDiaChi.latitude & longitude
SELECT 'SoDiaChi' AS [TableName],
       COUNT(*) AS [TotalRows],
       COUNT(latitude) AS [Lat_NonNull],
       COUNT(longitude) AS [Lon_NonNull]
FROM SoDiaChi;


-- =================================================================================
-- 2. TRUY VẤN XÁC MINH CÁC NHÓM GIỮ LẠI / KIỂM TRA THÊM (CHỈ XEM - KHÔNG DROP)
-- =================================================================================
PRINT '';
PRINT '--- [2] TRUY VẤN XÁC MINH CÁC NHÓM CẦN KIỂM TRA THÊM ---';

-- Group A: LEGACY_ZERO_ONLY (HoaDon.so_tien_giam_gia)
PRINT 'A. Xác minh HoaDon.so_tien_giam_gia (LEGACY_ZERO_ONLY):';
SELECT 'HoaDon.so_tien_giam_gia' AS [Column],
       COUNT(*) AS [Total],
       COUNT(CASE WHEN so_tien_giam_gia = 0 THEN 1 END) AS [ZeroCount],
       COUNT(CASE WHEN so_tien_giam_gia <> 0 AND so_tien_giam_gia IS NOT NULL THEN 1 END) AS [NonZeroCount]
FROM HoaDon;

-- Group B: MIGRATION_REQUIRED_BEFORE_DROP (DotGiamGia.trang_thai vs kich_hoat)
PRINT 'B. Xác minh sự khác biệt DotGiamGia.trang_thai vs kich_hoat:';
SELECT id, ten_chien_dich, trang_thai AS [Old_TrangThai], kich_hoat AS [New_KichHoat]
FROM DotGiamGia 
WHERE trang_thai IS NOT NULL;

-- Group C: WAIT_FOR_TOKEN_EXPIRY (MaKhoiPhuc.token)
PRINT 'C. Kiểm tra số lượng token cũ (UUID) so với mã xác nhận OTP mới:';
SELECT COUNT(token) AS [Legacy_UUID_Tokens],
       COUNT(ma_xac_nhan) AS [Modern_OTP_Tokens],
       COUNT(CASE WHEN thoi_gian_het_han > GETDATE() AND token IS NOT NULL THEN 1 END) AS [Active_Legacy_Tokens]
FROM MaKhoiPhuc;

-- Group D: ARCHIVE_AND_REVIEW (Bảng TichHopVanChuyen)
PRINT 'D. Kiểm tra dữ liệu bảng trung gian TichHopVanChuyen:';
SELECT t.id, t.id_hoa_don, h.ma_don_hang, t.ma_van_don, t.trang_thai_ghn 
FROM TichHopVanChuyen t
LEFT JOIN HoaDon h ON t.id_hoa_don = h.id;

-- Group E: TAIKHOAN_LEGACY_FLAGS (Kiểm tra 3 cột cờ phân quyền cũ trên TaiKhoan)
PRINT 'E. Kiểm tra 3 cột cờ phân quyền cũ la_khach_hang, la_nhan_vien, la_quan_ly trên TaiKhoan:';
SELECT COUNT(*) AS [TotalAccounts],
       COUNT(la_khach_hang) AS [la_khach_hang_count],
       COUNT(la_nhan_vien) AS [la_nhan_vien_count],
       COUNT(la_quan_ly) AS [la_quan_ly_count]
FROM TaiKhoan;


-- =================================================================================
-- 3. KỊCH BẢN THỰC THI DỌN DẸP CỘT THỪA (CHỈ CHẠY KHI @DoExecute = 1)
-- =================================================================================
IF @DoExecute = 0
BEGIN
    PRINT '=================================================================================';
    PRINT 'DRY-RUN HOÀN TẤT. KHÔNG CÓ THAY ĐỔI NÀO ĐƯỢC THỰC THI TRÊN DATABASE SMDB_FINAL.';
    PRINT 'Để chạy thật, hãy đổi giá trị DECLARE @DoExecute BIT = 1;';
    PRINT '=================================================================================';
END
ELSE
BEGIN
    PRINT '=================================================================================';
    PRINT 'ĐANG BẮT ĐẦU CHẠY THẬT (EXECUTE MODE) TRÊN SMDB_FINAL TRONG TRANSACTION...';
    PRINT '=================================================================================';

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @SQL NVARCHAR(MAX);
        DECLARE @ConstraintName NVARCHAR(128);

        -- =============================================================================
        -- A. Dọn dẹp table DotGiamGia -> Cột ten_dot
        -- =============================================================================
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DotGiamGia') AND name = 'ten_dot')
        BEGIN
            PRINT '  - Phát hiện cột DotGiamGia.ten_dot. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('DotGiamGia') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('DotGiamGia'), 'ten_dot', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE DotGiamGia DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE DotGiamGia DROP COLUMN ten_dot;
            PRINT '    -> Đã drop thành công cột DotGiamGia.ten_dot';
        END

        -- =============================================================================
        -- B. Dọn dẹp table GiaoDichThanhToan -> Cột gateway & status
        -- =============================================================================
        -- Cột gateway
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('GiaoDichThanhToan') AND name = 'gateway')
        BEGIN
            PRINT '  - Phát hiện cột GiaoDichThanhToan.gateway. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('GiaoDichThanhToan') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('GiaoDichThanhToan'), 'gateway', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE GiaoDichThanhToan DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE GiaoDichThanhToan DROP COLUMN gateway;
            PRINT '    -> Đã drop thành công cột GiaoDichThanhToan.gateway';
        END

        -- Cột status
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('GiaoDichThanhToan') AND name = 'status')
        BEGIN
            PRINT '  - Phát hiện cột GiaoDichThanhToan.status. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('GiaoDichThanhToan') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('GiaoDichThanhToan'), 'status', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE GiaoDichThanhToan DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE GiaoDichThanhToan DROP COLUMN status;
            PRINT '    -> Đã drop thành công cột GiaoDichThanhToan.status';
        END

        -- =============================================================================
        -- C. Dọn dẹp table KhachHang -> Cột sdt
        -- =============================================================================
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('KhachHang') AND name = 'sdt')
        BEGIN
            PRINT '  - Phát hiện cột KhachHang.sdt. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('KhachHang') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('KhachHang'), 'sdt', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE KhachHang DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE KhachHang DROP COLUMN sdt;
            PRINT '    -> Đã drop thành công cột KhachHang.sdt';
        END

        -- =============================================================================
        -- D. Dọn dẹp table NhanVien -> Cột ho_ten_nv
        -- =============================================================================
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('NhanVien') AND name = 'ho_ten_nv')
        BEGIN
            PRINT '  - Phát hiện cột NhanVien.ho_ten_nv. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('NhanVien') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('NhanVien'), 'ho_ten_nv', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE NhanVien DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE NhanVien DROP COLUMN ho_ten_nv;
            PRINT '    -> Đã drop thành công cột NhanVien.ho_ten_nv';
        END

        -- =============================================================================
        -- E. Dọn dẹp table SoDiaChi -> Cột latitude & longitude
        -- =============================================================================
        -- Cột latitude
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SoDiaChi') AND name = 'latitude')
        BEGIN
            PRINT '  - Phát hiện cột SoDiaChi.latitude. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('SoDiaChi') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('SoDiaChi'), 'latitude', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE SoDiaChi DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE SoDiaChi DROP COLUMN latitude;
            PRINT '    -> Đã drop thành công cột SoDiaChi.latitude';
        END

        -- Cột longitude
        IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('SoDiaChi') AND name = 'longitude')
        BEGIN
            PRINT '  - Phát hiện cột SoDiaChi.longitude. Bắt đầu tìm kiếm default constraint...';
            SET @ConstraintName = NULL;
            SELECT @ConstraintName = name 
            FROM sys.default_constraints 
            WHERE parent_object_id = OBJECT_ID('SoDiaChi') 
              AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('SoDiaChi'), 'longitude', 'ColumnId');

            IF @ConstraintName IS NOT NULL
            BEGIN
                SET @SQL = 'ALTER TABLE SoDiaChi DROP CONSTRAINT [' + @ConstraintName + ']';
                EXEC sp_executesql @SQL;
                PRINT '    -> Đã drop default constraint: ' + @ConstraintName;
            END

            ALTER TABLE SoDiaChi DROP COLUMN longitude;
            PRINT '    -> Đã drop thành công cột SoDiaChi.longitude';
        END

        COMMIT TRANSACTION;
        PRINT '=================================================================================';
        PRINT 'TRANSACTION ĐÃ ĐƯỢC COMMIT THÀNH CÔNG. TẤT CẢ CỘT THỪA ĐÃ ĐƯỢC XÓA KHỎI DATABASE SMDB_FINAL.';
        PRINT '=================================================================================';

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
        BEGIN
            ROLLBACK TRANSACTION;
            PRINT '=================================================================================';
            PRINT 'GẶP LỖI TRONG QUÁ TRÌNH THỰC THI. TRANSACTION ĐÃ ĐƯỢC ROLLBACK.';
            PRINT 'Chi tiết lỗi: ' + ERROR_MESSAGE();
            PRINT '=================================================================================';
        END
    END CATCH
END
GO
