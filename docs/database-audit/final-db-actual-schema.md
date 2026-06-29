# SQL Server Actual Database Schema - SMDB_FINAL

Báo cáo chi tiết về lược đồ cơ sở dữ liệu thực tế đang tồn tại trong SQL Server SMDB_FINAL.

* **Tổng số bảng**: 37

---

## Table: `Blog`
* **Row Count**: 4

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `tieu_de` | `nvarchar` | 255 | NO | NO | `` |  |
| `duong_dan` | `varchar` | 255 | YES | NO | `` |  |
| `tom_tat` | `nvarchar` | 500 | YES | NO | `` |  |
| `noi_dung` | `nvarchar` | MAX | YES | NO | `` |  |
| `hinh_anh` | `varchar` | MAX | YES | NO | `` |  |
| `ngay_dang` | `date` |  | YES | NO | `` |  |
| `trang_thai` | `varchar` | 20 | YES | NO | `('DRAFT')` |  |
| `da_xoa` | `bit` |  | YES | NO | `((0))` |  |
| `ngay_xoa` | `datetime` |  | YES | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `(getdate())` |  |
| `danh_muc` | `nvarchar` | 255 | YES | NO | `` |  |
| `the` | `nvarchar` | 255 | YES | NO | `` |  |
| `updated_by` | `varchar` | 255 | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UQ_Blog_DuongDan` | `duong_dan` | YES |
| `IX_BLOG_PUBLISH_DATE` | `ngay_dang` | NO |

---

## Table: `BlogComment`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_blog` | `int` |  | YES | NO | `` | FK -> Blog(id) |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `id_binh_luan_cha` | `int` |  | YES | NO | `` | FK -> BlogComment(id) |
| `noi_dung` | `nvarchar` | 1000 | YES | NO | `` |  |
| `da_xoa` | `bit` |  | YES | NO | `((0))` |  |
| `ly_do_xoa` | `nvarchar` | 255 | YES | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `ngay_xoa` | `datetime` |  | YES | NO | `` |  |

---

## Table: `ChatConversation`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_khach_hang` | `int` |  | YES | NO | `` | FK -> KhachHang(id) |
| `session_id` | `varchar` | 100 | YES | NO | `` |  |
| `trang_thai` | `varchar` | 20 | YES | NO | `('ACTIVE')` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `tieu_de` | `nvarchar` | 255 | YES | NO | `` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `IX_ChatConversation_Session_Unique` | `session_id` | YES |

---

## Table: `ChatFeedback`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tin_nhan` | `bigint` |  | NO | NO | `` | FK -> ChatMessage(id) |
| `diem_danh_gia` | `bit` |  | NO | NO | `` |  |
| `noi_dung` | `nvarchar` | 500 | YES | NO | `` |  |
| `ngay_tao` | `datetime` |  | NO | NO | `(getdate())` |  |

---

## Table: `ChatMessage`
* **Row Count**: 6

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `bigint` |  | NO | YES | `` | PK |
| `id_cuoc_tro_chuyen` | `int` |  | NO | NO | `` | FK -> ChatConversation(id) |
| `loai_nguoi_gui` | `varchar` | 10 | NO | NO | `` |  |
| `noi_dung` | `nvarchar` | MAX | NO | NO | `` |  |
| `thoi_gian` | `datetime` |  | NO | NO | `(getdate())` |  |

---

