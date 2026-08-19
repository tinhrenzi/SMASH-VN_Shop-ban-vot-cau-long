# 08. TÍCH HỢP HỆ THỐNG BÊN NGOÀI, SCHEDULERS & KHÓA (INTEGRATIONS & CONCURRENCY)

---

## 1. CỔNG THANH TOÁN SEPAY (SEPAY PAYMENT GATEWAY)

### 1.1. Cấu hình & Thông số Kết Nối
- **File cấu hình:** `src/main/java/com/smashvn/shop/config/SepayConfig.java`
- **File properties:** `src/main/resources/application.properties`
  - `sepay.base-url`: `https://my.sepay.vn`
  - `sepay.ipn-secret`: Chuỗi bí mật xác thực Header `Authorization: Apikey {secret}`
  - `sepay.ip-verification`: Cờ bật/tắt kiểm tra dải IP chính thức của SePay
  - `sepay.whitelisted-ips`: Danh sách dải IP máy chủ SePay được phép gửi webhook

### 1.2. Các File & Endpoint Xử Lý Webhook IPN
- **Controller:** `SepayIpnController.java` (Endpoint: `POST /api/payment/sepay/ipn`)
- **Service Điều Phối:** `SepayGatewayService.java` (Method: `handleIpn`)
- **Service Giao Dịch & Tồn Kho:** `SepayOrderPaymentService.java` (Method: `xuLyThanhToanSePay`)
- **Repository Giao Dịch:** `PaymentTransactionRepository.java` (Lưu table `payment_transactions` / `GiaoDichThanhToan`)

### 1.3. Cơ Chế Bảo Vệ & Xử Lý Sự Cố Thanh Toán
1. **Chống trùng lặp Webhook (Idempotency):** Tra cứu `paymentTransactionRepository.findByTransactionId(maGiaoDich)`. Nếu giao dịch đã tồn tại, lập tức trả về HTTP 200 OK `Already processed` và không thực thi lặp lại.
2. **Khóa Đơn Hàng:** Dùng `hoaDonRepository.findByIdWithLock(orderId)` với `PESSIMISTIC_WRITE` lock ngăn chặn 2 Webhook xử lý cùng 1 đơn tại cùng một thời điểm.
3. **Xử lý thiếu kho (Stock Conflict):** Nếu khách chuyển khoản thành công nhưng kho hàng đã bị đơn khác mua mất ➔ Hệ thống ghi nhận `PaymentTransaction` với status `PAID_INSUFFICIENT_STOCK`, chuyển đơn sang `YEU_CAU_HUY` và `CHO_HOAN_TIEN`, lưu cờ `RefundStatus.PENDING`. Tuyệt đối không throw Exception để bảo toàn dấu vết nhận tiền của khách.
4. **Xử lý chuyển tiền đơn đã hủy (Late Webhook):** Ghi nhận status `PAID_RECEIVED_AFTER_CANCEL`, ghi log cảnh báo Admin hoàn tiền thủ công cho khách.
5. **Xử lý sai số tiền chuyển khoản:** Ghi nhận `AMOUNT_MISMATCH` để Admin can thiệp xử lý.

---

## 2. DỊCH VỤ VẬN CHUYỂN GIAO HÀNG NHANH (GHN v2 API)

### 2.1. Cấu hình & Thông số
- **File cấu hình:** `src/main/java/com/smashvn/shop/config/GhnConfig.java`
- **Properties:** `ghn.api.token`, `ghn.api.shop-id`, `ghn.api.base-url`, `ghn.webhook.secret`
- **Địa chỉ kho xuất hàng mặc định (Smash-VN Warehouse):** Tích hợp Province ID, District ID, Ward Code trong cấu hình.

### 2.2. Các API GHN Tích Hợp
- **Service:** `GhnService.java` & `GhnShipmentPersistenceService.java`
- **Bảng lưu trữ:** `TichHopVanChuyen` (Phân biệt rõ ràng theo nhà cung cấp: `GHN`, `GHN_RETURN`, `GHN_EXCHANGE`).
- **Tính phí ship realtime:** `calculateShipFee(toDistrictId, toWardCode, weight, insuranceValue)` ➔ Gọi `POST /shiip/public-api/v2/shipping-order/fee`.
- **Tạo vận đơn giao hàng chính thức:** `createShippingOrder(hoaDon, items, toDistrictId, toWardCode)` ➔ Gọi `POST /shiip/public-api/v2/shipping-order/create`. Nhận về `ghnOrderCode` (VD: `L3Z7P9`).
- **Tạo đơn thu hồi đổi trả:** `createReturnOrder(hoaDon, reason)` ➔ Gọi API tạo đơn thu hồi bưu tá với mã phân loại `GHN_RETURN`.
- **Tạo đơn gửi đổi hàng mới:** `createExchangeOrder(hoaDon, newItem)` ➔ Tạo đơn bưu tá với mã `GHN_EXCHANGE`.
- **Tra cứu lộ trình bưu tá:** `trackOrder(orderCode)` ➔ Gọi `POST /v2/shipping-order/detail`.
- **Hủy vận đơn khi hủy đơn hàng:** `cancelOrder(ghnOrderCode)` ➔ Gọi `POST /shiip/public-api/v2/switch-status/cancel`.

