-- Flyway Migration V1.17: Thêm trường hoàn hàng và hoàn tiền vào bảng HoaDon
ALTER TABLE HoaDon ADD trang_thai_hoan_hang NVARCHAR(50) NULL;
ALTER TABLE HoaDon ADD ngay_xac_nhan_hoan_hang DATETIME NULL;
ALTER TABLE HoaDon ADD id_nhan_vien_xac_nhan INT NULL;

ALTER TABLE HoaDon ADD refund_status NVARCHAR(50) NULL;
ALTER TABLE HoaDon ADD refund_time DATETIME NULL;
ALTER TABLE HoaDon ADD id_nhan_vien_xac_nhan_hoan_tien INT NULL;

-- Thêm khóa ngoại trỏ sang bảng NhanVien
ALTER TABLE HoaDon ADD CONSTRAINT FK_HoaDon_NhanVienXacNhan FOREIGN KEY (id_nhan_vien_xac_nhan) REFERENCES NhanVien(id);
ALTER TABLE HoaDon ADD CONSTRAINT FK_HoaDon_RefundConfirmedBy FOREIGN KEY (id_nhan_vien_xac_nhan_hoan_tien) REFERENCES NhanVien(id);
