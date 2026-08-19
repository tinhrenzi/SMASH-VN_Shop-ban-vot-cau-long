# 03. TÀI LIỆU CƠ SỞ DỮ LIỆU & SCHEMA TOÀN DIỆN (DATABASE DOCUMENTATION)

> **Cơ sở dữ liệu:** Microsoft SQL Server 2022  
> **Database Name:** `BadmintonShopDB1`  
> **File SQL Schema mới nhất:** `scratch/BadmintonShopDB1_ban_moi_nhat_.sql` (171.8 KB, 42 Tables)  
> **Công cụ ORM & Truy vấn:** Hibernate 6.x / Spring Data JPA + Native SQL (TichHopVanChuyen)  
> **Quản lý Migration:** Flyway Migration (`flyway_schema_history`)  

---

## 1. TỔNG QUAN 42 BẢNG DATABASE TRONG `BadmintonShopDB1_ban_moi_nhat_.sql`

| STT | Table Name | Entity / Cơ Chế Ánh Xạ | Khóa Chính | Vai Trò & Nghiệp Vụ Trong Hệ Thống |
| :---: | :--- | :--- | :--- | :--- |
| 1 | `san_pham` (hoặc `SanPham`) | `SanPham.java` | `id (int)` | Sản phẩm cha (Tên, mô tả, ảnh đại diện, danh mục, thương hiệu, điểm đánh giá trung bình) |
| 2 | `san_pham_chi_tiet` (hoặc `SanPhamChiTiet`) | `SanPhamChiTiet.java` | `id (int)` | Biến thể con (Màu sắc, size, trọng lượng, mức căng, giá bán, tồn kho kinh doanh `soLuongTon`, tồn kho hàng lỗi `soLuongSpLoi`, SKU) |
| 3 | `hoa_don` (hoặc `HoaDon`) | `HoaDon.java` | `id (int)` | Hóa đơn / Đơn hàng (Mã đơn, tổng tiền, phí ship, giảm giá voucher, trạng thái đơn, trạng thái thanh toán, GHN) |
| 4 | `hoa_don_chi_tiet` (hoặc `HoaDonChiTiet`) | `HoaDonChiTiet.java` | `id (int)` | Dòng chi tiết đơn (Số lượng, đơn giá, giá gốc, snapshots tên/SKU/đợt giảm giá) |
| 5 | `tai_khoan` (hoặc `TaiKhoan`) | `TaiKhoan.java` | `id (int)` | Tài khoản đăng nhập (Username, mật khẩu BCrypt, vai trò QL/NV/KH, trạng thái GUEST/ACTIVE/LOCKED) |
| 6 | `khach_hang` (hoặc `KhachHang`) | `KhachHang.java` | `id (int)` | Hồ sơ khách hàng (Họ tên, SĐT, giới tính, ngày sinh, nhận bản tin) |
| 7 | `nhan_vien` (hoặc `NhanVien`) | `NhanVien.java` | `id (int)` | Hồ sơ nhân viên (Họ tên, SĐT, chức vụ, trạng thái) |
| 8 | `so_dia_chi` (hoặc `SoDiaChi`) | `SoDiaChi.java` | `id (int)` | Sổ địa chỉ nhận hàng (Địa chỉ cụ thể, Province ID, District ID, Ward Code GHN, cờ mặc định) |
| 9 | `phieu_giam_gia` (hoặc `PhieuGiamGia`) | `PhieuGiamGia.java` | `id (int)` | Phiếu giảm giá / Voucher (Mã code, giá trị, đơn vị %/VND, giảm tối đa, đơn tối thiểu, số lượng còn lại) |
| 10 | `dot_giam_gia` (hoặc `DotGiamGia`) | `DotGiamGia.java` | `id (int)` | Đợt giảm giá trực tiếp (Mã chiến dịch, % giảm, kiểu áp dụng `ApDungKieu`, ngày bắt đầu/kết thúc) |
| 11 | `SanPham_DotGiamGia` | `@ManyToMany` Join Table | `(id_san_pham, id_dot_giam_gia)` | Bảng liên kết trung gian giữa Sản phẩm và Chiến dịch giảm giá |
| 12 | `phieu_nhap` (hoặc `PhieuNhap`) | `PhieuNhap.java` | `id (int)` | Phiếu nhập hàng vào kho (Mã phiếu nhập, nhà cung cấp, tổng tiền nhập, ngày nhập) |
| 13 | `phieu_nhap_chi_tiet` (hoặc `PhieuNhapChiTiet`) | `PhieuNhapChiTiet.java` | `id (int)` | Lô nhập hàng theo biến thể phục vụ xuất kho FIFO (Số lượng nhập, giá nhập, số lượng còn lại của lô) |
| 14 | `TichHopVanChuyen` | Native SQL & `@Formula` | `id (int)` | **Bảng tích hợp vận chuyển GHN**: Lưu lịch sử vận đơn giao hàng (`GHN`), thu hồi (`GHN_RETURN`), đổi mới (`GHN_EXCHANGE`) và trạng thái bưu tá |
| 15 | `GiaoDichThanhToan` (hoặc `payment_transactions`) | `PaymentTransaction.java` | `id (int)` | **Lịch sử giao dịch SePay**: Mã giao dịch unique, số tiền, cổng thanh toán, status (`SUCCESS`, `PAID_INSUFFICIENT_STOCK`...), raw payload |
| 16 | `danh_gia` (hoặc `DanhGia`) | `DanhGia.java` | `id (int)` | Đánh giá & nhận xét sản phẩm của khách hàng (Số sao 1-5, bình luận, trạng thái) |
| 17 | `HinhAnhDanhGia` (hoặc `danh_gia_anh`) | `HinhAnhDanhGia.java` | `id (int)` | Hình ảnh đính kèm trong bài đánh giá của khách hàng |
| 18 | `danh_muc` (hoặc `DanhMuc`) | `DanhMuc.java` | `id (int)` | Danh mục sản phẩm (Vợt, Giày, Trang phục, Balo, Phụ kiện) |
| 19 | `thuong_hieu` (hoặc `ThuongHieu`) | `ThuongHieu.java` | `id (int)` | Thương hiệu (Yonex, Victor, Li-Ning, Lining, Mizuno, Kawasaki...) |
| 20 | `thuoc_tinh` (hoặc `ThuocTinh`) | `ThuocTinh.java` | `id (int)` | Từ điển thuộc tính động (Màu sắc, Size, Trọng lượng, Mức căng...) |
| 21 | `danh_muc_thuoc_tinh` (hoặc `DanhMucThuocTinh`) | `DanhMucThuocTinh.java` | `id (int)` | Ràng buộc thuộc tính cho phép trên từng danh mục |
| 22 | `san_pham_chi_tiet_thuoc_tinh` | `SanPhamChiTietThuocTinh.java` | `id (int)` | Giá trị thuộc tính cụ thể của từng biến thể sản phẩm |
| 23 | `hinh_anh_san_pham` (hoặc `HinhAnhSanPham`) | `HinhAnhSanPham.java` | `id (int)` | Thư viện hình ảnh chi tiết của sản phẩm |
| 24 | `gio_hang` (hoặc `GioHang`) | `GioHang.java` | `id (int)` | Giỏ hàng của thành viên trong database |
| 25 | `gio_hang_chi_tiet` (hoặc `GioHangChiTiet`) | `GioHangChiTiet.java` | `id (int)` | Chi tiết từng món trong giỏ hàng thành viên |
| 26 | `TrangThaiGioHang` | `TrangThaiGioHang.java` | `id (int)` | Lưu vết trạng thái hoạt động của giỏ hàng |
| 27 | `san_pham_yeu_thich` (hoặc `SanPhamYeuThich`) | `SanPhamYeuThich.java` | `id (int)` | Danh sách sản phẩm yêu thích (Wishlist) của khách hàng |
| 28 | `don_vi_van_chuyen` (hoặc `DonViVanChuyen`) | `DonViVanChuyen.java` | `id (int)` | Đơn vị vận chuyển (GHN, GHTK, Mặc định) và mức phí Local / Nationwide |
| 29 | `phuong_thuc_thanh_toan` (hoặc `PhuongThucThanhToan`) | `PhuongThucThanhToan.java` | `id (int)` | Phương thức thanh toán (COD, SePay, ZaloPay) |
| 30 | `LichSuTrangThaiDonHang` | `LichSuTrangThaiDonHang.java` | `id (int)` | Ghi vết chuyển đổi trạng thái đơn hàng (Audit trail timeline) |
| 31 | `MaKhoiPhuc` (hoặc `token_khoi_phuc`) | `TokenKhoiPhuc.java` | `id (int)` | Token xác thực khôi phục mật khẩu hoặc kích hoạt tài khoản Guest |
| 32 | `Blog` | `Blog.java` | `id (int)` | Bài viết tin tức & cẩm nang (Tiêu đề, slug SEO, nội dung CKEditor, ảnh, published, soft-deleted) |
| 33 | `BlogComment` | `BlogComment.java` | `id (int)` | Bình luận trong bài viết Blog |
| 34 | `NewsletterSubscriber` | `NewsletterSubscriber.java` | `id (int)` | Danh sách email đăng ký nhận bản tin khuyến mãi |
| 35 | `ThongBao` | `ThongBao.java` | `id (int)` | Thông báo hệ thống gửi tới tài khoản người dùng |
| 36 | `EditLog` | `EditLog.java` | `id (int)` | Nhật ký kiểm toán thao tác chỉnh sửa dữ liệu của Quản trị viên |
| 37 | `CommentModerationKeyword` | `CommentModerationKeyword.java` | `id (int)` | Từ điển từ khóa thô tục, xúc phạm, spam cần lọc |
| 38 | `CommentViolationLog` | `CommentViolationLog.java` | `id (int)` | Nhật ký vi phạm bình luận của người dùng phục vụ tự động khóa tài khoản |
| 39 | `ChatConversation` | `ChatConversation.java` | `id (int)` | Phiên hội thoại giữa khách hàng và Trợ lý ảo AI Gemini |
| 40 | `ChatMessage` | `ChatMessage.java` | `id (int)` | Nội dung từng tin nhắn trong phiên chat (User / Assistant / System) |
| 41 | `ChatFeedback` | `ChatFeedback.java` | `id (int)` | Đánh giá phản hồi (Like/Dislike) của khách về câu trả lời của AI |
| 42 | `flyway_schema_history` | Flyway Core | `installed_rank (int)` | Bảng quản lý lịch sử versioning database migration |

