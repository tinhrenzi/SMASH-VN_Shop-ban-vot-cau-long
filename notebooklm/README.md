# NOTEBOOKLM DOCUMENTATION — SMASH-VN BADMINTON E-COMMERCE

Bộ tài liệu này được thiết kế, cập nhật và tối ưu chuyên biệt để import vào **Google NotebookLM**, giúp hệ thống AI hiểu sâu sắc và tra cứu chính xác 100% cấu trúc, logic nghiệp vụ, database, query, API endpoints và luồng xử lý của dự án **SMASH-VN**.

---

## 📚 DANH SÁCH CÁC TÀI LIỆU TRONG BỘ SOURCE NOTEBOOKLM

| File | Tên Tài Liệu | Vai Trò & Nội Dung Tra Cứu |
| :--- | :--- | :--- |
| **`01_tong_quan_he_thong.md`** | Tổng Quan Hệ Thống | Hiểu mục đích dự án, công nghệ sử dụng, kiến trúc phân tầng, 13 module nghiệp vụ chính, 4 nhóm người dùng RBAC, vòng đời request và cấu trúc thư mục. |
| **`02_phan_tich_chi_tiet.html`** | Tài Liệu Tra Cứu Chi Tiết | Tra cứu toàn diện từng chức năng (Giao diện -> Controller -> Service -> Repository -> Query -> Entity -> DB -> State -> File sửa). Tích hợp tìm kiếm & menu tương tác. |
| **`03_database.md`** | Cơ Sở Dữ Liệu & Entity Schema | Tra cứu 42 bảng database (`BadmintonShopDB1_ban_moi_nhat_.sql`), 32 JPA Entities, quan hệ khóa ngoại, bảng tích hợp `TichHopVanChuyen`, bảng giao dịch `GiaoDichThanhToan`, quản lý lô nhập `phieu_nhap`/`phieu_nhap_chi_tiet`, kho hàng lỗi `soLuongSpLoi` và bản đồ toàn bộ Enums / State Machines. |
| **`04_endpoint_map.md`** | Bản Đồ Toàn Bộ Endpoints | Danh mục toàn diện 120+ URL: HTTP Method, Controller, Method, Service được gọi, Quyền hạn (RBAC) và Mô tả chức năng phân theo 9 phân hệ (Auth, Catalog, Cart/Checkout, SePay, User RMA, Admin Orders & Returns, POS Quầy, Kho Lô FIFO & Hàng Lỗi, REST APIs/Chatbot). |
| **`05_business_flow.md`** | Luồng Nghiệp Vụ Cốt Lõi | Tra cứu 10 luồng nghiệp vụ thực tế từ góc nhìn kiến trúc phân tầng: Luồng COD, Luồng SePay QR (Thành công, Thiếu kho, Trễ), Luồng RMA Đổi trả (Đính kèm Video/Ảnh, Thu hồi GHN, Kiểm kho Hàng tốt vs Lỗi, Đổi mới vs Hoàn tiền), Luồng POS Bán tại quầy, Luồng Hủy đơn COD chưa TT, Luồng Hủy đơn Online đã TT & Xác nhận hoàn tiền, Luồng Nhập kho Lô FIFO, Luồng Xử lý Hàng lỗi, Luồng Khách Guest, Luồng Gemini AI RAG. |
| **`06_file_index.md`** | Chỉ Mục Tra Cứu File & Method | Tra cứu nhanh vai trò của từng File trong dự án: Controllers, Services, Repositories, DTOs, Schedulers, Configs và Templates. |
| **`07_query_sql.md`** | Tra Cứu Query, SQL & Sắp Xếp | Tra cứu chi tiết toàn bộ JPQL, Native Query, Criteria Specification, Derived Queries và phân tích chuyên sâu quy tắc `ORDER BY` sản phẩm tại tất cả màn hình (Cửa hàng, Trang chủ, Autocomplete, Chatbot). |
| **`08_tich_hop_he_thong.md`** | Tích Hợp Bên Thứ 3, Schedulers & Khóa | Tra cứu chi tiết Cổng thanh toán SePay QR, Dịch vụ vận chuyển Giao Hàng Nhanh (GHN v2), Trợ lý ảo Google Gemini 2.0 Flash AI (RAG), Bảng tổng hợp 4 Schedulers chạy ngầm, Cơ chế Khóa chống xung đột Pessimistic Locks và Idempotency Guards. |

