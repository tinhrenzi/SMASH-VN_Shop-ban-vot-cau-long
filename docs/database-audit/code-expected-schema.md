# Code Expected Database Schema

Lược đồ cơ sở dữ liệu kỳ vọng được khai báo bởi các Entity Java trong mã nguồn hiện tại.

* **Tổng số bảng kỳ vọng**: 33

---

## Table: `Blog` (Class: `Blog`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `da_xoa` | `deleted` | `bit` |  | NO | NO |
| `danh_muc` | `category` | `nvarchar` |  | YES | NO |
| `duong_dan` | `slug` | `nvarchar` |  | NO | NO |
| `hinh_anh` | `image` | `nvarchar` |  | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_tai_khoan` | `nguoiDang` | `int` |  | YES | NO |
| `ngay_cap_nhat` | `updatedAt` | `datetime` |  | YES | NO |
| `ngay_dang` | `publishDate` | `date` |  | YES | NO |
| `ngay_tao` | `createdAt` | `datetime` |  | NO | NO |
| `ngay_xoa` | `deletedAt` | `datetime` |  | YES | NO |
| `noi_dung` | `content` | `text` |  | YES | NO |
| `the` | `tags` | `nvarchar` |  | YES | NO |
| `tieu_de` | `title` | `nvarchar` |  | NO | NO |
| `tom_tat` | `summary` | `nvarchar` | 1000 | YES | NO |
| `trang_thai` | `status` | `nvarchar` |  | NO | NO |
| `updated_by` | `updatedBy` | `nvarchar` |  | YES | NO |

---

## Table: `BlogComment` (Class: `BlogComment`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `da_xoa` | `deleted` | `bit` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_binh_luan_cha` | `parentComment` | `int` |  | YES | NO |
| `id_blog` | `blog` | `int` |  | NO | NO |
| `id_tai_khoan` | `taiKhoan` | `int` |  | NO | NO |
| `ly_do_xoa` | `deletedReason` | `nvarchar` | 500 | YES | NO |
| `ngay_tao` | `createdAt` | `datetime` |  | NO | NO |
| `ngay_xoa` | `deletedAt` | `datetime` |  | YES | NO |
| `noi_dung` | `content` | `nvarchar` | 1000 | NO | NO |

---

## Table: `ChatConversation` (Class: `ChatConversation`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `id_khach_hang` | `khachHang` | `int` |  | NO | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | YES | NO |
| `session_id` | `sessionId` | `nvarchar` | 100 | YES | NO |
| `tieu_de` | `tieuDe` | `nvarchar` | 255 | YES | NO |
| `trang_thai` | `trangThai` | `nvarchar` | 20 | YES | NO |

---

## Table: `ChatFeedback` (Class: `ChatFeedback`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `diem_danh_gia` | `danhGia` | `bit` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_tin_nhan` | `message` | `bigint` |  | NO | NO |
| `ngay_tao` | `thoiGian` | `datetime` |  | YES | NO |
| `noi_dung` | `ghiChu` | `nvarchar` | 500 | YES | NO |

---

## Table: `ChatMessage` (Class: `ChatMessage`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `bigint` |  | YES | YES |
| `id_cuoc_tro_chuyen` | `conversation` | `int` |  | NO | NO |
| `loai_nguoi_gui` | `senderType` | `nvarchar` | 10 | NO | NO |
| `noi_dung` | `noiDung` | `nvarchar` |  | NO | NO |
| `thoi_gian` | `thoiGian` | `datetime` |  | YES | NO |

---

## Table: `CommentModerationKeyword` (Class: `CommentModerationKeyword`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `kich_hoat` | `active` | `bit` |  | NO | NO |
| `ngay_tao` | `createdAt` | `datetime` |  | NO | NO |
| `tu_khoa` | `keyword` | `nvarchar` |  | NO | NO |

---

