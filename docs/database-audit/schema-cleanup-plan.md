# Database Schema Cleanup Plan - SMDB_FINAL

Tài liệu này vạch ra kế hoạch dọn dẹp (cleanup) các thành phần lược đồ cơ sở dữ liệu dư thừa trên database **SMDB_FINAL**, phân tích rủi ro, kế hoạch rollback và kế hoạch chạy thử nghiệm (test plan) sau khi dọn dẹp.

---

## 1. Danh Sách Các Cột Được Phép Drop (Phase 1)

Dưới đây là các cột đã được xác minh **100% trống rỗng** (chỉ chứa NULL hoặc giá trị mặc định bằng 0) trên database **SMDB_FINAL** và hoàn toàn không có tham chiếu nào trong mã nguồn Java hay giao diện:

| Table | Column | Legacy Type | Row Count | Null Count | Phân Loại | Khuyến Nghị |
|---|---|---|---|---|---|---|
| `DotGiamGia` | `ten_dot` | `nvarchar` | 2 | 2 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |
| `GiaoDichThanhToan` | `gateway` | `varchar` | 6 | 6 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |
| `GiaoDichThanhToan` | `status` | `varchar` | 6 | 6 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |
| `KhachHang` | `sdt` | `varchar` | 9 | 9 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |
| `NhanVien` | `ho_ten_nv` | `nvarchar` | 3 | 3 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |
| `SoDiaChi` | `latitude` | `float` | 4 | 4 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |
| `SoDiaChi` | `longitude` | `float` | 4 | 4 | **DROP_CANDIDATE_AFTER_BACKUP** | Drop an toàn |

---

## 2. Danh Sách Các Bảng / Cột Chưa Được Drop (Giữ Lại)

Dưới đây là danh sách các thành phần cơ sở dữ liệu **phải giữ lại** hoặc chưa được phép drop ở Phase 1 cùng với lý do kỹ thuật chi tiết:

### A. Bảng `LichSuTrangThaiDonHang`
* **Lý do giữ lại**: Không ánh xạ JPA Entity nhưng đang được tham chiếu bởi các truy vấn SQL thuần (native query) trong [HoaDonRepository.java](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/repository/HoaDonRepository.java) để dọn dẹp lịch sử trạng thái khi xóa hóa đơn.
* **Rủi ro nếu xóa**: Gây lỗi cú pháp SQL và phá vỡ luồng xóa/hủy đơn hàng.

### B. Bảng `TichHopVanChuyen`
* **Lý do giữ lại**: Chứa dữ liệu vận chuyển lịch sử (1 dòng liên kết với hóa đơn ID 89). Việc xóa bảng này cần được thực hiện độc lập sau khi archive dữ liệu cũ.
* **Rủi ro nếu xóa**: Mất vết liên kết vận chuyển lịch sử của hóa đơn 89 (nếu có nhu cầu đối soát).

### C. Bảng `sysdiagrams` (Mới phát hiện trên SMDB_FINAL)
* **Lý do giữ lại**: Đây là bảng hệ thống do SQL Server Management Studio (SSMS) tự tạo để quản lý các sơ đồ quan hệ database (Database Diagrams).
* **Rủi ro nếu xóa**: Mất các diagram đã vẽ trong SSMS.
* **Hành động**: **SAFE_TO_KEEP** (Giữ nguyên).

### D. Các cột `TaiKhoan.la_khach_hang`, `la_nhan_vien`, `la_quan_ly` (Mới phát hiện trên SMDB_FINAL)
* **Lý do giữ lại**: Đây là 3 cột cờ phân quyền cũ của thiết kế ban đầu (`bit`), hiện tại code đã chuyển sang dùng cột chuỗi đơn `vai_tro`.
* **Phân loại**: **MIGRATION_REQUIRED_BEFORE_DROP**.
* **Hành động**: Cần chạy query đối chiếu kiểm tra xem toàn bộ các tài khoản đã được migrate phân quyền chính xác sang cột `vai_tro` (`KH`, `NV`, `QL`) trước khi tiến hành drop 3 cột cờ này.

### E. Cột `MaKhoiPhuc.token` và `ngay_het_han`
* **Lý do giữ lại**: Chứa **12 bản ghi lịch sử** của các mã khôi phục UUID cũ (tổng số dòng trong bảng hiện là 109 dòng, trong đó có 97 dòng dùng mã OTP mới). Việc xóa các cột này ngay lập tức sẽ làm gãy các liên kết khôi phục mật khẩu còn hiệu lực trong hộp thư của người dùng cũ.
* **Rủi ro nếu xóa**: Người dùng click vào link khôi phục cũ sẽ gặp lỗi máy chủ.
* **Hành động**: Đợi các token cũ hết hạn (cool-off period) rồi mới drop.

