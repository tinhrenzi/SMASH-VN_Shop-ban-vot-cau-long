# 03. TÀI LIỆU CƠ SỞ DỮ LIỆU & SCHEMA TOÀN DIỆN (DATABASE DOCUMENTATION)

> **Cơ sở dữ liệu:** Microsoft SQL Server 2022  
> **Tên Database:** `BadmintonShopDB1`  
> **File SQL Schema mới nhất:** `scratch/BadmintonShopDB1_ban_moi_nhat_.sql` (42 Tables)  
> **Công cụ ORM & Truy vấn:** Hibernate 6.x / Spring Data JPA + Native SQL (TichHopVanChuyen)  
> **Quản lý Migration:** Flyway Migration (`flyway_schema_history`)  

---

## 1. BẢNG TỔNG HỢP 42 BẢNG DATABASE TRONG HỆ THỐNG

| STT | Tên Bảng (Table Name) | Entity / Cơ Chế Ánh Xạ | Khóa Chính (PK) | Vai Trò & Nghiệp Vụ Trong Hệ Thống |
| :---: | :--- | :--- | :--- | :--- |
| 1 | `san_pham` | `SanPham.java` | `id (int)` | **Sản phẩm cha**: Tên sản phẩm, mô tả, ảnh đại diện, danh mục, thương hiệu, trạng thái, điểm đánh giá trung bình. |
| 2 | `san_pham_chi_tiet` | `SanPhamChiTiet.java` | `id (int)` | **Biến thể con**: Màu sắc, size, trọng lượng, mức căng, giá bán, SKU/Barcode, tồn kho kinh doanh `soLuongTon`, tồn kho hàng lỗi `soLuongSpLoi`. |
| 3 | `hoa_don` | `HoaDon.java` | `id (int)` | **Hóa đơn / Đơn hàng**: Mã đơn hàng, tổng tiền, phí ship, voucher giảm, trạng thái đơn, trạng thái thanh toán, lý do hủy, thông tin hoàn tiền, video bằng chứng trả hàng. |
| 4 | `hoa_don_chi_tiet` | `HoaDonChiTiet.java` | `id (int)` | **Chi tiết đơn hàng**: Số lượng, đơn giá, giá gốc, snapshot tên sản phẩm, SKU, thông tin lô nhập phân bổ FIFO. |
| 5 | `tai_khoan` | `TaiKhoan.java` | `id (int)` | **Tài khoản đăng nhập**: Username, mật khẩu BCrypt, vai trò (ROLE_QL, ROLE_NV, ROLE_KH), trạng thái (GUEST, ACTIVE, LOCKED). |
| 6 | `khach_hang` | `KhachHang.java` | `id (int)` | **Hồ sơ khách hàng**: Họ tên, số điện thoại, email, giới tính, ngày sinh, cờ đăng ký nhận bản tin khuyến mãi. |
| 7 | `nhan_vien` | `NhanVien.java` | `id (int)` | **Hồ sơ nhân viên**: Họ tên, số điện thoại, email, chức vụ, trạng thái làm việc, ngày bắt đầu. |
| 8 | `so_dia_chi` | `SoDiaChi.java` | `id (int)` | **Sổ địa chỉ nhận hàng**: Tên người nhận, SĐT, địa chỉ chi tiết, Province ID, District ID, Ward Code GHN, cờ địa chỉ mặc định. |
| 9 | `phieu_giam_gia` | `PhieuGiamGia.java` | `id (int)` | **Phiếu giảm giá / Voucher**: Mã code, loại giảm (% / VND), giá trị giảm, giảm tối đa, đơn hàng tối thiểu, số lượng phát hành, số lượng còn lại, ngày bắt đầu/kết thúc. |
| 10 | `dot_giam_gia` | `DotGiamGia.java` | `id (int)` | **Đợt giảm giá trực tiếp**: Mã chiến dịch, tên chiến dịch, % giảm giá, kiểu áp dụng (`ApDungKieu`), ngày bắt đầu/kết thúc, trạng thái. |
| 11 | `SanPham_DotGiamGia` | `@ManyToMany` Join Table | `(id_san_pham, id_dot_giam_gia)` | Bảng liên kết trung gian giữa Sản phẩm cha và Chiến dịch giảm giá trực tiếp. |
| 12 | `phieu_nhap` | `PhieuNhap.java` | `id (int)` | **Phiếu nhập kho**: Mã phiếu nhập (PN-YYYYMMDD-XXXX), nhà cung cấp, tổng tiền nhập, ghi chú, nhân viên lập phiếu, ngày tạo. |
| 13 | `phieu_nhap_chi_tiet` | `PhieuNhapChiTiet.java` | `id (int)` | **Chi tiết lô nhập hàng FIFO**: Liên kết biến thể `SanPhamChiTiet`, số lượng nhập ban đầu, đơn giá nhập, số lượng tồn kho còn lại của lô (`soLuongTonLo`), ngày nhập. |
| 14 | `TichHopVanChuyen` | Native SQL & `@Formula` | `id (int)` | **Bảng tích hợp vận chuyển GHN**: Lưu lịch sử vận đơn giao hàng (`GHN`), đơn thu hồi đổi trả (`GHN_RETURN`), đơn giao đổi mới (`GHN_EXCHANGE`), mã vận đơn bưu tá, trạng thái bưu cục realtime. |
| 15 | `GiaoDichThanhToan` | `PaymentTransaction.java` | `id (int)` | **Lịch sử giao dịch thanh toán**: Transaction ID unique từ SePay, số tiền, cổng (`SEPAY`, `ORDER_CANCEL_REFUND`), trạng thái (`SUCCESS`, `PAID_INSUFFICIENT_STOCK`, `REFUND_SUCCESS`), raw JSON payload. |
| 16 | `danh_gia` | `DanhGia.java` | `id (int)` | **Đánh giá & Review**: Điểm số sao (1-5 sao), bình luận nhận xét, trạng thái hiển thị/ẩn, cờ vi phạm kiểm duyệt. |
| 17 | `HinhAnhDanhGia` | `HinhAnhDanhGia.java` | `id (int)` | Hình ảnh đính kèm trong bài đánh giá của khách hàng. |
| 18 | `danh_muc` | `DanhMuc.java` | `id (int)` | **Danh mục sản phẩm**: Tên danh mục, slug, icon, trạng thái, danh mục cha/con. |
| 19 | `thuong_hieu` | `ThuongHieu.java` | `id (int)` | **Thương hiệu**: Tên thương hiệu (Yonex, Victor, Li-Ning, Mizuno, Kawasaki, Kumpoo...), xuất xứ, logo, trạng thái. |
| 20 | `thuoc_tinh` | `ThuocTinh.java` | `id (int)` | **Từ điển thuộc tính động**: Màu sắc, Size, Trọng lượng (3U, 4U, 5U), Mức căng cước (lbs), Chu vi cán vợt (G4, G5). |
| 21 | `danh_muc_thuoc_tinh` | `DanhMucThuocTinh.java` | `id (int)` | Ràng buộc các thuộc tính được phép áp dụng trên từng danh mục sản phẩm cụ thể. |
| 22 | `san_pham_chi_tiet_thuoc_tinh` | `SanPhamChiTietThuocTinh.java` | `id (int)` | Giá trị thuộc tính cụ thể của từng biến thể con (EAV Pattern). |
| 23 | `hinh_anh_san_pham` | `HinhAnhSanPham.java` | `id (int)` | Thư viện hình ảnh chi tiết và góc chụp của sản phẩm. |
| 24 | `gio_hang` | `GioHang.java` | `id (int)` | Giỏ hàng lưu trong cơ sở dữ liệu của khách hàng thành viên. |
| 25 | `gio_hang_chi_tiet` | `GioHangChiTiet.java` | `id (int)` | Chi tiết từng biến thể và số lượng đặt trong giỏ hàng thành viên. |
| 26 | `TrangThaiGioHang` | `TrangThaiGioHang.java` | `id (int)` | Lưu vết trạng thái hoạt động của giỏ hàng (ACTIVE, ABANDONED, CONVERTED). |
| 27 | `san_pham_yeu_thich` | `SanPhamYeuThich.java` | `id (int)` | Danh sách sản phẩm yêu thích (Wishlist) của khách hàng. |
| 28 | `don_vi_van_chuyen` | `DonViVanChuyen.java` | `id (int)` | Đơn vị vận chuyển (GHN, GHTK, Mặc định) và mức phí Local / Nationwide fallback. |
| 29 | `phuong_thuc_thanh_toan` | `PhuongThucThanhToan.java` | `id (int)` | Phương thức thanh toán (COD, SePay QR, Tiền mặt tại quầy). |
| 30 | `LichSuTrangThaiDonHang` | `LichSuTrangThaiDonHang.java` | `id (int)` | Lịch sử chuyển đổi trạng thái đơn hàng (Audit trail timeline, người thực hiện, thời gian, ghi chú). |
| 31 | `MaKhoiPhuc` | `TokenKhoiPhuc.java` | `id (int)` | Token xác thực khôi phục mật khẩu hoặc kích hoạt tài khoản Guest qua Email. |
| 32 | `Blog` | `Blog.java` | `id (int)` | Bài viết tin tức & cẩm nang (Tiêu đề, slug SEO, tóm tắt, nội dung HTML CKEditor, ảnh đại diện, cờ published, soft delete `deleted`). |
| 33 | `BlogComment` | `BlogComment.java` | `id (int)` | Bình luận của độc giả trong bài viết Blog. |
| 34 | `NewsletterSubscriber` | `NewsletterSubscriber.java` | `id (int)` | Danh sách email đăng ký nhận bản tin khuyến mãi. |
| 35 | `ThongBao` | `ThongBao.java` | `id (int)` | Thông báo hệ thống gửi tới từng tài khoản người dùng (Đơn hàng, khuyến mãi, đổi trả). |
| 36 | `EditLog` | `EditLog.java` | `id (int)` | Nhật ký kiểm toán thao tác quản trị: chỉnh sửa sản phẩm, đổi đơn hàng, xử lý hàng lỗi (`XUAT_TRA_NCC`, `THANH_LY`, `TIEU_HUY`). |
| 37 | `CommentModerationKeyword` | `CommentModerationKeyword.java` | `id (int)` | Từ điển các từ khóa thô tục, xúc phạm, spam cần hệ thống tự động lọc. |
| 38 | `CommentViolationLog` | `CommentViolationLog.java` | `id (int)` | Nhật ký vi phạm bình luận của người dùng phục vụ cảnh báo và tự động khóa tài khoản. |
| 39 | `ChatConversation` | `ChatConversation.java` | `id (int)` | Phiên hội thoại giữa khách hàng và Trợ lý ảo AI Gemini. |
| 40 | `ChatMessage` | `ChatMessage.java` | `id (int)` | Nội dung từng tin nhắn trong phiên chat (Sender: User / Assistant / System). |
| 41 | `ChatFeedback` | `ChatFeedback.java` | `id (int)` | Đánh giá phản hồi (Like / Dislike / Góp ý) của người dùng về câu trả lời của AI. |
| 42 | `flyway_schema_history` | Flyway Core | `installed_rank (int)` | Bảng quản lý lịch sử versioning database migration tự động. |

