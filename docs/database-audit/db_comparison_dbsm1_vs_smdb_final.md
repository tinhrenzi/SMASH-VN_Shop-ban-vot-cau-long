# Báo Cáo Đối Chiếu Cơ Sở Dữ Liệu: DBSM1 vs SMDB_FINAL

Báo cáo so sánh chi tiết cấu trúc (lược đồ) và dữ liệu thực tế giữa hai cơ sở dữ liệu **DBSM1** và **SMDB_FINAL** trong SQL Server.

---

## 1. So Sánh Lược Đồ Cấu Trúc (Structural Schema Comparison)

* **Số lượng bảng**:
  * `DBSM1`: 36 bảng.
  * `SMDB_FINAL`: 37 bảng.
  * **Chênh lệch**: Bảng `sysdiagrams` chỉ xuất hiện trên `SMDB_FINAL` (Bảng hệ thống của SSMS dùng để vẽ Database Diagram, không tham chiếu trong mã nguồn Java).
* **Đồng bộ cấu trúc**:
  * **36 bảng chung** khớp nhau tuyệt đối (100%) về: Tên cột, kiểu dữ liệu, độ rộng cột, thuộc tính khóa chính/khóa ngoại và cấu hình Nullable.
  * Không phát hiện bất kỳ sự sai lệch kiểu dữ liệu (TYPE_MISMATCH) nào giữa hai database trong các bảng nghiệp vụ chính.

---

## 2. So Sánh Số Lượng Bản Ghi (Row Count Comparison)

Dưới đây là so sánh số lượng bản ghi thực tế giữa hai database:

| Tên Bảng | Dòng trong DBSM1 | Dòng trong SMDB_FINAL | Chênh lệch | Nhận xét trạng thái |
|---|---|---|---|---|
| `MaKhoiPhuc` | 20 | 109 | **+89** | SMDB_FINAL có nhiều OTP khôi phục hơn |
| `GioHang` | 4 | 11 | **+7** | SMDB_FINAL có nhiều giỏ hàng hơn |
| `HoaDon` | 9 | 11 | **+2** | SMDB_FINAL có nhiều hóa đơn hơn |
| `HoaDonChiTiet` | 9 | 12 | **+3** | SMDB_FINAL có nhiều chi tiết hóa đơn hơn |
| `SoDiaChi` | 2 | 4 | **+2** | SMDB_FINAL có nhiều địa chỉ hơn |
| `EditLog` | 33 | 36 | **+3** | SMDB_FINAL ghi nhận nhiều log chỉnh sửa hơn |
| `TaiKhoan` | 10 | 11 | **+1** | SMDB_FINAL có nhiều tài khoản hơn |
| `GiaoDichThanhToan`| 5 | 6 | **+1** | SMDB_FINAL có nhiều giao dịch hơn |
| `SanPham` | 43 | 23 | **-20** | DBSM1 có nhiều sản phẩm hơn |
| `HinhAnhSanPham` | 39 | 23 | **-16** | DBSM1 có nhiều ảnh sản phẩm hơn |
| `SanPhamChiTiet` | 39 | 25 | **-14** | DBSM1 có nhiều biến thể hơn |

---

## 3. Khác Biệt Đối Với Mã Nguồn Java (Code Compatibility)

1. **Khả năng tương thích**:
   * Cả hai database đều tương thích hoàn toàn với các Entity Java của dự án hiện tại vì cấu trúc trường/cột của chúng hoàn toàn khớp nhau.
2. **Khác biệt về dữ liệu nghiệp vụ**:
   * **Dữ liệu giao dịch**: `SMDB_FINAL` chứa nhiều dữ liệu giao dịch thực tế của người dùng hơn (nhiều tài khoản hơn, giỏ hàng, hóa đơn, đặc biệt là lịch sử mã xác nhận/khôi phục mật khẩu lớn gấp 5 lần).
   * **Dữ liệu danh mục sản phẩm**: `DBSM1` có nhiều sản phẩm hơn (43 so với 23 của `SMDB_FINAL`). 
     * *Lý do*: Trong các tác vụ trước đó, mã nguồn chèn danh mục và 16 sản phẩm vợt cầu lông hiệu **Lining** mới được chạy và ghi nhận trực tiếp vào database mặc định cũ `DBSM1`. Khi chuyển sang `SMDB_FINAL`, danh mục sản phẩm mới này chưa có trong DB.

---

## 4. Đánh Giá & Nhận Xét Bản Nào Tối Ưu Hơn

* **Về mặt cấu trúc**: Cả hai bản có độ tối ưu **như nhau** (vì lược đồ cấu trúc giống hệt nhau).
* **Về mặt nghiệp vụ thực tế**: **`SMDB_FINAL` là bản tối ưu và chính xác nhất để sử dụng**. 
  * Đây là cơ sở dữ liệu chứa toàn bộ lịch sử giao dịch, dữ liệu người dùng, và giỏ hàng thật của hệ thống.
* **⚠️ Khuyến nghị quan trọng**: Do 16 sản phẩm Lining và hình ảnh tương ứng mới chỉ được ghi nhận ở `DBSM1`, khi chuyển sang dùng `SMDB_FINAL`, ứng dụng sẽ bị thiếu danh mục sản phẩm này trên giao diện. Quản trị viên nên chạy lại kịch bản import sản phẩm Lining vào `SMDB_FINAL` để đồng bộ catalog sản phẩm.