---

## 3. TRỢ LÝ ẢO AI GOOGLE GEMINI 2.0 FLASH

### 3.1. Cấu hình & Giao thức Kết Nối
- **File cấu hình:** `src/main/java/com/smashvn/shop/config/GeminiHttpClientConfig.java`
- **Model:** `gemini-2.0-flash`
- **Giao thức:** OpenAI-compatible REST API (`https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`)
- **API Key:** `gemini.api.key`

### 3.2. Kiến Trúc RAG (Retrieval-Augmented Generation)
- **Service:** `ChatbotServiceImpl.java`
- **Quy trình 4 bước:**
  1. Phân tích thực thể & khoảng giá trong câu hỏi người dùng qua `VietnamesePriceParser.java`.
  2. Truy vấn dữ liệu thực tế từ database qua `SanPhamChiTietRepository.searchForChatbot()`.
  3. Ghép danh sách sản phẩm còn hàng, thông số kỹ thuật (độ cứng, điểm cân bằng, mức căng) và giá bán thực tế vào System Prompt làm ngữ cảnh.
  4. Gửi request tới model Gemini 2.0 Flash và trả về câu trả lời văn bản tự nhiên kèm danh sách thẻ sản phẩm gợi ý tương tác trực tiếp trên giao diện web.

---

## 4. BẢNG TỔNG HỢP 4 BACKGROUND SCHEDULERS

| Tác Vụ Ngầm | Class Khai Báo | Tần Suất / Chu Kỳ | Mục Đích & Dữ Liệu Tác Động |
| :--- | :--- | :--- | :--- |
| **Đồng Bộ Vận Đơn GHN** | `GhnPollingScheduler.java` | Mỗi 10 phút (`fixedDelay = 600000ms`) | Quét các đơn đang vận chuyển (`cho_xac_nhan`, `dang_giao`, `dang_thu_hoi`), gọi GHN Track API để tự động cập nhật sang Đang giao, Đã giao hoặc Đang kiểm tra trong database. |
| **Quét Kiểm Duyệt Đánh Giá** | `CommentModerationScheduler.java` | Mỗi 5 phút (`fixedDelay = 300000ms`) | Quét toàn bộ đánh giá mới tạo, kiểm tra từ ngữ nhạy cảm/thô tục và tự động ẩn các nội dung vi phạm, ghi log vi phạm `CommentViolationLog`. |
| **Dọn Rác Blog Đã Xóa Mềm** | `BlogService.java` | 3:00 AM mỗi ngày (`cron = "0 0 3 * * *"`) | Xóa vĩnh viễn các bài viết trong `blog` có `deleted = true` đã quá 90 ngày và xóa file ảnh liên quan khỏi đĩa. |
| **Dọn Dẹp Checkout Hết Hạn** | `PendingCheckoutRegistry.java` | Mỗi 1 phút (`fixedRate = 60000ms`) | Giải phóng các snapshot checkout online quá 30 phút trong bộ nhớ RAM để tránh rò rỉ bộ nhớ máy chủ. |

---

## 5. CƠ CHẾ KHÓA & CHỐNG XUNG ĐỘT ĐỒNG THỜI (LOCKS & CONCURRENCY)

| Entity | Method Khóa | Loại Lock | Mục Đích Bảo Vệ |
| :--- | :--- | :--- | :--- |
| `SanPham` | `SanPhamRepository.findByIdWithLock` | `PESSIMISTIC_WRITE` | Khóa sản phẩm cha theo thứ tự ID ASC khi phân bổ tồn kho FIFO trong `InventoryLotService` nhằm triệt tiêu hoàn toàn hiện tượng Deadlock đa luồng. |
| `SanPhamChiTiet` | `SanPhamChiTietRepository.findByIdWithLock` | `PESSIMISTIC_WRITE` | Khóa độc quyền bản ghi biến thể khi kiểm tra tồn kho tại bước thêm vào giỏ hàng và tạo đơn hàng. Ngăn chặn hiện tượng bán vượt tồn kho (Overselling). |
| `PhieuGiamGia` | `PhieuGiamGiaRepository.findByMaPhieuWithLock` | `PESSIMISTIC_WRITE` | Khóa bản ghi Voucher khi trừ `soLuongConLai`. Đảm bảo voucher chỉ có thể được sử dụng đúng số lượng đã phát hành. |
| `HoaDon` | `HoaDonRepository.findByIdWithLock` | `PESSIMISTIC_WRITE` | Khóa đơn hàng khi xử lý SePay Webhook IPN hoặc Hủy đơn, ngăn chặn 2 request Webhook/Admin xử lý trùng lặp cùng lúc. |

---
*Tài liệu Tích hợp hệ thống hoàn chỉnh của dự án SMASH-VN.*