---

## 2. CHI TIẾT CÁC BẢNG ĐẶC THÙ & CẤU TRÚC DDL CHUYÊN BIỆT

### 2.1. Bảng `TichHopVanChuyen` (GHN Multi-Carrier Integration)
- **Cấu trúc DDL từ `BadmintonShopDB1_ban_moi_nhat_.sql`:**
```sql
CREATE TABLE [dbo].[TichHopVanChuyen](
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [id_hoa_don] [int] NOT NULL,
    [nha_cung_cap] [nvarchar](50) NOT NULL, -- 'GHN', 'GHN_RETURN', 'GHN_EXCHANGE', 'GHN_REJECT_RETURN'
    [ma_don_hang_ngoai] [nvarchar](100) NULL,
    [ma_van_don] [nvarchar](100) NULL,      -- Mã bưu tá GHN (VD: 'L3Z7P9')
    [trang_thai] [nvarchar](100) NULL,      -- Trạng thái bưu tá ('ready_to_pick', 'delivering', 'delivered'...)
    [du_lieu_yeu_cau] [nvarchar](max) NULL, -- JSON Request payload gửi GHN
    [du_lieu_phan_hoi] [nvarchar](max) NULL,-- JSON Response payload từ GHN
    [ngay_tao] [datetime] NOT NULL
)
```
- **Cơ chế ánh xạ trong Hibernate / JPA:** Được truy vấn bằng **Native SQL** trong `HoaDonRepository.findActiveShippingOrders` và ánh xạ ảo vào các trường `@Formula` của `HoaDon.java`:
  - `ghnOrderCode` = `(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC)`
  - `ghnReturnOrderCode` = `(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap = 'GHN_RETURN' ORDER BY t.id DESC)`
  - `ghnStatus` = `(SELECT TOP 1 t.trang_thai FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC)`

