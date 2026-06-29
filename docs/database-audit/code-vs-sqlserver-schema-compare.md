# Database Schema Comparison - Code vs SQL Server

Bảng đối chiếu lược đồ cơ sở dữ liệu kỳ vọng trong Code và lược đồ thực tế trong SQL Server.

## Table & Column Comparison Matrix

| Table | Column | In Code Expected | In SQL Server | Code Type | SQL Server Type | Code Nullable | SQL Nullable | Status | Evidence | Recommendation |
|---|---|---|---|---|---|---|---|---|---|---|
| Blog | da_xoa | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| Blog | danh_muc | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | duong_dan | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| Blog | hinh_anh | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| Blog | id_tai_khoan | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | ngay_dang | YES | YES | date | date | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| Blog | ngay_xoa | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | noi_dung | YES | YES | text | nvarchar | YES | YES | TYPE_MISMATCH | Code expects text, SQL Server has nvarchar | Alter column type to match code: text |
| Blog | the | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | tieu_de | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| Blog | tom_tat | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| Blog | trang_thai | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| Blog | updated_by | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| BlogComment | da_xoa | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| BlogComment | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| BlogComment | id_binh_luan_cha | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| BlogComment | id_blog | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| BlogComment | id_tai_khoan | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| BlogComment | ly_do_xoa | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| BlogComment | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| BlogComment | ngay_xoa | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| BlogComment | noi_dung | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| ChatConversation | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| ChatConversation | id_khach_hang | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| ChatConversation | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| ChatConversation | ngay_tao | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| ChatConversation | session_id | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| ChatConversation | tieu_de | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| ChatConversation | trang_thai | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| ChatFeedback | diem_danh_gia | YES | YES | bit | bit | NO | NO | MATCH | Metadata & Code match | No action needed |
| ChatFeedback | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| ChatFeedback | id_tin_nhan | YES | YES | bigint | bigint | NO | NO | MATCH | Metadata & Code match | No action needed |
| ChatFeedback | ngay_tao | YES | YES | datetime | datetime | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| ChatFeedback | noi_dung | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| ChatMessage | id | YES | YES | bigint | bigint | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| ChatMessage | id_cuoc_tro_chuyen | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| ChatMessage | loai_nguoi_gui | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| ChatMessage | noi_dung | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| ChatMessage | thoi_gian | YES | YES | datetime | datetime | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| CommentModerationKeyword | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| CommentModerationKeyword | kich_hoat | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| CommentModerationKeyword | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| CommentModerationKeyword | tu_khoa | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| CommentViolationLog | id_danh_gia | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | id_san_pham | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | id_tai_khoan | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | muc_do_vi_pham | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | ngay_tao | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | ngay_vi_pham | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | noi_dung_da_loc | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | noi_dung_goc | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | so_lan_vi_pham | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| CommentViolationLog | thoi_han_khoa | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | an_binh_luan | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGia | an_hinh_anh | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGia | da_xoa | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGia | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| DanhGia | id_khach_hang | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGia | id_nguoi_an_binh_luan | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | id_nguoi_an_hinh_anh | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | id_nguoi_hien_binh_luan | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | id_nguoi_hien_hinh_anh | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | id_nguoi_xoa | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | id_san_pham | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGia | ngay_an_binh_luan | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | ngay_an_hinh_anh | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | ngay_hien_binh_luan | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | ngay_hien_hinh_anh | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGia | ngay_xoa | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | noi_dung | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhGia | so_sao | YES | YES | float | float | NO | NO | MATCH | Metadata & Code match | No action needed |
| DanhGiaAnh | duong_dan | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| DanhGiaAnh | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| DanhGiaAnh | id_danh_gia | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhGiaAnh | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DanhMuc | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| DanhMuc | mo_ta | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DanhMuc | ten_danh_muc | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| DanhMuc | trang_thai | YES | YES | bit | bit | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | dia_chi_kho | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| DonViVanChuyen | ma_client | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | ma_don_vi | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | ma_token | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | phi_noi_dia | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | phi_toan_quoc | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | phien_ban | YES | YES | bigint | bigint | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | so_hotline | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | ten_don_vi | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DonViVanChuyen | trang_web | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| DotGiamGia | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| DotGiamGia | id_nhan_vien | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DotGiamGia | kich_hoat | YES | YES | bit | bit | YES | YES | MATCH | Metadata & Code match | No action needed |
| DotGiamGia | loai_giam_gia | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DotGiamGia | ngay_bat_dau | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| DotGiamGia | ngay_ket_thuc | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| DotGiamGia | phan_tram_giam | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DotGiamGia | ten_chien_dich | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| DotGiamGia | ten_dot | NO | YES | N/A | nvarchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| DotGiamGia | trang_thai | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| EditLog | dia_chi_ip | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| EditLog | ghi_chu | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| EditLog | gia_tri_cu | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| EditLog | gia_tri_moi | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| EditLog | hanh_dong | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| EditLog | id | YES | YES | bigint | bigint | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| EditLog | id_ban_ghi | YES | YES | bigint | bigint | NO | NO | MATCH | Metadata & Code match | No action needed |
| EditLog | id_tai_khoan | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| EditLog | ten_bang | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| EditLog | thoi_gian | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| EditLog | vai_tro_thuc_hien | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| GiaoDichThanhToan | cong_thanh_toan | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| GiaoDichThanhToan | du_lieu_tho | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| GiaoDichThanhToan | gateway | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| GiaoDichThanhToan | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| GiaoDichThanhToan | id_hoa_don | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| GiaoDichThanhToan | ma_giao_dich | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| GiaoDichThanhToan | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| GiaoDichThanhToan | so_tien | YES | YES | decimal | decimal | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| GiaoDichThanhToan | status | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| GiaoDichThanhToan | trang_thai | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| GioHang | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| GioHang | id_khach_hang | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| GioHang | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| GioHang | ngay_tao | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| GioHang | session_id | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| GioHangChiTiet | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| GioHangChiTiet | id_gio_hang | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| GioHangChiTiet | id_san_pham_chi_tiet | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| GioHangChiTiet | id_trang_thai | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| GioHangChiTiet | ngay_them | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| GioHangChiTiet | so_luong | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| HinhAnhSanPham | duong_dan | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| HinhAnhSanPham | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| HinhAnhSanPham | id_san_pham_chi_tiet | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HinhAnhSanPham | la_anh_chinh | YES | YES | bit | bit | YES | YES | MATCH | Metadata & Code match | No action needed |
| HinhAnhSanPham | mau_sac | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | dia_chi_nhan | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | ghi_chu | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ghn_order_code | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ghn_status | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ghn_to_district_id | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ghn_to_ward_code | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| HoaDon | id_dia_chi | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | id_don_vi_van_chuyen | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | id_khach_hang | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | id_nhan_vien | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | id_nhan_vien_xac_nhan | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | id_nhan_vien_xac_nhan_hoan_tien | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | id_phieu_giam_gia | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | id_phuong_thuc_thanh_toan | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | loai_don_hang | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ma_don_hang | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ma_giam_gia_ap_dung | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ma_giao_dich | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ma_giao_dich_ung_dung | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | mo_ta_giam_gia_snapshot | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | ngay_thanh_toan | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ngay_xac_nhan_hoan_hang | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | nguoi_xac_nhan_thanh_toan | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | phan_hoi_cong_tt | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | phi_van_chuyen | YES | YES | decimal | decimal | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | phuong_thuc_thanh_toan | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | sdt_nhan | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | so_tien_giam_gia | NO | YES | N/A | decimal | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| HoaDon | so_tien_giam_voucher | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ten_giam_gia_ap_dung | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | ten_nguoi_nhan | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | thoi_gian_xac_nhan | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | tong_tien | YES | YES | decimal | decimal | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | tong_tien_hang | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | trang_thai_don_hang | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDon | trang_thai_hoan_hang | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDon | trang_thai_thanh_toan | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | danh_muc_snapshot | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | don_gia | YES | YES | decimal | decimal | NO | NO | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | gia_niem_yet | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| HoaDonChiTiet | id_dot_giam_gia | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | id_hoa_don | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDonChiTiet | id_san_pham_chi_tiet | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| HoaDonChiTiet | ma_hang_snapshot | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | phan_tram_giam | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | so_luong | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | so_tien_giam_san_pham | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | ten_dot_giam_gia | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | ten_san_pham_snapshot | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | thanh_tien | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | thuoc_tinh_snapshot | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| HoaDonChiTiet | thuong_hieu_snapshot | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| KhachHang | ho_kh | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| KhachHang | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| KhachHang | id_tai_khoan | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| KhachHang | la_tai_khoan_noi_bo | YES | YES | bit | bit | YES | YES | MATCH | Metadata & Code match | No action needed |
| KhachHang | loai_khach_hang | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| KhachHang | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| KhachHang | ngay_tao | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| KhachHang | nguon_tao | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| KhachHang | nhan_ban_tin | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| KhachHang | sdt | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| KhachHang | so_dien_thoai_kh | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| KhachHang | ten_kh | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| LichSuTrangThaiDonHang | id | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| LichSuTrangThaiDonHang | id_hoa_don | NO | YES | N/A | int | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| LichSuTrangThaiDonHang | trang_thai_cu | NO | YES | N/A | nvarchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| LichSuTrangThaiDonHang | trang_thai_moi | NO | YES | N/A | nvarchar | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| LichSuTrangThaiDonHang | ghi_chu | NO | YES | N/A | nvarchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| LichSuTrangThaiDonHang | nguoi_thuc_hien | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| LichSuTrangThaiDonHang | thoi_gian | NO | YES | N/A | datetime | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| MaKhoiPhuc | da_su_dung | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| MaKhoiPhuc | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| MaKhoiPhuc | id_tai_khoan | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| MaKhoiPhuc | loai_xac_nhan | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| MaKhoiPhuc | ma_xac_nhan | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| MaKhoiPhuc | ngay_het_han | NO | YES | N/A | datetime | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| MaKhoiPhuc | thoi_gian_het_han | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| MaKhoiPhuc | token | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| NhanVien | chuc_vu | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| NhanVien | ho_ten | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| NhanVien | ho_ten_nv | NO | YES | N/A | nvarchar | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| NhanVien | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| NhanVien | id_tai_khoan | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| NhanVien | so_dien_thoai | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | don_vi | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| PhieuGiamGia | gia_tri | YES | YES | decimal | decimal | NO | NO | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | gia_tri_don_hang_toi_thieu | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | gia_tri_giam_toi_da | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| PhieuGiamGia | id_nhan_vien | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| PhieuGiamGia | kich_hoat | YES | YES | bit | bit | YES | YES | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | loai_giam_gia | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| PhieuGiamGia | ma_phieu | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | ngay_bat_dau | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | ngay_ket_thuc | YES | YES | datetime | datetime | NO | NO | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | so_luong_con_lai | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| PhieuGiamGia | ten_phieu | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| PhieuGiamGia | trang_thai | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| PhuongThucThanhToan | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| PhuongThucThanhToan | ma_phuong_thuc | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| PhuongThucThanhToan | ten_phuong_thuc | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham | diem_trung_binh | YES | YES | float | float | NO | NO | MATCH | Metadata & Code match | No action needed |
| SanPham | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| SanPham | id_danh_muc | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham | id_nhan_vien | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham | id_thuong_hieu | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham | mo_ta | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPham | ngay_tao | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPham | so_danh_gia | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham | ten_san_pham | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| SanPham | trang_thai | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | SKU | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | barcode | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | chat_lieu | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | gia_ban | YES | YES | decimal | decimal | NO | NO | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | gia_nhap | YES | YES | decimal | decimal | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| SanPhamChiTiet | id_san_pham | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPhamChiTiet | kich_thuoc | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | mau_sac | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | muc_cang | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | ngay_cap_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | ngay_tao | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | so_luong_ton | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPhamChiTiet | trang_thai | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SanPhamChiTiet | trong_luong | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPhamYeuThich | id | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| SanPhamYeuThich | id_khach_hang | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPhamYeuThich | id_san_pham | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPhamYeuThich | ngay_them | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SanPham_DotGiamGia | id_dot_giam_gia | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| SanPham_DotGiamGia | id_san_pham | YES | YES | int | int | NO | NO | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | dia_chi_cu_the | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | district_id | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | district_name | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | ho_nguoi_nhan | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| SoDiaChi | id_khach_hang | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | kinh_do | YES | YES | float | float | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | la_mac_dinh_giao_hang | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | la_mac_dinh_thanh_toan | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | latitude | NO | YES | N/A | float | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| SoDiaChi | longitude | NO | YES | N/A | float | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| SoDiaChi | province_id | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | province_name | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | quoc_gia | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | sdt_nguoi_nhan | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | ten_nguoi_nhan | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | thanh_pho | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | tinh_thanh | YES | YES | nvarchar | nvarchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| SoDiaChi | vi_do | YES | YES | float | float | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | ward_code | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| SoDiaChi | ward_name | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | email | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| TaiKhoan | la_khach_hang | NO | YES | N/A | bit | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| TaiKhoan | la_nhan_vien | NO | YES | N/A | bit | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| TaiKhoan | la_quan_ly | NO | YES | N/A | bit | N/A | YES | SQLSERVER_ONLY | Database column | Verify and keep or drop |
| TaiKhoan | mat_khau | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | ngay_khoa_binh_luan_den | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | ngay_vi_pham_gan_nhat | YES | YES | datetime | datetime | YES | YES | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | so_lan_mua_thanh_cong | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| TaiKhoan | so_lan_nhac_nho_vi_pham | YES | YES | int | int | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| TaiKhoan | token_xac_thuc_khoa | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | trang_thai | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | trang_thai_tai_khoan | YES | YES | nvarchar | varchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| TaiKhoan | vai_tro | YES | YES | nvarchar | varchar | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| ThongBao | da_doc | YES | YES | bit | bit | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| ThongBao | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| ThongBao | id_tai_khoan | YES | YES | int | int | YES | YES | MATCH | Metadata & Code match | No action needed |
| ThongBao | loai_thong_bao | YES | YES | nvarchar | varchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| ThongBao | ngay_tao | YES | YES | datetime | datetime | NO | YES | NULLABLE_MISMATCH | Code expects nullable=NO, SQL Server has nullable=YES | Alter column nullability to: NO |
| ThongBao | noi_dung | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| ThongBao | tieu_de | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| ThuongHieu | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| ThuongHieu | mo_ta | YES | YES | nvarchar | nvarchar | YES | YES | MATCH | Metadata & Code match | No action needed |
| ThuongHieu | ten_thuong_hieu | YES | YES | nvarchar | nvarchar | NO | NO | MATCH | Metadata & Code match | No action needed |
| ThuongHieu | trang_thai | YES | YES | bit | bit | YES | YES | MATCH | Metadata & Code match | No action needed |
| TichHopVanChuyen | id | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| TichHopVanChuyen | id_hoa_don | NO | YES | N/A | int | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| TichHopVanChuyen | ma_van_don | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| TichHopVanChuyen | trang_thai_ghn | NO | YES | N/A | varchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| TichHopVanChuyen | ngay_cap_nhat | NO | YES | N/A | datetime | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| TrangThaiGioHang | id | YES | YES | int | int | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| TrangThaiGioHang | ten_trang_thai | YES | YES | nvarchar | nvarchar | YES | NO | NULLABLE_MISMATCH | Code expects nullable=YES, SQL Server has nullable=NO | Alter column nullability to: YES |
| flyway_schema_history | installed_rank | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | version | NO | YES | N/A | nvarchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | description | NO | YES | N/A | nvarchar | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | type | NO | YES | N/A | nvarchar | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | script | NO | YES | N/A | nvarchar | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | checksum | NO | YES | N/A | int | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | installed_by | NO | YES | N/A | nvarchar | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | installed_on | NO | YES | N/A | datetime | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | execution_time | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| flyway_schema_history | success | NO | YES | N/A | bit | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| sysdiagrams | name | NO | YES | N/A | nvarchar | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| sysdiagrams | principal_id | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| sysdiagrams | diagram_id | NO | YES | N/A | int | N/A | NO | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| sysdiagrams | version | NO | YES | N/A | int | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |
| sysdiagrams | definition | NO | YES | N/A | varbinary | N/A | YES | SQLSERVER_ONLY | Database schema | Legacy column. Verify before drop |

