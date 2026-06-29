# Database Schema Cleanup Verification Report - DBSM1

Bản báo cáo này thực hiện xác minh bổ sung cho các bảng và cột đang bị đánh dấu `SQLSERVER_ONLY` hoặc `TYPE_MISMATCH` / `NULLABLE_MISMATCH` nhằm đưa ra các đề xuất dọn dẹp (cleanup) an toàn và chuẩn xác.

---

## 1. Kiểm Tra Toàn Diện Bảng `TichHopVanChuyen`

### A. Kết quả quét mã nguồn (Source Code Usage)
Hệ thống đã thực hiện tìm kiếm toàn bộ các từ khóa: `TichHopVanChuyen`, `tich_hop_van_chuyen`, `id_hoa_don`, `ghn`, `ghn_order_code`, `shipping`, `van_chuyen`, `vận chuyển` trên toàn bộ dự án (`Entity`, `Repository`, `Service`, `Controller`, `DTO`, `templates`, `static JS`, `test`):
* **Kết quả**: Bảng `TichHopVanChuyen` **hoàn toàn không được tham chiếu hay khai báo** trong bất kỳ tệp nguồn Java hay tệp giao diện Thymeleaf/JS nào.
* **Thay thế nghiệp vụ**: Trong cấu trúc thực tế hiện tại, các thông tin tích hợp GHN như mã vận đơn (`ghn_order_code`) và trạng thái vận chuyển (`ghn_status`) đã được **tích hợp trực tiếp** thành các trường thuộc Entity [HoaDon.java](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/entity/HoaDon.java).

### B. Thống kê dữ liệu thực tế (Row Count & Data Sample)
* **Tổng số dòng (Row Count)**: 1 dòng.
* **Mẫu dữ liệu thực tế (TOP 20 - Đã che thông tin nhạy cảm)**:
  * `id`: 1
  * `id_hoa_don`: 89 *(Hóa đơn DHSVN20260624103836-17542A)*
  * `ma_van_don`: NULL
  * `trang_thai_ghn`: NULL
  * `ngay_cap_nhat`: `2026-06-24 18:02:41.263`

### C. Đánh giá trạng thái
* **Phân loại**: **LEGACY_WITH_DATA** / **POSSIBLY_UNUSED_NEEDS_REVIEW**
* **Kết luận**: Bảng chứa 1 dòng dữ liệu liên kết với Hóa đơn 89 nhưng các cột nghiệp vụ vận chuyển đều là `NULL`. Đây là tàn dư thiết kế cũ và không còn bất kỳ luồng nghiệp vụ nào sử dụng.

---

## 2. Thống Kê & So Sánh Các Cột `SQLSERVER_ONLY`

Dưới đây là thống kê chi tiết từ database **DBSM1** và so sánh với cột mới tương ứng:

### 1. `DotGiamGia.ten_dot` vs `DotGiamGia.ten_chien_dich`
* **Legacy Column (`ten_dot`)**: 2 dòng NULL, 0 dòng chứa dữ liệu. Distinct = 0.
* **Modern Column (`ten_chien_dich`)**: 2 dòng có dữ liệu. Distinct = 2.
* **Trạng thái**: **LEGACY_EMPTY**. Dữ liệu đã chuyển dịch hoàn toàn sang cột mới.

### 2. `DotGiamGia.trang_thai` vs `DotGiamGia.kich_hoat`
* **Legacy Column (`trang_thai`)**: 2 dòng chứa giá trị `'ACTIVE'`. Distinct = 1.
* **Modern Column (`kich_hoat`)**: 2 dòng chứa giá trị `1` (bit). Distinct = 2.
* **Trạng thái**: **LEGACY_WITH_DATA** (Trùng thông tin). Cột mới sử dụng kiểu `bit` thay thế cho kiểu chuỗi cũ.

### 3. `GiaoDichThanhToan.gateway` vs `GiaoDichThanhToan.cong_thanh_toan`
* **Legacy Column (`gateway`)**: 5 dòng NULL, 0 dòng có dữ liệu. Distinct = 0.
* **Modern Column (`cong_thanh_toan`)**: 5 dòng chứa dữ liệu. Distinct = 2.
* **Trạng thái**: **LEGACY_EMPTY**. Cột cũ đã trống hoàn toàn.

### 4. `GiaoDichThanhToan.status` vs `GiaoDichThanhToan.trang_thai`
* **Legacy Column (`status`)**: 5 dòng NULL, 0 dòng có dữ liệu. Distinct = 0.
* **Modern Column (`trang_thai`)**: 5 dòng chứa dữ liệu. Distinct = 1.
* **Trạng thái**: **LEGACY_EMPTY**. Cột cũ đã trống hoàn toàn.