## Table: `CommentModerationKeyword`
* **Row Count**: 1

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `tu_khoa` | `nvarchar` | 100 | NO | NO | `` |  |
| `kich_hoat` | `bit` |  | YES | NO | `((1))` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UQ_CommentModerationKeyword_TuKhoa` | `tu_khoa` | YES |

---

## Table: `CommentViolationLog`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | NO | NO | `` |  |
| `id_danh_gia` | `int` |  | YES | NO | `` | FK -> DanhGia(id) |
| `id_san_pham` | `int` |  | NO | NO | `` |  |
| `noi_dung_goc` | `nvarchar` | MAX | NO | NO | `` |  |
| `noi_dung_da_loc` | `nvarchar` | MAX | NO | NO | `` |  |
| `muc_do_vi_pham` | `nvarchar` | 50 | NO | NO | `` |  |
| `so_lan_vi_pham` | `int` |  | NO | NO | `` |  |
| `thoi_han_khoa` | `nvarchar` | 100 | YES | NO | `` |  |
| `ngay_vi_pham` | `datetime` |  | NO | NO | `(getdate())` |  |
| `ngay_tao` | `datetime` |  | NO | NO | `(getdate())` |  |

---

## Table: `DanhGia`
* **Row Count**: 1

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_san_pham` | `int` |  | YES | NO | `` | FK -> SanPham(id) |
| `id_khach_hang` | `int` |  | YES | NO | `` | FK -> KhachHang(id) |
| `so_sao` | `float` |  | NO | NO | `` |  |
| `noi_dung` | `nvarchar` | MAX | YES | NO | `` |  |
| `an_binh_luan` | `bit` |  | YES | NO | `((0))` |  |
| `an_hinh_anh` | `bit` |  | YES | NO | `((0))` |  |
| `da_xoa` | `bit` |  | YES | NO | `((0))` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `(getdate())` |  |
| `ngay_xoa` | `datetime` |  | YES | NO | `` |  |
| `ngay_an_binh_luan` | `datetime` |  | YES | NO | `` |  |
| `ngay_hien_binh_luan` | `datetime` |  | YES | NO | `` |  |
| `ngay_an_hinh_anh` | `datetime` |  | YES | NO | `` |  |
| `ngay_hien_hinh_anh` | `datetime` |  | YES | NO | `` |  |
| `id_nguoi_xoa` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `id_nguoi_an_binh_luan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `id_nguoi_hien_binh_luan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `id_nguoi_an_hinh_anh` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `id_nguoi_hien_hinh_anh` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UX_DanhGia_KH_SP_Active` | `id_khach_hang` | YES |
| `UX_DanhGia_KH_SP_Active` | `id_san_pham` | YES |

---

## Table: `DanhGiaAnh`
* **Row Count**: 1

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_danh_gia` | `int` |  | YES | NO | `` | FK -> DanhGia(id) |
| `duong_dan` | `varchar` | MAX | NO | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `DanhMuc`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ten_danh_muc` | `nvarchar` | 100 | NO | NO | `` |  |
| `mo_ta` | `nvarchar` | 500 | YES | NO | `` |  |
| `trang_thai` | `bit` |  | YES | NO | `((1))` |  |

---

## Table: `DonViVanChuyen`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ma_don_vi` | `varchar` | 50 | YES | NO | `` |  |
| `ten_don_vi` | `nvarchar` | 100 | YES | NO | `` |  |
| `phien_ban` | `bigint` |  | YES | NO | `((0))` |  |
| `so_hotline` | `varchar` | 20 | YES | NO | `` |  |
| `trang_web` | `varchar` | 100 | YES | NO | `` |  |
| `ma_token` | `varchar` | 255 | YES | NO | `` |  |
| `ma_client` | `varchar` | 100 | YES | NO | `` |  |
| `dia_chi_kho` | `nvarchar` | 500 | YES | NO | `` |  |
| `phi_noi_dia` | `decimal` | 18,2 | YES | NO | `` |  |
| `phi_toan_quoc` | `decimal` | 18,2 | YES | NO | `` |  |

---

## Table: `DotGiamGia`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ten_dot` | `nvarchar` | 150 | YES | NO | `` |  |
| `id_nhan_vien` | `int` |  | YES | NO | `` | FK -> NhanVien(id) |
| `phan_tram_giam` | `int` |  | YES | NO | `` |  |
| `ngay_bat_dau` | `datetime` |  | NO | NO | `` |  |
| `ngay_ket_thuc` | `datetime` |  | NO | NO | `` |  |
| `trang_thai` | `varchar` | 50 | YES | NO | `('ACTIVE')` |  |
| `ten_chien_dich` | `nvarchar` | 255 | YES | NO | `` |  |
| `loai_giam_gia` | `nvarchar` | 100 | YES | NO | `` |  |
| `kich_hoat` | `bit` |  | YES | NO | `((1))` |  |

---

## Table: `EditLog`
* **Row Count**: 36

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `bigint` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `ten_bang` | `varchar` | 100 | NO | NO | `` |  |
| `id_ban_ghi` | `bigint` |  | NO | NO | `` |  |
| `hanh_dong` | `varchar` | 20 | NO | NO | `` |  |
| `gia_tri_cu` | `nvarchar` | MAX | YES | NO | `` |  |
| `gia_tri_moi` | `nvarchar` | MAX | YES | NO | `` |  |
| `thoi_gian` | `datetime` |  | NO | NO | `(getdate())` |  |
| `dia_chi_ip` | `varchar` | 50 | YES | NO | `` |  |
| `ghi_chu` | `varchar` | 500 | YES | NO | `` |  |
| `vai_tro_thuc_hien` | `varchar` | 20 | YES | NO | `` |  |