## Table: `CommentViolationLog` (Class: `CommentViolationLog`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `id_danh_gia` | `danhGia` | `int` |  | YES | NO |
| `id_san_pham` | `sanPham` | `int` |  | NO | NO |
| `id_tai_khoan` | `taiKhoan` | `int` |  | NO | NO |
| `muc_do_vi_pham` | `mucDoViPham` | `nvarchar` | 50 | NO | NO |
| `ngay_tao` | `createdAt` | `datetime` |  | NO | NO |
| `ngay_vi_pham` | `ngayViPham` | `datetime` |  | NO | NO |
| `noi_dung_da_loc` | `noiDungDaLoc` | `nvarchar` |  | NO | NO |
| `noi_dung_goc` | `noiDungGoc` | `nvarchar` |  | NO | NO |
| `so_lan_vi_pham` | `soLanViPham` | `int` |  | NO | NO |
| `thoi_han_khoa` | `thoiHanKhoa` | `nvarchar` | 100 | YES | NO |

---

## Table: `DanhGia` (Class: `DanhGia`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `an_binh_luan` | `anBinhLuan` | `bit` |  | NO | NO |
| `an_hinh_anh` | `anHinhAnh` | `bit` |  | NO | NO |
| `da_xoa` | `daXoa` | `bit` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_khach_hang` | `khachHang` | `int` |  | NO | NO |
| `id_nguoi_an_binh_luan` | `nguoiAnBinhLuan` | `int` |  | YES | NO |
| `id_nguoi_an_hinh_anh` | `nguoiAnHinhAnh` | `int` |  | YES | NO |
| `id_nguoi_hien_binh_luan` | `nguoiHienBinhLuan` | `int` |  | YES | NO |
| `id_nguoi_hien_hinh_anh` | `nguoiHienHinhAnh` | `int` |  | YES | NO |
| `id_nguoi_xoa` | `nguoiXoa` | `int` |  | YES | NO |
| `id_san_pham` | `sanPham` | `int` |  | NO | NO |
| `ngay_an_binh_luan` | `ngayAnBinhLuan` | `datetime` |  | YES | NO |
| `ngay_an_hinh_anh` | `ngayAnHinhAnh` | `datetime` |  | YES | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_hien_binh_luan` | `ngayHienBinhLuan` | `datetime` |  | YES | NO |
| `ngay_hien_hinh_anh` | `ngayHienHinhAnh` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayDanhGia` | `datetime` |  | NO | NO |
| `ngay_xoa` | `ngayXoa` | `datetime` |  | YES | NO |
| `noi_dung` | `binhLuan` | `nvarchar` |  | YES | NO |
| `so_sao` | `soSao` | `float` |  | NO | NO |

---

## Table: `DanhGiaAnh` (Class: `DanhGiaAnh`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `duong_dan` | `duongDan` | `nvarchar` | 255 | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_danh_gia` | `danhGia` | `int` |  | NO | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | NO | NO |

---

## Table: `DanhMuc` (Class: `DanhMuc`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `mo_ta` | `moTa` | `nvarchar` | 500 | YES | NO |
| `ten_danh_muc` | `tenDanhMuc` | `nvarchar` |  | NO | NO |
| `trang_thai` | `trangThai` | `bit` |  | YES | NO |

---

## Table: `DonViVanChuyen` (Class: `DonViVanChuyen`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `dia_chi_kho` | `diaChiKho` | `nvarchar` | 500 | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `ma_client` | `clientId` | `nvarchar` | 100 | YES | NO |
| `ma_don_vi` | `maDonVi` | `nvarchar` | 50 | YES | NO |
| `ma_token` | `token` | `nvarchar` | 255 | YES | NO |
| `phi_noi_dia` | `phiLocal` | `decimal` |  | YES | NO |
| `phi_toan_quoc` | `phiNationwide` | `decimal` |  | YES | NO |
| `phien_ban` | `version` | `bigint` |  | YES | NO |
| `so_hotline` | `hotline` | `nvarchar` | 20 | YES | NO |
| `ten_don_vi` | `tenDonVi` | `nvarchar` | 100 | YES | NO |
| `trang_web` | `website` | `nvarchar` | 100 | YES | NO |