---

## 2. CHI TIẾT CÁC BẢNG ĐẶC THÙ & CƠ CHẾ ÁNH XẠ

### 2.1. Bảng `TichHopVanChuyen` (GHN Integration Table)
- **Cấu trúc DDL từ `BadmintonShopDB1_ban_moi_nhat_.sql`:**
```sql
CREATE TABLE [dbo].[TichHopVanChuyen](
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [id_hoa_don] [int] NOT NULL,
    [nha_cung_cap] [nvarchar](50) NOT NULL, -- 'GHN', 'GHN_RETURN', 'GHN_EXCHANGE', 'GHN_REJECT_RETURN'
    [ma_don_hang_ngoai] [nvarchar](100) NULL,
    [ma_van_don] [nvarchar](100) NULL,      -- Mã bưu tá GHN (VD: 'L3Z7P9')
    [trang_thai] [nvarchar](100) NULL,      -- Trạng thái bưu tá ('ready_to_pick', 'delivering', 'delivered'...)
    [du_lieu_yeu_cau] [nvarchar](max) NULL,
    [du_lieu_phan_hoi] [nvarchar](max) NULL,
    [ngay_tao] [datetime] NOT NULL
)
```
- **Cơ chế ánh xạ trong Java:** Bảng này được truy vấn bằng **Native SQL** trong `HoaDonRepository.findActiveShippingOrders` và ánh xạ ảo vào các trường `@Formula` của `HoaDon.java`:
  - `ghnOrderCode` = `(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC)`
  - `ghnReturnOrderCode` = `(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap = 'GHN_RETURN' ORDER BY t.id DESC)`
  - `ghnStatus` = `(SELECT TOP 1 t.trang_thai FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC)`

