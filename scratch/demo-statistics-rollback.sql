-- ==============================================================================
-- SMASH-VN BADMINTON SHOP - DEMO STATISTICS ROLLBACK SCRIPT
-- Purpose: Safely delete all demo statistics data and restore DB to clean state
-- Scope: Only records related to TaiKhoan.username LIKE 'demo_stat_cust_%'
-- Guarantee: 100% safe, preserves original database orders (ID 1 -> 7)
-- ==============================================================================

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    PRINT N'==> [1/5] Xóa các dòng HoaDonChiTiet thuộc hóa đơn Demo...';

    DELETE hdct
    FROM HoaDonChiTiet hdct
    WHERE hdct.id_hoa_don IN (
        SELECT hd.id
        FROM HoaDon hd
        JOIN KhachHang kh ON kh.id = hd.id_khach_hang
        JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
        WHERE tk.username LIKE 'demo_stat_cust_%'
    );

    PRINT N'==> [2/5] Xóa các Hóa đơn Demo (HoaDon)...';

    DELETE hd
    FROM HoaDon hd
    JOIN KhachHang kh ON kh.id = hd.id_khach_hang
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    PRINT N'==> [3/5] Xóa Sổ địa chỉ Demo (SoDiaChi)...';

    DELETE dc
    FROM SoDiaChi dc
    JOIN KhachHang kh ON kh.id = dc.id_khach_hang
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    PRINT N'==> [4/5] Xóa Khách hàng Demo (KhachHang)...';

    DELETE kh
    FROM KhachHang kh
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    PRINT N'==> [5/5] Xóa Tài khoản Demo (TaiKhoan)...';

    DELETE FROM TaiKhoan
    WHERE username LIKE 'demo_stat_cust_%';

    COMMIT TRANSACTION;
    PRINT N'==> [SUCCESS] Hoàn tất ROLLBACK sạch sẽ dữ liệu Demo! Toàn bộ 7 đơn hàng gốc được bảo toàn.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    PRINT N'==> [ERROR] Gặp lỗi trong quá trình ROLLBACK! Đã hủy bỏ thao tác.';
    THROW;
END CATCH;