---

## Table: `DotGiamGia` (Class: `DotGiamGia`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `id_nhan_vien` | `nhanVien` | `int` |  | NO | NO |
| `kich_hoat` | `active` | `bit` |  | YES | NO |
| `loai_giam_gia` | `loaiGiamGia` | `nvarchar` | 100 | NO | NO |
| `ngay_bat_dau` | `ngayBatDau` | `datetime` |  | NO | NO |
| `ngay_ket_thuc` | `ngayKetThuc` | `datetime` |  | NO | NO |
| `phan_tram_giam` | `phanTramGiam` | `int` |  | NO | NO |
| `ten_chien_dich` | `tenChienDich` | `nvarchar` |  | NO | NO |

---

## Table: `EditLog` (Class: `EditLog`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `dia_chi_ip` | `diaChiIp` | `nvarchar` | 50 | YES | NO |
| `ghi_chu` | `ghiChu` | `nvarchar` | 500 | YES | NO |
| `gia_tri_cu` | `giaTriCu` | `nvarchar` |  | YES | NO |
| `gia_tri_moi` | `giaTriMoi` | `nvarchar` |  | YES | NO |
| `hanh_dong` | `hanhDong` | `nvarchar` | 20 | NO | NO |
| `id` | `id` | `bigint` |  | YES | YES |
| `id_ban_ghi` | `idBanGhi` | `bigint` |  | NO | NO |
| `id_tai_khoan` | `taiKhoan` | `int` |  | YES | NO |
| `ten_bang` | `tenBang` | `nvarchar` | 100 | NO | NO |
| `thoi_gian` | `thoiGian` | `datetime` |  | NO | NO |
| `vai_tro_thuc_hien` | `vaiTroThucHien` | `nvarchar` | 20 | YES | NO |

---

## Table: `GiaoDichThanhToan` (Class: `PaymentTransaction`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `cong_thanh_toan` | `gateway` | `nvarchar` | 50 | NO | NO |
| `du_lieu_tho` | `rawPayload` | `nvarchar` |  | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_hoa_don` | `order` | `int` |  | YES | NO |
| `ma_giao_dich` | `transactionId` | `nvarchar` | 100 | NO | NO |
| `ngay_tao` | `createdAt` | `datetime` |  | NO | NO |
| `so_tien` | `amount` | `decimal` | 18,2 | NO | NO |
| `trang_thai` | `status` | `nvarchar` | 50 | NO | NO |

---

## Table: `GioHang` (Class: `GioHang`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `id_khach_hang` | `khachHang` | `int` |  | YES | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | YES | NO |
| `session_id` | `sessionId` | `nvarchar` | 100 | YES | NO |

---

## Table: `GioHangChiTiet` (Class: `GioHangChiTiet`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `id_gio_hang` | `gioHang` | `int` |  | NO | NO |
| `id_san_pham_chi_tiet` | `sanPhamChiTiet` | `int` |  | NO | NO |
| `id_trang_thai` | `trangThai` | `int` |  | NO | NO |
| `ngay_them` | `ngayThem` | `datetime` |  | YES | NO |
| `so_luong` | `soLuong` | `int` |  | NO | NO |

---

## Table: `HinhAnhSanPham` (Class: `HinhAnhSanPham`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `duong_dan` | `urlHinhAnh` | `nvarchar` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_san_pham_chi_tiet` | `sanPhamChiTiet` | `int` |  | NO | NO |
| `la_anh_chinh` | `laAnhChinh` | `bit` |  | YES | NO |
| `mau_sac` | `mauSac` | `nvarchar` | 50 | YES | NO |

---

