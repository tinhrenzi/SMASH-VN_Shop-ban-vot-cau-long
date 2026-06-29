-- =================================================================================
-- SQL SERVER CLEANUP SCRIPT: ORPHAN CARTS
-- Target Database: SMDB_FINAL
-- =================================================================================

USE SMDB_FINAL;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

-- =================================================================================
-- BIẾN ĐIỀU KHIỂN CHÍNH (MASTER CONTROLLER FLAG)
-- =================================================================================
-- 0 = DRY-RUN MODE: Chỉ SELECT hiển thị giỏ hàng sẽ xóa, không thực hiện xóa.
-- 1 = EXECUTE MODE: Thực thi XÓA trong TRANSACTION (Có rollback khi lỗi).
-- =================================================================================
DECLARE @DoExecute BIT = 0;

PRINT '=================================================================================';
PRINT 'KỊCH BẢN DỌN DẸP GIỎ HÀNG MỒ CÔI (SMDB_FINAL)';
PRINT 'Trạng thái chạy thử nghiệm (DoExecute) = ' + CAST(@DoExecute AS VARCHAR(1));
PRINT '=================================================================================';

-- Danh sách 6 ID giỏ hàng mồ côi đã xác minh: 587, 588, 627, 628, 667, 668

-- =================================================================================
-- 1. DRY-RUN: XEM TRƯỚC DỮ LIỆU SẼ BỊ XÓA
-- =================================================================================
IF @DoExecute = 0
BEGIN
    PRINT '--- [1] CÁC CHI TIẾT GIỎ HÀNG MỒ CÔI SẼ BỊ XÓA ---';
    SELECT gc.id AS [CartItem_ID],
           gc.id_gio_hang AS [Cart_ID],
           gc.id_san_pham_chi_tiet AS [ProductVariant_ID],
           gc.so_luong,
           gc.ngay_them
    FROM SMDB_FINAL.dbo.GioHangChiTiet gc
    JOIN SMDB_FINAL.dbo.GioHang g ON gc.id_gio_hang = g.id
    LEFT JOIN SMDB_FINAL.dbo.KhachHang k ON g.id_khach_hang = k.id
    WHERE g.id_khach_hang IS NOT NULL 
      AND k.id IS NULL
      AND g.id IN (587, 588, 627, 628, 667, 668);

    PRINT '--- [2] CÁC GIỎ HÀNG MỒ CÔI SẼ BỊ XÓA ---';
    SELECT g.id AS [Cart_ID],
           g.id_khach_hang AS [Missing_Customer_ID],
           g.ngay_tao,
           g.ngay_cap_nhat
    FROM SMDB_FINAL.dbo.GioHang g
    LEFT JOIN SMDB_FINAL.dbo.KhachHang k ON g.id_khach_hang = k.id
    WHERE g.id_khach_hang IS NOT NULL 
      AND k.id IS NULL
      AND g.id IN (587, 588, 627, 628, 667, 668);

    PRINT '=================================================================================';
    PRINT 'DRY-RUN HOÀN TẤT. KHÔNG CÓ DỮ LIỆU NÀO BỊ XÓA KHỎI SMDB_FINAL.';
    PRINT 'Để thực hiện xóa thật, hãy đổi giá trị DECLARE @DoExecute BIT = 1;';
    PRINT '=================================================================================';
END
ELSE
BEGIN
    PRINT '=================================================================================';
    PRINT 'ĐANG BẮT ĐẦU THỰC THI XÓA (EXECUTE MODE) TRONG TRANSACTION...';
    PRINT '=================================================================================';

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Kiểm tra số lượng giỏ hàng mồ côi trước khi xóa phải khớp đúng 6
        DECLARE @OrphanCount INT;
        SELECT @OrphanCount = COUNT(*)
        FROM SMDB_FINAL.dbo.GioHang g
        LEFT JOIN SMDB_FINAL.dbo.KhachHang k ON g.id_khach_hang = k.id
        WHERE g.id_khach_hang IS NOT NULL 
          AND k.id IS NULL
          AND g.id IN (587, 588, 627, 628, 667, 668);

        IF @OrphanCount <> 6
        BEGIN
            -- Số lượng không khớp đúng 6, throw error để rollback transaction
            DECLARE @ErrMSg NVARCHAR(255) = N'LƯU Ý: Số lượng giỏ hàng mồ côi thực tế (' + CAST(@OrphanCount AS NVARCHAR(10)) + N') khác 6! Hủy bỏ dọn dẹp để bảo đảm an toàn.';
            THROW 50000, @ErrMSg, 1;
        END

        -- Bước A: Xóa chi tiết giỏ hàng mồ côi trước (Dữ liệu con)
        PRINT '  - Bước A: Đang xóa chi tiết giỏ hàng mồ côi...';
        DELETE gc
        FROM SMDB_FINAL.dbo.GioHangChiTiet gc
        JOIN SMDB_FINAL.dbo.GioHang g ON gc.id_gio_hang = g.id
        LEFT JOIN SMDB_FINAL.dbo.KhachHang k ON g.id_khach_hang = k.id
        WHERE g.id_khach_hang IS NOT NULL 
          AND k.id IS NULL
          AND g.id IN (587, 588, 627, 628, 667, 668);
          
        DECLARE @ItemDeleteCount INT = @@ROWCOUNT;
        PRINT '    -> Đã xóa thành công ' + CAST(@ItemDeleteCount AS VARCHAR(10)) + ' chi tiết giỏ hàng.';

        -- Bước B: Xóa giỏ hàng mồ côi (Dữ liệu cha)
        PRINT '  - Bước B: Đang xóa giỏ hàng mồ côi...';
        DELETE g
        FROM SMDB_FINAL.dbo.GioHang g
        LEFT JOIN SMDB_FINAL.dbo.KhachHang k ON g.id_khach_hang = k.id
        WHERE g.id_khach_hang IS NOT NULL 
          AND k.id IS NULL
          AND g.id IN (587, 588, 627, 628, 667, 668);

        DECLARE @CartDeleteCount INT = @@ROWCOUNT;
        PRINT '    -> Đã xóa thành công ' + CAST(@CartDeleteCount AS VARCHAR(10)) + ' giỏ hàng.';

        -- Kiểm tra lại sau khi xóa xem còn giỏ hàng mồ côi thuộc các ID này không
        DECLARE @OrphanRemainCount INT;
        SELECT @OrphanRemainCount = COUNT(*)
        FROM SMDB_FINAL.dbo.GioHang WHERE id IN (587, 588, 627, 628, 667, 668);

        IF @OrphanRemainCount > 0
        BEGIN
            THROW 50001, N'LỖI: Vẫn còn giỏ hàng mồ côi chưa được xóa sạch khỏi database!', 1;
        END

        COMMIT TRANSACTION;
        PRINT '=================================================================================';
        PRINT 'TRANSACTION ĐÃ ĐƯỢC COMMIT THÀNH CÔNG. 6 GIỎ HÀNG MỒ CÔI ĐÃ ĐƯỢC DỌN DẸP.';
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
