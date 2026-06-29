# Database Schema Summary - SMDB_FINAL

Tóm tắt cấu trúc cơ sở dữ liệu thực tế kết nối: **SMDB_FINAL**

* **Tổng số bảng thực tế**: 37

## Danh sách các bảng và số lượng bản ghi:

| Tên Bảng | Số lượng dòng (Row Count) | Khóa chính (PK) |
|---|---|---|
| `Blog` | 4 | `id` |
| `BlogComment` | 0 | `id` |
| `ChatConversation` | 2 | `id` |
| `ChatFeedback` | 0 | `id` |
| `ChatMessage` | 6 | `id` |
| `CommentModerationKeyword` | 1 | `id` |
| `CommentViolationLog` | 0 | `id` |
| `DanhGia` | 1 | `id` |
| `DanhGiaAnh` | 1 | `id` |
| `DanhMuc` | 2 | `id` |
| `DonViVanChuyen` | 2 | `id` |
| `DotGiamGia` | 2 | `id` |
| `EditLog` | 36 | `id` |
| `flyway_schema_history` | 0 | `installed_rank` |
| `GiaoDichThanhToan` | 6 | `id` |
| `GioHang` | 11 | `id` |
| `GioHangChiTiet` | 2 | `id` |
| `HinhAnhSanPham` | 23 | `id` |
| `HoaDon` | 11 | `id` |
| `HoaDonChiTiet` | 12 | `id` |
| `KhachHang` | 9 | `id` |
| `LichSuTrangThaiDonHang` | 0 | `id` |
| `MaKhoiPhuc` | 109 | `id` |
| `NhanVien` | 3 | `id` |
| `PhieuGiamGia` | 1 | `id` |
| `PhuongThucThanhToan` | 6 | `id` |
| `SanPham` | 23 | `id` |
| `SanPham_DotGiamGia` | 46 | `id_san_pham` |
| `SanPhamChiTiet` | 25 | `id` |
| `SanPhamYeuThich` | 0 | `id` |
| `SoDiaChi` | 4 | `id` |
| `sysdiagrams` | 0 | `diagram_id` |
| `TaiKhoan` | 11 | `id` |
| `ThongBao` | 3 | `id` |
| `ThuongHieu` | 2 | `id` |
| `TichHopVanChuyen` | 1 | `id` |
| `TrangThaiGioHang` | 2 | `id` |