## Table: `HoaDon` (Class: `HoaDon`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `dia_chi_nhan` | `diaChiNhan` | `nvarchar` | 500 | NO | NO |
| `ghi_chu` | `ghiChu` | `nvarchar` | 500 | YES | NO |
| `ghn_order_code` | `ghnOrderCode` | `nvarchar` | 50 | YES | NO |
| `ghn_status` | `ghnStatus` | `nvarchar` | 100 | YES | NO |
| `ghn_to_district_id` | `ghnToDistrictId` | `int` |  | YES | NO |
| `ghn_to_ward_code` | `ghnToWardCode` | `nvarchar` | 20 | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_dia_chi` | `diaChi` | `int` |  | YES | NO |
| `id_don_vi_van_chuyen` | `donViVanChuyen` | `int` |  | NO | NO |
| `id_khach_hang` | `khachHang` | `int` |  | NO | NO |
| `id_nhan_vien` | `nhanVien` | `int` |  | YES | NO |
| `id_nhan_vien_xac_nhan` | `nhanVienXacNhan` | `int` |  | YES | NO |
| `id_nhan_vien_xac_nhan_hoan_tien` | `refundConfirmedBy` | `int` |  | YES | NO |
| `id_phieu_giam_gia` | `phieuGiamGia` | `int` |  | YES | NO |
| `id_phuong_thuc_thanh_toan` | `phuongThucThanhToan` | `int` |  | NO | NO |
| `loai_don_hang` | `loaiDonHang` | `nvarchar` | 30 | YES | NO |
| `ma_don_hang` | `maDonHang` | `nvarchar` | 50 | YES | NO |
| `ma_giam_gia_ap_dung` | `maVoucherApDung` | `nvarchar` | 50 | YES | NO |
| `ma_giao_dich` | `transactionId` | `nvarchar` | 100 | YES | NO |
| `ma_giao_dich_ung_dung` | `appTransId` | `nvarchar` | 100 | YES | NO |
| `mo_ta_giam_gia_snapshot` | `moTaVoucherSnapshot` | `nvarchar` | 500 | YES | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | NO | NO |
| `ngay_thanh_toan` | `paidAt` | `datetime` |  | YES | NO |
| `ngay_xac_nhan_hoan_hang` | `ngayXacNhanHoanHang` | `datetime` |  | YES | NO |
| `nguoi_xac_nhan_thanh_toan` | `nguoiXacNhanThanhToan` | `nvarchar` | 100 | YES | NO |
| `phan_hoi_cong_tt` | `gatewayResponse` | `nvarchar` |  | YES | NO |
| `phi_van_chuyen` | `phiVanChuyen` | `decimal` |  | NO | NO |
| `phuong_thuc_thanh_toan` | `paymentMethod` | `nvarchar` | 50 | YES | NO |
| `sdt_nhan` | `sdtNhan` | `nvarchar` | 15 | NO | NO |
| `so_tien_giam_voucher` | `soTienGiamVoucher` | `decimal` |  | YES | NO |
| `ten_giam_gia_ap_dung` | `tenVoucherApDung` | `nvarchar` | 255 | YES | NO |
| `ten_nguoi_nhan` | `tenNguoiNhan` | `nvarchar` | 100 | YES | NO |
| `thoi_gian_xac_nhan` | `thoiGianXacNhan` | `datetime` |  | YES | NO |
| `tong_tien` | `tongTien` | `decimal` |  | NO | NO |
| `tong_tien_hang` | `tongTienHang` | `decimal` |  | YES | NO |
| `trang_thai_don_hang` | `trangThaiDonHang` | `nvarchar` | 50 | NO | NO |
| `trang_thai_hoan_hang` | `trangThaiHoanHang` | `nvarchar` | 50 | YES | NO |
| `trang_thai_thanh_toan` | `paymentStatus` | `nvarchar` | 50 | YES | NO |

---

## Table: `HoaDonChiTiet` (Class: `HoaDonChiTiet`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `danh_muc_snapshot` | `danhMucSnapshot` | `nvarchar` | 100 | YES | NO |
| `don_gia` | `donGia` | `decimal` |  | NO | NO |
| `gia_niem_yet` | `giaNiemYet` | `decimal` |  | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_dot_giam_gia` | `idDotGiamGia` | `int` |  | YES | NO |
| `id_hoa_don` | `hoaDon` | `int` |  | NO | NO |
| `id_san_pham_chi_tiet` | `sanPhamChiTiet` | `int` |  | NO | NO |
| `ma_hang_snapshot` | `skuSnapshot` | `nvarchar` | 100 | YES | NO |
| `phan_tram_giam` | `phanTramGiam` | `decimal` |  | YES | NO |
| `so_luong` | `soLuong` | `int` |  | NO | NO |
| `so_tien_giam_san_pham` | `soTienGiamSanPham` | `decimal` |  | YES | NO |
| `ten_dot_giam_gia` | `tenDotGiamGia` | `nvarchar` | 100 | YES | NO |
| `ten_san_pham_snapshot` | `tenSanPhamSnapshot` | `nvarchar` | 255 | YES | NO |
| `thanh_tien` | `thanhTien` | `decimal` |  | YES | NO |
| `thuoc_tinh_snapshot` | `thuocTinhSnapshot` | `nvarchar` | 500 | YES | NO |
| `thuong_hieu_snapshot` | `thuongHieuSnapshot` | `nvarchar` | 100 | YES | NO |