---

### 2.2. Bảng `GiaoDichThanhToan` (`PaymentTransaction`)
- **Cấu trúc DDL từ `BadmintonShopDB1_ban_moi_nhat_.sql`:**
```sql
CREATE TABLE [dbo].[GiaoDichThanhToan](
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [ma_giao_dich] [nvarchar](100) NOT NULL, -- Transaction ID từ SePay (Unique)
    [id_hoa_don] [int] NULL,
    [so_tien] [decimal](18, 2) NOT NULL,
    [cong_thanh_toan] [nvarchar](50) NOT NULL, -- 'SEPAY', 'VNPAY', 'ZALOPAY'
    [trang_thai] [nvarchar](50) NOT NULL,      -- 'SUCCESS', 'PAID_INSUFFICIENT_STOCK', 'PAID_RECEIVED_AFTER_CANCEL'
    [du_lieu_tho] [nvarchar](max) NULL,        -- Toàn bộ JSON payload từ SePay IPN Webhook
    [ngay_tao] [datetime] NOT NULL
)
```
- **Cơ chế:** Đảm bảo **Idempotency** chống xử lý trùng lặp webhook SePay.

---

### 2.3. Bảng `san_pham_chi_tiet` & Hai Cột Tồn Kho
- **Cột `soLuongTon` (Tồn kho kinh doanh):** Phục vụ bán hàng online và POS. Được trừ theo thuật toán FIFO khi đặt hàng.
- **Cột `soLuongSpLoi` (Kho sản phẩm lỗi):** Lưu số lượng hàng đổi trả bị lỗi do sản xuất hoặc vận chuyển. Hoàn toàn cách ly khỏi giỏ hàng khách.

