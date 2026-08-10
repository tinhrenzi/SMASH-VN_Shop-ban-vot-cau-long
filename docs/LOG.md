# NHẬT KÝ THAY ĐỔI NGHIỆP VỤ (SMASH-VN CHANGELOG)

> **Trạng thái hệ thống**: `BUSINESS LOGIC FROZEN (KHÓA NGHIỆP VỤ)`
> **Kiểm thử tự động**: `58 / 58 INTEGRATION TESTS PASSED (100% BUILD SUCCESS)`
> **Cấu trúc CSDL**: `0 ALTER TABLE / 0 ADD COLUMN / 0 CREATE TABLE`

---

## 1. GIAO DIỆN BÁN HÀNG TẠI QUẦY
- **Khôi phục đầy đủ giao diện Bán Hàng Tại Quầy**: Bảo toàn đường dẫn `/admin/pos` và phân quyền cho Quản lý (`QL`) và Nhân viên (`NV`).
- **Loại bỏ chữ (POS) đóng mở ngoặc**: Chuẩn hóa tên hiển thị trên Sidebar, Tab danh sách đơn hàng và Tiêu đề trang thành: **Bán Hàng Tại Quầy**.

---

## 2. PHÂN LẬP PROVIDER VẬN CHUYỂN & SỬA LỖI AUDIT
- **Hibernate `@Formula`**: Sửa cột ảo trong `HoaDon.java` chỉ lấy mã vận đơn thu hồi `nha_cung_cap = 'GHN_RETURN' ORDER BY id DESC` cho trường `ghnReturnOrderCode`.
- **Native Query Synchronization**: Bổ sung subquery alias `AS ghnReturnOrderCode` trong hàm `HoaDonRepository.findActiveShippingOrders` tránh lỗi SQL Server.
- **Đồng bộ trạng thái thanh toán**: Bảo toàn trạng thái `DA_HOAN_TIEN` khi thực hiện hoàn tiền cho đơn trả hàng.

---

## 3. PHASE 6 - ĐỔI HÀNG ĐÚNG MÃ BIẾN THỂ (EXACT SPCT ID)
- **Trừ kho Exact SPCT ID**: Phân bổ chính xác theo `idSanPhamChiTiet` mà khách đã mua. Tuyệt đối không tự động chọn biến thể khác cùng sản phẩm cha.
- **Kiểm soát All-Or-Nothing**: Nếu đơn đổi chứa nhiều sản phẩm và có ít nhất 1 sản phẩm thiếu tồn kho ➔ Tự động Rollback toàn bộ.
- **Vận đơn đổi hàng GHN**: Khởi tạo vận đơn `GHN_EXCHANGE` với `COD = 0`.

---

## 4. PHASE 5 - BẢO VỆ HOÀN TIỀN 2 LỚP (DOUBLE REFUND GUARD)
- **Cơ chế Double Refund Guard 2 Lớp**:
  - *Layer 1*: Kiểm tra `ReturnStatus == REFUNDED`.
  - *Layer 2 + Reconcile*: Kiểm tra giao dịch thành công trong `PaymentTransaction` (`REFUND_SUCCESS`). Nếu đã tồn tại ➔ Thực hiện Reconcile chuyển trạng thái `REFUNDED` & `DA_HOAN_TIEN` mà không gọi refund trùng.
- **Dọn dẹp file tự động**: Tự động xóa file chứng từ vừa upload nếu dịch vụ hoàn tiền bị sự cố.

---

## 5. PHASE 3 & 4 - TÍCH HỢP GHN & KIỂM ĐỊNH HÀNG HOÀN AT SHOP
- **Transaction vận đơn độc lập**: `GhnShipmentPersistenceService` lưu mã vận đơn GHN bằng transaction `REQUIRES_NEW` độc lập.
- **3 Hướng xử lý kiểm định tại shop**:
  - `BAN_LAI`: Cộng lại tồn kho bán (`soLuongTon`).
  - `HANG_LOI`: Chuyển vào kho lỗi (`soLuongSpLoi`).
  - `TU_CHOI`: Tạo vận đơn `GHN_REJECT_RETURN` gửi trả lại sản phẩm cho khách hàng.

---

## 6. PHASE 1 & 2 - RÀNG BUỘC HẠN ĐỔI TRẢ 7 NGÀY & KHO LỖI
- **Quản lý kho lỗi**: Lưu trường `soLuongSpLoi` trong `SanPhamChiTiet` không thay đổi cấu trúc bảng CSDL (0 ALTER TABLE).
- **Hạn đổi trả 7 ngày**: Lấy mốc thời gian giao hàng từ `EditLog` và chỉ tính hạn 7 ngày nếu có log do khách hàng (`KHACH_HANG`) tự bấm xác nhận `hoan_thanh`.
