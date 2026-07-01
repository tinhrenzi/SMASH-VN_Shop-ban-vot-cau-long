# 📋 SMASH-VN — DANH SÁCH & TIẾN ĐỘ CÔNG VIỆC (TASKS.MD)

> **Lần cập nhật cuối:** 30/06/2026 — 10:46 (GMT+7)
> **Trạng thái hệ thống:** Đang triển khai bảo mật Giai đoạn 1 & 2. Hoàn thành tối ưu hóa luồng Thanh toán, Phí Vận chuyển GHN, bảo mật địa chỉ giao hàng và luồng điều hướng thêm mới địa chỉ.

Tài liệu này dùng để theo dõi toàn bộ chức năng của dự án **SMASH-VN (Website bán vợt cầu lông)**. Được chia làm hai phần chính:

1. **Kiểm duyệt thứ tự ưu tiên** (Chức năng cần làm kỹ trước vs. Chức năng làm sau).
2. **Danh sách Tasks chi tiết** & **Nhật ký chỉnh sửa (Changelog)** cập nhật theo ngày/giờ.

---

## 🎯 1. KIỂM DUYỆT THỨ TỰ ƯU TIÊN (PRIORITIZATION)

Để đảm bảo hệ thống vận hành ổn định và an toàn, thứ tự triển khai được chia làm 2 nhóm rõ rệt:

### 🔥 NHÓM 1: LÀM KỸ TRƯỚC (Bảo mật & Củng cố chức năng đã có)

> [!IMPORTANT]
> Đây là các chức năng cốt lõi đã có code nền tảng nhưng chứa nhiều lỗ hổng nghiêm trọng (Critical/High) hoặc lỗi logic. Cần tập trung sửa chữa và tối ưu hóa thật kỹ trước khi code tính năng mới.

1. **Khắc phục Bảo mật Hệ thống (Hàng đầu - Khẩn cấp):**
   - **Tài khoản & Xác thực:** Mã hóa toàn bộ mật khẩu (kể cả Guest), sửa lỗi lộ mật khẩu qua JSON Entity serialization (`@JsonIgnore`), thêm validation sức mạnh mật khẩu khi reset.
   - **Bảo mật File Upload:** Áp dụng logic kiểm tra MIME type, extension whitelist và chống path traversal cho `AdminBienTheService` (giống như `AdminSanPhamService` đã có).
   - **Chống OS Command Injection:** Sanitize tuyệt đối dữ liệu đầu vào của AI Chatbox khi chạy binary qua CLI.
   - **Bảo vệ luồng thao tác:** Đổi các request xóa/sửa từ `GET` sang `POST/DELETE` để kích hoạt CSRF protection.
2. **Hoàn thiện Logic Nghiệp vụ hiện tại:**
   - **Tạo đơn hàng tại quầy (POS):** Sửa lỗi làm tròn số của `BigDecimal.divide()`, ngăn chặn race condition khi check số lượng tồn kho.
   - **Khuyến mãi (Voucher & Đợt giảm giá):** Thêm ràng buộc `@Min`, `@Max`, `@Positive` trên Entity để tránh việc nhập số âm hoặc giảm giá >100%.

### ⚡ NHÓM 2: LÀM SAU (Phát triển tính năng mới)

> [!NOTE]
> Các tính năng chưa được xây dựng theo tài liệu đặc tả dự án. Sẽ thực hiện cuốn chiếu sau khi Nhóm 1 đã hoàn thành và kiểm thử bảo mật thành công.

1. **Quản lý Đơn hàng online (Nhân viên):** Phê duyệt, cập nhật trạng thái đơn hàng từ khách hàng đặt trực tuyến (Rất quan trọng nhưng cần làm sau khi POS ổn định).
2. **Đánh giá & Bình luận (Khách hàng) & Kiểm duyệt (Admin):** Khách hàng đánh giá sản phẩm; Hệ thống tự động lọc các từ tục tĩu thay thế bằng `***`.
3. **Quản lý Bảo hành theo IMEI:** Lưu trữ IMEI, kích hoạt bảo hành, tra cứu lịch sử sửa chữa theo thiết bị.
4. **Quản lý Hoàn trả (RMA):** Tiếp nhận và xử lý yêu cầu đổi trả hàng trong 7 ngày.
5. **Tin tức / Blog & Nội dung:** Soạn bài viết blog, quản lý banner trang chủ.
6. **So sánh sản phẩm & Đa ngôn ngữ:** So sánh 2-3 vợt cầu lông, tích hợp nút dịch nhanh ngôn ngữ.

