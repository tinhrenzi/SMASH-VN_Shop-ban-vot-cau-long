# Báo Cáo Tổng Hợp Kiểm Toán & Đồng Bộ Cơ Sở Dữ Liệu: SMDB_FINAL

Tài liệu này tổng hợp toàn bộ kết quả kiểm toán lược đồ cấu trúc, xác minh môi trường kết nối, phân tích giỏ hàng mồ côi và kế hoạch đồng bộ danh mục sản phẩm của dự án.

---

## 1. Cấu Hình Kết Nối Thực Tế (Target Verification)
* **Tệp cấu hình**: [application.properties](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/resources/application.properties) (Đã đổi mặc định mặc định sang database final).
* **JDBC URL**: `spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=SMDB_FINAL;encrypt=true;trustServerCertificate=true;}`
* **Ràng buộc**: `spring.jpa.hibernate.ddl-auto=validate` (Hibernate đối chiếu và yêu cầu cấu trúc DB khớp tuyệt đối khi khởi chạy).
* **Kết quả đối chiếu**: 
  ```text
  APP_DATABASE_TARGET = SMDB_FINAL
  SQL_SERVER_TARGET = SMDB_FINAL
  MATCH_STATUS = MATCH
  ```

---

## 2. Đối Chiếu Hai Cơ Sở Dữ Liệu: DBSM1 vs SMDB_FINAL
* **Lược đồ cấu trúc**: Giống nhau 100% đối với 36 bảng nghiệp vụ chính. `SMDB_FINAL` có thêm bảng hệ thống `sysdiagrams` của SSMS.
* **Dữ liệu giao dịch**: `SMDB_FINAL` lưu giữ đầy đủ hơn lịch sử tài khoản và giao dịch thực tế (109 OTP khôi phục mật khẩu so với 20 ở DBSM1, 11 hóa đơn so với 9 ở DBSM1).
* **Dữ liệu sản phẩm**: `DBSM1` có nhiều sản phẩm hơn (43 so với 23) do 16 sản phẩm Lining chèn trước đó đang nằm tại đây.
* **Khuyến nghị**: Sử dụng **`SMDB_FINAL`** làm database chính. Tiến hành đồng bộ catalog sản phẩm Lining từ `DBSM1` sang `SMDB_FINAL`.

---

## 3. Báo Cáo Giỏ Hàng Mồ Côi (Orphan Carts)
Phát hiện **6 giỏ hàng mồ côi** trong bảng `GioHang` (trỏ đến các Customer ID không tồn tại trên `KhachHang`):
* Giỏ hàng **`587`** (chứa 1 chi tiết giỏ hàng trỏ đến sản phẩm chi tiết ID `972`).
* Giỏ hàng **`588`** (chứa 1 chi tiết giỏ hàng trỏ đến sản phẩm chi tiết ID `973`).
* Giỏ hàng **`627`, `628`, `667`, `668`**: Các giỏ hàng trống, không chứa sản phẩm.
* **Kế hoạch dọn dẹp**: Xóa các chi tiết giỏ hàng mồ côi trước, sau đó xóa bản ghi giỏ hàng mồ côi trong transaction sau khi đã sao lưu database.

---

## 4. Kế Hoạch Đồng Bộ Sản Phẩm Lining (Catalog Sync)
* **Số lượng đồng bộ**: **16 sản phẩm Lining**, **25 biến thể**, **23 hình ảnh** tương ứng (Bỏ qua 4 bản ghi trùng lặp của Yonex do integration test cũ tạo ra).
* **Chiến lược ánh xạ động**: Không sao chép cứng khóa chính `id` để SQL Server tự tăng, ánh xạ khóa ngoại động qua Business Keys (`ten_san_pham`, `SKU`, `barcode`, `duong_dan`).
* **Các tệp kịch bản đã tạo**:
  * [catalog-sync-dbsm1-to-smdb-final-plan.md](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/catalog-sync-dbsm1-to-smdb-final-plan.md): Kế hoạch đồng bộ chi tiết.
  * [catalog-sync-dbsm1-to-smdb-final-dry-run.sql](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/catalog-sync-dbsm1-to-smdb-final-dry-run.sql): Script tự động import dữ liệu.

---

## 5. Dọn Dẹp Schema & Ràng Buộc An Toàn (Cleanup Plan)
* **Tệp kịch bản dọn dẹp**: [schema-cleanup-dry-run.sql](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/schema-cleanup-dry-run.sql)
* **Cơ chế an toàn**:
  * Mặc định đặt chế độ dry-run (`@DoExecute = 0`) để chỉ hiển thị dữ liệu thống kê trước khi xóa.
  * Tự động tìm và drop Default Constraints động trên cột trước khi DROP COLUMN.
  * **Đã sửa**: Bổ sung lệnh `SET @ConstraintName = NULL;` trước mỗi lần tìm ràng buộc để loại bỏ hoàn toàn lỗi gán đè tên constraint của cột trước sang cột sau.
  * Độc lập hoàn toàn, không drop bảng `TichHopVanChuyen` và các cột `MaKhoiPhuc.token`, `DotGiamGia.trang_thai`, `HoaDon.so_tien_giam_gia` ở pha này.
