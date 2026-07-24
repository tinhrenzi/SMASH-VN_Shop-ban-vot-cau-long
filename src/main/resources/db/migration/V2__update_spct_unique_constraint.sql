BEGIN TRANSACTION;
BEGIN TRY

    -- 1. Kiểm tra dữ liệu trùng lặp theo quy tắc constraint mới
    IF EXISTS (
        SELECT id_san_pham, mau_sac, trong_luong, kich_thuoc, COUNT(*)
        FROM SanPhamChiTiet
        GROUP BY id_san_pham, mau_sac, trong_luong, kich_thuoc
        HAVING COUNT(*) > 1
    )
    BEGIN
        THROW 51000, N'LỖI MIGRATION: Phát hiện dữ liệu biến thể trùng lặp theo (id_san_pham, mau_sac, trong_luong, kich_thuoc)! Vui lòng dọn dẹp dữ liệu trước.', 1;
    END;

    -- 2. Kiểm tra và Drop constraint cũ thông qua sys.key_constraints & parent_object_id
    DECLARE @ConstraintName NVARCHAR(200);
    SELECT @ConstraintName = name 
    FROM sys.key_constraints 
    WHERE parent_object_id = OBJECT_ID(N'[dbo].[SanPhamChiTiet]') 
      AND name = N'UQ_SANPHAM_VARIANT';

    IF @ConstraintName IS NOT NULL
    BEGIN
        EXEC('ALTER TABLE SanPhamChiTiet DROP CONSTRAINT [' + @ConstraintName + ']');
    END;

    -- 3. Tạo UNIQUE Constraint mới
    ALTER TABLE SanPhamChiTiet ADD CONSTRAINT UQ_SANPHAM_VARIANT 
        UNIQUE (id_san_pham, mau_sac, trong_luong, kich_thuoc);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