---

## Table: `flyway_schema_history`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `installed_rank` | `int` |  | NO | NO | `` | PK |
| `version` | `nvarchar` | 50 | YES | NO | `` |  |
| `description` | `nvarchar` | 200 | YES | NO | `` |  |
| `type` | `nvarchar` | 20 | NO | NO | `` |  |
| `script` | `nvarchar` | 1000 | NO | NO | `` |  |
| `checksum` | `int` |  | YES | NO | `` |  |
| `installed_by` | `nvarchar` | 100 | NO | NO | `` |  |
| `installed_on` | `datetime` |  | NO | NO | `(getdate())` |  |
| `execution_time` | `int` |  | NO | NO | `` |  |
| `success` | `bit` |  | NO | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `flyway_schema_history_s_idx` | `success` | NO |

---

## Table: `GiaoDichThanhToan`
* **Row Count**: 6

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_hoa_don` | `int` |  | YES | NO | `` | FK -> HoaDon(id) |
| `ma_giao_dich` | `varchar` | 100 | YES | NO | `` |  |
| `gateway` | `varchar` | 50 | YES | NO | `` |  |
| `so_tien` | `decimal` | 18,2 | YES | NO | `` |  |
| `status` | `varchar` | 50 | YES | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `cong_thanh_toan` | `varchar` | 50 | NO | NO | `` |  |
| `trang_thai` | `varchar` | 50 | NO | NO | `` |  |
| `du_lieu_tho` | `nvarchar` | MAX | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UQ_GiaoDichThanhToan_Ma` | `ma_giao_dich` | YES |
| `IX_GiaoDichThanhToan_HoaDon` | `id_hoa_don` | NO |
| `IX_GiaoDichThanhToan_MaGiaoDich` | `ma_giao_dich` | NO |

---