---

## 📊 2. TỔNG QUAN TIẾN ĐỘ THỰC HIỆN

| Giai đoạn             | Nội dung công việc                          | Tổng Tasks |  Chưa làm  | Đang làm | Hoàn thành |   Tiến độ   |
| :---------------------- | :--------------------------------------------- | :----------: | :----------: | :---------: | :----------: | :------------: |
| **Giai đoạn 1** | Sửa lỗi bảo mật Khẩn cấp (Critical) | 8 | 2 | 0 | 6 | 75% |
| **Giai đoạn 2** | Khắc phục bảo mật Ưu tiên cao (High) | 12 | 9 | 0 | 3 | 25% |
| **Giai đoạn 3** | Tối ưu hóa & Sửa lỗi Trung bình (Medium) | 18 | 14 | 0 | 4 | 22.2% |
| **Giai đoạn 4** | Phát triển tính năng mới & Hoàn thiện | 9 | 4 | 0 | 5 | 55.6% |
| **TỔNG CỘNG**   |                                                | **47** | **29** | **0** | **18** | **38.3%** |

---

## 📝 3. CHI TIẾT DANH SÁCH TASKS

### 🔴 GIAI ĐOẠN 1: KHẨN CẤP (LÀM KỸ TRƯỚC)

- [X] **T-01:** Thêm `.env` vào `.gitignore` + Xóa khỏi Git tracking.
  - *Files:* `.gitignore` (Hoàn thành: 04/06/2026 10:35)
- [X] **T-02:** Rotate toàn bộ secrets (DB `sa`, Gmail App Password, Google OAuth Client Secret).
  - *Files:* `.env`
- [X] **T-03:** Thay credentials thật trong `.env.example` bằng placeholder.
  - *Files:* `.env.example` (Hoàn thành: 04/06/2026 10:35)
- [ ] **T-04:** Thêm `@JsonIgnore` cho các trường bảo mật (`matKhau`, `tokenXacThucKhoa`, `maXacNhan`).
  - *Files:* `TaiKhoan.java`, `TokenKhoiPhuc.java`
- [X] **T-05:** Đồng bộ logic bảo mật upload (MIME check, extension whitelist, path traversal) từ `AdminSanPhamService` sang `AdminBienTheService`.
  - *Files:* `AdminBienTheService.java`, `AdminBienTheController.java`, `bienthe-list.html`, `bienthe-edit.html` (Hoàn thành: 12/06/2026 09:27)