### 5. `HoaDon.so_tien_giam_gia` vs `HoaDon.so_tien_giam_voucher`
* **Legacy Column (`so_tien_giam_gia`)**: 9 dòng chứa giá trị `0.00` (`.00`). Distinct = 1.
* **Modern Column (`so_tien_giam_voucher`)**: 9 dòng chứa dữ liệu giảm giá thực tế. Distinct = 1.
* **Trạng thái**: **LEGACY_WITH_DATA** (Trống thực tế).

### 6. `KhachHang.sdt` vs `KhachHang.so_dien_thoai_kh`
* **Legacy Column (`sdt`)**: 8 dòng NULL, 0 dòng có dữ liệu. Distinct = 0.
* **Modern Column (`so_dien_thoai_kh`)**: 8 dòng chứa dữ liệu. Distinct = 4.
* **Trạng thái**: **LEGACY_EMPTY**. Cột cũ trống hoàn toàn.

### 7. `MaKhoiPhuc.token` vs `MaKhoiPhuc.ma_xac_nhan`
* **Legacy Column (`token`)**: 12 dòng có giá trị UUID, 8 dòng NULL. Distinct = 12.
* **Modern Column (`ma_xac_nhan`)**: 20 dòng có dữ liệu (mã xác nhận). Distinct = 20.
* **Trạng thái**: **LEGACY_WITH_DATA**. Cột cũ lưu trữ token khôi phục UUID lịch sử.

### 8. `MaKhoiPhuc.ngay_het_han` vs `MaKhoiPhuc.thoi_gian_het_han`
* **Legacy Column (`ngay_het_han`)**: 12 dòng chứa ngày hết hạn, 8 dòng NULL. Distinct = 12.
* **Modern Column (`thoi_gian_het_han`)**: 20 dòng chứa dữ liệu. Distinct = 20.
* **Trạng thái**: **LEGACY_WITH_DATA**.

### 9. `NhanVien.ho_ten_nv` vs `NhanVien.ho_ten`
* **Legacy Column (`ho_ten_nv`)**: 3 dòng NULL, 0 dòng có dữ liệu. Distinct = 0.
* **Modern Column (`ho_ten`)**: 3 dòng có dữ liệu. Distinct = 3.
* **Trạng thái**: **LEGACY_EMPTY**.

### 10. `SoDiaChi.latitude` vs `SoDiaChi.vi_do`
* **Legacy Column (`latitude`)**: 2 dòng NULL, 0 dòng có dữ liệu. Distinct = 0.
* **Modern Column (`vi_do`)**: 1 dòng có dữ liệu, 1 dòng NULL. Distinct = 1.
* **Trạng thái**: **LEGACY_EMPTY**.

### 11. `SoDiaChi.longitude` vs `SoDiaChi.kinh_do`
* **Legacy Column (`longitude`)**: 2 dòng NULL, 0 dòng có dữ liệu. Distinct = 0.
* **Modern Column (`kinh_do`)**: 1 dòng có dữ liệu, 1 dòng NULL. Distinct = 1.
* **Trạng thái**: **LEGACY_EMPTY**.

---

## 3. Phân Loại Trạng Thái Schema

| Bảng / Cột | Trạng thái phân loại | Chi tiết / Đánh giá rủi ro |
|---|---|---|
| Bảng `LichSuTrangThaiDonHang` | **USED_BY_NATIVE_SQL** | Không có Entity nhưng được gọi trực tiếp bởi `HoaDonRepository.java`. **Giữ lại**. |
| Bảng `TichHopVanChuyen` | **POSSIBLY_UNUSED_NEEDS_REVIEW** | Bảng cũ không dùng trong code, có 1 dòng dữ liệu rác. **Xem xét dọn dẹp**. |
| Bảng `flyway_schema_history` | **SAFE_TO_KEEP** | Bảng quản lý lịch sử migration của Flyway. **Giữ lại**. |
| `DotGiamGia.ten_dot` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `ten_chien_dich`. |
| `DotGiamGia.trang_thai` | **MIGRATION_REQUIRED_BEFORE_DROP**| Chứa dữ liệu lịch sử `'ACTIVE'`. Cần xác minh đồng bộ sang `kich_hoat`. |
| `GiaoDichThanhToan.gateway` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `cong_thanh_toan`. |
| `GiaoDichThanhToan.status` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `trang_thai`. |
| `HoaDon.so_tien_giam_gia` | **DROP_CANDIDATE_AFTER_BACKUP** | Chỉ chứa `0.00`. Đã chuyển sang `so_tien_giam_voucher`. |
| `KhachHang.sdt` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `so_dien_thoai_kh`. |
| `MaKhoiPhuc.token` | **MIGRATION_REQUIRED_BEFORE_DROP**| Chứa 12 mã UUID khôi phục cũ. Cần để các token cũ hết hạn tự nhiên trước khi xóa. |
| `MaKhoiPhuc.ngay_het_han` | **MIGRATION_REQUIRED_BEFORE_DROP**| Chứa dữ liệu thời hạn cũ. |
| `NhanVien.ho_ten_nv` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `ho_ten`. |
| `SoDiaChi.latitude` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `vi_do`. |
| `SoDiaChi.longitude` | **DROP_CANDIDATE_AFTER_BACKUP** | Trống hoàn toàn. Đã chuyển sang `kinh_do`. |

