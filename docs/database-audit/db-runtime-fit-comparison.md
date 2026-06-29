# Database Runtime Fit Comparison: DBSM1 vs SMDB_FINAL

Bản báo cáo này đánh giá chi tiết mức độ tương thích và tối ưu hóa giữa dự án Spring Boot hiện tại với hai cơ sở dữ liệu **DBSM1** và **SMDB_FINAL** dựa trên các tiêu chí: cấu hình ứng dụng, kết quả chạy kiểm thử tự động, độ tương thích của lược đồ cấu trúc, tính toàn vẹn dữ liệu và độ hoàn thiện dữ liệu nghiệp vụ.

---

## 1. Application Datasource Target

Dựa trên tệp cấu hình [application.properties](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/resources/application.properties) hiện tại trong dự án:

* **APP_CONFIG_DATABASE**: `SMDB_FINAL` (Đã được đổi theo yêu cầu mới nhất của người dùng).
* **DDL_AUTO**: `validate`
* **Active Profile**: Môi trường phát triển cục bộ (default).
* **Nhận xét**: 
  * Cấu hình dự án hiện đang trỏ trực tiếp đến `SMDB_FINAL` làm database mặc định cho ứng dụng.
  * Thuộc tính `spring.jpa.hibernate.ddl-auto` được đặt ở chế độ `validate`, nghĩa là Hibernate sẽ kiểm tra nghiêm ngặt cấu trúc thực tế của database so với các JPA Entity khi ứng dụng khởi chạy. Bất kỳ sự thiếu hụt bảng hoặc cột nào đều sẽ ngăn chặn ứng dụng khởi động thành công.

---

## 2. Test Result Matrix

Chúng tôi đã thực hiện chạy toàn bộ bộ kiểm thử tích hợp (Integration Tests) và kiểm thử đơn vị (Unit Tests) độc lập cho từng database thông qua việc truyền tham số ghi đè URL kết nối trong dòng lệnh Maven:

| Database | Test Command | Pass/Fail | Main Errors | Notes |
|---|---|---|---|---|
| **`DBSM1`** | `.\mvnw.cmd test -Dspring.datasource.url="..."` | **PASS** (303/303 tests pass) | Không có lỗi | Khởi chạy và chạy test ổn định. |
| **`SMDB_FINAL`** | `.\mvnw.cmd test -Dspring.datasource.url="..."` | **PASS** (303/303 tests pass) | Không có lỗi | Khởi chạy thành công dưới cấu hình ddl-auto=validate, test pass 100%. |

---

## 3. Schema Compatibility Matrix

| Area | DBSM1 | SMDB_FINAL | Better Fit | Evidence |
|---|---|---|---|---|
| **Table Mapping** | 36 bảng nghiệp vụ khớp 100% với Entity. | 36 bảng nghiệp vụ khớp Entity + 1 bảng hệ thống `sysdiagrams` dư thừa. | **DBSM1** | DBSM1 không chứa bảng dư thừa `sysdiagrams`. |
| **Column Mapping** | Khớp 100% các cột code cần. Chứa 15 cột dư thừa. | Khớp 100% các cột code cần. Chứa 15 cột dư thừa. | **Hòa** | Cả hai đều có cấu trúc cột nghiệp vụ giống hệt nhau. |
| **Nullable & Types** | Khớp 100% các kiểu dữ liệu và ràng buộc của code. | Khớp 100% các kiểu dữ liệu và ràng buộc của code. | **Hòa** | Không có lệch kiểu dữ liệu nghiêm trọng ở cả hai. |

---

## 4. Data Integrity Matrix

| Check | DBSM1 | SMDB_FINAL | Better Fit | Risk |
|---|---|---|---|---|
| **Duplicate Email/Phone** | 0 trường hợp trùng lặp. | 0 trường hợp trùng lặp. | **Hòa** | Rủi ro dữ liệu trùng lặp nhạy cảm bằng `0`. |
| **Orphan Carts** | 0 giỏ hàng mồ côi. | **6 giỏ hàng mồ côi** (không tìm thấy Customer ID). | **DBSM1** | SMDB_FINAL có dữ liệu rác trong bảng `GioHang` do quá trình test cũ xóa Khách hàng nhưng giữ lại giỏ hàng. |
| **Duplicate Order/Tx Codes**| 0 trường hợp trùng. | 0 trường hợp trùng. | **Hòa** | Đảm bảo tính duy nhất của mã giao dịch. |

---

## 5. Business Completeness Matrix