---

### 2.2. Bảng `GiaoDichThanhToan` (`PaymentTransaction`)
- **Cấu trúc DDL từ `BadmintonShopDB1_ban_moi_nhat_.sql`:**
```sql
CREATE TABLE [dbo].[GiaoDichThanhToan](
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [ma_giao_dich] [nvarchar](100) NOT NULL, -- Transaction ID từ SePay hoặc Refund Transaction Code
    [id_hoa_don] [int] NULL,
    [so_tien] [decimal](18, 2) NOT NULL,
    [cong_thanh_toan] [nvarchar](50) NOT NULL, -- 'SEPAY', 'VNPAY', 'ORDER_CANCEL_REFUND'
    [trang_thai] [nvarchar](50) NOT NULL,      -- 'SUCCESS', 'PAID_INSUFFICIENT_STOCK', 'PAID_RECEIVED_AFTER_CANCEL', 'REFUND_SUCCESS'
    [du_lieu_tho] [nvarchar](max) NULL,        -- Toàn bộ JSON payload từ SePay IPN Webhook hoặc Audit hoàn tiền
    [ngay_tao] [datetime] NOT NULL
)
```
- **Cơ chế Idempotency & Audit:** 
  - Đảm bảo chống xử lý trùng lặp webhook SePay bằng truy vấn `findByTransactionId`.
  - Khi Hủy đơn Online đã thanh toán, hệ thống ghi thêm 1 bản ghi mới với `cong_thanh_toan = 'ORDER_CANCEL_REFUND'` và `trang_thai = 'REFUND_SUCCESS'`, giữ nguyên vẹn bản ghi thanh toán gốc ban đầu.