## Table: `GioHang`
* **Row Count**: 11

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_khach_hang` | `int` |  | YES | NO | `` | FK -> KhachHang(id) |
| `session_id` | `varchar` | 100 | YES | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `GioHangChiTiet`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_gio_hang` | `int` |  | YES | NO | `` | FK -> GioHang(id) |
| `id_san_pham_chi_tiet` | `int` |  | YES | NO | `` | FK -> SanPhamChiTiet(id) |
| `id_trang_thai` | `int` |  | YES | NO | `` | FK -> TrangThaiGioHang(id) |
| `so_luong` | `int` |  | NO | NO | `` |  |
| `ngay_them` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `HinhAnhSanPham`
* **Row Count**: 23

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_san_pham_chi_tiet` | `int` |  | YES | NO | `` | FK -> SanPhamChiTiet(id) |
| `duong_dan` | `varchar` | MAX | NO | NO | `` |  |
| `la_anh_chinh` | `bit` |  | YES | NO | `((0))` |  |
| `mau_sac` | `nvarchar` | 50 | YES | NO | `` |  |

---

## Table: `HoaDon`
* **Row Count**: 11

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ma_don_hang` | `varchar` | 50 | YES | NO | `` |  |
| `id_khach_hang` | `int` |  | YES | NO | `` | FK -> KhachHang(id) |
| `id_nhan_vien` | `int` |  | YES | NO | `` | FK -> NhanVien(id) |
| `id_phuong_thuc_thanh_toan` | `int` |  | YES | NO | `` | FK -> PhuongThucThanhToan(id) |
| `id_don_vi_van_chuyen` | `int` |  | YES | NO | `` | FK -> DonViVanChuyen(id) |
| `id_dia_chi` | `int` |  | YES | NO | `` | FK -> SoDiaChi(id) |
| `id_phieu_giam_gia` | `int` |  | YES | NO | `` | FK -> PhieuGiamGia(id) |
| `trang_thai_don_hang` | `nvarchar` | 50 | YES | NO | `(N'cho_xac_nhan')` |  |
| `trang_thai_thanh_toan` | `varchar` | 50 | YES | NO | `('CHO_THANH_TOAN')` |  |
| `tong_tien` | `decimal` | 18,2 | YES | NO | `((0))` |  |
| `so_tien_giam_gia` | `decimal` | 18,2 | YES | NO | `((0))` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `phi_van_chuyen` | `decimal` | 18,2 | YES | NO | `((0))` |  |
| `ten_nguoi_nhan` | `nvarchar` | 100 | YES | NO | `` |  |
| `sdt_nhan` | `varchar` | 15 | YES | NO | `` |  |
| `dia_chi_nhan` | `nvarchar` | 255 | YES | NO | `` |  |
| `ghi_chu` | `nvarchar` | 500 | YES | NO | `` |  |
| `ma_giao_dich` | `nvarchar` | 100 | YES | NO | `` |  |
| `nguoi_xac_nhan_thanh_toan` | `nvarchar` | 100 | YES | NO | `` |  |
| `thoi_gian_xac_nhan` | `datetime` |  | YES | NO | `` |  |
| `phuong_thuc_thanh_toan` | `varchar` | 50 | YES | NO | `` |  |
| `phan_hoi_cong_tt` | `nvarchar` | MAX | YES | NO | `` |  |
| `ngay_thanh_toan` | `datetime` |  | YES | NO | `` |  |
| `ma_giao_dich_ung_dung` | `varchar` | 100 | YES | NO | `` |  |
| `ghn_order_code` | `varchar` | 50 | YES | NO | `` |  |
| `ghn_status` | `varchar` | 100 | YES | NO | `` |  |
| `ghn_to_district_id` | `int` |  | YES | NO | `` |  |
| `ghn_to_ward_code` | `varchar` | 20 | YES | NO | `` |  |
| `trang_thai_hoan_hang` | `varchar` | 50 | YES | NO | `` |  |
| `ngay_xac_nhan_hoan_hang` | `datetime` |  | YES | NO | `` |  |
| `ma_giam_gia_ap_dung` | `varchar` | 50 | YES | NO | `` |  |
| `ten_giam_gia_ap_dung` | `varchar` | 255 | YES | NO | `` |  |
| `mo_ta_giam_gia_snapshot` | `varchar` | 500 | YES | NO | `` |  |
| `id_nhan_vien_xac_nhan` | `int` |  | YES | NO | `` | FK -> NhanVien(id) |
| `id_nhan_vien_xac_nhan_hoan_tien` | `int` |  | YES | NO | `` | FK -> NhanVien(id) |
| `loai_don_hang` | `varchar` | 30 | YES | NO | `` |  |
| `tong_tien_hang` | `decimal` | 18,2 | YES | NO | `` |  |
| `so_tien_giam_voucher` | `decimal` | 18,2 | YES | NO | `` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UQ_HoaDon_Ma` | `ma_don_hang` | YES |
| `IX_HoaDon_KhachHang_NgayTao` | `id_khach_hang` | NO |
| `IX_HoaDon_KhachHang_NgayTao` | `ngay_tao` | NO |
| `IX_HoaDon_LoaiDonHang_NgayTao` | `loai_don_hang` | NO |
| `IX_HoaDon_LoaiDonHang_NgayTao` | `ngay_tao` | NO |

---

## Table: `HoaDonChiTiet`
* **Row Count**: 12

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_hoa_don` | `int` |  | YES | NO | `` | FK -> HoaDon(id) |
| `id_san_pham_chi_tiet` | `int` |  | YES | NO | `` | FK -> SanPhamChiTiet(id) |
| `so_luong` | `int` |  | NO | NO | `` |  |
| `don_gia` | `decimal` | 18,2 | NO | NO | `` |  |
| `thanh_tien` | `decimal` | 18,2 | YES | NO | `` |  |
| `gia_niem_yet` | `decimal` | 18,2 | YES | NO | `` |  |
| `phan_tram_giam` | `decimal` | 18,2 | YES | NO | `` |  |
| `so_tien_giam_san_pham` | `decimal` | 18,2 | YES | NO | `` |  |
| `ten_dot_giam_gia` | `nvarchar` | 100 | YES | NO | `` |  |
| `id_dot_giam_gia` | `int` |  | YES | NO | `` |  |
| `ten_san_pham_snapshot` | `nvarchar` | 255 | YES | NO | `` |  |
| `ma_hang_snapshot` | `varchar` | 100 | YES | NO | `` |  |
| `thuoc_tinh_snapshot` | `nvarchar` | 500 | YES | NO | `` |  |
| `thuong_hieu_snapshot` | `nvarchar` | 100 | YES | NO | `` |  |
| `danh_muc_snapshot` | `nvarchar` | 100 | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `IX_HoaDonChiTiet_HoaDon` | `id_hoa_don` | NO |

