-- =================================================================================
-- T-SQL SCRIPT FOR CATALOG SYNCHRONIZATION: DBSM1 -> SMDB_FINAL
-- Target Database: SMDB_FINAL
-- Reference Database: DBSM1
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
-- 0 = DRY-RUN MODE: Chỉ hiển thị dữ liệu dự kiến import/bỏ qua, không lưu thay đổi.
-- 1 = EXECUTE MODE: Thực thi chèn dữ liệu thực tế trong TRANSACTION (Có rollback).
-- =================================================================================
DECLARE @DoExecute BIT = 0;

PRINT '=================================================================================';
PRINT 'KỊCH BẢN ĐỒNG BỘ DANH MỤC SẢN PHẨM LINING (DBSM1 -> SMDB_FINAL)';
PRINT 'Trạng thái chạy thử nghiệm (DoExecute) = ' + CAST(@DoExecute AS VARCHAR(1));
PRINT '=================================================================================';

-- ---------------------------------------------------------------------------------
-- A. XÁC MINH CẤU TRÚC PHỤ THUỘC (THƯƠNG HIỆU & DANH MỤC)
-- ---------------------------------------------------------------------------------
-- 1. Kiểm tra thương hiệu Lining
DECLARE @TargetLiningBrandId INT;
SELECT @TargetLiningBrandId = id FROM SMDB_FINAL.dbo.ThuongHieu WHERE ten_thuong_hieu = 'Lining';

-- 2. Kiểm tra danh mục nguồn của các sản phẩm Lining từ DBSM1
IF OBJECT_ID('tempdb..#DanhMucReview') IS NOT NULL DROP TABLE #DanhMucReview;
CREATE TABLE #DanhMucReview (ten_danh_muc NVARCHAR(100), is_missing BIT);

INSERT INTO #DanhMucReview (ten_danh_muc, is_missing)
SELECT DISTINCT dm_src.ten_danh_muc,
       CASE WHEN dm_dst.id IS NULL THEN 1 ELSE 0 END
FROM DBSM1.dbo.SanPham s
JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
WHERE th.ten_thuong_hieu = 'Lining';

DECLARE @MissingCatCount INT;
SELECT @MissingCatCount = COUNT(*) FROM #DanhMucReview WHERE is_missing = 1;

IF @TargetLiningBrandId IS NULL OR @MissingCatCount > 0
BEGIN
    PRINT '=================================================================================';
    PRINT 'CRITICAL ERROR: Phát hiện thiếu Danh mục hoặc Thương hiệu phụ thuộc (NEEDS_REVIEW)!';
    IF @TargetLiningBrandId IS NULL
        PRINT '  - Thương hiệu "Lining" KHÔNG tồn tại trên database SMDB_FINAL.';
    IF @MissingCatCount > 0
    BEGIN
        PRINT '  - Các danh mục nguồn chưa có trên SMDB_FINAL:';
        DECLARE @CatName NVARCHAR(100);
        DECLARE CatCursor CURSOR FOR SELECT ten_danh_muc FROM #DanhMucReview WHERE is_missing = 1;
        OPEN CatCursor;
        FETCH NEXT FROM CatCursor INTO @CatName;
        WHILE @@FETCH_STATUS = 0
        BEGIN
            PRINT '    * ' + @CatName;
            FETCH NEXT FROM CatCursor INTO @CatName;
        END;
        CLOSE CatCursor;
        DEALLOCATE CatCursor;
    END
    PRINT '=================================================================================';
    -- Ép buộc chuyển về dry-run và báo lỗi
    SET @DoExecute = 0;
END

