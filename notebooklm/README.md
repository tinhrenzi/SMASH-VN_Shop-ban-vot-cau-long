# NOTEBOOKLM DOCUMENTATION — SMASH-VN BADMINTON E-COMMERCE

Bộ tài liệu này được thiết kế và tối ưu chuyên biệt để import vào **Google NotebookLM**, giúp hệ thống AI hiểu sâu sắc và tra cứu chính xác 100% cấu trúc, logic nghiệp vụ, database, query và luồng xử lý của dự án **SMASH-VN**.

---

## 📚 DANH SÁCH CÁC TÀI LIỆU TRONG BỘ SOURCE

| File | Tên Tài Liệu | Vai Trò & Nội Dung Tra Cứu |
| :--- | :--- | :--- |
| **`01_tong_quan_he_thong.md`** | Tổng Quan Hệ Thống | Hiểu mục đích dự án, công nghệ, kiến trúc tầng, các module, nhóm người dùng và luồng request tổng quát. |
| **`02_phan_tich_chi_tiet.html`** | Tài Liệu Tra Cứu Chi Tiết | Tra cứu toàn diện từng chức năng (Giao diện -> Controller -> Service -> Repository -> Query -> Entity -> DB -> State -> File sửa). Tích hợp tìm kiếm & menu. |
| **`03_database.md`** | Tài Liệu Cơ Sở Dữ Liệu & Entity | Tra cứu 32 JPA Entities, quan hệ Entity, bảng database, khóa chính/ngoại, trường nghiệp vụ, Enums và Locks. |
| **`04_endpoint_map.md`** | Bản Đồ Toàn Bộ Endpoints | Tra cứu danh mục 110+ URL: HTTP Method, Controller, Method, Service được gọi, Quyền hạn (RBAC) và Chức năng. |
| **`05_business_flow.md`** | Luồng Nghiệp Vụ Cốt Lõi | Tra cứu 18 luồng nghiệp vụ thực tế từ góc nhìn kiến trúc (Bắt đầu từ đâu, qua Controller/Service nào, thay đổi bảng nào, trạng thái cuối là gì). |
| **`06_file_index.md`** | Chỉ Mục Tra Cứu File & Method | Tra cứu nhanh vai trò của từng File trong dự án: Controller, Service, Repository, DTO, Config, Scheduler, Template. |
| **`07_query_sql.md`** | Tra Cứu Query, SQL & Sắp Xếp | Tra cứu chi tiết toàn bộ JPQL, Native Query, Criteria Specification, Derived Queries và phân tích chuyên sâu mệnh đề `ORDER BY` sản phẩm. |
| **`08_tich_hop_he_thong.md`** | Tích Hợp Bên Thứ 3 & Schedulers | Tra cứu SePay Payment Gateway, GHN API, Gemini 2.0 Flash AI, Google OAuth2, 4 Schedulers ngầm, Locks và Idempotency. |

---

## 💡 HƯỚNG DẪN ĐẶT CÂU HỎI TRONG NOTEBOOKLM

Khi đưa các file này vào Google NotebookLM, bạn có thể đặt các câu hỏi mẫu như:

1. **Về File & Vị trí code:**
   - *"Logic thanh toán nằm ở file nào?"* ➔ NotebookLM sẽ chỉ ra `CheckoutController.java`, `GioHangService.java`, `SepayOrderPaymentService.java`.
   - *"Một chức năng cụ thể liên quan tới những file nào?"* ➔ Xem `06_file_index.md` và `02_phan_tich_chi_tiet.html`.
   - *"Muốn đổi thứ tự hiển thị sản phẩm thì sửa file nào?"* ➔ `SanPhamSpecification.java` hoặc `SanPhamRepository.java`.

2. **Về Database & Query:**
   - *"Query lấy danh sách sản phẩm nằm ở Repository nào?"* ➔ Xem `07_query_sql.md`.
   - *"Khi hoàn hàng thì bảng nào bị thay đổi?"* ➔ `hoa_don`, `san_pham_chi_tiet`, `lich_su_trang_thai_don_hang`.
   - *"Sản phẩm trang chủ đang sort theo trường gì, ASC hay DESC?"* ➔ Xem `07_query_sql.md` (Ưu tiên còn hàng `hasStock DESC`, sau đó `sp.id DESC` hoặc `SUM(hdct.soLuong) DESC`).

3. **Về Luồng Nghiệp Vụ & API:**
   - *"Luồng đặt hàng chạy qua những file nào?"* ➔ Xem `05_business_flow.md`.
   - *"Khi nào hệ thống thực hiện trừ tồn kho?"* ➔ Đơn COD trừ ngay khi submit; Đơn SePay chỉ trừ khi nhận IPN thành công qua `InventoryLotService.allocateFifo()`.
   - *"Scheduler GHN hoạt động như thế nào?"* ➔ `GhnPollingScheduler.java` chạy mỗi 10 phút, tra cứu đơn vận chuyển và đồng bộ trạng thái.

---
*Tài liệu được khởi tạo chuẩn hóa từ phân tích 100% mã nguồn thực tế của dự án SMASH-VN.*