---

## Table: `KhachHang`
* **Row Count**: 9

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `ho_kh` | `nvarchar` | 50 | YES | NO | `` |  |
| `ten_kh` | `nvarchar` | 50 | YES | NO | `` |  |
| `sdt` | `varchar` | 15 | YES | NO | `` |  |
| `nhan_ban_tin` | `bit` |  | YES | NO | `((0))` |  |
| `la_tai_khoan_noi_bo` | `bit` |  | YES | NO | `((0))` |  |
| `so_dien_thoai_kh` | `varchar` | 15 | NO | NO | `` |  |
| `loai_khach_hang` | `varchar` | 30 | YES | NO | `` |  |
| `nguon_tao` | `varchar` | 50 | YES | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UX_KhachHang_TaiKhoan` | `id_tai_khoan` | YES |
| `UX_KhachHang_SoDienThoaiKh` | `so_dien_thoai_kh` | YES |

---

## Table: `LichSuTrangThaiDonHang`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_hoa_don` | `int` |  | YES | NO | `` | FK -> HoaDon(id) |
| `trang_thai_cu` | `nvarchar` | 50 | YES | NO | `` |  |
| `trang_thai_moi` | `nvarchar` | 50 | NO | NO | `` |  |
| `ghi_chu` | `nvarchar` | 255 | YES | NO | `` |  |
| `nguoi_thuc_hien` | `varchar` | 100 | YES | NO | `` |  |
| `thoi_gian` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `MaKhoiPhuc`
* **Row Count**: 109

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `token` | `varchar` | 255 | YES | NO | `` |  |
| `loai_xac_nhan` | `varchar` | 10 | YES | NO | `` |  |
| `da_su_dung` | `bit` |  | YES | NO | `((0))` |  |
| `ngay_het_han` | `datetime` |  | YES | NO | `` |  |
| `ma_xac_nhan` | `varchar` | 255 | NO | NO | `` |  |
| `thoi_gian_het_han` | `datetime` |  | NO | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `IX_MaKhoiPhuc_Token_Unique` | `token` | YES |

---

## Table: `NhanVien`
* **Row Count**: 3

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `ho_ten_nv` | `nvarchar` | 100 | YES | NO | `` |  |
| `ho_ten` | `nvarchar` | 100 | NO | NO | `` |  |
| `chuc_vu` | `nvarchar` | 100 | NO | NO | `` |  |
| `so_dien_thoai` | `varchar` | 15 | NO | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UX_NhanVien_TaiKhoan` | `id_tai_khoan` | YES |
| `UX_NhanVien_SoDienThoai` | `so_dien_thoai` | YES |

---

## Table: `PhieuGiamGia`
* **Row Count**: 1

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ma_phieu` | `varchar` | 50 | NO | NO | `` |  |
| `ten_phieu` | `nvarchar` | 100 | YES | NO | `` |  |
| `id_nhan_vien` | `int` |  | YES | NO | `` | FK -> NhanVien(id) |
| `don_vi` | `varchar` | 10 | YES | NO | `` |  |
| `gia_tri` | `decimal` | 18,2 | NO | NO | `` |  |
| `gia_tri_giam_toi_da` | `decimal` | 18,2 | YES | NO | `` |  |
| `gia_tri_don_hang_toi_thieu` | `decimal` | 18,2 | YES | NO | `((0))` |  |
| `so_luong_con_lai` | `int` |  | YES | NO | `((0))` |  |
| `ngay_bat_dau` | `datetime` |  | NO | NO | `` |  |
| `ngay_ket_thuc` | `datetime` |  | NO | NO | `` |  |
| `trang_thai` | `varchar` | 50 | YES | NO | `('ACTIVE')` |  |
| `loai_giam_gia` | `nvarchar` | 100 | YES | NO | `` |  |
| `kich_hoat` | `bit` |  | YES | NO | `((1))` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UQ_PhieuGiamGia_Ma` | `ma_phieu` | YES |
| `IX_PhieuGiamGia_MaPhieu` | `ma_phieu` | NO |

---

## Table: `PhuongThucThanhToan`
* **Row Count**: 6

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ma_phuong_thuc` | `varchar` | 50 | YES | NO | `` |  |
| `ten_phuong_thuc` | `nvarchar` | 100 | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `IX_PTTT_Ma_Unique` | `ma_phuong_thuc` | YES |