### F. Cột `DotGiamGia.trang_thai`
* **Lý do giữ lại**: Chứa giá trị lịch sử `'ACTIVE'` phục vụ đối chiếu dữ liệu cũ. Cần chạy đối chiếu kiểm tra đồng bộ sang cột mới `kich_hoat = 1` trước khi drop.

### G. Cột `HoaDon.so_tien_giam_gia`
* **Lý do giữ lại**: Chứa giá trị `0.00` mặc định. Cần rà soát toàn bộ các hóa đơn lịch sử để đảm bảo không có hóa đơn nào ghi nhận tiền giảm giá vào cột này trước khi thực hiện xóa.

---

## 3. Kế Hoạch Sao Lưu & Khôi Phục (Rollback Plan)

### A. Quy trình sao lưu trước khi dọn dẹp
Trước khi chạy kịch bản dọn dẹp với biến `@DoExecute = 1`, quản trị viên hệ thống **bắt buộc** phải chạy lệnh backup database:
```bash
# Thực hiện backup database SMDB_FINAL sang file .bak
sqlcmd -S localhost -U sa -P 123 -Q "BACKUP DATABASE SMDB_FINAL TO DISK='C:\backup\SMDB_FINAL_before_cleanup.bak'"
```

### B. Kịch bản Rollback nhanh (Phục hồi cấu trúc cột)
Nếu sau khi dọn dẹp xảy ra lỗi tương thích, chạy các câu lệnh dưới đây để phục hồi lại cấu trúc cột thừa ban đầu:
```sql
USE SMDB_FINAL;
GO

-- Khôi phục các cột bảng DotGiamGia
ALTER TABLE DotGiamGia ADD ten_dot NVARCHAR(255) NULL;
ALTER TABLE DotGiamGia ADD trang_thai VARCHAR(50) NULL;

-- Khôi phục các cột bảng GiaoDichThanhToan
ALTER TABLE GiaoDichThanhToan ADD gateway VARCHAR(50) NULL;
ALTER TABLE GiaoDichThanhToan ADD status VARCHAR(50) NULL;

-- Khôi phục các cột bảng KhachHang
ALTER TABLE KhachHang ADD sdt VARCHAR(15) NULL;

-- Khôi phục các cột bảng NhanVien
ALTER TABLE NhanVien ADD ho_ten_nv NVARCHAR(100) NULL;

-- Khôi phục các cột bảng SoDiaChi
ALTER TABLE SoDiaChi ADD latitude FLOAT NULL;
ALTER TABLE SoDiaChi ADD longitude FLOAT NULL;
GO
```

---

## 4. Kế Hoạch Kiểm Thử Xác Minh (Test Plan)

Sau khi chạy kịch bản dọn dẹp, lập trình viên và QA phải thực hiện đầy đủ checklist kiểm thử sau:

1. **Chạy kiểm thử tự động toàn bộ hệ thống**:
   ```bash
   .\mvnw.cmd test
   ```
   *(Yêu cầu: Tất cả các bộ kiểm thử tích hợp và kiểm thử đơn vị phải vượt qua 100%).*
2. **Kiểm thử chức năng Admin / Nhân viên**:
   - Đăng nhập tài khoản admin/quản lý.
   - Truy cập trang tổng quan quản trị và quản lý nhân viên.
3. **Kiểm thử Guest Checkout (Đặt hàng vãng lai)**:
   - Thực hiện mua hàng trực tuyến không đăng nhập tài khoản.
   - Nhận email kích hoạt tài khoản guest gửi về hòm thư.
4. **Kiểm thử Thanh toán SePay**:
   - Thực hiện luồng thanh toán chuyển khoản quét mã QR SePay.
   - Xác minh trạng thái giao dịch được ghi nhận đúng vào bảng `GiaoDichThanhToan`.
5. **Kiểm thử bán hàng tại quầy POS**:
   - Nhân viên bán hàng tạo đơn hàng tại quầy POS.
   - Áp dụng voucher và in hóa đơn K80 thành công.
6. **Kiểm thử Quản lý Khuyến mãi**:
   - Tạo mới đợt giảm giá (`DotGiamGia`) và phiếu giảm giá (`PhieuGiamGia`).
   - Sửa đổi và vô hiệu hóa khuyến mãi.
7. **Kiểm thử Khôi phục mật khẩu & Activation**:
   - Gửi yêu cầu quên mật khẩu.
   - Nhận mã xác nhận OTP và thực hiện thay đổi mật khẩu thành công.
8. **Kiểm thử Sổ địa chỉ khách hàng**:
   - Thêm mới, sửa đổi và đặt địa chỉ giao hàng mặc định trong phần Dashboard.
9. **Kiểm thử Đơn hàng GHN**:
   - Tạo đơn hàng trực tuyến có tính năng giao hàng nhanh GHN.
   - Xác nhận mã vận đơn GHN hiển thị chính xác trong chi tiết hóa đơn.