---

## 💡 HƯỚNG DẪN ĐẶT CÂU HỎI TRONG NOTEBOOKLM

Khi import bộ tài liệu này vào Google NotebookLM, bạn có thể đặt các câu hỏi mẫu như:

1. **Về File & Vị trí Source Code:**
   - *"Logic thanh toán SePay và kiểm tra tồn kho nằm ở file nào?"* ➔ NotebookLM sẽ chỉ ra `SepayIpnController.java`, `SepayGatewayService.java`, `SepayOrderPaymentService.java`, `InventoryLotService.java`.
   - *"Một chức năng cụ thể liên quan tới những file nào?"* ➔ Tra cứu `06_file_index.md` và `02_phan_tich_chi_tiet.html`.
   - *"Muốn thay đổi thứ tự ưu tiên hiển thị sản phẩm ở trang Shop thì sửa file nào, đoạn nào?"* ➔ `SanPhamSpecification.java` (mục Sorting `hasStock DESC`, `sp.id DESC` hoặc `MIN(spct.giaBan)`).

2. **Về Database & Query:**
   - *"Query lấy danh sách sản phẩm bán chạy nằm ở đâu và viết như thế nào?"* ➔ Xem `07_query_sql.md` (`SanPhamRepository.findBestSellers`).
   - *"Khi đổi trả hàng hoặc hủy đơn online đã thanh toán thì những bảng nào bị thay đổi?"* ➔ `hoa_don`, `san_pham_chi_tiet` (hoàn kho hoặc cộng `soLuongSpLoi`), `phieu_nhap_chi_tiet`, `payment_transactions` (thêm bản ghi `ORDER_CANCEL_REFUND`), `lich_su_trang_thai_don_hang`.
   - *"Sản phẩm trang chủ đang sort theo trường gì, ASC hay DESC?"* ➔ Xem `07_query_sql.md` (Ưu tiên còn hàng `hasStock DESC`, sau đó `sp.id DESC` hoặc `SUM(hdct.soLuong) DESC`).

3. **Về Luồng Nghiệp Vụ & APIs:**
   - *"Luồng hủy đơn hàng online đã thanh toán chạy qua các bước nào và xử lý giao dịch ra sao?"* ➔ Xem `05_business_flow.md` (Mục 6: Xác nhận hoàn tiền, hoàn kho FIFO, hủy GHN, tạo transaction `REFUND_SUCCESS`, giữ nguyên transaction gốc, cập nhật `DA_HUY` + `REFUNDED`).
   - *"Quy trình xử lý hàng lỗi và kiểm kho RMA hoạt động như thế nào?"* ➔ Xem `05_business_flow.md` (Mục 3 & 8) và `03_database.md` (Cột `soLuongSpLoi` trong `san_pham_chi_tiet`).
   - *"Khi nào hệ thống thực hiện trừ tồn kho giữa COD, SePay và POS?"* ➔ Xem `05_business_flow.md`:
     + **Đơn COD Online**: **KHÔNG trừ kho ngay lúc đặt**; chỉ trừ kho FIFO khi Quản lý/Nhân viên bấm **"Xác nhận đơn hàng"** (chuyển sang `DA_XAC_NHAN`).
     + **Đơn SePay Online**: Chỉ trừ kho FIFO khi **SePay Webhook IPN** báo chuyển khoản thành công (`PAID`).
     + **Bán hàng tại quầy POS**: Trừ kho FIFO **ngay lập tức** khi thu ngân bấm thanh toán thành công.
   - *"Scheduler GHN và Webhook GHN hoạt động như thế nào?"* ➔ `GhnPollingScheduler.java` (quét mỗi 10 phút) và `GhnRestController.java` (`POST /api/ghn/webhook`).

---
*Bộ tài liệu được chuẩn hóa và cập nhật đồng bộ 100% từ mã nguồn thực tế của dự án SMASH-VN.*