---

## Table: `SanPham`
* **Row Count**: 23

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_danh_muc` | `int` |  | YES | NO | `` | FK -> DanhMuc(id) |
| `id_thuong_hieu` | `int` |  | YES | NO | `` | FK -> ThuongHieu(id) |
| `id_nhan_vien` | `int` |  | YES | NO | `` | FK -> NhanVien(id) |
| `ten_san_pham` | `nvarchar` | 255 | NO | NO | `` |  |
| `mo_ta` | `nvarchar` | MAX | YES | NO | `` |  |
| `diem_trung_binh` | `float` |  | NO | NO | `((0.0))` |  |
| `so_danh_gia` | `int` |  | YES | NO | `((0))` |  |
| `trang_thai` | `varchar` | 50 | YES | NO | `('dang_ban')` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `SanPham_DotGiamGia`
* **Row Count**: 46

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id_san_pham` | `int` |  | NO | NO | `` | PK |
| `id_dot_giam_gia` | `int` |  | NO | NO | `` | FK -> DotGiamGia(id) |

---

## Table: `SanPhamChiTiet`
* **Row Count**: 25

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_san_pham` | `int` |  | YES | NO | `` | FK -> SanPham(id) |
| `SKU` | `varchar` | 100 | YES | NO | `` |  |
| `barcode` | `varchar` | 100 | YES | NO | `` |  |
| `mau_sac` | `nvarchar` | 50 | NO | NO | `` |  |
| `kich_thuoc` | `nvarchar` | 50 | YES | NO | `` |  |
| `chat_lieu` | `nvarchar` | 50 | YES | NO | `` |  |
| `trong_luong` | `varchar` | 50 | YES | NO | `` |  |
| `gia_nhap` | `decimal` | 18,2 | YES | NO | `((0))` |  |
| `gia_ban` | `decimal` | 18,2 | NO | NO | `` |  |
| `so_luong_ton` | `int` |  | YES | NO | `((0))` |  |
| `trang_thai` | `varchar` | 50 | YES | NO | `('dang_ban')` |  |
| `muc_cang` | `nvarchar` | 20 | NO | NO | `` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `IX_SPCT_SKU_Unique` | `SKU` | YES |
| `IX_SPCT_Barcode_Unique` | `barcode` | YES |
| `UQ_SanPhamChiTiet_UniqueAttrs` | `id_san_pham` | YES |
| `UQ_SanPhamChiTiet_UniqueAttrs` | `mau_sac` | YES |
| `UQ_SanPhamChiTiet_UniqueAttrs` | `trong_luong` | YES |
| `UQ_SanPhamChiTiet_UniqueAttrs` | `muc_cang` | YES |
| `IX_SanPhamChiTiet_SanPham` | `id_san_pham` | NO |

---

## Table: `SanPhamYeuThich`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_khach_hang` | `int` |  | YES | NO | `` | FK -> KhachHang(id) |
| `id_san_pham` | `int` |  | YES | NO | `` | FK -> SanPham(id) |
| `ngay_them` | `datetime` |  | YES | NO | `(getdate())` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UX_SanPhamYeuThich_KH_SP` | `id_khach_hang` | YES |
| `UX_SanPhamYeuThich_KH_SP` | `id_san_pham` | YES |

---

## Table: `SoDiaChi`
* **Row Count**: 4

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_khach_hang` | `int` |  | YES | NO | `` | FK -> KhachHang(id) |
| `ho_nguoi_nhan` | `nvarchar` | 50 | YES | NO | `` |  |
| `ten_nguoi_nhan` | `nvarchar` | 50 | YES | NO | `` |  |
| `sdt_nguoi_nhan` | `varchar` | 15 | YES | NO | `` |  |
| `dia_chi_cu_the` | `nvarchar` | 255 | YES | NO | `` |  |
| `tinh_thanh` | `nvarchar` | 100 | YES | NO | `` |  |
| `quoc_gia` | `nvarchar` | 100 | YES | NO | `` |  |
| `latitude` | `float` |  | YES | NO | `` |  |
| `longitude` | `float` |  | YES | NO | `` |  |
| `la_mac_dinh_giao_hang` | `bit` |  | YES | NO | `((0))` |  |
| `la_mac_dinh_thanh_toan` | `bit` |  | YES | NO | `((0))` |  |
| `thanh_pho` | `nvarchar` | 100 | YES | NO | `` |  |
| `vi_do` | `float` |  | YES | NO | `` |  |
| `kinh_do` | `float` |  | YES | NO | `` |  |
| `province_id` | `int` |  | YES | NO | `` |  |
| `district_id` | `int` |  | YES | NO | `` |  |
| `ward_code` | `varchar` | 20 | YES | NO | `` |  |
| `province_name` | `nvarchar` | 100 | YES | NO | `` |  |
| `district_name` | `nvarchar` | 100 | YES | NO | `` |  |
| `ward_name` | `nvarchar` | 100 | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `IX_SoDiaChi_KhachHang` | `id_khach_hang` | NO |