---

## Table: `KhachHang` (Class: `KhachHang`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `ho_kh` | `hoKh` | `nvarchar` | 50 | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_tai_khoan` | `taiKhoan` | `int` |  | NO | NO |
| `la_tai_khoan_noi_bo` | `laTaiKhoanNoiBo` | `bit` |  | YES | NO |
| `loai_khach_hang` | `loaiKhachHang` | `nvarchar` | 30 | YES | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | YES | NO |
| `nguon_tao` | `nguonTao` | `nvarchar` | 50 | YES | NO |
| `nhan_ban_tin` | `nhanBanTin` | `bit` |  | NO | NO |
| `so_dien_thoai_kh` | `soDienThoaiKh` | `nvarchar` | 15 | NO | NO |
| `ten_kh` | `tenKh` | `nvarchar` | 50 | NO | NO |

---

## Table: `MaKhoiPhuc` (Class: `TokenKhoiPhuc`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `da_su_dung` | `daSuDung` | `bit` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_tai_khoan` | `taiKhoan` | `int` |  | NO | NO |
| `loai_xac_nhan` | `loaiXacNhan` | `nvarchar` | 20 | NO | NO |
| `ma_xac_nhan` | `maXacNhan` | `nvarchar` |  | NO | NO |
| `thoi_gian_het_han` | `thoiGianHetHan` | `datetime` |  | NO | NO |

---

## Table: `NhanVien` (Class: `NhanVien`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `chuc_vu` | `chucVu` | `nvarchar` | 100 | NO | NO |
| `ho_ten` | `hoTenNv` | `nvarchar` | 100 | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_tai_khoan` | `taiKhoan` | `int` |  | NO | NO |
| `so_dien_thoai` | `soDienThoaiNv` | `nvarchar` | 15 | NO | NO |

---

## Table: `PhieuGiamGia` (Class: `PhieuGiamGia`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `don_vi` | `donVi` | `nvarchar` | 10 | NO | NO |
| `gia_tri` | `giaTri` | `decimal` |  | NO | NO |
| `gia_tri_don_hang_toi_thieu` | `giaTriDonHangToiThieu` | `decimal` |  | YES | NO |
| `gia_tri_giam_toi_da` | `giaTriGiamToiDa` | `decimal` |  | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_nhan_vien` | `nhanVien` | `int` |  | NO | NO |
| `kich_hoat` | `active` | `bit` |  | YES | NO |
| `loai_giam_gia` | `loaiGiamGia` | `nvarchar` | 100 | NO | NO |
| `ma_phieu` | `maPhieu` | `nvarchar` | 50 | NO | NO |
| `ngay_bat_dau` | `ngayBatDau` | `datetime` |  | NO | NO |
| `ngay_ket_thuc` | `ngayKetThuc` | `datetime` |  | NO | NO |
| `so_luong_con_lai` | `soLuongConLai` | `int` |  | NO | NO |
| `ten_phieu` | `tenPhieu` | `nvarchar` | 100 | YES | NO |
| `trang_thai` | `trangThai` | `nvarchar` | 50 | YES | NO |