- [X] **T-06:** Sanitize đầu vào cho ChatService CLI chống OS Command Injection (loại bỏ `| & ^ > < ; \`).
  - *Files:* `ChatService.java` (Hoàn thành: 23/06/2026 - Đã thay thế CLI hermes bằng tích hợp API Gemini qua HTTP client)
- [ ] **T-07:** Hash mật khẩu cho tài khoản khách mua tại quầy (`GUEST_NO_PASSWORD` -> BCrypt hash).
  - *Files:* `AdminPosService.java`
- [X] **T-08:** Bảo mật và Tối ưu hóa luồng Thanh toán — Chống giả mạo phí vận chuyển, IDOR địa chỉ, Tạo đơn GHN sau IPN.
  - *Files:* `application.properties`, `ShippingZoneResolver.java`, `ShippingFeeCalculator.java`, `GhnService.java`, `GioHangService.java`, `CheckoutController.java`, `ShippingApiController.java`, `SepayGatewayService.java`, `checkout.html`, `CheckoutValidationIntegrationTest.java` (Hoàn thành: 12/06/2026 16:41)

### 🟠 GIAI ĐOẠN 2: ƯU TIÊN CAO (LÀM KỸ TRƯỚC)

- [ ] **T-09:** Chuyển đổi toàn bộ 12+ endpoints xóa/sửa bằng `GET` -> `POST` hoặc `DELETE` + Thêm CSRF token vào UI.
  - *Files:* `AdminBienTheController.java`, `AdminDanhMucController.java`, `AdminKhuyenMaiController.java`, `SanPhamYeuThichController.java`, `UserAddressController.java`
- [ ] **T-10:** Chuyển endpoint đăng xuất `GET /dang-xuat` sang `POST` + Bật bảo vệ CSRF.
  - *Files:* `UserDangNhapController.java`, `SecurityConfig.java`
- [X] **T-11:** Cấu hình thuộc tính an toàn cho session cookie (`secure=true`, `http-only=true`, `same-site=Lax`).
  - *Files:* `application.properties` (Hoàn thành: 15/06/2026)
- [ ] **T-12:** Validate định dạng IP đầu vào và chặn dải IP Private/Loopback trước khi gọi external API (Chống SSRF).
  - *Files:* `LocationService.java`
- [X] **T-13:** Thêm kiểm tra độ mạnh mật khẩu mới khi người dùng thực hiện reset mật khẩu.
  - *Files:* `UserQuenMatKhauService.java`
- [ ] **T-14:** Chuyển đổi các quan hệ `@ManyToOne` và `@OneToOne` mặc định sang `FetchType.LAZY` để tránh truy vấn thừa thông tin nhạy cảm.
  - *Files:* Toàn bộ các Entity files trong `entity/`
- [ ] **T-15:** Chuẩn hóa thông báo lỗi chuyển hướng (Redirect Error handling) - Không hiển thị raw exception message ra URL.
  - *Files:* `AdminDanhMucController.java`, `AdminKhuyenMaiController.java`, v.v.
- [X] **T-16:** Thêm Rate Limiting cho luồng Đăng ký tài khoản và Quên mật khẩu.
  - *Files:* `UserQuenMatKhauService.java`, `UserDangKyController.java`
- [ ] **T-17:** Bổ sung Validation đầu vào cho các Controllers còn thiếu (giá bán, số lượng, định dạng email, sđt).
  - *Files:* Các file controller liên quan.
- [ ] **T-18:** Khai báo tường minh `spring-boot-starter-security` trong file quản lý dependency.
  - *Files:* `pom.xml`
- [ ] **T-19:** Thiết lập unique constraints trong Entity cho số điện thoại khách hàng, tên danh mục, tên thương hiệu.
  - *Files:* `KhachHang.java`, `DanhMuc.java`, `ThuongHieu.java`
- [ ] **T-20:** Giới hạn dung lượng tối đa khi upload file (max-file-size 5MB, max-request-size 20).
  - *Files:* `application.properties`

### 🟡 GIAI ĐOẠN 3: TỐI ƯU HÓA (LÀM SAU KHI XONG GĐ1 & GĐ2)

- [ ] **T-21:** Chuyển đổi các trạng thái dạng Chuỗi (String status) sang Enum của Java để tránh sai sót dữ liệu.
- [ ] **T-22:** Bổ sung Content Security Policy (CSP) và HSTS headers toàn cục.
- [ ] **T-23:** Khắc phục lỗi Race Condition khi cập nhật giỏ hàng bằng cách sử dụng `pessimistic lock` kiểm tra tồn kho.
- [X] **T-24:** Xử lý rò rỉ danh tính người dùng (User enumeration) tại trang đăng ký/khôi phục mật khẩu.
  - *Files:* Các file controller liên quan.
- [ ] **T-25:** Khắc phục bypass rate limit bằng cách giả mạo IP qua header `X-Forwarded-For`.
- [ ] **T-26:** Tắt chế độ hiển thị SQL (`show-sql=false`) trên môi trường Production.
- [ ] **T-27:** Thêm `@JsonIgnore` cho các mối quan hệ hai chiều để tránh lặp vô hạn khi serialize.
- [ ] **T-28:** Chỉ định `RoundingMode.HALF_UP` khi chia BigDecimal trong tính toán tiền POS.
- [ ] **T-29:** Sửa rò rỉ bộ nhớ (Memory leak) trong cơ chế lưu vết IP của Rate Limiter.
- [ ] **T-30:** Mã hóa (Hash SHA-256) các Token khôi phục mật khẩu trước khi lưu trữ xuống Database.
- [ ] **T-31:** Thêm cấu hình `@Transactional(readOnly=true)` cho các phương thức chỉ đọc dữ liệu.
- [ ] **T-32:** Tự động tạo mốc thời gian bằng `@PrePersist` thay vì gán thủ công `LocalDateTime.now()`.
- [ ] **T-42:** Loại bỏ `AdminInterceptor` và tích hợp kiểm tra trạng thái hoạt động tài khoản vào Spring Security (sử dụng UserDetailsService chuẩn).
  - *Files:* `AdminInterceptor.java`, `SecurityConfig.java`
- [ ] **T-43:** Loại bỏ thư viện `jbcrypt` khỏi `pom.xml`, đồng bộ hóa việc băm mật khẩu bằng `BCryptPasswordEncoder` của Spring Security.
  - *Files:* `pom.xml`, `PlaintextPasswordMigrator.java`, v.v.
- [X] **T-44:** Đồng bộ hóa cơ chế upload path động (sử dụng `app.upload.path` thay vì hardcode `Paths.get("uploads/product/")`).
  - *Files:* `AdminBienTheService.java` (Hoàn thành: 12/06/2026)
- [ ] **T-45:** Tái cấu trúc Rate Limiter sử dụng Caffeine Cache có sẵn để tự động giải phóng bộ nhớ (eviction) và tránh memory leak.
  - *Files:* `LocationRestController.java`, `LoginRateLimiter.java`
- [X] **T-46:** Tách logic bảo mật upload (MIME check, extension whitelist) thành class tiện ích dùng chung `FileStorageService`.
  - *Files:* `FileStorageService.java` (Hoàn thành: 15/06/2026 - Được tích hợp vào BlogService và DanhGiaService)
- [X] **T-47:** Dọn dẹp các import trùng lặp trong file Interceptor.
  - *Files:* `AdminInterceptor.java` (Hoàn thành: 15/06/2026)

### 🟢 GIAI ĐOẠN 4: PHÁT TRIỂN CHỨC NĂNG MỚI (LÀM SAU CÙNG)

- [X] **T-33:** **Quản lý Đơn hàng online:** Thao tác xem, duyệt, xác nhận đơn hàng do khách đặt từ web.
  - *Files:* `AdminController.java`, `donhang-list.html` (Hoàn thành: 21/06/2026)
- [X] **T-34:** **Bình luận & Đánh giá:** Khách hàng đánh giá số sao, đính kèm ảnh thực tế.
  - *Files:* `DanhGiaService.java`, `AdminDanhGiaController.java`, `product-detail.html` (Hoàn thành: 16/06/2026)
- [X] **T-35:** **Kiểm duyệt bình luận:** Bộ lọc tự động phát hiện và chuyển từ tục tĩu thành `***`.
  - *Files:* `ProfanityFilter.java`, `CommentModerationAdminController.java` (Hoàn thành: 17/06/2026)
- [ ] **T-36:** **Quản lý Bảo hành theo IMEI:** Lưu serial/IMEI sản phẩm, kiểm tra hạn bảo hành.
- [X] **T-37:** **Quản lý Hoàn trả:** Xử lý yêu cầu hoàn tiền/đổi trả sản phẩm lỗi trong vòng 7 ngày.
  - *Files:* `AdminController.java` (Hoàn thành: 21/06/2026)
- [X] **T-38:** **Quản lý Tin tức / Blog:** Soạn thảo, đăng tải các bài review, hướng dẫn chọn vợt.
  - *Files:* `BlogService.java`, `BlogController.java`, `AdminBlogController.java` (Hoàn thành: 15/06/2026)
- [ ] **T-39:** **So sánh sản phẩm:** Giao diện so sánh thông số của 2-3 vợt cầu lông.
- [ ] **T-40:** **Đa ngôn ngữ:** Tích hợp bộ chuyển dịch ngôn ngữ giao diện (Google Translate widget).
- [ ] **T-41:** **Liên hệ:** Form gửi phản hồi từ khách hàng đến hệ thống quản trị.

---

## 🔍 5. KIỂM DUYỆT KIẾN TRÚC & MÃ DƯ THỪA (REDUNDANCY AUDIT)

Qua quá trình phân tích mã nguồn thực tế, chúng tôi đã phát hiện một số điểm bất cập về mặt kiến trúc dẫn đến mã nguồn bị dư thừa hoặc xung đột:

1. **Xung đột Kiểm tra Đăng nhập:** `AdminInterceptor` thủ công kiểm tra Session đè lên cơ chế lọc chuẩn của `Spring Security`, gây dư thừa bộ lọc và dễ lỗi desync session.
2. **Trùng lặp Thư viện Mã hóa:** Dùng song song cả `jbcrypt` (thư viện ngoài) và `BCryptPasswordEncoder` có sẵn của Spring Security.
3. **Không đồng bộ đường dẫn Upload:** `AdminBienTheService` đang viết cứng (hardcode) đường dẫn `"uploads/product/"` trong khi hệ thống đã có cấu hình động `${app.upload.path}` và bộ kiểm duyệt `UploadPathVerifier`.
4. **Rate Limiter bộ nhớ tay:** Tự viết Map (`ConcurrentHashMap`) quản lý IP và lượt thử đăng nhập, dễ gây rò rỉ bộ nhớ (Memory Leak), thay vì dùng thư viện cache chuyên dụng Caffeine đã khai báo trong `pom.xml`.
5. **Lặp code validate file:** Cả hai service sản phẩm và biến thể đều tự viết lại cơ chế validate MIME và chống Path Traversal thay vì dùng chung một lớp tiện ích `FileStorageService`.

---

## 🕒 4. NHẬT KÝ CẬP NHẬT (CHANGELOG)

Mỗi khi có thay đổi (bắt đầu làm, sửa đổi code, hoàn thành task), cập nhật chính xác ngày, giờ và chi tiết file sửa đổi vào bảng dưới đây.

| Ngày giờ (GMT+7)         | Mã Task             | Hành động                    | Files ảnh hưởng / Chi tiết sửa đổi                                                                                                    | Người thực hiện |
| :------------------------- | :------------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------- | :------------------ |
| **04/06/2026 10:30** | —                   | **Khởi tạo tài liệu** | Tạo mới file `tasks.md` tích hợp thứ tự ưu tiên chức năng và danh sách bảo mật.                                              | Antigravity AI      |
| **04/06/2026 10:35** | **T-01, T-03** | **Đã hoàn thành**     | Thêm `.env` vào `.gitignore` (verify an toàn, không bị tracked) và thay thế credentials trong `.env.example` bằng placeholder. | Antigravity AI      |
| **12/06/2026 09:27** | **T-05**             | **Đã hoàn thành**     | Đồng bộ logic bảo mật upload và củng cố validate cho Biến thể Sản phẩm sử dụng Tika, ImageIO, UUID, startsWith chống path traversal và Flash Attributes. | Antigravity AI      |
| **04/06/2026 11:05** | —                   | **Cập nhật kiến trúc** | Cập nhật tasks.md: bổ sung T-42 đến T-47 dựa trên kết quả phân tích cấu trúc, mã nguồn dư thừa và bất cập giữa Spring Security, Interceptor và Cấu hình upload. | Antigravity AI      |
| **12/06/2026 16:41** | **T-08**             | **Đã hoàn thành**     | Tối ưu hóa luồng Thanh toán & Bảo mật Phí Vận chuyển GHN: (1) Cấu hình Province ID từ `application.properties`; (2) `ShippingZoneResolver` dùng districtId cache chống giả mạo vùng; (3) `GhnService.resolveGhnAddress()` resolve server-side; (4) `GioHangService.createOrder()` reload địa chỉ trong transaction, validate IDOR, tính phí độc lập; (5) `SepayGatewayService.handleIpn()` tạo đơn GHN sau SePay IPN; (6) UX: manual-address-fields ẩn mặc định, debounce 300ms, isGhnLoadingMasterData guard; (7) 5 integration test mới: fee tampering, IDOR address, deleted address, recalculation, GHN mapping missing. | Antigravity AI      |
| **12/06/2026 16:47** | —                   | **Sửa lỗi UX**         | Phân luồng redirect sau khi thêm địa chỉ: nếu vào từ `/checkout` (param `from=checkout`) thì redirect về `/checkout` sau khi lưu thành công; vào từ trang cá nhân thì redirect về `/user/address`. Files: `UserAddressController.java` (thêm `@RequestParam from`), `checkout.html` (thêm `?from=checkout`), `dash-address-add.html` (cập nhật form action giữ param). | Antigravity AI      |
| **12/06/2026 16:55** | —                   | **Sửa lỗi URL cứng**   | Thay thế toàn bộ URL ngrok hardcode trong `checkout.html` bằng Thymeleaf `th:href="@{/user/address/add(from='checkout')}"`. Thymeleaf tự build URL từ domain hiện tại — tự động đúng trên local (`localhost:8080`) lẫn ngrok, không cần đổi code khi chuyển môi trường. | Antigravity AI      |
| **25/06/2026 09:00** | **T-06, T-11, T-33, T-34, T-35, T-37, T-38, T-44, T-46, T-47** | **Đã hoàn thành** | Cập nhật tiến độ sau các commit phát triển: (1) Chatbot dùng Gemini API (T-06); (2) Cookie Security (T-11); (3) Order Online & Refund (T-33, T-37); (4) Đánh giá & Profanity Filter (T-34, T-35); (5) Blog (T-38); (6) Upload Path động & FileStorageService (T-44, T-46); (7) Xóa import trùng lặp (T-47). | Antigravity AI      |
| **30/06/2026 10:46** | **T-08**             | **Đã hoàn thành**     | Tối ưu & Vá lỗi luồng Thanh toán/Địa chỉ: (1) Sửa lỗi mapping tên người nhận trên GHN; (2) Tự động chuẩn hóa địa chỉ lưu sẵn thiếu ID GHN (Auto-Heal); (3) Ghép Phường/Xã vào chuỗi địa chỉ đầy đủ không trùng lặp; (4) Đồng bộ hiển thị sổ địa chỉ & dashboard; (5) Thêm integration test `testSubmitCheckout_SaveAndReuseAddress` thành công. | Antigravity AI      |
| **30/06/2026 14:48** | — (UX Polish)        | **Đã hoàn thành**     | Sửa lỗi giao diện bảng Đơn hàng (`donhang-list.html`): (1) Đổi các nút hành động thành icon vuông nhỏ gọn và thêm tooltip hiển thị khi hover; (2) Cho phép text khách hàng và các badge trạng thái tự động wrap xuống dòng; (3) Thu hẹp độ rộng cột thao tác và giảm min-width của bảng từ 1175px xuống 990px; (4) Thêm cấu hình `spring.thymeleaf.cache=false` vào `application.properties`. | Antigravity AI      |
| **30/06/2026 15:05** | — (UX Polish)        | **Đã hoàn thành**     | Sửa lỗi giao diện chi tiết sản phẩm trong đơn hàng của khách (`dash-manage-order.html`): (1) Làm phẳng và chồng dọc danh sách sản phẩm thay vì hiển thị dạng cột bị chèn ép; (2) Khắc phục lỗi co cụm làm biến mất ảnh đại diện sản phẩm bằng cách thêm `flex-shrink: 0` và chuẩn hóa kích thước; (3) Sắp xếp thông tin chi tiết đơn giá, số lượng, thành tiền dưới dạng các cột nhãn rõ ràng; (4) Đảm bảo độ tương thích responsive trên di động và máy tính bảng. | Antigravity AI      |
| **30/06/2026 15:15** | — (Bug Fix)          | **Đã hoàn thành**     | Sửa lỗi mất icon của phương thức COD tại trang phương thức thanh toán (`dash-payment-option.html`): Đổi class icon FontAwesome từ `fa-hand-holding-dollar` (mismatch v6) thành `fa-hand-holding-usd` (chuẩn v5). | Antigravity AI      |
| **30/06/2026 15:40** | — (Feature & Polish) | **Đã hoàn thành**     | Thêm hiển thị mã vận đơn GHN và loại bỏ Giao Hàng Tiết Kiệm (GHTK): (1) Đưa `ghnOrderCode` vào map thuộc tính `order` ở `OrderViewService.java`; (2) Hiển thị mã vận đơn GHN dưới dạng mã vạch trực quan tại `dash-manage-order.html` khi có giá trị; (3) Loại bỏ GHTK khỏi danh sách vận chuyển hiển thị ở trang checkout (`CheckoutController.java`) và cấu hình admin (`AdminShippingService.java`). | Antigravity AI      |
| **30/06/2026 16:05** | — (Feature & Security)| **Đã hoàn thành**     | Gửi mã đơn hàng qua email sau khi mua hàng và chuẩn hóa chức năng theo dõi đơn hàng: (1) Thêm phương thức gửi email xác nhận đặt hàng HTML đẹp mắt `sendOrderConfirmationEmail` chạy bất đồng bộ ở `GuestCheckoutService.java`; (2) Gọi gửi email này trong luồng xử lý checkout của `CheckoutController.java` cho cả thành viên và khách; (3) Cập nhật `hienThiTrackOrder` và `submitTrackOrder` tại `UserDashboardController.java` cho phép khách vãng lai và thành viên theo dõi đơn hàng bằng mã đơn hàng (văn bản) hoặc ID (số) một cách an toàn thông qua xác thực email/SĐT; (4) Cập nhật giao diện `dash-track-order.html` động để hiển thị form phù hợp theo trạng thái đăng nhập. | Antigravity AI      |
| **30/06/2026 16:18** | — (Feature)          | **Đã hoàn thành**     | Tự động tạo thông báo cho khách hàng khi có cập nhật trạng thái đơn hàng: (1) Khai báo và tiêm `ThongBaoRepository` vào `OrderViewService.java`; (2) Tự động thêm bản ghi `ThongBao` vào CSDL cho tài khoản khách hàng khi admin/nhân viên thủ công cập nhật trạng thái đơn hàng (`updateOrderStatusByAdmin`), chuyển trạng thái hoàn trả hàng (`updateReturnStatusByAdmin`), hoặc tự động cập nhật qua webhook vận chuyển GHN (`updateGhnStatus`). | Antigravity AI      |
| **30/06/2026 16:26** | — (UX Polish)        | **Đã hoàn thành**     | Thêm chỉ báo chấm đỏ thông báo chưa đọc ở header: (1) Tạo `@ControllerAdvice` toàn cục `GlobalNotificationAdvice.java` để tự động đếm và cung cấp thuộc tính `unreadNotificationCount` cho tất cả các view của khách; (2) Cập nhật `header.html` hiển thị chấm đỏ tuyệt đẹp ở góc phải của icon người dùng trên header chính, và góc phải của icon cái chuông cạnh mục "Thông báo" trong menu dropdown khi có thông báo chưa đọc. | Antigravity AI      |
| **01/07/2026 10:15** | — (UX Polish)        | **Đã hoàn thành**     | Tinh chỉnh và cân đối bảng Tóm tắt đơn hàng ở trang Checkout (`checkout.html`): (1) Thiết lập độ rộng cột cố định (75% cho Sản phẩm, 25% cho Tạm tính) và căn lề rõ ràng (`text-align: left/right`) khắc phục khoảng trống lệch; (2) Thêm ảnh đại diện sản phẩm (thumbnail kích thước 50x50px) bo góc với hiệu ứng viền nhẹ cạnh tên sản phẩm giúp giao diện tóm tắt trực quan và chuyên nghiệp. | Antigravity AI      |
| **01/07/2026 15:35** | — (UX Redesign)      | **Đã hoàn thành**     | Tái thiết kế giao diện quản trị (QL & NV) sang dạng Top Navigation dựa theo wireframe: (1) Ẩn sidebar cũ (`sidebar.html`) và thiết lập layout cột chính `.main-content` chiếm trọn 100% chiều rộng được đóng gói trong một container tối đa `1200px` căn giữa màn hình tránh bị tràn; (2) Redesign lại Header (`header.html`) gồm Logo bên trái, thanh tìm kiếm sản phẩm chính giữa có tích hợp JS lọc động, và cụm tài khoản/nút trở về trang chủ bên phải; (3) Tạo dòng menu điều hướng động theo vai trò (QL: Tổng quan, Nhân viên, Khách hàng, Thống kê cùng nút "M" chứa dropdown đa cấp quản lý nâng cao; NV: Bán tại quầy, Khách hàng, Đơn hàng, Đánh giá, Bình luận, Bài viết); (4) Tích hợp tự động footer chứa tên shop và thông số Metadata CSDL thực tế (`dbName`, `dbVersion` qua `GlobalNotificationAdvice.java`) chuyển dịch thông minh xuống cuối cùng nội dung trang; (5) Thêm nút cuộn lên đầu nhanh (Scroll to Top) góc dưới bên phải. | Antigravity AI      |
| **01/07/2026 16:15** | — (UX Redesign)      | **Đã hoàn thành**     | Căn giữa thanh điều hướng, đưa ngôi nhà xuống hàng dưới và mở rộng nền Header/Footer tràn màn hình: (1) Xử lý dọn dẹp Hotline/Email và chuyển biểu tượng Ngôi nhà (`fas fa-home`) xuống góc phải của dòng điều hướng dưới; (2) Tái lập cấu trúc dòng dưới `header-nav-row` thành bố cục flex 3 cột (`.header-nav-left/center/right`) giúp căn giữa tuyệt đối danh sách tab điều hướng và giữ nút M vuông (`34x34px`) bên trái; (3) Loại bỏ caret mũi tên dropdown của biểu tượng Tài khoản và nút M để đồng bộ hoàn toàn với MEGA Menu khách hàng; (4) Giải phóng giới hạn `1200px` của `.main-content` (cho phép padding: 0 và width: 100%) giúp nền trắng của Header và Footer kéo dài tràn mép màn hình, đồng thời tự động căn giữa toàn bộ các phần tử nội dung con khác bằng bộ chọn CSS chọn lọc. | Antigravity AI      |
| **01/07/2026 16:30** | — (Feature)          | **Đã hoàn thành**     | Thêm phân trang cho cột trái trang Bán tại quầy (POS) đồng bộ phong cách khách hàng: (1) Khai báo các thuộc tính `shop-p__pagination` và định dạng hover, active trong `admin-custom.css` khớp hoàn toàn với giao diện mua sắm; (2) Bổ sung thanh phân trang `#posPagination` chứa bộ chọn kích thước trang "Hiển thị: 12/24/36/48", danh sách nút số trang và mô tả trang thực tế; (3) Viết logic phân trang Javascript bằng lát cắt `slice(start, end)` trên mảng kết quả `allVariants` phản hồi tức thì mà không cần tải lại trang. | Antigravity AI      |
| **01/07/2026 16:55** | — (UX Redesign)      | **Đã hoàn thành**     | Đồng bộ FontAwesome v5 và loại bỏ nền khi hover thanh tiêu đề: (1) Downgrade FontAwesome CDN từ v6.4.0 về v5.15.4 trên toàn bộ 29 trang giao diện quản trị Admin để thống nhất hình dáng biểu tượng ngôi nhà và trọng lượng nét vẽ với trang khách hàng; (2) Cập nhật CSS của `.nav-tab-item` trong `admin-custom.css` để loại bỏ hoàn toàn dải nền xám nhạt (`rgba(255, 69, 0, 0.05)`) khi hover/active, chỉ thay đổi màu chữ sang sắc cam đỏ thương hiệu `#ff4500` và giữ chữ đậm (`font-weight: 700`) trên nền trong suốt. | Antigravity AI      |

---

### 💡 Hướng dẫn cập nhật trạng thái:

1. Khi bắt đầu làm Task `T-XX`, đổi trạng thái từ `[ ]` sang `[/]` và cập nhật bảng **Tổng quan tiến độ**.
2. Khi hoàn thành, đổi thành `[x]`, ghi nhận ngày giờ hoàn thành và lưu thông tin chi tiết vào **Nhật ký cập nhật**.
3. Luôn bảo toàn định dạng và liên kết file trong suốt quá trình phát triển.
