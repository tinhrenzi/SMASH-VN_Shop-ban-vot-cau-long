# Báo Cáo Giỏ Hàng Mồ Côi Trên Database SMDB_FINAL

Tài liệu này ghi nhận kết quả xác minh chi tiết về 6 giỏ hàng mồ côi (không có khách hàng tương ứng) được phát hiện trong cơ sở dữ liệu **SMDB_FINAL**.

---

## 1. Chi Tiết Các Giỏ Hàng Mồ Côi (Orphan Carts)

Kết quả thực thi truy vấn xác minh trên `SMDB_FINAL.dbo.GioHang` cho thấy:

| ID Giỏ Hàng | ID Khách Hàng (Bị Thiếu) | Ngày tạo | Ngày cập nhật | Có Chi Tiết Giỏ Hàng? |
|---|---|---|---|---|
| `587` | `1209` | 2026-06-26 14:47:04.450 | 2026-06-26 14:47:04.450 | **Có** (1 sản phẩm) |
| `588` | `1210` | 2026-06-26 14:47:05.170 | 2026-06-26 14:47:05.170 | **Có** (1 sản phẩm) |
| `627` | `1307` | 2026-06-26 14:54:25.537 | 2026-06-26 14:54:25.537 | **Không** |
| `628` | `1308` | 2026-06-26 14:54:27.160 | 2026-06-26 14:54:27.160 | **Không** |
| `667` | `1405` | 2026-06-26 14:56:08.223 | 2026-06-26 14:56:08.223 | **Không** |
| `668` | `1406` | 2026-06-26 14:56:09.870 | 2026-06-26 14:56:09.870 | **Không** |

---

## 2. Chi Tiết Các Sản Phẩm Trong Giỏ Hàng Mồ Côi (Orphan Cart Items)

Truy vấn đối chiếu chi tiết giỏ hàng (`GioHangChiTiet`) liên quan đến các giỏ hàng mồ côi trên trả về **2 dòng**:

* **Giỏ hàng 587**: Chứa 1 bản ghi `GioHangChiTiet` (ID = 679) trỏ đến biến thể sản phẩm chi tiết có ID `972`, số lượng = `1`.
* **Giỏ hàng 588**: Chứa 1 bản ghi `GioHangChiTiet` (ID = 680) trỏ đến biến thể sản phẩm chi tiết có ID `973`, số lượng = `1`.
* Các giỏ hàng `627`, `628`, `667`, `668` đều là giỏ hàng trống rỗng, không chứa sản phẩm.

---

## 3. Nguyên Nhân Phát Sinh
Các giỏ hàng này được tạo ra trong quá trình chạy kiểm thử hoặc quy trình tạo giỏ hàng vãng lai (Guest Checkout) trước đó. Khi các bản ghi Khách hàng (`KhachHang`) liên quan bị xóa hoặc rollback dọn dẹp dữ liệu kiểm thử, hệ thống đã **không thực hiện xóa CASCADE hoặc SET NULL** khóa ngoại `id_khach_hang` của bảng `GioHang`, dẫn tới việc các bản ghi giỏ hàng này mất liên kết (mồ côi).

---

## 4. Đề Xuất Xử Lý

> [!IMPORTANT]
> **Không thực hiện xóa trực tiếp ngay lập tức trên Production khi chưa sao lưu.**

* **Giải pháp khuyến nghị**: **CLEANUP_AFTER_BACKUP**.
* **Các bước thực thi an toàn**:
  1. Thực hiện backup toàn bộ database `SMDB_FINAL`.
  2. Tạo kịch bản dọn dẹp các chi tiết giỏ hàng mồ côi trước, sau đó xóa các bản ghi giỏ hàng mồ côi:
     ```sql
     -- Bước A: Xóa chi tiết giỏ hàng mồ côi
     DELETE gc
     FROM GioHangChiTiet gc
     JOIN GioHang g ON gc.id_gio_hang = g.id
     LEFT JOIN KhachHang k ON g.id_khach_hang = k.id
     WHERE g.id_khach_hang IS NOT NULL AND k.id IS NULL;

     -- Bước B: Xóa giỏ hàng mồ côi
     DELETE g
     FROM GioHang g
     LEFT JOIN KhachHang k ON g.id_khach_hang = k.id
     WHERE g.id_khach_hang IS NOT NULL AND k.id IS NULL;
     ```
  3. Kiểm tra lại toàn vẹn dữ liệu giỏ hàng để đảm bảo hệ thống hoạt động ổn định.