-- =================================================================================
-- 1. DRY-RUN MODE: THỐNG KÊ CHI TIẾT
-- =================================================================================
IF @DoExecute = 0
BEGIN
    PRINT '--- [1] DANH SÁCH SẢN PHẨM LINING DỰ KIẾN IMPORT ---';
    SELECT s.id AS [DBSM1_Product_ID],
           s.ten_san_pham,
           dm_src.ten_danh_muc AS [Source_Category],
           th.ten_thuong_hieu AS [Source_Brand],
           s.trang_thai
    FROM DBSM1.dbo.SanPham s
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    WHERE th.ten_thuong_hieu = 'Lining'
      AND sf.id IS NULL; -- Chưa tồn tại ở SMDB_FINAL

    PRINT '--- [2] SẢN PHẨM BỊ BỎ QUA VÌ ĐÃ TỒN TẠI ---';
    SELECT s.id AS [DBSM1_Product_ID],
           s.ten_san_pham,
           sf.id AS [SMDB_FINAL_Product_ID]
    FROM DBSM1.dbo.SanPham s
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    WHERE th.ten_thuong_hieu = 'Lining';

    PRINT '--- [3] SỐ LƯỢNG BIẾN THỂ DỰ KIẾN IMPORT VS BỊ BỎ QUA ---';
    DECLARE @VarImportCount INT;
    DECLARE @VarSkipCount INT;

    SELECT @VarImportCount = COUNT(*)
    FROM DBSM1.dbo.SanPhamChiTiet sc
    JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    LEFT JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
        (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
        (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
        (sf.id IS NOT NULL AND sf.id = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang)
    WHERE th.ten_thuong_hieu = 'Lining'
      AND scf.id IS NULL;

    SELECT @VarSkipCount = COUNT(*)
    FROM DBSM1.dbo.SanPhamChiTiet sc
    JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
        (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
        (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
        (sf.id IS NOT NULL AND sf.id = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang)
    WHERE th.ten_thuong_hieu = 'Lining';

    PRINT '  - Biến thể dự kiến chèn mới: ' + CAST(@VarImportCount AS VARCHAR(10));
    PRINT '  - Biến thể bỏ qua vì đã trùng: ' + CAST(@VarSkipCount AS VARCHAR(10));

    PRINT '--- [4] DANH SÁCH BIẾN THỂ BỊ BỎ QUA VÌ ĐÃ TỒN TẠI ---';
    SELECT sc.id AS [DBSM1_Variant_ID],
           sc.SKU,
           sc.barcode,
           scf.id AS [SMDB_FINAL_Variant_ID]
    FROM DBSM1.dbo.SanPhamChiTiet sc
    JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
        (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
        (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
        (sf.id IS NOT NULL AND sf.id = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang)
    WHERE th.ten_thuong_hieu = 'Lining';

    PRINT '--- [5] SỐ LƯỢNG HÌNH ẢNH DỰ KIẾN IMPORT VS BỊ BỎ QUA ---';
    DECLARE @ImgImportCount INT;
    DECLARE @ImgSkipCount INT;

    SELECT @ImgImportCount = COUNT(*)
    FROM DBSM1.dbo.HinhAnhSanPham h
    JOIN DBSM1.dbo.SanPhamChiTiet sc ON h.id_san_pham_chi_tiet = sc.id
    JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    LEFT JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
        (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
        (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
        (sf.id IS NOT NULL AND sf.id = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang)
    LEFT JOIN SMDB_FINAL.dbo.HinhAnhSanPham hf ON scf.id = hf.id_san_pham_chi_tiet AND h.duong_dan = hf.duong_dan
    WHERE th.ten_thuong_hieu = 'Lining'
      AND (scf.id IS NULL OR hf.id IS NULL);

    SELECT @ImgSkipCount = COUNT(*)
    FROM DBSM1.dbo.HinhAnhSanPham h
    JOIN DBSM1.dbo.SanPhamChiTiet sc ON h.id_san_pham_chi_tiet = sc.id
    JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
    JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
    JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
    LEFT JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
    LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
        AND sf.id_thuong_hieu = @TargetLiningBrandId
        AND sf.id_danh_muc = dm_dst.id
    JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
        (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
        (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
        (sf.id IS NOT NULL AND sf.id = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang)
    JOIN SMDB_FINAL.dbo.HinhAnhSanPham hf ON scf.id = hf.id_san_pham_chi_tiet AND h.duong_dan = hf.duong_dan
    WHERE th.ten_thuong_hieu = 'Lining';

    PRINT '  - Hình ảnh dự kiến chèn mới: ' + CAST(@ImgImportCount AS VARCHAR(10));
    PRINT '  - Hình ảnh bỏ qua vì đã trùng: ' + CAST(@ImgSkipCount AS VARCHAR(10));

    PRINT '--- [6] DANH SÁCH DANH MỤC THIẾU (NEEDS_REVIEW) ---';
    SELECT ten_danh_muc AS [Missing_Category] FROM #DanhMucReview WHERE is_missing = 1;

    PRINT '=================================================================================';
    PRINT 'DRY-RUN HOÀN TẤT. KHÔNG CÓ THAY ĐỔI DỮ LIỆU NÀO TRÊN SMDB_FINAL.';
    PRINT '=================================================================================';
END
-- =================================================================================
-- 2. EXECUTE MODE: CHẠY THẬT TRONG TRANSACTION
-- =================================================================================
ELSE
BEGIN
    PRINT '=================================================================================';
    PRINT 'ĐANG BẮT ĐẦU IMPORT DỮ LIỆU TRÊN SMDB_FINAL TRONG TRANSACTION...';
    PRINT '=================================================================================';

    -- Tạo các bảng Map tạm
    IF OBJECT_ID('tempdb..#ProductMap') IS NOT NULL DROP TABLE #ProductMap;
    CREATE TABLE #ProductMap (
        SourceProductId INT,
        TargetProductId INT,
        ProductName NVARCHAR(255)
    );

    IF OBJECT_ID('tempdb..#VariantMap') IS NOT NULL DROP TABLE #VariantMap;
    CREATE TABLE #VariantMap (
        SourceVariantId INT,
        TargetVariantId INT,
        SKU VARCHAR(100),
        Barcode VARCHAR(100)
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Kiểm tra lại lần cuối lỗi thiếu metadata
        IF @TargetLiningBrandId IS NULL OR @MissingCatCount > 0
        BEGIN
            THROW 50002, N'LỖI: Thiếu Danh mục/Thương hiệu cần thiết để ánh xạ dữ liệu! Hủy bỏ import.', 1;
        END

        -- -----------------------------------------------------------------------------
        -- 1. Insert Sản Phẩm mới (Không copy ID, map bằng Tên + Brand + Cat)
        -- -----------------------------------------------------------------------------
        PRINT '  - Bước 1: Đang chèn sản phẩm mới...';
        INSERT INTO SMDB_FINAL.dbo.SanPham (
            ten_san_pham, 
            mo_ta, 
            id_danh_muc, 
            id_thuong_hieu, 
            id_nhan_vien, 
            trang_thai, 
            ngay_tao, 
            ngay_cap_nhat, 
            diem_trung_binh, 
            so_danh_gia
        )
        SELECT 
            s.ten_san_pham,
            s.mo_ta,
            dm_dst.id, -- map động danh mục đích
            @TargetLiningBrandId, -- map động thương hiệu đích
            s.id_nhan_vien,
            s.trang_thai,
            s.ngay_tao,
            s.ngay_cap_nhat,
            s.diem_trung_binh,
            s.so_danh_gia
        FROM DBSM1.dbo.SanPham s
        JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
        JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
        JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
        LEFT JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
            AND sf.id_thuong_hieu = @TargetLiningBrandId
            AND sf.id_danh_muc = dm_dst.id
        WHERE th.ten_thuong_hieu = 'Lining'
          AND sf.id IS NULL; -- Chỉ chèn sản phẩm chưa tồn tại
        
        DECLARE @ProductInsertCount INT = @@ROWCOUNT;
        PRINT '    -> Đã chèn thành công ' + CAST(@ProductInsertCount AS VARCHAR(10)) + ' sản phẩm.';

        -- Populate Bảng Map sản phẩm (gồm cả sản phẩm mới chèn và sản phẩm cũ đã có sẵn)
        INSERT INTO #ProductMap (SourceProductId, TargetProductId, ProductName)
        SELECT s.id, sf.id, s.ten_san_pham
        FROM DBSM1.dbo.SanPham s
        JOIN DBSM1.dbo.ThuongHieu th ON s.id_thuong_hieu = th.id
        JOIN DBSM1.dbo.DanhMuc dm_src ON s.id_danh_muc = dm_src.id
        JOIN SMDB_FINAL.dbo.DanhMuc dm_dst ON dm_dst.ten_danh_muc = dm_src.ten_danh_muc
        JOIN SMDB_FINAL.dbo.SanPham sf ON s.ten_san_pham = sf.ten_san_pham
            AND sf.id_thuong_hieu = @TargetLiningBrandId
            AND sf.id_danh_muc = dm_dst.id
        WHERE th.ten_thuong_hieu = 'Lining';

        -- Kiểm tra ánh xạ sản phẩm mơ hồ (1 source map sang nhiều target)
        IF EXISTS (SELECT SourceProductId FROM #ProductMap GROUP BY SourceProductId HAVING COUNT(*) > 1)
        BEGIN
            THROW 50005, N'LỖI: Phát hiện sản phẩm nguồn ánh xạ tới nhiều sản phẩm đích (mơ hồ)!', 1;
        END

        -- -----------------------------------------------------------------------------
        -- 2. Insert Biến Thể Chi Tiết (Có khử trùng theo 3 mức độ)
        -- -----------------------------------------------------------------------------
        PRINT '  - Bước 2: Đang chèn biến thể sản phẩm chi tiết...';
        INSERT INTO SMDB_FINAL.dbo.SanPhamChiTiet (
            id_san_pham,
            mau_sac,
            kich_thuoc,
            trong_luong,
            chat_lieu,
            muc_cang,
            SKU,
            barcode,
            gia_nhap,
            gia_ban,
            so_luong_ton,
            trang_thai,
            ngay_tao,
            ngay_cap_nhat
        )
        SELECT 
            pm.TargetProductId, -- Lấy ID cha thật từ bảng map tạm
            sc.mau_sac,
            sc.kich_thuoc,
            sc.trong_luong,
            sc.chat_lieu,
            sc.muc_cang,
            sc.SKU,
            sc.barcode,
            sc.gia_nhap,
            sc.gia_ban,
            sc.so_luong_ton,
            sc.trang_thai,
            sc.ngay_tao,
            sc.ngay_cap_nhat
        FROM DBSM1.dbo.SanPhamChiTiet sc
        JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
        JOIN #ProductMap pm ON s.id = pm.SourceProductId
        LEFT JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
            (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
            (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
            ((sc.SKU IS NULL OR sc.SKU = '' OR scf.SKU IS NULL OR scf.SKU = '') AND 
             (sc.barcode IS NULL OR sc.barcode = '' OR scf.barcode IS NULL OR scf.barcode = '') AND
             pm.TargetProductId = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang)
        WHERE scf.id IS NULL; -- Chỉ chèn biến thể chưa có

        DECLARE @VariantInsertCount INT = @@ROWCOUNT;
        PRINT '    -> Đã chèn thành công ' + CAST(@VariantInsertCount AS VARCHAR(10)) + ' biến thể chi tiết.';

        -- Populate Bảng Map biến thể (gồm cả mới chèn và đã tồn tại từ trước)
        INSERT INTO #VariantMap (SourceVariantId, TargetVariantId, SKU, Barcode)
        SELECT sc.id, scf.id, sc.SKU, sc.barcode
        FROM DBSM1.dbo.SanPhamChiTiet sc
        JOIN DBSM1.dbo.SanPham s ON sc.id_san_pham = s.id
        JOIN #ProductMap pm ON s.id = pm.SourceProductId
        JOIN SMDB_FINAL.dbo.SanPhamChiTiet scf ON 
            (sc.SKU IS NOT NULL AND sc.SKU <> '' AND sc.SKU = scf.SKU) OR
            (sc.barcode IS NOT NULL AND sc.barcode <> '' AND sc.barcode = scf.barcode) OR
            (pm.TargetProductId = scf.id_san_pham AND sc.mau_sac = scf.mau_sac AND sc.trong_luong = scf.trong_luong AND sc.muc_cang = scf.muc_cang);

        -- Kiểm tra ánh xạ biến thể mơ hồ (1 source map sang nhiều target)
        IF EXISTS (SELECT SourceVariantId FROM #VariantMap GROUP BY SourceVariantId HAVING COUNT(*) > 1)
        BEGIN
            THROW 50006, N'LỖI: Phát hiện biến thể nguồn ánh xạ tới nhiều biến thể đích (mơ hồ)!', 1;
        END

        -- -----------------------------------------------------------------------------
        -- 3. Insert Hình Ảnh (Map qua VariantMap, khử trùng bằng variant_id + duong_dan)
        -- -----------------------------------------------------------------------------
        PRINT '  - Bước 3: Đang chèn hình ảnh biến thể...';
        INSERT INTO SMDB_FINAL.dbo.HinhAnhSanPham (
            id_san_pham_chi_tiet,
            duong_dan,
            la_anh_chinh,
            mau_sac
        )
        SELECT 
            vm.TargetVariantId, -- Lấy ID biến thể thật tại SMDB_FINAL từ bảng map
            h.duong_dan,
            h.la_anh_chinh,
            h.mau_sac
        FROM DBSM1.dbo.HinhAnhSanPham h
        JOIN #VariantMap vm ON h.id_san_pham_chi_tiet = vm.SourceVariantId
        LEFT JOIN SMDB_FINAL.dbo.HinhAnhSanPham hf ON vm.TargetVariantId = hf.id_san_pham_chi_tiet AND h.duong_dan = hf.duong_dan
        WHERE hf.id IS NULL; -- Chỉ chèn ảnh chưa tồn tại của biến thể đó

        DECLARE @ImageInsertCount INT = @@ROWCOUNT;
        PRINT '    -> Đã chèn thành công ' + CAST(@ImageInsertCount AS VARCHAR(10)) + ' hình ảnh.';

        COMMIT TRANSACTION;
        PRINT '=================================================================================';
        PRINT 'TRANSACTION ĐÃ ĐƯỢC COMMIT THÀNH CÔNG. DANH MỤC SẢN PHẨM LINING ĐÃ ĐƯỢC ĐỒNG BỘ.';
        PRINT '=================================================================================';

        -- SELECT báo cáo thống kê thực tế trên SMDB_FINAL sau khi chạy
        PRINT '--- [BÁO CÁO THỰC TẾ TRÊN SMDB_FINAL SAU KHI ĐỒNG BỘ] ---';
        SELECT COUNT(DISTINCT s.id) AS [Total_Lining_Products],
               COUNT(DISTINCT sc.id) AS [Total_Lining_Variants],
               COUNT(DISTINCT h.id) AS [Total_Lining_Images]
        FROM SMDB_FINAL.dbo.SanPham s
        LEFT JOIN SMDB_FINAL.dbo.SanPhamChiTiet sc ON s.id = sc.id_san_pham
        LEFT JOIN SMDB_FINAL.dbo.HinhAnhSanPham h ON sc.id = h.id_san_pham_chi_tiet
        WHERE s.id_thuong_hieu = @TargetLiningBrandId;

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