---

## Table: `PhuongThucThanhToan` (Class: `PhuongThucThanhToan`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `ma_phuong_thuc` | `maPhuongThuc` | `nvarchar` | 50 | YES | NO |
| `ten_phuong_thuc` | `tenPhuongThuc` | `nvarchar` | 100 | NO | NO |

---

## Table: `SanPham` (Class: `SanPham`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `diem_trung_binh` | `diemTrungBinh` | `float` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_danh_muc` | `danhMuc` | `int` |  | NO | NO |
| `id_nhan_vien` | `nhanVien` | `int` |  | NO | NO |
| `id_thuong_hieu` | `thuongHieu` | `int` |  | NO | NO |
| `mo_ta` | `moTa` | `nvarchar` |  | NO | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | YES | NO |
| `so_danh_gia` | `soDanhGia` | `int` |  | NO | NO |
| `ten_san_pham` | `tenSanPham` | `nvarchar` |  | NO | NO |
| `trang_thai` | `trangThai` | `nvarchar` | 50 | YES | NO |

---

## Table: `SanPhamChiTiet` (Class: `SanPhamChiTiet`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `SKU` | `sku` | `nvarchar` | 100 | YES | NO |
| `barcode` | `barcode` | `nvarchar` | 100 | YES | NO |
| `chat_lieu` | `chatLieu` | `nvarchar` | 50 | YES | NO |
| `gia_ban` | `giaBan` | `decimal` |  | NO | NO |
| `gia_nhap` | `giaNhap` | `decimal` |  | YES | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_san_pham` | `sanPham` | `int` |  | NO | NO |
| `kich_thuoc` | `kichThuoc` | `nvarchar` | 50 | YES | NO |
| `mau_sac` | `mauSac` | `nvarchar` | 50 | NO | NO |
| `muc_cang` | `mucCang` | `nvarchar` | 20 | NO | NO |
| `ngay_cap_nhat` | `ngayCapNhat` | `datetime` |  | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | YES | NO |
| `so_luong_ton` | `soLuongTon` | `int` |  | NO | NO |
| `trang_thai` | `trangThai` | `nvarchar` | 50 | YES | NO |
| `trong_luong` | `trongLuong` | `nvarchar` | 20 | NO | NO |

---

## Table: `SanPhamYeuThich` (Class: `SanPhamYeuThich`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id_khach_hang` | `khachHang` | `int` |  | NO | NO |
| `id_san_pham` | `sanPham` | `int` |  | NO | NO |
| `ngay_them` | `ngayThem` | `datetime` |  | NO | NO |

---

## Table: `SanPham_DotGiamGia` (Class: `DotGiamGia JoinTable`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id_dot_giam_gia` | `join_column` | `int` |  | NO | YES |
| `id_san_pham` | `join_column` | `int` |  | NO | YES |

---

