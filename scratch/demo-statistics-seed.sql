-- ==============================================================================
-- SMASH-VN BADMINTON SHOP - TOÀN BỘ DỮ LIỆU MẪU HỆ THỐNG (DEMO SEED SCRIPT)
-- Mục đích: Khởi tạo dữ liệu toàn diện cho Báo cáo, Đồ án & Thuyết minh:
--   1. Dữ liệu Nhập hàng & Lô hàng (PhieuNhap, PhieuNhapChiTiet) cho TẤT CẢ biến thể SP
--   2. 18 Tài khoản khách hàng & Sổ địa chỉ (TaiKhoan, KhachHang, SoDiaChi)
--   3. 41 Hóa đơn Thống kê Doanh thu (30 ngày qua & kỳ trước)
--   4. 10 Hóa đơn Online với ĐẦY ĐỦ 10 TRẠNG THÁI chuẩn quy trình GHN
--   5. Đánh giá sản phẩm & Hình ảnh thực tế (DanhGia, HinhAnhDanhGia)
--   6. Danh mục từ khóa cấm & Nhật ký vi phạm 4 cấp độ (CommentModerationKeyword, CommentViolationLog)
--   7. 2 Bài viết Blog chuyên sâu & Bình luận tương tác (Blog, BlogComment)
-- ==============================================================================

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    PRINT N'==============================================================================';
    PRINT N'==> [1/7] Khởi tạo Dữ liệu Nhập Hàng & Lô Hàng (PhieuNhap, PhieuNhapChiTiet)...';
    PRINT N'==============================================================================';

    -- Lấy ID nhân viên quản trị đầu tiên trong hệ thống
    DECLARE @ID_NV INT;
    SELECT TOP 1 @ID_NV = id FROM NhanVien ORDER BY id ASC;

    IF @ID_NV IS NULL
    BEGIN
        -- Nếu chưa có nhân viên, tạo tài khoản và nhân viên admin mặc định
        IF NOT EXISTS (SELECT 1 FROM TaiKhoan WHERE username = 'admin_demo_system')
        BEGIN
            INSERT INTO TaiKhoan (username, mat_khau, vai_tro, trang_thai_tai_khoan, ngay_tao)
            VALUES ('admin_demo_system', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'ADMIN', 'ACTIVE', '2026-06-01 08:00:00');
        END;

        DECLARE @ID_TK_NV INT = (SELECT id FROM TaiKhoan WHERE username = 'admin_demo_system');
        INSERT INTO NhanVien (id_tai_khoan, ho_ten, chuc_vu, so_dien_thoai_nv, ngay_tao)
        VALUES (@ID_TK_NV, N'Quản Lý Kho SmashVN', N'Quản trị viên', '0988888888', '2026-06-01 08:00:00');

        SELECT @ID_NV = SCOPE_IDENTITY();
    END;

    -- 1.1 Khởi tạo các Phiếu Nhập mẫu nếu chưa có
    IF NOT EXISTS (SELECT 1 FROM PhieuNhap WHERE ma_phieu_nhap = 'PN20260601-001')
    BEGIN
        INSERT INTO PhieuNhap (ma_phieu_nhap, id_nhan_vien, ngay_nhap, tong_tien, ghi_chu, ngay_tao, ngay_cap_nhat)
        VALUES ('PN20260601-001', @ID_NV, '2026-06-01 09:00:00', 0, N'Nhập lô hàng Vợt & Phụ kiện Yonex, Lining hè 2026', '2026-06-01 09:00:00', '2026-06-01 09:00:00');
    END;

    IF NOT EXISTS (SELECT 1 FROM PhieuNhap WHERE ma_phieu_nhap = 'PN20260701-002')
    BEGIN
        INSERT INTO PhieuNhap (ma_phieu_nhap, id_nhan_vien, ngay_nhap, tong_tien, ghi_chu, ngay_tao, ngay_cap_nhat)
        VALUES ('PN20260701-002', @ID_NV, '2026-07-01 10:30:00', 0, N'Nhập bổ sung lô Giày, Trang phục & Dụng cụ tập luyện', '2026-07-01 10:30:00', '2026-07-01 10:30:00');
    END;

    IF NOT EXISTS (SELECT 1 FROM PhieuNhap WHERE ma_phieu_nhap = 'PN20260801-003')
    BEGIN
        INSERT INTO PhieuNhap (ma_phieu_nhap, id_nhan_vien, ngay_nhap, tong_tien, ghi_chu, ngay_tao, ngay_cap_nhat)
        VALUES ('PN20260801-003', @ID_NV, '2026-08-01 14:15:00', 0, N'Nhập lô hàng chính hãng chuẩn bị cho chiến dịch Flash Sale', '2026-08-01 14:15:00', '2026-08-01 14:15:00');
    END;

    DECLARE @ID_PN1 INT = (SELECT id FROM PhieuNhap WHERE ma_phieu_nhap = 'PN20260601-001');
    DECLARE @ID_PN2 INT = (SELECT id FROM PhieuNhap WHERE ma_phieu_nhap = 'PN20260701-002');
    DECLARE @ID_PN3 INT = (SELECT id FROM PhieuNhap WHERE ma_phieu_nhap = 'PN20260801-003');

    -- 1.2 Tạo Chi Tiết Phiếu Nhập (PhieuNhapChiTiet) cho tất cả biến thể chưa có dữ liệu nhập
    INSERT INTO PhieuNhapChiTiet (id_phieu_nhap, id_san_pham_chi_tiet, so_luong, gia_nhap, thanh_tien)
    SELECT
        CASE 
            WHEN spct.id % 3 = 1 THEN @ID_PN1
            WHEN spct.id % 3 = 2 THEN @ID_PN2
            ELSE @ID_PN3
        END AS id_phieu_nhap,
        spct.id AS id_san_pham_chi_tiet,
        CASE 
            WHEN ISNULL(spct.so_luong_ton, 0) > 0 THEN spct.so_luong_ton + 15
            ELSE 30
        END AS so_luong,
        CASE
            WHEN spct.gia_nhap IS NOT NULL AND spct.gia_nhap > 0 THEN spct.gia_nhap
            WHEN spct.gia_ban IS NOT NULL AND spct.gia_ban > 0 THEN CAST(spct.gia_ban * 0.65 AS DECIMAL(18,2))
            ELSE 1500000.00
        END AS gia_nhap,
        (
            CASE 
                WHEN ISNULL(spct.so_luong_ton, 0) > 0 THEN spct.so_luong_ton + 15
                ELSE 30
            END
        ) * (
            CASE
                WHEN spct.gia_nhap IS NOT NULL AND spct.gia_nhap > 0 THEN spct.gia_nhap
                WHEN spct.gia_ban IS NOT NULL AND spct.gia_ban > 0 THEN CAST(spct.gia_ban * 0.65 AS DECIMAL(18,2))
                ELSE 1500000.00
            END
        ) AS thanh_tien
    FROM SanPhamChiTiet spct
    WHERE spct.id NOT IN (SELECT id_san_pham_chi_tiet FROM PhieuNhapChiTiet);

    -- Cập nhật giá nhập vào SanPhamChiTiet nếu còn thiếu
    UPDATE SanPhamChiTiet
    SET gia_nhap = CAST(gia_ban * 0.65 AS DECIMAL(18,2))
    WHERE gia_nhap IS NULL OR gia_nhap <= 0;

    -- Cập nhật lại tổng tiền trên từng Phiếu Nhập
    UPDATE pn
    SET pn.tong_tien = ISNULL(sub.tong, 0)
    FROM PhieuNhap pn
    JOIN (
        SELECT id_phieu_nhap, SUM(thanh_tien) AS tong
        FROM PhieuNhapChiTiet
        GROUP BY id_phieu_nhap
    ) sub ON sub.id_phieu_nhap = pn.id;

    PRINT N'  -> Đã cập nhật thành công Phiếu Nhập & Chi Tiết Lô Hàng cho toàn bộ biến thể!';

    PRINT N'==============================================================================';
    PRINT N'==> [2/7] Khởi tạo 18 Tài khoản Demo & Sổ Địa Chỉ...';
    PRINT N'==============================================================================';

    -- Xóa dữ liệu demo cũ theo đúng thứ tự ràng buộc khóa ngoại để tránh xung đột FK
    DELETE hdct
    FROM HoaDonChiTiet hdct
    WHERE hdct.id_hoa_don IN (
        SELECT hd.id
        FROM HoaDon hd
        JOIN KhachHang kh ON kh.id = hd.id_khach_hang
        JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
        WHERE tk.username LIKE 'demo_stat_cust_%'
    );

    DELETE hd
    FROM HoaDon hd
    JOIN KhachHang kh ON kh.id = hd.id_khach_hang
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    DELETE hd
    FROM HoaDon hd
    WHERE hd.ghi_chu LIKE 'DEMO_STAT_D%' OR hd.ma_giao_dich LIKE 'DHSVN-%';

    IF OBJECT_ID('GioHangChiTiet', 'U') IS NOT NULL
    BEGIN
        DELETE ghct
        FROM GioHangChiTiet ghct
        WHERE ghct.id_gio_hang IN (
            SELECT gh.id FROM GioHang gh
            JOIN KhachHang kh ON kh.id = gh.id_khach_hang
            JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
            WHERE tk.username LIKE 'demo_stat_cust_%'
        );
    END;

    IF OBJECT_ID('GioHang', 'U') IS NOT NULL
    BEGIN
        DELETE gh
        FROM GioHang gh
        JOIN KhachHang kh ON kh.id = gh.id_khach_hang
        JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
        WHERE tk.username LIKE 'demo_stat_cust_%';
    END;

    DELETE dc
    FROM SoDiaChi dc
    JOIN KhachHang kh ON kh.id = dc.id_khach_hang
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    DELETE kh
    FROM KhachHang kh
    JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan
    WHERE tk.username LIKE 'demo_stat_cust_%';

    DELETE FROM TaiKhoan WHERE username LIKE 'demo_stat_cust_%';

    INSERT INTO TaiKhoan (
        username, mat_khau, vai_tro, trang_thai_tai_khoan,
        so_lan_mua_thanh_cong, so_lan_nhac_nho_vi_pham, ngay_tao
    ) VALUES
    ('demo_stat_cust_01', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 4, 0, '2026-06-20 08:00:00'),
    ('demo_stat_cust_02', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-06-20 08:05:00'),
    ('demo_stat_cust_03', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-06-20 08:10:00'),
    ('demo_stat_cust_04', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:15:00'),
    ('demo_stat_cust_05', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:20:00'),
    ('demo_stat_cust_06', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-06-20 08:25:00'),
    ('demo_stat_cust_07', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:30:00'),
    ('demo_stat_cust_08', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-06-20 08:35:00'),
    ('demo_stat_cust_09', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 3, 0, '2026-07-20 08:00:00'),
    ('demo_stat_cust_10', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:05:00'),
    ('demo_stat_cust_11', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:10:00'),
    ('demo_stat_cust_12', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:15:00'),
    ('demo_stat_cust_13', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:20:00'),
    ('demo_stat_cust_14', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:25:00'),
    ('demo_stat_cust_15', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:30:00'),
    ('demo_stat_cust_16', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-07-20 08:35:00'),
    ('demo_stat_cust_17', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 2, 0, '2026-07-20 08:40:00'),
    ('demo_stat_cust_18', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F78R/y7V.nF8i8s0zP1v8y8F8cKe', 'KH', 'ACTIVE', 1, 0, '2026-07-20 08:45:00');

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

    PRINT N'==============================================================================';
    PRINT N'==> [3/7] Khởi tạo 41 Hóa Đơn Thống Kê & 10 Hóa Đơn Online 10 Trạng Thái...';
    PRINT N'==============================================================================';

    -- Lookup các phương thức thanh toán
    DECLARE @PTTT_COD INT = (SELECT TOP 1 id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'COD' OR UPPER(ten_phuong_thuc) LIKE N'%KHI NHẬN HÀNG%');
    DECLARE @PTTT_VNPAY INT = (SELECT TOP 1 id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'VNPAY' OR UPPER(ten_phuong_thuc) LIKE N'%VNPAY%');
    DECLARE @PTTT_MOMO INT = (SELECT TOP 1 id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'MOMO' OR UPPER(ten_phuong_thuc) LIKE N'%MOMO%');
    DECLARE @PTTT_SEPAY INT = (SELECT TOP 1 id FROM PhuongThucThanhToan WHERE UPPER(ma_phuong_thuc) = 'SEPAY' OR UPPER(ten_phuong_thuc) LIKE N'%SEPAY%');

    IF @PTTT_COD IS NULL SET @PTTT_COD = (SELECT TOP 1 id FROM PhuongThucThanhToan ORDER BY id ASC);
    IF @PTTT_VNPAY IS NULL SET @PTTT_VNPAY = @PTTT_COD;
    IF @PTTT_MOMO IS NULL SET @PTTT_MOMO = @PTTT_VNPAY;
    IF @PTTT_SEPAY IS NULL SET @PTTT_SEPAY = @PTTT_VNPAY;

    -- Lookup Đơn vị vận chuyển GHN & Tại Quầy
    DECLARE @ID_DVVC_GHN INT = (SELECT TOP 1 id FROM DonViVanChuyen WHERE UPPER(ma_don_vi) = 'GHN' OR UPPER(ten_don_vi) LIKE N'%GIAO HÀNG NHANH%');
    IF @ID_DVVC_GHN IS NULL SET @ID_DVVC_GHN = (SELECT TOP 1 id FROM DonViVanChuyen ORDER BY id ASC);

    DECLARE @ID_DVVC_TAIQUAY INT = (SELECT TOP 1 id FROM DonViVanChuyen WHERE UPPER(ma_don_vi) = 'TAIQUAY' OR UPPER(ten_don_vi) LIKE N'%QUẦY%' OR UPPER(ten_don_vi) LIKE N'%QUAY%');
    IF @ID_DVVC_TAIQUAY IS NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM DonViVanChuyen WHERE ma_don_vi = 'TAIQUAY')
        BEGIN
            INSERT INTO DonViVanChuyen (ma_don_vi, ten_don_vi, so_hotline, dia_chi_kho, phi_noi_dia, phi_toan_quoc)
            VALUES ('TAIQUAY', N'Mua tại quầy', '19008888', N'Tại cửa hàng SmashVN', 0, 0);
        END;
        SET @ID_DVVC_TAIQUAY = (SELECT TOP 1 id FROM DonViVanChuyen WHERE ma_don_vi = 'TAIQUAY');
    END;
    IF @ID_DVVC_TAIQUAY IS NULL SET @ID_DVVC_TAIQUAY = @ID_DVVC_GHN;

    -- Lấy 2 biến thể sản phẩm đầu tiên để gán chi tiết đơn hàng
    DECLARE @SPCT_1 INT = (SELECT TOP 1 id FROM SanPhamChiTiet ORDER BY id ASC);
    DECLARE @SPCT_2 INT = (SELECT TOP 1 id FROM SanPhamChiTiet WHERE id <> @SPCT_1 ORDER BY id ASC);
    IF @SPCT_2 IS NULL SET @SPCT_2 = @SPCT_1;

    -- Bảng tạm mapping khách hàng
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

    -- 3.1 Nạp 41 Hóa đơn Thống kê
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
        NULL, @ID_DVVC_GHN, m.id_dia_chi, d.ngay_tao, d.ngay_thanh_toan,
        d.tien_hang, d.phi_van_chuyen, d.so_tien_giam_gia, d.tong_tien,
        d.trang_thai_don_hang, d.trang_thai_thanh_toan, m.ho_ten_kh,
        m.sdt_kh, d.email, m.dia_chi, d.ly_do_huy,
        'DEMO_STAT_' + d.order_code, d.ma_giao_dich
    FROM (VALUES
        -- KỲ TRƯỚC (D01 -> D20)
        ('D01', 'C01', 'SEPAY', '2026-06-22 09:15:00', '2026-06-22 09:16:30', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202606220915_C01'),
        ('D02', 'C02', 'VNPAY', '2026-06-24 14:20:00', '2026-06-24 14:21:45', 5650000.00, 30000.00, 0.00, 5680000.00, N'da_giao', N'DA_THANH_TOAN', 'binh.tran@example.com', NULL, 'VNP_DEMO_202606241420_C02'),
        ('D03', 'C03', 'COD',   '2026-06-26 10:30:00', '2026-06-28 11:00:00', 4190000.00, 30000.00, 0.00, 4220000.00, N'da_giao', N'DA_THANH_TOAN', 'cuong.le@example.com', NULL, NULL),
        ('D04', 'C04', 'MOMO',  '2026-06-28 16:45:00', '2026-06-28 16:46:12', 4709000.00, 30000.00, 0.00, 4739000.00, N'da_giao', N'DA_THANH_TOAN', 'duc.pham@example.com', NULL, 'MM_DEMO_202606281645_C04'),
        ('D05', 'C05', 'SEPAY', '2026-06-30 11:10:00', '2026-06-30 11:11:05', 4490000.00, 30000.00, 0.00, 4520000.00, N'da_giao', N'DA_THANH_TOAN', 'hanh.do@example.com', NULL, 'SP_DEMO_202606301110_C05'),
        ('D06', 'C06', 'COD',   '2026-07-02 08:50:00', '2026-07-04 09:30:00', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'khoi.hoang@example.com', NULL, NULL),
        ('D07', 'C07', 'VNPAY', '2026-07-03 15:30:00', '2026-07-03 15:31:20', 5650000.00, 30000.00, 0.00, 5680000.00, N'da_giao', N'DA_THANH_TOAN', 'lan.vu@example.com', NULL, 'VNP_DEMO_202607031530_C07'),
        ('D08', 'C08', 'SEPAY', '2026-07-05 19:20:00', '2026-07-05 19:22:10', 4190000.00, 30000.00, 0.00, 4220000.00, N'da_giao', N'DA_THANH_TOAN', 'nam.bui@example.com', NULL, 'SP_DEMO_202607051920_C08'),
        ('D09', 'C01', 'SEPAY', '2026-07-07 10:15:00', '2026-07-07 10:16:00', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202607071015_C01'),
        ('D10', 'C02', 'MOMO',  '2026-07-09 14:00:00', '2026-07-09 14:01:30', 4709000.00, 30000.00, 0.00, 4739000.00, N'da_giao', N'DA_THANH_TOAN', 'binh.tran@example.com', NULL, 'MM_DEMO_202607091400_C02'),
        ('D11', 'C03', 'COD',   '2026-07-11 17:35:00', '2026-07-13 18:00:00', 5650000.00, 30000.00, 0.00, 5680000.00, N'da_giao', N'DA_THANH_TOAN', 'cuong.le@example.com', NULL, NULL),
        ('D12', 'C04', 'VNPAY', '2026-07-13 09:40:00', '2026-07-13 09:42:00', 4490000.00, 30000.00, 0.00, 4520000.00, N'da_giao', N'DA_THANH_TOAN', 'duc.pham@example.com', NULL, 'VNP_DEMO_202607130940_C04'),
        ('D13', 'C05', 'SEPAY', '2026-07-15 13:25:00', '2026-07-15 13:26:40', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'hanh.do@example.com', NULL, 'SP_DEMO_202607151325_C05'),
        ('D14', 'C06', 'COD',   '2026-07-17 16:10:00', '2026-07-19 16:40:00', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'khoi.hoang@example.com', NULL, NULL),
        ('D15', 'C07', 'SEPAY', '2026-07-19 20:05:00', '2026-07-19 20:06:15', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'lan.vu@example.com', NULL, 'SP_DEMO_202607192005_C07'),
        ('D16', 'C08', 'COD',   '2026-06-25 11:00:00', NULL, 4190000.00, 30000.00, 0.00, 4220000.00, N'dang_giao', N'CHO_THANH_TOAN', 'nam.bui@example.com', NULL, NULL),
        ('D17', 'C01', 'SEPAY', '2026-07-10 15:00:00', NULL, 5099000.00, 30000.00, 0.00, 5129000.00, N'cho_xac_nhan', N'CHO_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202607101500_C01'),
        ('D18', 'C02', 'COD',   '2026-06-27 09:30:00', NULL, 4709000.00, 30000.00, 0.00, 4739000.00, N'da_huy', N'CHO_THANH_TOAN', 'binh.tran@example.com', N'Khách đổi ý muốn mua màu khác', NULL),
        ('D19', 'C03', 'COD',   '2026-07-06 14:40:00', NULL, 5650000.00, 30000.00, 0.00, 5680000.00, N'da_huy', N'CHO_THANH_TOAN', 'cuong.le@example.com', N'Trùng đơn hàng', NULL),
        ('D20', 'C04', 'COD',   '2026-07-18 18:20:00', NULL, 4490000.00, 30000.00, 0.00, 4520000.00, N'da_huy', N'CHO_THANH_TOAN', 'duc.pham@example.com', N'Thời gian giao hàng lâu', NULL),

        -- KỲ HIỆN TẠI (D21 -> D41)
        ('D21', 'C09', 'SEPAY', '2026-07-22 09:30:00', '2026-07-22 09:31:15', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'ngoc.phan@example.com', NULL, 'SP_DEMO_202607220930_C09'),
        ('D22', 'C10', 'VNPAY', '2026-07-24 14:15:00', '2026-07-24 14:16:40', 4190000.00, 30000.00, 0.00, 4220000.00, N'da_giao', N'DA_THANH_TOAN', 'phuong.trinh@example.com', NULL, 'VNP_DEMO_202607241415_C10'),
        ('D23', 'C11', 'COD',   '2026-07-26 10:45:00', '2026-07-28 11:30:00', 5650000.00, 30000.00, 0.00, 5680000.00, N'da_giao', N'DA_THANH_TOAN', 'quan.dang@example.com', NULL, NULL),
        ('D24', 'C12', 'MOMO',  '2026-07-27 16:20:00', '2026-07-27 16:21:10', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'quynh.mai@example.com', NULL, 'MM_DEMO_202607271620_C12'),
        ('D25', 'C01', 'SEPAY', '2026-07-29 11:05:00', '2026-07-29 11:06:20', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'an.nguyen@example.com', NULL, 'SP_DEMO_202607291105_C01'),
        ('D26', 'C13', 'VNPAY', '2026-07-31 15:40:00', '2026-07-31 15:41:50', 4709000.00, 30000.00, 0.00, 4739000.00, N'da_giao', N'DA_THANH_TOAN', 'son.ha@example.com', NULL, 'VNP_DEMO_202607311540_C13'),
        ('D27', 'C14', 'SEPAY', '2026-08-02 08:30:00', '2026-08-02 08:31:05', 4490000.00, 30000.00, 0.00, 4520000.00, N'da_giao', N'DA_THANH_TOAN', 'tam.ngo@example.com', NULL, 'SP_DEMO_202608020830_C14'),
        ('D28', 'C15', 'COD',   '2026-08-04 19:15:00', '2026-08-06 20:00:00', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'tuan.duong@example.com', NULL, NULL),
        ('D29', 'C16', 'MOMO',  '2026-08-06 10:50:00', '2026-08-06 10:51:30', 4190000.00, 30000.00, 0.00, 4220000.00, N'da_giao', N'DA_THANH_TOAN', 'uyen.vo@example.com', NULL, 'MM_DEMO_202608061050_C16'),
        ('D30', 'C17', 'SEPAY', '2026-08-08 14:35:00', '2026-08-08 14:36:25', 5650000.00, 30000.00, 0.00, 5680000.00, N'da_giao', N'DA_THANH_TOAN', 'vinh.truong@example.com', NULL, 'SP_DEMO_202608081435_C17'),
        ('D31', 'C18', 'COD',   '2026-08-09 17:00:00', '2026-08-11 17:45:00', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'yen.lam@example.com', NULL, NULL),
        ('D32', 'C03', 'VNPAY', '2026-08-11 09:20:00', '2026-08-11 09:22:15', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'cuong.le@example.com', NULL, 'VNP_DEMO_202608110920_C03'),
        ('D33', 'C09', 'SEPAY', '2026-08-12 13:45:00', '2026-08-12 13:46:30', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'ngoc.phan@example.com', NULL, 'SP_DEMO_202608121345_C09'),
        ('D34', 'C10', 'MOMO',  '2026-08-14 16:10:00', '2026-08-14 16:11:40', 4709000.00, 30000.00, 0.00, 4739000.00, N'da_giao', N'DA_THANH_TOAN', 'phuong.trinh@example.com', NULL, 'MM_DEMO_202608141610_C10'),
        ('D35', 'C11', 'SEPAY', '2026-08-15 11:25:00', '2026-08-15 11:26:10', 4190000.00, 30000.00, 0.00, 4220000.00, N'da_giao', N'DA_THANH_TOAN', 'quan.dang@example.com', NULL, 'SP_DEMO_202608151125_C11'),
        ('D36', 'C12', 'COD',   '2026-08-16 15:50:00', '2026-08-18 16:20:00', 5650000.00, 30000.00, 0.00, 5680000.00, N'da_giao', N'DA_THANH_TOAN', 'quynh.mai@example.com', NULL, NULL),
        ('D37', 'C13', 'VNPAY', '2026-08-17 08:40:00', '2026-08-17 08:41:20', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'son.ha@example.com', NULL, 'VNP_DEMO_202608170840_C13'),
        ('D38', 'C06', 'SEPAY', '2026-08-17 14:10:00', '2026-08-17 14:11:15', 5099000.00, 30000.00, 0.00, 5129000.00, N'da_giao', N'DA_THANH_TOAN', 'khoi.hoang@example.com', NULL, 'SP_DEMO_202608171410_C06'),
        ('D39', 'C14', 'MOMO',  '2026-08-18 10:00:00', '2026-08-18 10:01:45', 300000.00, 30000.00, 0.00, 330000.00, N'da_giao', N'DA_THANH_TOAN', 'tam.ngo@example.com', NULL, 'MM_DEMO_202608181000_C14'),
        ('D40', 'C15', 'COD',   '2026-08-13 10:30:00', NULL, 4190000.00, 30000.00, 0.00, 4220000.00, N'dang_giao', N'CHO_THANH_TOAN', 'tuan.duong@example.com', NULL, NULL),
        ('D41', 'C16', 'COD',   '2026-08-07 16:00:00', NULL, 4709000.00, 30000.00, 0.00, 4739000.00, N'da_huy', N'CHO_THANH_TOAN', 'uyen.vo@example.com', N'Khách muốn hủy đặt lại', NULL)
    ) AS d(order_code, cust_code, pttt_code, ngay_tao, ngay_thanh_toan, tien_hang, phi_van_chuyen, so_tien_giam_gia, tong_tien, trang_thai_don_hang, trang_thai_thanh_toan, email, ly_do_huy, ma_giao_dich)
    JOIN @CustMap m ON m.cust_code = d.cust_code;

    -- Gán chi tiết HoaDonChiTiet cho 41 đơn thống kê
    INSERT INTO HoaDonChiTiet (
        id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia,
        gia_goc, gia_sau_giam, ten_san_pham_snapshot, sku_snapshot, ngay_tao
    )
    SELECT
        hd.id,
        @SPCT_1,
        1,
        hd.tien_hang,
        hd.tien_hang,
        hd.tien_hang,
        N'Vợt Cầu Lông Cao Cấp SmashVN',
        'SKU-SPCT-' + CAST(@SPCT_1 AS NVARCHAR(10)),
        hd.ngay_tao
    FROM HoaDon hd
    WHERE hd.ghi_chu LIKE 'DEMO_STAT_D%'
      AND hd.id NOT IN (SELECT id_hoa_don FROM HoaDonChiTiet);

    -- 3.2 Nạp 10 Đơn hàng Bán Online với ĐẦY ĐỦ 10 TRẠNG THÁI (Hiển thị tab Bán Online)
    DECLARE @OnlineOrders TABLE (
        stt INT,
        ma_giao_dich NVARCHAR(100),
        trang_thai_don NVARCHAR(50),
        trang_thai_tt NVARCHAR(50),
        ghi_chu NVARCHAR(500),
        ly_do_huy NVARCHAR(500),
        ly_do_hoan NVARCHAR(500)
    );

    INSERT INTO @OnlineOrders VALUES
    (1,  'DHSVN-001', 'cho_xac_nhan',        'CHO_THANH_TOAN', N'Khách hẹn giao trong giờ hành chính', NULL, NULL),
    (2,  'DHSVN-002', 'cho_thanh_toan',       'CHO_THANH_TOAN', N'Đơn thanh toán VNPAY đang chờ khách quét QR', NULL, NULL),
    (3,  'DHSVN-003', 'da_xac_nhan',          'CHO_THANH_TOAN', N'Đã liên hệ xác nhận đơn hàng thành công', NULL, NULL),
    (4,  'DHSVN-004', 'dang_chuan_bi_hang',   'DA_THANH_TOAN',  N'Đang lấy hàng và kiểm tra tại kho', NULL, NULL),
    (5,  'DHSVN-005', 'san_sang_giao',        'DA_THANH_TOAN',  N'Đã đóng gói thùng carton niêm phong SmashVN', NULL, NULL),
    (6,  'DHSVN-006', 'da_ban_giao_ghn',      'CHO_THANH_TOAN', N'Đã xuất kho và bàn giao bưu tá Giao Hàng Nhanh', NULL, NULL),
    (7,  'DHSVN-007', 'dang_giao',            'CHO_THANH_TOAN', N'Shipper đang trên đường phát hàng tới khách', NULL, NULL),
    (8,  'DHSVN-008', 'da_giao',              'DA_THANH_TOAN',  N'Giao hàng và thu tiền thành công', NULL, NULL),
    (9,  'DHSVN-009', 'giao_that_bai',        'CHO_THANH_TOAN', N'Khách hàng hẹn dời lịch giao lại do bận đi công tác', NULL, N'Khách hẹn giao lại ngày mai'),
    (10, 'DHSVN-010', 'da_huy',               'CHO_THANH_TOAN', N'Khách yêu cầu hủy để đổi mẫu vợt khác', N'Khách đổi ý muốn mua màu khác', NULL);

    INSERT INTO HoaDon (
        id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, id_phieu_giam_gia,
        id_don_vi_van_chuyen, id_dia_chi, ngay_tao, ngay_thanh_toan,
        tien_hang, phi_van_chuyen, so_tien_giam_gia, tong_tien,
        trang_thai_don_hang, trang_thai_thanh_toan, ten_nguoi_nhan,
        sdt_nhan, email_nguoi_nhan, dia_chi_nhan, ly_do_huy, ly_do_hoan_tra,
        ghi_chu, ma_giao_dich
    )
    SELECT
        m.id_khach_hang, NULL, @PTTT_COD, NULL,
        @ID_DVVC_GHN, m.id_dia_chi, DATEADD(DAY, -o.stt, GETDATE()), 
        CASE WHEN o.trang_thai_tt = 'DA_THANH_TOAN' THEN DATEADD(DAY, -o.stt, GETDATE()) ELSE NULL END,
        3500000.00, 30000.00, 0.00, 3530000.00,
        o.trang_thai_don, o.trang_thai_tt, m.ho_ten_kh,
        m.sdt_kh, 'online.cust' + CAST(o.stt AS VARCHAR(5)) + '@example.com', m.dia_chi,
        o.ly_do_huy, o.ly_do_hoan,
        o.ghi_chu, o.ma_giao_dich
    FROM @OnlineOrders o
    JOIN @CustMap m ON m.cust_code = 'C' + RIGHT('0' + CAST(o.stt AS VARCHAR(2)), 2);

    -- Chi tiết cho 10 đơn online
    INSERT INTO HoaDonChiTiet (
        id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia,
        gia_goc, gia_sau_giam, ten_san_pham_snapshot, sku_snapshot, ngay_tao
    )
    SELECT
        hd.id, @SPCT_1, 1, 3500000.00, 3500000.00, 3500000.00,
        N'Vợt Cầu Lông Yonex Astrox Cao Cấp', 'SPCT-' + CAST(@SPCT_1 AS NVARCHAR(10)), hd.ngay_tao
    FROM HoaDon hd
    WHERE hd.ma_giao_dich LIKE 'DHSVN-%'
      AND hd.id NOT IN (SELECT id_hoa_don FROM HoaDonChiTiet);

    -- 3.3 Nạp Hóa đơn Bán Hàng Tại Quầy (POS - Chỉ có trạng thái ĐÃ THANH TOÁN và ĐÃ HỦY)
    DECLARE @PosOrders TABLE (
        stt INT,
        ma_giao_dich NVARCHAR(100),
        pttt_id INT,
        trang_thai_don NVARCHAR(50),
        trang_thai_tt NVARCHAR(50),
        tien_hang DECIMAL(18,2),
        so_tien_giam DECIMAL(18,2),
        tong_tien DECIMAL(18,2),
        ghi_chu NVARCHAR(500),
        ly_do_huy NVARCHAR(500)
    );

    INSERT INTO @PosOrders VALUES
    (1,  'POS-TM-001', @PTTT_COD,   'da_giao', 'DA_THANH_TOAN',  1850000.00,      0.00, 1850000.00, N'Bán trực tiếp tại quầy thu ngân (Tiền mặt)', NULL),
    (2,  'POS-CK-002', @PTTT_SEPAY, 'da_giao', 'DA_THANH_TOAN',   150000.00,  50000.00,  100000.00, N'Bán trực tiếp tại quầy thu ngân (Quét VietQR SePay - Áp voucher)', NULL),
    (3,  'POS-TM-003', @PTTT_COD,   'da_giao', 'DA_THANH_TOAN',   450000.00,      0.00,  450000.00, N'Khách mua phụ kiện quấn cán và cước tại quầy', NULL),
    (4,  'POS-VN-004', @PTTT_VNPAY, 'da_giao', 'DA_THANH_TOAN',  4890000.00, 100000.00, 4790000.00, N'Khách quẹt thẻ VNPAY-QR tại quầy', NULL),
    (5,  'POS-TM-005', @PTTT_COD,   'da_giao', 'DA_THANH_TOAN',  2190000.00,      0.00, 2190000.00, N'Bán tại quầy - Thanh toán tiền mặt đủ', NULL),
    (6,  'POS-CK-006', @PTTT_SEPAY, 'da_giao', 'DA_THANH_TOAN',  5450000.00,      0.00, 5450000.00, N'Khách chuyển khoản ngân hàng SePay tại quầy', NULL),
    (7,  'POS-MM-007', @PTTT_MOMO,  'da_giao', 'DA_THANH_TOAN',   980000.00,      0.00,  980000.00, N'Khách thanh toán Ví MoMo tại quầy', NULL),
    (8,  'POS-TM-008', @PTTT_COD,   'da_giao', 'DA_THANH_TOAN',  3600000.00,      0.00, 3600000.00, N'Bán lẻ vợt kèm cước căng sẵn tại quầy', NULL),
    (9,  'POS-HUY-009',@PTTT_COD,   'da_huy',  'CHO_THANH_TOAN', 2800000.00,      0.00, 2800000.00, N'Khách quên mang ví và chưa chuyển khoản được', N'Khách chưa thanh toán được tại quầy'),
    (10, 'POS-HUY-010',@PTTT_COD,   'da_huy',  'CHO_THANH_TOAN', 1500000.00,      0.00, 1500000.00, N'Khách chọn nhầm thông số muốn đổi mẫu khác', N'Hủy đơn tại quầy để tạo đơn mới');

    INSERT INTO HoaDon (
        id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, id_phieu_giam_gia,
        id_don_vi_van_chuyen, id_dia_chi, ngay_tao, ngay_thanh_toan,
        tien_hang, phi_van_chuyen, so_tien_giam_gia, tong_tien,
        trang_thai_don_hang, trang_thai_thanh_toan, ten_nguoi_nhan,
        sdt_nhan, email_nguoi_nhan, dia_chi_nhan, ly_do_huy, ly_do_hoan_tra,
        ghi_chu, ma_giao_dich
    )
    SELECT
        m.id_khach_hang, @ID_NV, p.pttt_id, NULL,
        @ID_DVVC_TAIQUAY, NULL, DATEADD(HOUR, -p.stt * 3, GETDATE()),
        CASE WHEN p.trang_thai_tt = 'DA_THANH_TOAN' THEN DATEADD(HOUR, -p.stt * 3, GETDATE()) ELSE NULL END,
        p.tien_hang, 0.00, p.so_tien_giam, p.tong_tien,
        p.trang_thai_don, p.trang_thai_tt, m.ho_ten_kh,
        m.sdt_kh, NULL, N'Bán tại quầy',
        p.ly_do_huy, NULL,
        p.ghi_chu, p.ma_giao_dich
    FROM @PosOrders p
    JOIN @CustMap m ON m.cust_code = 'C' + RIGHT('0' + CAST(p.stt AS VARCHAR(2)), 2);

    -- Chi tiết cho 10 hóa đơn bán hàng tại quầy (Snapshot đầy đủ tên và thuộc tính hiển thị)
    INSERT INTO HoaDonChiTiet (
        id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia,
        gia_goc, gia_sau_giam, ten_san_pham_snapshot, sku_snapshot, thuoc_tinh_snapshot, ngay_tao
    )
    SELECT
        hd.id, @SPCT_1, 1, hd.tien_hang, hd.tien_hang, hd.tien_hang,
        CASE (hd.id % 3)
            WHEN 0 THEN N'Vợt Cầu Lông Yonex Astrox 88D Pro'
            WHEN 1 THEN N'Dây Cước Căng Vợt Yonex BG 66 Ultimax'
            ELSE N'Vợt Cầu Lông Lining Axforce 80'
        END,
        'POS-SPCT-' + CAST(@SPCT_1 AS NVARCHAR(10)),
        CASE (hd.id % 3)
            WHEN 0 THEN N'4U-G5 | Đỏ/Đen | Sức căng: 11.5 kg'
            WHEN 1 THEN N'0.65mm | Trắng | Căng 11.0 kg'
            ELSE N'4U-G5 | Đen/Vàng | Sức căng: 11.0 kg'
        END,
        hd.ngay_tao
    FROM HoaDon hd
    WHERE hd.ma_giao_dich LIKE 'POS-%'
      AND hd.id NOT IN (SELECT id_hoa_don FROM HoaDonChiTiet);

    PRINT N'  -> Đã tạo thành công Đơn Bán Online (10 trạng thái) và Hóa đơn Bán Tại Quầy (Đã thanh toán/Đã hủy)!';

    PRINT N'==============================================================================';
    PRINT N'==> [4/7] Khởi tạo Đánh Giá Sản Phẩm & Hình Ảnh Đánh Giá (DanhGia)...';
    PRINT N'==============================================================================';

    -- Lấy 1 khách hàng và 1 sản phẩm
    DECLARE @ID_KH_REVIEW INT = (SELECT TOP 1 id FROM KhachHang ORDER BY id ASC);
    DECLARE @ID_SP_REVIEW INT = (SELECT TOP 1 id FROM SanPham ORDER BY id ASC);

    IF @ID_KH_REVIEW IS NOT NULL AND @ID_SP_REVIEW IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM DanhGia WHERE id_khach_hang = @ID_KH_REVIEW AND id_san_pham = @ID_SP_REVIEW AND da_xoa = 0)
        BEGIN
            INSERT INTO DanhGia (id_khach_hang, id_san_pham, so_sao, noi_dung, binh_luan_an, hinh_anh_an, da_xoa, ngay_tao, ngay_cap_nhat)
            VALUES (@ID_KH_REVIEW, @ID_SP_REVIEW, 5.0, N'Vợt đánh cực kỳ thoát tay, phông cầu đầm và đập cầu rất cắm. Căng cước 11kg đánh tiếng nổ đanh vang, shop đóng gói hàng rất kỹ có ống chống sốc. 10/10!', 0, 0, 0, GETDATE(), GETDATE());

            DECLARE @ID_DG_NEW INT = SCOPE_IDENTITY();
            INSERT INTO HinhAnhDanhGia (id_danh_gia, url_hinh_anh, ngay_tao)
            VALUES (@ID_DG_NEW, '/images/products/product-1.jpg', GETDATE());
        END;
    END;

    PRINT N'==============================================================================';
    PRINT N'==> [5/7] Khởi tạo Từ Khóa Cấm & Nhật Ký Vi Phạm (CommentModerationKeyword)...';
    PRINT N'==============================================================================';

    INSERT INTO CommentModerationKeyword (tu_khoa, kich_hoat, ngay_tao)
    SELECT k.tu_khoa, 1, GETDATE()
    FROM (VALUES
        (N'lừa đảo'), (N'lua dao'), (N'hàng giả'), (N'hang gia'), (N'fake'),
        (N'chửi thề'), (N'dm'), (N'vkl'), (N'cờ bạc'), (N'vay tiền'), (N'quảng cáo')
    ) AS k(tu_khoa)
    WHERE k.tu_khoa NOT IN (SELECT tu_khoa FROM CommentModerationKeyword);

    DECLARE @ID_TK_VIOLATION INT = (SELECT TOP 1 id FROM TaiKhoan WHERE vai_tro = 'KH' ORDER BY id ASC);
    IF @ID_TK_VIOLATION IS NOT NULL AND @ID_SP_REVIEW IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM CommentViolationLog WHERE id_tai_khoan = @ID_TK_VIOLATION)
        BEGIN
            INSERT INTO CommentViolationLog (id_tai_khoan, id_san_pham, noi_dung_goc, noi_dung_da_loc, muc_do_vi_pham, so_lan_vi_pham, thoi_han_khoa, ngay_vi_pham, ngay_tao)
            VALUES
            (@ID_TK_VIOLATION, @ID_SP_REVIEW, N'Shop này bán hàng lừa đảo người tiêu dùng!', N'Shop này bán hàng *** người tiêu dùng!', 'LOW', 1, N'Cảnh cáo lần 1', DATEADD(DAY, -5, GETDATE()), GETDATE()),
            (@ID_TK_VIOLATION, @ID_SP_REVIEW, N'Hàng fake kém chất lượng vkl shop ơi', N'Hàng *** kém chất lượng *** shop ơi', 'MEDIUM', 2, N'Tạm khóa bình luận 24 giờ', DATEADD(DAY, -3, GETDATE()), GETDATE()),
            (@ID_TK_VIOLATION, @ID_SP_REVIEW, N'Đồ lừa đảo hàng giả mà cũng bán được', N'Đồ *** *** mà cũng bán được', 'HIGH', 3, N'Khóa bình luận 7 ngày', DATEADD(DAY, -1, GETDATE()), GETDATE()),
            (@ID_TK_VIOLATION, @ID_SP_REVIEW, N'dm shop lừa đảo cờ bạc vay tiền', N'*** shop *** *** ***', 'CRITICAL', 4, N'Khóa tài khoản vĩnh viễn', GETDATE(), GETDATE());
        END;
    END;

    PRINT N'==============================================================================';
    PRINT N'==> [6/7] Khởi tạo 2 Bài Viết Blog Chuyên Sâu & Bình Luận (Blog, BlogComment)...';
    PRINT N'==============================================================================';

    DECLARE @ID_TK_ADMIN INT = (SELECT TOP 1 id FROM TaiKhoan WHERE vai_tro = 'ADMIN' ORDER BY id ASC);
    IF @ID_TK_ADMIN IS NULL SET @ID_TK_ADMIN = (SELECT TOP 1 id FROM TaiKhoan ORDER BY id ASC);

    IF NOT EXISTS (SELECT 1 FROM Blog WHERE duong_dan = 'top-5-vot-cau-long-cong-thu-toan-dien-dang-mua-nhat-2026')
    BEGIN
        INSERT INTO Blog (
            id_tai_khoan, tieu_de, duong_dan, tom_tat, noi_dung,
            hinh_anh, danh_muc, the, trang_thai, ngay_dang, da_xoa, ngay_tao, ngay_cap_nhat, updated_by
        ) VALUES (
            @ID_TK_ADMIN,
            N'Top 5 Cây Vợt Cầu Lông Công Thủ Toàn Diện Đáng Mua Nhất 2026',
            'top-5-vot-cau-long-cong-thu-toan-dien-dang-mua-nhat-2026',
            N'Khám phá danh sách 5 mẫu vợt cầu lông công thủ toàn diện được các lông thủ phong trào và bán chuyên săn đón nhiều nhất năm 2026.',
            N'<h3>1. Yonex Arcsaber 11 Pro</h3><p>Được mệnh danh là ông vua điều cầu, Arcsaber 11 Pro mang lại khả năng kiểm soát cầu tuyệt đối với khung vợt linh hoạt.</p><h3>2. Lining Axforce 80</h3><p>Cây vợt cân bằng hoàn hảo giữa sức mạnh tấn công và tốc độ phản tạt nhanh gọn trong đánh đôi.</p><h3>3. Victor DriveX 9X</h3><p>Khung vợt khí động học giúp tăng tốc độ vung vợt và độ ổn định khi phòng thủ bền bỉ.</p>',
            '/images/blog/blog-1.jpg',
            N'Tư Vấn Chọn Vợt',
            N'yonex,lining,victor,vot-cau-long',
            'PUBLISHED',
            CAST(GETDATE() AS DATE),
            0,
            GETDATE(),
            GETDATE(),
            N'Ban Biên Tập SmashVN'
        );

        DECLARE @ID_BLOG_1 INT = SCOPE_IDENTITY();
        INSERT INTO BlogComment (id_blog, id_tai_khoan, noi_dung, da_xoa, ngay_tao)
        VALUES (@ID_BLOG_1, @ID_TK_ADMIN, N'Bài viết phân tích rất chi tiết và đúng nhu cầu của anh em chơi phong trào!', 0, GETDATE());
    END;

    IF NOT EXISTS (SELECT 1 FROM Blog WHERE duong_dan = 'bi-quyet-thuc-hien-cu-smash-dap-cau-uy-luc-chuan-van-dong-vien')
    BEGIN
        INSERT INTO Blog (
            id_tai_khoan, tieu_de, duong_dan, tom_tat, noi_dung,
            hinh_anh, danh_muc, the, trang_thai, ngay_dang, da_xoa, ngay_tao, ngay_cap_nhat, updated_by
        ) VALUES (
            @ID_TK_ADMIN,
            N'Bí Quyết Thực Hiện Cú Smash (Đập Cầu) Uy Lực Chuẩn Vận Động Viên',
            'bi-quyet-thuc-hien-cu-smash-dap-cau-uy-luc-chuan-van-dong-vien',
            N'Hướng dẫn chi tiết từng bước phát lực từ hông, vai đến cổ tay để tạo ra cú đập cầu uy lực cắm sàn như vận động viên chuyên nghiệp.',
            N'<h3>Bước 1: Di chuyển bộ chân và tư thế chuẩn bị</h3><p>Luôn đón cầu ở phía trước mặt, không để cầu rơi quá sâu ra phía sau lưng.</p><h3>Bước 2: Mở vai và dẫn vợt</h3><p>Xoay hông tạo góc 90 độ, đưa cùi chỏ lên cao để chuẩn bị vung vợt giải phóng lực tối đa.</p><h3>Bước 3: Phát lực tiếp xúc cầu và gập cổ tay</h3><p>Tiếp xúc cầu ở điểm cao nhất, kết hợp gập cổ tay nhanh và dứt khoát để cầu đi cắm sàn hiểm hóc.</p>',
            '/images/blog/blog-2.jpg',
            N'Kỹ Thuật Cầu Lông',
            N'ky-thuat,smash,dap-cau,tap-luyen',
            'PUBLISHED',
            CAST(GETDATE() AS DATE),
            0,
            GETDATE(),
            GETDATE(),
            N'HLV Cầu Lông SmashVN'
        );

        DECLARE @ID_BLOG_2 INT = SCOPE_IDENTITY();
        INSERT INTO BlogComment (id_blog, id_tai_khoan, noi_dung, da_xoa, ngay_tao)
        VALUES (@ID_BLOG_2, @ID_TK_ADMIN, N'Cảm ơn shop đã chia sẻ kỹ thuật rất dễ hiểu, mình đã áp dụng và thấy lực đập cải thiện rõ!', 0, GETDATE());
    END;

    PRINT N'==============================================================================';
    PRINT N'==> [7/7] HOÀN TẤT NẠP 100% DỮ LIỆU DEMO SMASH-VN THÀNH CÔNG RỰC RỠ!';
    PRINT N'==============================================================================';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    PRINT N'==> [ERROR] Gặp lỗi trong quá trình thực thi seed data! Đã ROLLBACK.';
    THROW;
END CATCH;

