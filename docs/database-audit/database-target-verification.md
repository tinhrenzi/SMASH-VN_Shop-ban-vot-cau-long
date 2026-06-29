# Database Target Verification Report

Báo cáo này được thực hiện nhằm xác minh chính xác cơ sở dữ liệu (database) thực tế mà dự án Spring Boot đang cấu hình kết nối, đối chiếu với các database có mặt trên SQL Server, và bảo đảm tính chính xác trước khi thực hiện bất kỳ hoạt động đối chiếu cấu trúc hay dọn dẹp nào.

---

## 1. Source Configuration

* **File cấu hình đọc được**: [application.properties](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/resources/application.properties)
* **JDBC URL (Đã che thông tin nhạy cảm)**:
  `spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=SMDB_FINAL;encrypt=true;trustServerCertificate=true;}`
* **Database Name trong URL**: `SMDB_FINAL` (Giá trị mặc định của biến môi trường `${DB_URL}`).
* **Username**: `sa` (Thông qua biến `${DB_USERNAME}`).
* **Hibernate ddl-auto**: `validate`
* **Active Profile**: Không cấu hình (Mặc định sử dụng môi trường phát triển cục bộ).

---

## 2. SQL Server Databases

Dưới đây là danh sách các cơ sở dữ liệu liên quan đến dự án (`DBSM` hoặc `SMDB`) được quét thực tế từ hệ quản trị cơ sở dữ liệu SQL Server:

| Tên Database | Ngày tạo (Create Date) |
|---|---|
| `DBSM1` | 2026-06-26 10:23:53.883 |
| `SMDB_FINAL` | 2026-06-24 17:46:16.833 |
| `SMDB2` | 2026-06-24 17:37:51.107 |
| `SMDB1` | 2026-06-24 17:10:14.120 |

* **Database được chọn để export**: `SMDB_FINAL`
* **Lý do chọn**: Database này trùng khớp hoàn toàn với cấu hình vừa cập nhật trong tệp nguồn `application.properties` của ứng dụng Spring Boot hiện tại.

---

## 3. Target Match Check

| Source | Database Name | Status |
|---|---|---|
| Spring Boot config | `SMDB_FINAL` | **MATCH** |
| SQL Server export target | `SMDB_FINAL` | **MATCH** |

> [!NOTE]
> Kết quả kiểm tra cho thấy tên database đích trong tệp cấu hình của ứng dụng trùng khớp hoàn toàn với database mục tiêu thực hiện truy vấn trích xuất schema trên SQL Server (`MATCH`).

---

## 4. Final Export Files

Các tệp kết xuất lược đồ thực tế của database mục tiêu (`SMDB_FINAL`) đã được ghi nhận thành công:
1. [final-db-actual-schema.sql](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/final-db-actual-schema.sql): Script DDL cấu trúc thực tế của database kết nối.
2. [final-db-actual-schema.md](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/final-db-actual-schema.md): Lược đồ chi tiết định dạng Markdown.
3. [final-db-schema-summary.md](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/docs/database-audit/final-db-schema-summary.md): Bảng tóm tắt số lượng dòng của các bảng thực tế.

---

## 5. Warning & Tránh Nhầm Lẫn

> [!WARNING]
> * **Không sử dụng cleanup script cho SMDB1**: Kịch bản dọn dẹp không được phép chạy trên database `SMDB1` hay bất kỳ database nào khác ngoài database kết nối thực tế của ứng dụng (`SMDB_FINAL`).
> * **Mọi dọn dẹp và kiểm toán phải trỏ đúng đích**: Mọi thay đổi về cấu trúc hoặc dọn dẹp phải chạy đúng trên database `SMDB_FINAL`.