## Table: `SoDiaChi` (Class: `SoDiaChi`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `dia_chi_cu_the` | `diaChiCuThe` | `nvarchar` | 255 | NO | NO |
| `district_id` | `districtId` | `int` |  | YES | NO |
| `district_name` | `districtName` | `nvarchar` | 100 | YES | NO |
| `ho_nguoi_nhan` | `hoNguoiNhan` | `nvarchar` | 50 | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_khach_hang` | `khachHang` | `int` |  | NO | NO |
| `kinh_do` | `longitude` | `float` |  | YES | NO |
| `la_mac_dinh_giao_hang` | `defaultShipping` | `bit` |  | NO | NO |
| `la_mac_dinh_thanh_toan` | `defaultBilling` | `bit` |  | NO | NO |
| `province_id` | `provinceId` | `int` |  | YES | NO |
| `province_name` | `provinceName` | `nvarchar` | 100 | YES | NO |
| `quoc_gia` | `quocGia` | `nvarchar` | 100 | NO | NO |
| `sdt_nguoi_nhan` | `sdtNguoiNhan` | `nvarchar` | 15 | NO | NO |
| `ten_nguoi_nhan` | `tenNguoiNhan` | `nvarchar` | 50 | NO | NO |
| `thanh_pho` | `thanhPho` | `nvarchar` | 100 | NO | NO |
| `tinh_thanh` | `tinhThanh` | `nvarchar` | 100 | NO | NO |
| `vi_do` | `latitude` | `float` |  | YES | NO |
| `ward_code` | `wardCode` | `nvarchar` | 20 | YES | NO |
| `ward_name` | `wardName` | `nvarchar` | 100 | YES | NO |

---

## Table: `TaiKhoan` (Class: `TaiKhoan`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `email` | `email` | `nvarchar` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `mat_khau` | `matKhau` | `nvarchar` |  | YES | NO |
| `ngay_khoa_binh_luan_den` | `ngayKhoaBinhLuanDen` | `datetime` |  | YES | NO |
| `ngay_vi_pham_gan_nhat` | `ngayViPhamGanNhat` | `datetime` |  | YES | NO |
| `so_lan_mua_thanh_cong` | `soLanMuaThanhCong` | `int` |  | NO | NO |
| `so_lan_nhac_nho_vi_pham` | `soLanNhacNhoViPham` | `int` |  | NO | NO |
| `token_xac_thuc_khoa` | `tokenXacThucKhoa` | `nvarchar` | 100 | YES | NO |
| `trang_thai` | `trangThai` | `nvarchar` | 50 | NO | NO |
| `trang_thai_tai_khoan` | `trangThaiTaiKhoan` | `nvarchar` | 20 | NO | NO |
| `vai_tro` | `vaiTro` | `nvarchar` | 10 | NO | NO |

---

## Table: `ThongBao` (Class: `ThongBao`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `da_doc` | `daDoc` | `bit` |  | NO | NO |
| `id` | `id` | `int` |  | YES | YES |
| `id_tai_khoan` | `taiKhoan` | `int` |  | YES | NO |
| `loai_thong_bao` | `loaiThongBao` | `nvarchar` | 50 | YES | NO |
| `ngay_tao` | `ngayTao` | `datetime` |  | NO | NO |
| `noi_dung` | `noiDung` | `nvarchar` |  | NO | NO |
| `tieu_de` | `tieuDe` | `nvarchar` |  | NO | NO |

---

## Table: `ThuongHieu` (Class: `ThuongHieu`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `mo_ta` | `moTa` | `nvarchar` | 500 | YES | NO |
| `ten_thuong_hieu` | `tenThuongHieu` | `nvarchar` |  | NO | NO |
| `trang_thai` | `trangThai` | `bit` |  | YES | NO |

---

## Table: `TrangThaiGioHang` (Class: `TrangThaiGioHang`)

### Expected Columns:
| Column | Field Name | Expected Type | Length/Prec | Nullable | Primary Key |
|---|---|---|---|---|---|
| `id` | `id` | `int` |  | YES | YES |
| `ten_trang_thai` | `tenTrangThai` | `nvarchar` | 50 | YES | NO |

---