## SQL Server Extra Tables/Columns

| Table | Column | Row Count | Null Count | Not Null Count | Found In Code? | Status | Recommendation |
|---|---|---|---|---|---|---|---|
| `DotGiamGia` | `ten_dot` | 2 | 0 | 2 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `DotGiamGia` | `trang_thai` | 2 | 0 | 2 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `GiaoDichThanhToan` | `gateway` | 6 | 0 | 6 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `GiaoDichThanhToan` | `status` | 6 | 0 | 6 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `HoaDon` | `so_tien_giam_gia` | 11 | 0 | 11 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `KhachHang` | `sdt` | 9 | 0 | 9 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `LichSuTrangThaiDonHang` | `*` | 0 | 0 | 0 | NO | SQLSERVER_ONLY | Legacy table. Cần xác minh trước khi drop. |
| `MaKhoiPhuc` | `ngay_het_han` | 109 | 0 | 109 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `MaKhoiPhuc` | `token` | 109 | 0 | 109 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `NhanVien` | `ho_ten_nv` | 3 | 0 | 3 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `SanPhamYeuThich` | `id` | 0 | 0 | 0 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `SoDiaChi` | `latitude` | 4 | 0 | 4 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `SoDiaChi` | `longitude` | 4 | 0 | 4 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `TaiKhoan` | `la_khach_hang` | 11 | 0 | 11 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `TaiKhoan` | `la_nhan_vien` | 11 | 0 | 11 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `TaiKhoan` | `la_quan_ly` | 11 | 0 | 11 | NO | SQLSERVER_ONLY | Legacy column. Cần xác minh trước khi drop. |
| `TichHopVanChuyen` | `*` | 1 | 0 | 0 | NO | SQLSERVER_ONLY | Legacy table. Cần xác minh trước khi drop. |
| `flyway_schema_history` | `*` | 0 | 0 | 0 | NO | SQLSERVER_ONLY | Legacy table. Cần xác minh trước khi drop. |
| `sysdiagrams` | `*` | 0 | 0 | 0 | NO | SQLSERVER_ONLY | Legacy table. Cần xác minh trước khi drop. |

## Code Required But Missing In SQL Server

| Table | Column | Source Evidence | Expected Type | Status | Impact | Recommendation |
|---|---|---|---|---|---|---|