---

### 2.3. Quản Lý Lô Nhập Hàng & Thuật Toán Xuất Kho FIFO (`phieu_nhap` & `phieu_nhap_chi_tiet`)
- **Cấu trúc Bảng `phieu_nhap` & `phieu_nhap_chi_tiet`:**
```sql
CREATE TABLE [dbo].[phieu_nhap](
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [ma_phieu_nhap] [varchar](50) NOT NULL UNIQUE,
    [nha_cung_cap] [nvarchar](255) NULL,
    [tong_tien_nhap] [decimal](18, 2) NULL,
    [ghi_chu] [nvarchar](500) NULL,
    [ngay_nhap] [datetime] NOT NULL,
    [id_nhan_vien] [int] NULL
)

CREATE TABLE [dbo].[phieu_nhap_chi_tiet](
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [id_phieu_nhap] [int] NOT NULL,
    [id_san_pham_chi_tiet] [int] NOT NULL,
    [so_luong_nhap] [int] NOT NULL,
    [don_gia_nhap] [decimal](18, 2) NOT NULL,
    [so_luong_ton_lo] [int] NOT NULL, -- Số lượng còn lại của lô này, trừ dần theo FIFO
    [ngay_nhap] [datetime] NOT NULL
)
```
- **Cơ chế:** Khi xuất kho, `InventoryLotService.allocateFifo()` quét các lô có `so_luong_ton_lo > 0` xếp theo `ngay_nhap ASC`, trừ lần lượt cho đến khi đủ số lượng đặt hàng.

---

### 2.4. Bảng `san_pham_chi_tiet` & Cơ Chế Cách Ly Hai Cột Tồn Kho
- **`soLuongTon` (Tồn kho kinh doanh khả dụng):** Dùng để bán hàng trên Web và POS. Tự động tăng khi nhập lô mới, giảm khi phân bổ FIFO, tăng lại khi hủy đơn.
- **`soLuongSpLoi` (Kho Hàng Lỗi / Quarantined Stock):** Lưu số lượng sản phẩm trả về bị lỗi (hư hỏng, lỗi nhà sản xuất do nhân viên kiểm kho phân loại). Tuyệt đối cách ly khỏi giỏ hàng khách và có trang quản trị riêng (`/admin/kho-san-pham-loi`).

---

## 3. BẢN ĐỒ TOÀN BỘ ENUMS & STATE MACHINES

### 3.1. `OrderStatus` (Trạng Thái Vòng Đời Đơn Hàng)
- `CHO_THANH_TOAN` ("cho_thanh_toan"): Đơn SePay QR vừa tạo, chờ khách quét mã chuyển khoản.
- `CHO_XAC_NHAN` ("cho_xac_nhan"): Đơn COD vừa đặt hoặc Đơn SePay đã thanh toán thành công, chờ Admin duyệt.
- `CHO_GIAO_HANG` ("dang_lay_hang" / "cho_giao_hang"): Đã xác nhận, đã trừ kho FIFO, đã sinh vận đơn GHN chờ bưu tá lấy.
- `DANG_GIAO_HANG` ("dang_giao"): Bưu tá GHN đang giao tới khách hàng.
- `DA_GIAO` ("da_giao"): Giao hàng thành công.
- `DA_HUY` ("da_huy"): Đơn hàng đã bị hủy (Khách tự hủy hoặc Admin hủy kèm hoàn kho / hoàn tiền).
- `TRA_HANG` ("tra_hang"): Đơn hàng đang trong quy trình RMA đổi trả.
- `STOCK_CONFLICT`: Xung đột tồn kho khi nhiều đơn tranh chấp.
- `YEU_CAU_HUY`: Yêu cầu hủy đơn phát sinh từ phía khách hoặc hệ thống (thiếu kho SePay).

