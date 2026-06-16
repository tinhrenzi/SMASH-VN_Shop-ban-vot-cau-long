-- 1. Thêm các trường snapshot mới vào bảng HoaDonChiTiet
ALTER TABLE HoaDonChiTiet ADD gia_niem_yet DECIMAL(18, 2) NULL;
ALTER TABLE HoaDonChiTiet ADD phan_tram_giam DECIMAL(5, 2) NULL;
ALTER TABLE HoaDonChiTiet ADD so_tien_giam_san_pham DECIMAL(18, 2) NULL;
ALTER TABLE HoaDonChiTiet ADD ten_dot_giam_gia NVARCHAR(100) NULL;
ALTER TABLE HoaDonChiTiet ADD id_dot_giam_gia INT NULL;

ALTER TABLE HoaDonChiTiet ADD ten_san_pham_snapshot NVARCHAR(255) NULL;
ALTER TABLE HoaDonChiTiet ADD sku_snapshot NVARCHAR(100) NULL;
ALTER TABLE HoaDonChiTiet ADD thuoc_tinh_snapshot NVARCHAR(500) NULL;
ALTER TABLE HoaDonChiTiet ADD thuong_hieu_snapshot NVARCHAR(100) NULL;
ALTER TABLE HoaDonChiTiet ADD danh_muc_snapshot NVARCHAR(100) NULL;

-- Thêm các trường lưu chi tiết chiết khấu voucher trực tiếp vào bảng HoaDon
ALTER TABLE HoaDon ADD so_tien_giam_voucher DECIMAL(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE HoaDon ADD ma_voucher_ap_dung NVARCHAR(50) NULL;
ALTER TABLE HoaDon ADD ten_voucher_ap_dung NVARCHAR(255) NULL;
ALTER TABLE HoaDon ADD mo_ta_voucher_snapshot NVARCHAR(500) NULL;
GO

-- 2. Chiến lược Backfill cho dữ liệu cũ (Backfill Strategy)
-- Đồng bộ dữ liệu HoaDonChiTiet
UPDATE hdct
SET 
    hdct.gia_niem_yet = COALESCE(spct.gia_ban, hdct.don_gia),
    hdct.phan_tram_giam = 0,
    hdct.so_tien_giam_san_pham = 0,
    hdct.ten_san_pham_snapshot = sp.ten_san_pham,
    hdct.sku_snapshot = 'SKU-' + CAST(sp.id AS VARCHAR) + '-' + CAST(spct.id AS VARCHAR),
    hdct.thuong_hieu_snapshot = th.ten_thuong_hieu,
    hdct.danh_muc_snapshot = dm.ten_danh_muc,
    hdct.thuoc_tinh_snapshot = 'Màu sắc: ' + COALESCE(spct.mau_sac, N'N/A') + 
                               CASE WHEN spct.trong_luong IS NOT NULL AND TRIM(spct.trong_luong) <> '' THEN ' | Trọng lượng: ' + spct.trong_luong ELSE '' END +
                               CASE WHEN spct.muc_cang IS NOT NULL AND TRIM(spct.muc_cang) <> '' THEN ' | Mức căng: ' + spct.muc_cang ELSE '' END
FROM HoaDonChiTiet hdct
JOIN SanPhamChiTiet spct ON hdct.id_san_pham_chi_tiet = spct.id
JOIN SanPham sp ON spct.id_san_pham = sp.id
LEFT JOIN ThuongHieu th ON sp.id_thuong_hieu = th.id
LEFT JOIN DanhMuc dm ON sp.id_danh_muc = dm.id;
GO

-- Đồng bộ dữ liệu HoaDon (Kế toán voucher cũ)
-- Tính toán số tiền hàng gốc từ chi tiết
WITH SubtotalCTE AS (
    SELECT id_hoa_don, SUM(don_gia * so_luong) AS tong_tien_hang
    FROM HoaDonChiTiet
    GROUP BY id_hoa_don
)
UPDATE hd
SET 
    hd.so_tien_giam_voucher = CASE 
        WHEN cte.tong_tien_hang + hd.phi_van_chuyen > hd.tong_tien 
        THEN (cte.tong_tien_hang + hd.phi_van_chuyen - hd.tong_tien)
        ELSE 0 
    END,
    hd.ma_voucher_ap_dung = pgg.ma_phieu,
    hd.ten_voucher_ap_dung = CASE WHEN pgg.ma_phieu IS NOT NULL THEN 'Voucher ' + pgg.ma_phieu ELSE NULL END
FROM HoaDon hd
JOIN SubtotalCTE cte ON hd.id = cte.id_hoa_don
LEFT JOIN PhieuGiamGia pgg ON hd.id_phieu_giam_gia = pgg.id;
GO
