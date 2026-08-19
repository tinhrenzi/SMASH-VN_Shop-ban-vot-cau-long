# 08. TÍCH HỢP HỆ THỐNG BÊN NGOÀI, SCHEDULERS & KHÓA (INTEGRATIONS & CONCURRENCY)

---

## 1. CỔNG THANH TOÁN SEPAY (SEPAY PAYMENT GATEWAY)

### 1.1. Cấu hình & Thông số
- **File cấu hình:** `src/main/java/com/smashvn/shop/config/SepayConfig.java`
- **File properties:** `src/main/resources/application.properties`
  - `sepay.base-url`: `https://my.sepay.vn`
  - `sepay.ipn-secret`: Chuỗi bí mật xác thực Header `Authorization: Apikey {secret}`
  - `sepay.ip-verification`: Cờ bật/tắt kiểm tra dải IP chính thức của SePay
  - `sepay.whitelisted-ips`: Danh sách dải IP máy chủ SePay

### 1.2. Các File Xử Lý Webhook IPN
- **Controller:** `SepayIpnController.java` (Endpoint: `POST /api/payment/sepay/ipn`)
- **Service Điều Phối:** `SepayGatewayService.java` (Method: `handleIpn`)
- **Service Giao Dịch & Tồn Kho:** `SepayOrderPaymentService.java` (Method: `xuLyThanhToanSePay`)
- **Repository Giao Dịch:** `PaymentTransactionRepository.java` (Lưu table `payment_transactions`)

### 1.3. Cơ Chế Bảo Vệ & Xử Lý Sự Cố
1. **Chống trùng lặp Webhook (Idempotency):** Tra cứu `paymentTransactionRepository.findByTransactionId(maGiaoDich)`. Nếu giao dịch đã tồn tại, lập tức trả về HTTP 200 OK `Already processed` và không thực thi lại.
2. **Khóa Đơn Hàng:** Dùng `hoaDonRepository.findByIdWithLock(orderId)` với `PESSIMISTIC_WRITE` lock ngăn chặn 2 Webhook xử lý cùng 1 đơn tại cùng thời điểm.
3. **Xử lý thiếu kho (Stock Conflict):** Nếu khách chuyển khoản thành công nhưng kho hàng đã bị đơn khác mua mất ➔ Hệ thống ghi nhận `PaymentTransaction` với status `PAID_INSUFFICIENT_STOCK`, chuyển đơn sang `YEU_CAU_HUY` và `CHO_HOAN_TIEN`, lưu cờ `RefundStatus.PENDING`. Tuyệt đối không throw Exception để không mất dấu vết nhận tiền của khách.
4. **Xử lý chuyển tiền đơn đã hủy (Late Webhook):** Ghi nhận status `PAID_RECEIVED_AFTER_CANCEL`, ghi log cảnh báo Admin hoàn tiền thủ công.

---

## 2. DỊCH VỤ VẬN CHUYỂN GIAO HÀNG NHANH (GHN API)

### 2.1. Cấu hình
- **File cấu hình:** `src/main/java/com/smashvn/shop/config/GhnConfig.java`
- **Properties:** `ghn.api.token`, `ghn.api.shop-id`, `ghn.api.base-url`, `ghn.webhook.secret`

### 2.2. Các API GHN Tích Hợp
- **Service:** `GhnService.java`
- **Tính phí ship realtime:** `calculateShipFee(toDistrictId, toWardCode, insuranceValue)` ➔ Gọi `POST /shiip/public-api/v2/shipping-order/fee`.
- **Tạo vận đơn giao hàng:** `createShippingOrder(hoaDon, items, toDistrictId, toWardCode)` ➔ Gọi `POST /shiip/public-api/v2/shipping-order/create`. Nhận về `ghnOrderCode` (VD: `L3Z7P9`).
- **Tạo đơn thu hồi đổi trả:** `createReturnOrder(hoaDon, reason)` ➔ Gọi `POST /v2/shipping-order/create` với mã nhà cung cấp `GHN_RETURN`.
- **Tra cứu lộ trình bưu tá:** `trackOrder(orderCode)` ➔ Gọi `POST /v2/shipping-order/detail`.
- **Hủy vận đơn khi hủy đơn hàng:** `cancelOrder(ghnOrderCode)` ➔ Gọi `POST /shiip/public-api/v2/switch-status/cancel`.

