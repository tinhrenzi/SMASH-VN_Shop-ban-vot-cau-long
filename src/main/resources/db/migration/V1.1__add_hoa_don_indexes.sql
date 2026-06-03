-- Create indexes for HoaDon statistics performance optimization
CREATE INDEX idx_hoa_don_ngay_tao ON HoaDon(ngay_tao);
CREATE INDEX idx_hoa_don_trang_thai_ngay_tao ON HoaDon(trang_thai_don_hang, ngay_tao);
