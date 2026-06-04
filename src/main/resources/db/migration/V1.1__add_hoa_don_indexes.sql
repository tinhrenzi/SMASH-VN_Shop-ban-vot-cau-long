-- Create indexes for HoaDon statistics performance optimization
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('HoaDon') AND name = 'idx_hoa_don_ngay_tao')
BEGIN
    CREATE INDEX idx_hoa_don_ngay_tao ON HoaDon(ngay_tao);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('HoaDon') AND name = 'idx_hoa_don_trang_thai_ngay_tao')
BEGIN
    CREATE INDEX idx_hoa_don_trang_thai_ngay_tao ON HoaDon(trang_thai_don_hang, ngay_tao);
END;
GO