---

## 3. BẢN ĐỒ TOÀN BỘ ENUMS & STATE MACHINES

### 3.1. `OrderStatus`
`CHO_THANH_TOAN` ("cho_thanh_toan"), `CHO_XAC_NHAN` ("cho_xac_nhan"), `CHO_GIAO_HANG` ("cho_giao_hang"), `DANG_GIAO_HANG` ("dang_giao"), `DA_GIAO` ("da_giao"), `DA_HUY` ("da_huy"), `TRA_HANG` ("tra_hang"), `STOCK_CONFLICT`, `YEU_CAU_HUY`.

### 3.2. `PaymentStatus`
`PENDING` ("pending"), `PAID` ("paid"), `FAILED` ("failed"), `EXPIRED` ("expired"), `AMOUNT_MISMATCH`, `PAID_RECEIVED_AFTER_CANCEL`, `PAID_INSUFFICIENT_STOCK`.

### 3.3. `ReturnStatus`
`REQUESTED` ("CHO_DUYET"), `APPROVED` ("DA_DUYET"), `REJECTED` ("TU_CHOI"), `PICKING` ("DANG_THU_HOI"), `PICKED` ("DA_THU_HOI"), `INSPECTING` ("DANG_KIEM_TRA"), `REFUND_PENDING` ("CHO_HOAN_TIEN"), `EXCHANGE_PENDING`, `EXCHANGE_SHIPPING`, `EXCHANGED`, `COMPLETED` ("HOAN_TIEN_THANH_CONG").

### 3.4. `RefundStatus`
`PENDING`, `COMPLETED`, `REJECTED`.

### 3.5. `AccountStatus`
`GUEST`, `ACTIVE`, `LOCKED`.

### 3.6. `CategoryType`
`VOT`, `GIAY`, `TRANG_PHUC`, `BALO`, `CUOC`, `QUAN_CAN`, `BANG_QUAN`, `HOP_CAU`, `OTHER`.

---
*Tài liệu Database hoàn chỉnh, cập nhật chính xác theo file SQL dump `scratch/BadmintonShopDB1_ban_moi_nhat_.sql`.*
