# Kế Hoạch Đồng Bộ Catalog: DBSM1 sang SMDB_FINAL

Tài liệu này vạch ra phương án đồng bộ danh mục sản phẩm cầu lông Lining và Yonex còn thiếu từ database tham chiếu **DBSM1** sang database chính thức **SMDB_FINAL**.

---

## 1. Xác Minh Trạng Thái Thực Tế (Verification)

Trước khi thực hiện đồng bộ, chúng tôi đã chạy kiểm tra tồn tại của các Danh mục (`DanhMuc`) và Thương hiệu (`ThuongHieu`) liên quan ở cả hai database:

* **Danh mục `Lining`**:
  * DBSM1: Tồn tại (ID = `381`).
  * SMDB_FINAL: Tồn tại (ID = `381`).
  * **Trạng thái**: **MATCH** (Không cần import danh mục).
* **Thương hiệu `Lining`**:
  * DBSM1: Tồn tại (ID = `381`).
  * SMDB_FINAL: Tồn tại (ID = `381`).
  * **Trạng thái**: **MATCH** (Không cần import thương hiệu).

---

## 2. Các Bản Ghi Sản Phẩm Dự Kiến Đồng Bộ (Missing Products)

Truy vấn đối chiếu bằng Business Key (`ten_san_pham`) giữa `DBSM1` và `SMDB_FINAL` phát hiện **20 sản phẩm** có trong `DBSM1` nhưng chưa có trong `SMDB_FINAL`:
* **16 sản phẩm Lining**: Các dòng vợt Lining Aeronaut, Axforce (Tiger Max, Bigbang, Thunder Cannon), Windstorm, Calibar, Hỏa, Phong vừa được đưa vào danh mục trong các phiên làm việc trước.
* **4 sản phẩm trùng lặp**: `Yonex Astrox 88D Play` xuất hiện nhiều bản ghi trùng lặp do kết quả của các lượt chạy integration test cũ trên `DBSM1`.
  * **Chiến lược đồng bộ**: Bộ lọc trong script chỉ thực hiện import **16 sản phẩm Lining** mới (thuộc thương hiệu Lining, ID = 381) và bỏ qua các bản ghi trùng lặp của Yonex để giữ cho database `SMDB_FINAL` sạch sẽ nhất.

---

## 3. Chiến Lược Ánh Xạ Không Trùng Lặp (Idempotent Mapping Strategy)

Script đồng bộ tuân thủ các nguyên tắc an toàn dữ liệu nghiêm ngặt:
1. **Không ghi đè (Overwrite)**: Chỉ chèn các bản ghi sản phẩm/biến thể/hình ảnh chưa tồn tại trên `SMDB_FINAL`.
2. **Không sao chép cột IDENTITY (Id)**: Khóa chính `id` của sản phẩm, biến thể, hình ảnh sẽ được SQL Server tự sinh trên `SMDB_FINAL`.
3. **Ánh xạ khóa ngoại động (Dynamic FK Mapping)**:
   * Ánh xạ danh mục và thương hiệu thông qua `ten_danh_muc = 'Lining'` và `ten_thuong_hieu = 'Lining'`.
   * Ánh xạ cha-con (`SanPham` $\rightarrow$ `SanPhamChiTiet`) bằng cách `JOIN` trên Business Key `ten_san_pham`.
   * Ánh xạ biến thể-ảnh (`SanPhamChiTiet` $\rightarrow$ `HinhAnhSanPham`) bằng cách `JOIN` trên Business Key `SKU` và `barcode`.

---

## 4. Cấu Trúc Các Tệp Liên Quan

* **Kế hoạch triển khai**: [catalog-sync-dbsm1-to-smdb-final-plan.md](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/catalog-sync-dbsm1-to-smdb-final-plan.md)
* **Kịch bản SQL thực thi**: [catalog-sync-dbsm1-to-smdb-final-dry-run.sql](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/catalog-sync-dbsm1-to-smdb-final-dry-run.sql)

---

## 5. Quy Trình Chạy Script An Toàn
1. **Bước 1: Chạy Dry-Run (Mặc định)**:
   * Chạy script với `DECLARE @DoExecute BIT = 0`.
   * Hệ thống sẽ hiển thị bảng dữ liệu 16 sản phẩm Lining cùng số lượng biến thể/ảnh dự kiến import để rà soát thủ công.
2. **Bước 2: Chạy thật (Execute Mode)**:
   * Chuyển giá trị biến `@DoExecute = 1`.
   * Toàn bộ thao tác ghi sẽ diễn ra trong một Transaction. Nếu gặp bất kỳ lỗi khóa ngoại hay ràng buộc nào, hệ thống sẽ thực hiện `ROLLBACK TRANSACTION` để bảo toàn trạng thái trước đó của `SMDB_FINAL`.