---

## 4. Báo Cáo Phân Tích Nullability Mismatch (115 Cột)

Phân tích chi tiết các trường hợp lệch nullability giữa Code và SQL Server:

### 🌟 Nhóm 1: `DB_NOT_NULL_BUT_CODE_MAY_NOT_SET` (34 cột - Nguy cơ trung bình)
Nhóm này gồm các cột có thuộc tính `NOT NULL` trong DB nhưng Entity Java chưa khai báo `@Column(nullable = false)`.
* **Danh sách phát hiện**:
  * Các trường khóa chính: `Blog.id`, `BlogComment.id`, `ChatConversation.id`, `ChatFeedback.id`, `ChatMessage.id`, `CommentModerationKeyword.id`, `CommentViolationLog.id`, `DanhGia.id`, `DanhGiaAnh.id`, `DanhMuc.id`, `DonViVanChuyen.id`, `DotGiamGia.id`, `EditLog.id`, `GiaoDichThanhToan.id`, `GioHang.id`, `GioHangChiTiet.id`, `HinhAnhSanPham.id`, `HoaDon.id`, `HoaDonChiTiet.id`, `KhachHang.id`, `MaKhoiPhuc.id`, `NhanVien.id`, `PhieuGiamGia.id`, `PhuongThucThanhToan.id`, `SanPham.id`, `SanPhamChiTiet.id`, `SoDiaChi.id`, `TaiKhoan.id`, `ThongBao.id`, `ThuongHieu.id`, `TrangThaiGioHang.id`.
  * Các trường thời gian/nghiệp vụ khác: `ChatFeedback.ngay_tao`, `ChatMessage.thoi_gian`, `TrangThaiGioHang.ten_trang_thai`.
* **Đánh giá rủi ro**:
  * **Rất thấp đối với khóa chính (`id`)** do các trường này được thiết lập `IDENTITY(1,1)` tự động sinh ở tầng DB nên không bao giờ bị null khi chèn.
  * **Thấp đối với các trường thời gian** do đã có cấu hình mặc định (ví dụ: `DEFAULT GETDATE()`) ở tầng DB hoặc được gán mặc định tại Java constructor. Tuy nhiên, nên cập nhật thêm thuộc tính `@Column(nullable = false)` để tăng tính đồng bộ.

### 🌟 Nhóm 2: `DB_NULLABLE_BUT_CODE_VALIDATES` (81 cột - Rủi ro cực thấp)
Nhóm các cột mà tầng DB cho phép lưu `NULL` nhưng mã nguồn Entity bắt buộc phải có giá trị thông qua `@Column(nullable = false)`.
* **Danh sách phát hiện**: Bao gồm 81 cột (như `Blog.da_xoa`, `Blog.duong_dan`, `KhachHang.ten_kh`, `SoDiaChi.dia_chi_cu_the`, v.v.).
* **Đánh giá rủi ro**: **Không có rủi ro**. Mã nguồn Java áp đặt kiểm soát chặt chẽ hơn tầng DB, đảm bảo dữ liệu chèn vào luôn hợp lệ và không thể bị Null.

### 🌟 Nhóm 3: `PARSER_UNCERTAIN` (0 cột)
* Không phát hiện cột nào nằm trong nhóm nghi ngờ không xác định.

---

## 5. Khuyến Nghị Quy Trình Dọn Dẹp An Toàn (Checklist)

Tuyệt đối **không chạy script DROP ngay lập tức** trong môi trường Production. Quy trình khuyến nghị thực hiện như sau:

1. **Bước 1: Sao lưu (Backup)**
   * Chạy sao lưu xuất cấu hình lược đồ hiện tại và kết xuất toàn bộ dữ liệu bảng `MaKhoiPhuc`, `DotGiamGia`, `HoaDon` ra tệp `.sql`.
2. **Bước 2: Chờ hết hạn tự nhiên (Cool-off Period)**
   * Với cột `MaKhoiPhuc.token` và `ngay_het_han`: Chờ 24–48 giờ để tất cả các token khôi phục mật khẩu cũ đã gửi qua email của khách hàng hết hạn tự nhiên trước khi drop cột.
3. **Bước 3: Chạy Kiểm Thử Xác Minh**
   * Sau khi drop thử nghiệm trên môi trường Staging/Dev, thực hiện chạy lại bộ test:
     `.\mvnw.cmd test`
     Để xác nhận không có bất kỳ câu truy vấn native nào bị gãy do thiếu cột.
4. **Bước 4: Phòng ngừa rủi ro**
   * Nếu xảy ra lỗi drop nhầm, có thể hồi phục nhanh bằng tệp schema backup được tạo ở Bước 1.