---

## 3. TRỢ LÝ ẢO AI GOOGLE GEMINI 2.0 FLASH

### 3.1. Cấu hình
- **File cấu hình:** `src/main/java/com/smashvn/shop/config/GeminiHttpClientConfig.java`
- **Model:** `gemini-2.0-flash`
- **Giao thức:** OpenAI-compatible REST API (`https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`)

### 3.2. Kiến Trúc RAG (Retrieval-Augmented Generation)
- **Service:** `ChatbotServiceImpl.java`
- **Quy trình:**
  1. Phân tích câu hỏi người dùng qua `VietnamesePriceParser.java`.
  2. Truy vấn dữ liệu thực tế từ database qua `SanPhamChiTietRepository.searchForChatbot()`.
  3. Ghép danh sách sản phẩm còn hàng, thông số, giá bán vào System Prompt làm ngữ cảnh.
  4. Gửi request tới model Gemini 2.0 Flash và trả về câu trả lời tự nhiên kèm danh sách thẻ sản phẩm gợi ý.

---

## 4. BẢNG TỔNG HỢP 4 BACKGROUND SCHEDULERS

| Tác Vụ Ngầm | Class Khai Báo | Tần Suất / Chu Kỳ | Mục Đích & Dữ Liệu Tác Động |
| :--- | :--- | :--- | :--- |
| **Đồng Bộ Vận Đơn GHN** | `GhnPollingScheduler.java` | Mỗi 10 phút (`fixedDelay = 600000ms`) | Quét các đơn đang giao (`cho_xac_nhan`, `dang_giao`, `dang_thu_hoi`), gọi GHN Track API để tự động cập nhật sang Đang giao, Đã giao hoặc Đang kiểm tra. |
| **Quét Kiểm Duyệt Đánh Giá** | `CommentModerationScheduler.java` | Mỗi 5 phút (`fixedDelay = 300000ms`) | Quét toàn bộ đánh giá mới tạo, kiểm tra từ ngữ nhạy cảm và tự động ẩn các nội dung vi phạm. |
| **Dọn Rác Blog Đã Xóa Mềm** | `BlogService.java` | 3:00 AM mỗi ngày (`cron = "0 0 3 * * *"`) | Xóa vĩnh viễn các bài viết trong `blog` có `deleted = true` đã quá 90 ngày và xóa file ảnh liên quan. |
| **Dọn Dẹp Checkout Hết Hạn** | `PendingCheckoutRegistry.java` | Mỗi 1 phút (`fixedRate = 60000ms`) | Giải phóng các snapshot checkout online quá 30 phút trong bộ nhớ RAM để tránh rò rỉ bộ nhớ. |

---

## 5. CƠ CHẾ KHÓA & CHỐNG XUNG ĐỘT (LOCKS & CONCURRENCY)

| Entity | Method Khóa | Loại Lock | Mục Đích Bảo Vệ |
| :--- | :--- | :--- | :--- |
| `SanPham` | `SanPhamRepository.findByIdWithLock` | `PESSIMISTIC_WRITE` | Khóa sản phẩm cha theo thứ tự ID ASC khi phân bổ tồn kho FIFO trong `InventoryLotService` nhằm triệt tiêu hoàn toàn Deadlock. |
| `SanPhamChiTiet` | `SanPhamChiTietRepository.findByIdWithLock` | `PESSIMISTIC_WRITE` | Khóa độc quyền bản ghi biến thể khi kiểm tra tồn kho tại bước thêm vào giỏ hàng và tạo đơn hàng. Ngăn chặn hiện tượng mua vượt tồn kho (Overselling). |
| `PhieuGiamGia` | `PhieuGiamGiaRepository.findByMaPhieuWithLock` | `PESSIMISTIC_WRITE` | Khóa bản ghi Voucher khi trừ `soLuongConLai`. Đảm bảo voucher chỉ có thể được sử dụng đúng số lượng đã phát hành. |
| `HoaDon` | `HoaDonRepository.findByIdWithLock` | `PESSIMISTIC_WRITE` | Khóa đơn hàng khi xử lý SePay Webhook IPN, ngăn chặn 2 request Webhook xử lý trùng nhau cùng lúc. |

---
*Tài liệu Tích hợp hệ thống hoàn chỉnh của dự án SMASH-VN.*
