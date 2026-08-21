-- ==============================================================================
-- SMASH-VN BADMINTON SHOP - DEMO STATISTICS SEED SCRIPT
-- Purpose: Seed realistic demo data for graduation defense statistics dashboard
-- Target Preset: "last_30_days" (Current: 21/07/2026 -> 19/08/2026 | Previous: 21/06/2026 -> 20/07/2026)
-- Marker: TaiKhoan.username LIKE 'demo_stat_cust_%' / HoaDon.ghi_chu LIKE 'DEMO_STAT_D%'
-- ==============================================================================

SET XACT_ABORT ON;

-- GUARD CHECK: Ensure demo data does not already exist
IF EXISTS (SELECT 1 FROM TaiKhoan WHERE username LIKE 'demo_stat_cust_%')
BEGIN
    THROW 50001, N'Dữ liệu Demo Thống Kê đã tồn tại trong database! Hãy chạy script demo-statistics-rollback.sql trước khi seed lại.', 1;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    PRINT N'==> [1/5] Khởi tạo 18 Tài khoản Demo (TaiKhoan)...';
    
    INSERT INTO TaiKhoan (
        username, mat_khau, vai_tro, trang_thai_tai_khoan,
        so_lan_mua_thanh_cong, so_lan_nhac_nho_vi_pham, ngay_tao
    ) VALUES
    ('demo_stat_cust_01', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-06-20 08:00:00'),
    ('demo_stat_cust_02', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:05:00'),
    ('demo_stat_cust_03', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-06-20 08:10:00'),
    ('demo_stat_cust_04', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:15:00'),
    ('demo_stat_cust_05', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:20:00'),
    ('demo_stat_cust_06', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-06-20 08:25:00'),
    ('demo_stat_cust_07', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:30:00'),
    ('demo_stat_cust_08', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-06-20 08:35:00'),
    ('demo_stat_cust_09', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:00:00'),
    ('demo_stat_cust_10', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:05:00'),
    ('demo_stat_cust_11', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:10:00'),
    ('demo_stat_cust_12', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:15:00'),
    ('demo_stat_cust_13', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:20:00'),
    ('demo_stat_cust_14', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:25:00'),
    ('demo_stat_cust_15', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-07-20 08:30:00'),
    ('demo_stat_cust_16', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-07-20 08:35:00'),
    ('demo_stat_cust_17', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-07-20 08:40:00'),
    ('demo_stat_cust_18', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-07-20 08:45:00');

    PRINT N'==> [2/5] Khởi tạo 18 Hồ sơ Khách hàng (KhachHang)...';

    INSERT INTO KhachHang (id_tai_khoan, ho_ten_kh, so_dien_thoai_kh, ngay_tao)
    SELECT tk.id, v.ho_ten_kh, v.so_dien_thoai_kh, tk.ngay_tao
    FROM (VALUES
        ('demo_stat_cust_01', N'Nguyễn Văn An', '0912100001'),
        ('demo_stat_cust_02', N'Trần Thị Bình', '0912100002'),
        ('demo_stat_cust_03', N'Lê Hoàng Cường', '0912100003'),
        ('demo_stat_cust_04', N'Phạm Minh Đức', '0912100004'),
        ('demo_stat_cust_05', N'Đỗ Thúy Hạnh', '0912100005'),
        ('demo_stat_cust_06', N'Hoàng Văn Khôi', '0912100006'),
        ('demo_stat_cust_07', N'Vũ Thị Lan', '0912100007'),
        ('demo_stat_cust_08', N'Bùi Quang Nam', '0912100008'),
        ('demo_stat_cust_09', N'Phan Bảo Ngọc', '0912100009'),
        ('demo_stat_cust_10', N'Trịnh Thu Phương', '0912100010'),
        ('demo_stat_cust_11', N'Đặng Quốc Quân', '0912100011'),
        ('demo_stat_cust_12', N'Mai Thảo Quỳnh', '0912100012'),
        ('demo_stat_cust_13', N'Hà Văn Sơn', '0912100013'),
        ('demo_stat_cust_14', N'Ngô Mỹ Tâm', '0912100014'),
        ('demo_stat_cust_15', N'Dương Anh Tuấn', '0912100015'),
        ('demo_stat_cust_16', N'Võ Hồng Uyên', '0912100016'),
        ('demo_stat_cust_17', N'Trương Tấn Vinh', '0912100017'),
        ('demo_stat_cust_18', N'Lâm Gia Yến', '0912100018')
    ) AS v(username, ho_ten_kh, so_dien_thoai_kh)
    JOIN TaiKhoan tk ON tk.username = v.username;

    PRINT N'==> [3/5] Khởi tạo 18 Sổ địa chỉ (SoDiaChi)...';

    INSERT INTO SoDiaChi (
        id_khach_hang, ho_va_ten_nguoi_nhan, sdt_nguoi_nhan,
        dia_chi_cu_the, tinh_thanh, quan_huyen, phuong_xa,
        dia_chi_mac_dinh, ngay_tao
    )
    SELECT kh.id, kh.ho_ten_kh, kh.so_dien_thoai_kh,
           N'Số ' + CAST(ROW_NUMBER() OVER(ORDER BY kh.id) * 12 AS NVARCHAR(10)) + N' Đường Cầu Lông',
           N'Hà Nội', N'Quận Cầu Giấy', N'Phường Dịch Vọng',
           1, kh.ngay_tao
    FROM KhachHang kh
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    PRINT N'==> [4/5] Khởi tạo 41 Hóa đơn Demo (HoaDon)...';

    -- Lookup động ID phương thức thanh toán
    DECLARE @PTTT_COD INT;
    DECLARE @PTTT_VNPAY INT;
    DECLARE @PTTT_MOMO INT;
    DECLARE @PTTT_SEPAY INT;

    SELECT @PTTT_COD = id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'COD' OR UPPER(ten_phuong_thuc) LIKE N'%KHI NHẬN HÀNG%';
    SELECT @PTTT_VNPAY = id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'VNPAY' OR UPPER(ten_phuong_thuc) LIKE N'%VNPAY%';
    SELECT @PTTT_MOMO = id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'MOMO' OR UPPER(ten_phuong_thuc) LIKE N'%MOMO%';
    SELECT @PTTT_SEPAY = id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'SEPAY' OR UPPER(ten_phuong_thuc) LIKE N'%SEPAY%';

    PRINT N'  -> COD ID   = ' + ISNULL(CAST(@PTTT_COD AS NVARCHAR(10)), N'NULL (KHÔNG TÌM THẤY)');
    PRINT N'  -> VNPAY ID = ' + ISNULL(CAST(@PTTT_VNPAY AS NVARCHAR(10)), N'NULL (KHÔNG TÌM THẤY)');
    PRINT N'  -> MOMO ID  = ' + ISNULL(CAST(@PTTT_MOMO AS NVARCHAR(10)), N'NULL (KHÔNG TÌM THẤY)');
    PRINT N'  -> SEPAY ID = ' + ISNULL(CAST(@PTTT_SEPAY AS NVARCHAR(10)), N'NULL (Tự động map sang VNPAY)');

    -- Fallback SePay nếu SePay không có trong DB
    IF @PTTT_SEPAY IS NULL
    BEGIN
        SET @PTTT_SEPAY = @PTTT_VNPAY;
    END;

    -- Kiểm tra phương thức bắt buộc
    IF @PTTT_COD IS NULL
        THROW 50002, N'Lỗi: Không tìm thấy phương thức thanh toán COD trong bảng PhuongThucThanhToan!', 1;
    IF @PTTT_VNPAY IS NULL
        THROW 50003, N'Lỗi: Không tìm thấy phương thức thanh toán VNPAY trong bảng PhuongThucThanhToan!', 1;
    IF @PTTT_MOMO IS NULL
        THROW 50004, N'Lỗi: Không tìm thấy phương thức thanh toán MOMO trong bảng PhuongThucThanhToan!', 1;

    -- Bảng tạm hỗ trợ lookup ID khách hàng và địa chỉ
    DECLARE @CustMap TABLE (
        cust_code VARCHAR(10),
        id_khach_hang INT,
        id_dia_chi INT,
        ho_ten_kh NVARCHAR(100),
        sdt_kh VARCHAR(15),
        dia_chi NVARCHAR(500)
    );

    INSERT INTO @CustMap (cust_code, id_khach_hang, id_dia_chi, ho_ten_kh, sdt_kh, dia_chi)
    SELECT REPLACE(tk.username, 'demo_stat_cust_', 'C'),
           kh.id, dc.id, kh.ho_ten_kh, kh.so_dien_thoai_kh,
           dc.dia_chi_cu_the + N', ' + dc.phuong_xa + N', ' + dc.quan_huyen + N', ' + dc.tinh_thanh
    FROM KhachHang kh
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    JOIN SoDiaChi dc ON dc.id_khach_hang = kh.id
    WHERE tk.username LIKE 'demo_stat_cust_%';

    -- Insert 41 HoaDon (D01 -> D20: Kỳ Trước | D21 -> D41: Kỳ Hiện Tại)
    INSERT INTO HoaDon (
        id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, id_phieu_giam_gia,
        id_don_vi_van_chuyen, id_dia_chi, ngay_tao, ngay_thanh_toan,
        tien_hang, phi_van_chuyen, so_tien_giam_gia, tong_tien,
        trang_thai_don_hang, trang_thai_thanh_toan, ten_nguoi_nhan,
        sdt_nhan, email_nguoi_nhan, dia_chi_nhan, ly_do_huy,
        ghi_chu, ma_giao_dich
    )
    SELECT
        m.id_khach_hang, NULL,
        CASE d.pttt_code
            WHEN 'COD' THEN @PTTT_COD
            WHEN 'VNPAY' THEN @PTTT_VNPAY
            WHEN 'MOMO' THEN @PTTT_MOMO
            WHEN 'SEPAY' THEN @PTTT_SEPAY
        END,
        NULL,
        NULL, m.id_dia_chi, d.ngay_tao, d.ngay_thanh_toan,
        d.tien_hang, d.phi_van_chuyen, d.so_tien_giam_gia, d.tong_tien,
        d.trang_thai_don_hang, d.trang_thai_thanh_toan, m.ho_ten_kh,
        m.sdt_kh, d.email, m.dia_chi, d.ly_do_huy,
        'DEMO_STAT_' + d.order_code, d.ma_giao_dich
    FROM (VALUES
        -- KỲ TRƯỚC (D01 -> D20)
        ('D01', 'C01', 'SEPAY', '2026-06-22 09:15:00', '2026-06-22 09:16:30', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202606220915_C01'),
        ('D02', 'C02', 'VNPAY', '2026-06-24 14:20:00', '2026-06-24 14:21:45', 5650000.00, 97400.00, 0.00, 5747400.00, N'da_giao', N'DA_THANH_TOAN', 'binh.tran@example.com', NULL, 'VNP_DEMO_202606241420_C02'),
        ('D03', 'C03', 'COD',   '2026-06-26 10:30:00', '2026-06-28 11:00:00', 4190000.00, 97400.00, 0.00, 4287400.00, N'da_giao', N'DA_THANH_TOAN', 'cuong.le@example.com', NULL, NULL),
        ('D04', 'C04', 'MOMO',  '2026-06-28 16:45:00', '2026-06-28 16:46:12', 4709000.00, 97400.00, 0.00, 4806400.00, N'da_giao', N'DA_THANH_TOAN', 'duc.pham@example.com', NULL, 'MM_DEMO_202606281645_C04'),
        ('D05', 'C05', 'SEPAY', '2026-06-30 11:10:00', '2026-06-30 11:11:05', 4490000.00, 97400.00, 0.00, 4587400.00, N'da_giao', N'DA_THANH_TOAN', 'hanh.do@example.com', NULL, 'SP_DEMO_202606301110_C05'),
        ('D06', 'C06', 'COD',   '2026-07-02 08:50:00', '2026-07-04 09:30:00', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'khoi.hoang@example.com', NULL, NULL),
        ('D07', 'C07', 'VNPAY', '2026-07-03 15:30:00', '2026-07-03 15:31:20', 5650000.00, 97400.00, 0.00, 5747400.00, N'da_giao', N'DA_THANH_TOAN', 'lan.vu@example.com', NULL, 'VNP_DEMO_202607031530_C07'),
        ('D08', 'C08', 'SEPAY', '2026-07-05 19:20:00', '2026-07-05 19:22:10', 4190000.00, 97400.00, 0.00, 4287400.00, N'da_giao', N'DA_THANH_TOAN', 'nam.bui@example.com', NULL, 'SP_DEMO_202607051920_C08'),
        ('D09', 'C01', 'SEPAY', '2026-07-07 10:15:00', '2026-07-07 10:16:00', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202607071015_C01'),
        ('D10', 'C02', 'MOMO',  '2026-07-09 14:00:00', '2026-07-09 14:01:30', 4709000.00, 97400.00, 0.00, 4806400.00, N'da_giao', N'DA_THANH_TOAN', 'binh.tran@example.com', NULL, 'MM_DEMO_202607091400_C02'),
        ('D11', 'C03', 'COD',   '2026-07-11 17:35:00', '2026-07-13 18:00:00', 5650000.00, 97400.00, 0.00, 5747400.00, N'da_giao', N'DA_THANH_TOAN', 'cuong.le@example.com', NULL, NULL),
        ('D12', 'C04', 'VNPAY', '2026-07-13 09:40:00', '2026-07-13 09:42:00', 4490000.00, 97400.00, 0.00, 4587400.00, N'da_giao', N'DA_THANH_TOAN', 'duc.pham@example.com', NULL, 'VNP_DEMO_202607130940_C04'),
        ('D13', 'C05', 'SEPAY', '2026-07-15 13:25:00', '2026-07-15 13:26:40', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'hanh.do@example.com', NULL, 'SP_DEMO_202607151325_C05'),
        ('D14', 'C06', 'COD',   '2026-07-17 16:10:00', '2026-07-19 16:40:00', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'khoi.hoang@example.com', NULL, NULL),
        ('D15', 'C07', 'SEPAY', '2026-07-19 20:05:00', '2026-07-19 20:06:15', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'lan.vu@example.com', NULL, 'SP_DEMO_202607192005_C07'),
        ('D16', 'C08', 'COD',   '2026-06-25 11:00:00', NULL, 4190000.00, 97400.00, 0.00, 4287400.00, N'dang_giao', N'CHO_THANH_TOAN', 'nam.bui@example.com', NULL, NULL),
        ('D17', 'C01', 'SEPAY', '2026-07-10 15:00:00', NULL, 5099000.00, 97400.00, 0.00, 5196400.00, N'cho_xac_nhan', N'CHO_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202607101500_C01'),
        ('D18', 'C02', 'COD',   '2026-06-27 09:30:00', NULL, 4709000.00, 97400.00, 0.00, 4806400.00, N'da_huy', N'CHO_THANH_TOAN', 'binh.tran@example.com', N'Khách đổi ý muốn mua màu khác', NULL),
        ('D19', 'C03', 'COD',   '2026-07-06 14:40:00', NULL, 5650000.00, 97400.00, 0.00, 5747400.00, N'da_huy', N'CHO_THANH_TOAN', 'cuong.le@example.com', N'Trùng đơn hàng', NULL),
        ('D20', 'C04', 'COD',   '2026-07-18 18:20:00', NULL, 4490000.00, 97400.00, 0.00, 4587400.00, N'da_huy', N'CHO_THANH_TOAN', 'duc.pham@example.com', N'Thời gian giao hàng lâu', NULL),

        -- KỲ HIỆN TẠI (D21 -> D41)
        ('D21', 'C09', 'SEPAY', '2026-07-22 09:30:00', '2026-07-22 09:31:15', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'ngoc.phan@example.com', NULL, 'SP_DEMO_202607220930_C09'),
        ('D22', 'C10', 'VNPAY', '2026-07-24 14:15:00', '2026-07-24 14:16:40', 4190000.00, 97400.00, 0.00, 4287400.00, N'da_giao', N'DA_THANH_TOAN', 'phuong.trinh@example.com', NULL, 'VNP_DEMO_202607241415_C10'),
        ('D23', 'C11', 'COD',   '2026-07-26 10:45:00', '2026-07-28 11:30:00', 5650000.00, 97400.00, 0.00, 5747400.00, N'da_giao', N'DA_THANH_TOAN', 'quan.dang@example.com', NULL, NULL),
        ('D24', 'C12', 'MOMO',  '2026-07-27 16:20:00', '2026-07-27 16:21:10', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'quynh.mai@example.com', NULL, 'MM_DEMO_202607271620_C12'),
        ('D25', 'C01', 'SEPAY', '2026-07-29 11:05:00', '2026-07-29 11:06:20', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202607291105_C01'),
        ('D26', 'C13', 'VNPAY', '2026-07-31 15:40:00', '2026-07-31 15:41:50', 4709000.00, 97400.00, 0.00, 4806400.00, N'da_giao', N'DA_THANH_TOAN', 'son.ha@example.com', NULL, 'VNP_DEMO_202607311540_C13'),
        ('D27', 'C14', 'SEPAY', '2026-08-02 08:30:00', '2026-08-02 08:31:05', 4490000.00, 97400.00, 0.00, 4587400.00, N'da_giao', N'DA_THANH_TOAN', 'tam.ngo@example.com', NULL, 'SP_DEMO_202608020830_C14'),
        ('D28', 'C15', 'COD',   '2026-08-04 19:15:00', '2026-08-06 20:00:00', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'tuan.duong@example.com', NULL, NULL),
        ('D29', 'C16', 'MOMO',  '2026-08-06 10:50:00', '2026-08-06 10:51:30', 4190000.00, 97400.00, 0.00, 4287400.00, N'da_giao', N'DA_THANH_TOAN', 'uyen.vo@example.com', NULL, 'MM_DEMO_202608061050_C16'),
        ('D30', 'C17', 'SEPAY', '2026-08-08 14:35:00', '2026-08-08 14:36:25', 5650000.00, 97400.00, 0.00, 5747400.00, N'da_giao', N'DA_THANH_TOAN', 'vinh.truong@example.com', NULL, 'SP_DEMO_202608081435_C17'),
        ('D31', 'C18', 'COD',   '2026-08-09 17:00:00', '2026-08-11 17:45:00', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'yen.lam@example.com', NULL, NULL),
        ('D32', 'C03', 'VNPAY', '2026-08-11 09:20:00', '2026-08-11 09:22:15', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'cuong.le@example.com', NULL, 'VNP_DEMO_202608110920_C03'),
        ('D33', 'C09', 'SEPAY', '2026-08-12 13:45:00', '2026-08-12 13:46:30', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'ngoc.phan@example.com', NULL, 'SP_DEMO_202608121345_C09'),
        ('D34', 'C10', 'MOMO',  '2026-08-14 16:10:00', '2026-08-14 16:11:40', 4709000.00, 97400.00, 0.00, 4806400.00, N'da_giao', N'DA_THANH_TOAN', 'phuong.trinh@example.com', NULL, 'MM_DEMO_202608141610_C10'),
        ('D35', 'C11', 'SEPAY', '2026-08-15 11:25:00', '2026-08-15 11:26:10', 4190000.00, 97400.00, 0.00, 4287400.00, N'da_giao', N'DA_THANH_TOAN', 'quan.dang@example.com', NULL, 'SP_DEMO_202608151125_C11'),
        ('D36', 'C12', 'COD',   '2026-08-16 15:50:00', '2026-08-18 16:20:00', 5650000.00, 97400.00, 0.00, 5747400.00, N'da_giao', N'DA_THANH_TOAN', 'quynh.mai@example.com', NULL, NULL),
        ('D37', 'C13', 'VNPAY', '2026-08-17 08:40:00', '2026-08-17 08:41:20', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'son.ha@example.com', NULL, 'VNP_DEMO_202608170840_C13'),
        ('D38', 'C06', 'SEPAY', '2026-08-17 14:10:00', '2026-08-17 14:11:15', 5099000.00, 97400.00, 0.00, 5196400.00, N'da_giao', N'DA_THANH_TOAN', 'khoi.hoang@example.com', NULL, 'SP_DEMO_202608171410_C06'),
        ('D39', 'C14', 'MOMO',  '2026-08-18 10:00:00', '2026-08-18 10:01:45', 300000.00, 97400.00, 0.00, 397400.00, N'da_giao', N'DA_THANH_TOAN', 'tam.ngo@example.com', NULL, 'MM_DEMO_202608181000_C14'),
        ('D40', 'C15', 'COD',   '2026-08-13 10:30:00', NULL, 4190000.00, 97400.00, 0.00, 4287400.00, N'dang_giao', N'CHO_THANH_TOAN', 'tuan.duong@example.com', NULL, NULL),
        ('D41', 'C16', 'COD',   '2026-08-07 16:00:00', NULL, 4709000.00, 97400.00, 0.00, 4806400.00, N'da_huy', N'CHO_THANH_TOAN', 'uyen.vo@example.com', N'Khách muốn hủy đặt lại', NULL)
    ) AS d(order_code, cust_code, pttt_code, ngay_tao, ngay_thanh_toan, tien_hang, phi_van_chuyen, so_tien_giam_gia, tong_tien, trang_thai_don_hang, trang_thai_thanh_toan, email, ly_do_huy, ma_giao_dich)
    JOIN @CustMap m ON m.cust_code = d.cust_code;

    PRINT N'==> [5/5] Khởi tạo 41 Dòng Chi tiết Hóa đơn (HoaDonChiTiet)...';

    -- Bảng tạm mapping giữa mã hóa đơn demo và actual ID trong DB
    DECLARE @OrderMap TABLE (
        order_code VARCHAR(10),
        id_hoa_don INT,
        ngay_tao DATETIME
    );

    INSERT INTO @OrderMap (order_code, id_hoa_don, ngay_tao)
    SELECT REPLACE(ghi_chu, 'DEMO_STAT_', ''), id, ngay_tao
    FROM HoaDon
    WHERE ghi_chu LIKE 'DEMO_STAT_D%';

    -- Insert 41 dòng HoaDonChiTiet
    INSERT INTO HoaDonChiTiet (
        id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia,
        gia_goc, gia_sau_giam, ten_san_pham_snapshot, sku_snapshot,
        ngay_tao
    )
    SELECT
        om.id_hoa_don, it.id_spct, it.so_luong, it.don_gia,
        it.gia_goc, it.gia_sau_giam, it.ten_sp_snapshot, it.sku_snapshot,
        om.ngay_tao
    FROM (VALUES
        -- Chi tiết Kỳ Trước (D01 -> D20)
        ('D01', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D02', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D03', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D04', 28, 1, 4709000.00, 4709000.00, 4709000.00, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', 'NF700P-4U-G5'),
        ('D05', 1,  1, 4490000.00, 4490000.00, 4490000.00, N'Lining Bladex 800 Speed 2026', 'BX800-4U'),
        ('D06', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D07', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D08', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D09', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D10', 28, 1, 4709000.00, 4709000.00, 4709000.00, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', 'NF700P-4U-G5'),
        ('D11', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D12', 1,  1, 4490000.00, 4490000.00, 4490000.00, N'Lining Bladex 800 Speed 2026', 'BX800-4U'),
        ('D13', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D14', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D15', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D16', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D17', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D18', 28, 1, 4709000.00, 4709000.00, 4709000.00, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', 'NF700P-4U-G5'),
        ('D19', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D20', 1,  1, 4490000.00, 4490000.00, 4490000.00, N'Lining Bladex 800 Speed 2026', 'BX800-4U'),

        -- Chi tiết Kỳ Hiện Tại (D21 -> D41)
        ('D21', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D22', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D23', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D24', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D25', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D26', 28, 1, 4709000.00, 4709000.00, 4709000.00, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', 'NF700P-4U-G5'),
        ('D27', 1,  1, 4490000.00, 4490000.00, 4490000.00, N'Lining Bladex 800 Speed 2026', 'BX800-4U'),
        ('D28', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D29', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D30', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D31', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D32', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D33', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D34', 28, 1, 4709000.00, 4709000.00, 4709000.00, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', 'NF700P-4U-G5'),
        ('D35', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D36', 2,  1, 5650000.00, 5650000.00, 5650000.00, N'Vợt cầu lông Lining Axforce 100 Gen 2', 'AX100-G2-4U'),
        ('D37', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D38', 27, 1, 5099000.00, 5099000.00, 5099000.00, N'Vợt cầu lông Yonex Nanoflare 1000Z', 'NF1000Z-4U-G5'),
        ('D39', 117, 2, 150000.00, 150000.00, 150000.00, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', 'AC102-EX-2P'),
        ('D40', 12, 1, 4190000.00, 4190000.00, 4190000.00, N'Vợt cầu lông Victor Auraspeed 99 J 2026', 'ARS99J-4U-G5'),
        ('D41', 28, 1, 4709000.00, 4709000.00, 4709000.00, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', 'NF700P-4U-G5')
    ) AS it(order_code, id_spct, so_luong, don_gia, gia_goc, gia_sau_giam, ten_sp_snapshot, sku_snapshot)
    JOIN @OrderMap om ON om.order_code = it.order_code;

    COMMIT TRANSACTION;
    PRINT N'==> [SUCCESS] Hoàn tất nạp 100% dữ liệu Demo Thống Kê SMASH-VN an toàn!';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    PRINT N'==> [ERROR] Gặp lỗi trong quá trình nạp Demo Statistics! Đã ROLLBACK toàn bộ transaction.';
    THROW;
END CATCH;