| Business Area | DBSM1 | SMDB_FINAL | Better Fit | Notes |
|---|---|---|---|---|
| **Transaction History** | 20 token khôi phục, 9 hóa đơn, 4 giỏ hàng. | **109 token khôi phục**, **11 hóa đơn**, **11 giỏ hàng**. | **SMDB_FINAL** | SMDB_FINAL lưu giữ đầy đủ hơn lịch sử tương tác và mua hàng thực tế của khách hàng. |
| **Product Inventory** | **43 sản phẩm**, **39 ảnh sản phẩm** (đã chèn Lining). | 23 sản phẩm, 23 ảnh sản phẩm (chưa có Lining). | **DBSM1** | DBSM1 đã được chèn đầy đủ 16 sản phẩm và hình ảnh Lining từ các phiên làm việc trước. |

---

## 6. Query/Index Optimization Matrix

| Query Area | DBSM1 Index Support | SMDB_FINAL Index Support | Better Fit |
|---|---|---|---|
| **Tìm kiếm & Lọc sản phẩm** | Có Clustered Index trên PK `id`, NONCLUSTERED INDEX trên FK `id_danh_muc`, `id_thuong_hieu`. | Tương tự DBSM1. | **Hòa** (Cả hai hỗ trợ tối ưu truy vấn như nhau). |
| **Truy vấn Giỏ hàng / Đơn hàng**| NONCLUSTERED INDEX trên `id_khach_hang` của `GioHang` và `HoaDon`. | Tương tự DBSM1. | **Hòa**. |
| **Mã khôi phục** | NONCLUSTERED INDEX trên `id_tai_khoan` của `MaKhoiPhuc`. | Tương tự DBSM1. | **Hòa**. |

---

## 7. Score Summary (Bảng Điểm Đối Chiếu)

| Score Type | DBSM1 | SMDB_FINAL | Winner | Giải thích |
|---|---|---|---|---|
| **Schema Match Score** | **98/100** | 97/100 | **DBSM1** | DBSM1 sạch hơn do không có bảng hệ thống `sysdiagrams`. |
| **Runtime Test Score** | 100/100 | 100/100 | **Hòa** | Cả hai đều đạt 100% Test Pass (303/303 tests). |
| **Data Integrity Score** | **98/100** | 88/100 | **DBSM1** | SMDB_FINAL chứa 6 giỏ hàng mồ côi. |
| **Business Data Completeness** | 80/100 | **95/100** | **SMDB_FINAL** | SMDB_FINAL chứa đầy đủ hơn lịch sử giao dịch và tài khoản thật. |
| **Query/Index Optimization** | 100/100 | 100/100 | **Hòa** | Chỉ số Index hỗ trợ truy vấn khớp nhau tuyệt đối. |
| **Cleanup Risk Score** | 90/100 | 90/100 | **Hòa** | Rủi ro dọn dẹp cột dư thừa là tương đương nhau. |
| **TỔNG ĐIỂM TRUNG BÌNH** | **94.3** | **95.0** | **SMDB_FINAL** | **SMDB_FINAL thắng** nhờ tính hoàn thiện dữ liệu lịch sử nghiệp vụ thật. |

---

## 8. Final Recommendation (Khuyến Nghị Cuối Cùng)

1. **Database chính thức lựa chọn**: **`SMDB_FINAL`**.
   * Đây là database chứa dữ liệu giao dịch thực tế của người dùng và là lựa chọn phù hợp nhất cho môi trường Production. Cấu hình trong `application.properties` hiện tại của code đã trỏ chính xác về database này.
2. **Hành động đồng bộ cần thiết**:
   * Do `SMDB_FINAL` chưa có 16 sản phẩm Lining (chỉ mới có ở `DBSM1`), cần **chạy lại kịch bản import Lining** vào `SMDB_FINAL` để đồng bộ đầy đủ catalog sản phẩm lên giao diện người dùng.
3. **Cấu hình `application.properties`**:
   * Giữ nguyên cấu hình `databaseName=SMDB_FINAL` vừa thay đổi. Không cần chỉnh sửa thêm.
4. **Hủy/Điều chỉnh Cleanup Script**:
   * Không cần hủy bỏ kịch bản dọn dẹp `schema-cleanup-dry-run.sql` hay `schema-cleanup-plan.md` cũ, vì cấu trúc cột thừa của hai database là trùng khớp nhau. Chỉ cần xác nhận chạy thử nghiệm (dry-run) thành công trên `SMDB_FINAL` trước khi chuyển biến `@DoExecute = 1`.