### 3.2. `PaymentStatus` (Trạng Thái Thanh Toán)
- `PENDING` ("pending"): Chờ thanh toán.
- `PAID` ("paid"): Đã thanh toán thành công.
- `FAILED` ("failed"): Thanh toán thất bại.
- `EXPIRED` ("expired"): Hết hạn phiên thanh toán.
- `AMOUNT_MISMATCH`: Khách chuyển sai số tiền so với hóa đơn.
- `PAID_RECEIVED_AFTER_CANCEL`: Nhận tiền sau khi đơn hàng đã bị hủy trước đó.
- `PAID_INSUFFICIENT_STOCK`: Đã nhận tiền nhưng hết hàng trong kho.
- `CANCELLED` ("CANCELLED"): Hủy thanh toán (COD).
- `REFUNDED` ("REFUNDED"): Đã hoàn trả tiền cho khách hàng.

### 3.3. `ReturnStatus` (Trạng Thái Quy Trình Đổi Trả RMA)
- `REQUESTED` ("CHO_DUYET"): Khách vừa gửi yêu cầu đổi trả kèm hình ảnh/video.
- `APPROVED` ("DA_DUYET"): Admin đã duyệt yêu cầu, tạo vận đơn thu hồi `GHN_RETURN`.
- `REJECTED` ("TU_CHOI"): Admin từ chối yêu cầu đổi trả.
- `PICKING` ("DANG_THU_HOI"): Bưu tá đang đến lấy hàng tại nhà khách.
- `PICKED` ("DA_THU_HOI"): Bưu tá đã lấy được kiện hàng.
- `INSPECTING` ("DANG_KIEM_TRA"): Hàng đã về tới kho Smash-VN, nhân viên đang mở kiện kiểm kho.
- `REFUND_PENDING` ("CHO_HOAN_TIEN"): Hàng kiểm xong hợp lệ, chờ Admin hoàn tiền.
- `EXCHANGE_PENDING`: Chờ tạo vận đơn gửi sản phẩm đổi mới `GHN_EXCHANGE`.
- `EXCHANGE_SHIPPING`: Đang vận chuyển sản phẩm đổi mới tới khách.
- `EXCHANGED`: Đã đổi hàng mới thành công.
- `COMPLETED` ("HOAN_TIEN_THANH_CONG"): Đã hoàn tiền thành công cho khách, kết thúc quy trình RMA.

### 3.4. `RefundStatus`
- `PENDING`: Đang chờ thực hiện chuyển khoản hoàn tiền.
- `COMPLETED`: Đã hoàn tất chuyển tiền và ghi nhận chứng từ.
- `REJECTED`: Từ chối hoàn tiền.

### 3.5. `ReturnInventoryStatus`
- `PENDING`: Chờ kiểm kho.
- `RESTOCKED_GOOD`: Hàng nguyên vẹn, đã hoàn lại kho bán bình thường.
- `QUARANTINED_FAULTY`: Hàng bị lỗi, đã chuyển vào Kho Sản Phẩm Lỗi (`soLuongSpLoi`).
- `DISPOSED`: Đã xử lý thanh lý / tiêu hủy.

### 3.6. `AccountStatus`
- `GUEST`: Tài khoản tạm thời cấp cho khách vãng lai, giới hạn tối đa 3 lần mua.
- `ACTIVE`: Tài khoản hoạt động bình thường.
- `LOCKED`: Tài khoản bị khóa (do vi phạm bình luận hoặc Admin khóa thủ công).

### 3.7. `CategoryType`
- `VOT`: Vợt cầu lông (có thuộc tính trọng lượng, mức căng, chu vi cán).
- `GIAY`: Giày cầu lông (có thuộc tính size giày, màu sắc).
- `TRANG_PHUC`: Áo, quần, váy thể thao (có size áo/quần, màu sắc).
- `BALO`: Balo, túi đựng vợt.
- `CUOC`: Cước đan vợt (đường kính mm, độ nảy).
- `QUAN_CAN`: Quấn cán vợt.
- `BANG_QUAN`: Băng bảo vệ cổ tay, đầu gối.
- `HOP_CAU`: Hộp quả cầu lông (tốc độ 76, 77).
- `OTHER`: Phụ kiện khác.

---
*Tài liệu Database hoàn chỉnh, cập nhật chính xác theo file SQL dump `scratch/BadmintonShopDB1_ban_moi_nhat_.sql`.*