---

## Table: `sysdiagrams`
* **Row Count**: 0

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `name` | `nvarchar` | 128 | NO | NO | `` |  |
| `principal_id` | `int` |  | NO | NO | `` |  |
| `diagram_id` | `int` |  | NO | YES | `` | PK |
| `version` | `int` |  | YES | NO | `` |  |
| `definition` | `varbinary` |  | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UK_principal_name` | `principal_id` | YES |
| `UK_principal_name` | `name` | YES |

---

## Table: `TaiKhoan`
* **Row Count**: 11

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `email` | `varchar` | 255 | NO | NO | `` |  |
| `mat_khau` | `nvarchar` | 255 | YES | NO | `` |  |
| `vai_tro` | `varchar` | 5 | YES | NO | `` |  |
| `trang_thai` | `nvarchar` | 50 | NO | NO | `(N'hoat_dong')` |  |
| `trang_thai_tai_khoan` | `varchar` | 50 | NO | NO | `('GUEST')` |  |
| `so_lan_nhac_nho_vi_pham` | `int` |  | YES | NO | `((0))` |  |
| `so_lan_mua_thanh_cong` | `int` |  | YES | NO | `((0))` |  |
| `token_xac_thuc_khoa` | `varchar` | 100 | YES | NO | `` |  |
| `ngay_khoa_binh_luan_den` | `datetime` |  | YES | NO | `` |  |
| `ngay_vi_pham_gan_nhat` | `datetime` |  | YES | NO | `` |  |
| `la_khach_hang` | `bit` |  | YES | NO | `` |  |
| `la_nhan_vien` | `bit` |  | YES | NO | `` |  |
| `la_quan_ly` | `bit` |  | YES | NO | `` |  |

### Indexes:
| Index Name | Column | Unique |
|---|---|---|
| `UQ_TaiKhoan_Email` | `email` | YES |

---

## Table: `ThongBao`
* **Row Count**: 3

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_tai_khoan` | `int` |  | YES | NO | `` | FK -> TaiKhoan(id) |
| `tieu_de` | `nvarchar` | 255 | NO | NO | `` |  |
| `noi_dung` | `nvarchar` | MAX | NO | NO | `` |  |
| `loai_thong_bao` | `varchar` | 50 | YES | NO | `` |  |
| `da_doc` | `bit` |  | YES | NO | `((0))` |  |
| `ngay_tao` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `ThuongHieu`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ten_thuong_hieu` | `nvarchar` | 100 | NO | NO | `` |  |
| `mo_ta` | `nvarchar` | 500 | YES | NO | `` |  |
| `trang_thai` | `bit` |  | YES | NO | `((1))` |  |

---

## Table: `TichHopVanChuyen`
* **Row Count**: 1

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `id_hoa_don` | `int` |  | YES | NO | `` | FK -> HoaDon(id) |
| `ma_van_don` | `varchar` | 100 | YES | NO | `` |  |
| `trang_thai_ghn` | `varchar` | 50 | YES | NO | `` |  |
| `ngay_cap_nhat` | `datetime` |  | YES | NO | `(getdate())` |  |

---

## Table: `TrangThaiGioHang`
* **Row Count**: 2

### Columns:
| Column | Data Type | Length/Prec/Scale | Nullable | Identity | Default | Key |
|---|---|---|---|---|---|---|
| `id` | `int` |  | NO | YES | `` | PK |
| `ten_trang_thai` | `nvarchar` | 50 | NO | NO | `` |  |

---
