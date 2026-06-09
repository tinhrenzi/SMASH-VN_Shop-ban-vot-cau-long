# 📋 SMASH-VN — DANH SÁCH & TIẾN ĐỘ CÔNG VIỆC (TASKS.MD)

> **Lần cập nhật cuối:** 04/06/2026 — 10:30 (GMT+7)
> **Trạng thái hệ thống:** Đang kiểm duyệt & Lên kế hoạch khắc phục bảo mật + Phát triển tính năng.

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
| **Giai đoạn 1** | Sửa lỗi bảo mật Khẩn cấp (Critical) | 8 | 6 | 0 | 2 | 25% |
| **Giai đoạn 2** | Khắc phục bảo mật Ưu tiên cao (High) | 12 | 12 | 0 | 0 | 0% |
| **Giai đoạn 3** | Tối ưu hóa & Sửa lỗi Trung bình (Medium) | 18 | 18 | 0 | 0 | 0% |
| **Giai đoạn 4** | Phát triển tính năng mới & Hoàn thiện | 9 | 9 | 0 | 0 | 0% |
| **TỔNG CỘNG**   |                                                | **47** | **45** | **0** | **2** | **4.3%** |

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
- [ ] **T-05:** Đồng bộ logic bảo mật upload (MIME check, extension whitelist, path traversal) từ `AdminSanPhamService` sang `AdminBienTheService`.
  - *Files:* `AdminBienTheService.java`
- [ ] **T-06:** Sanitize đầu vào cho ChatService CLI chống OS Command Injection (loại bỏ `| & ^ > < ; \`).
  - *Files:* `ChatService.java`
- [ ] **T-07:** Hash mật khẩu cho tài khoản khách mua tại quầy (`GUEST_NO_PASSWORD` -> BCrypt hash).
  - *Files:* `AdminPosService.java`
- [ ] **T-08:** Thêm validation annotations cho các Entity fields tài chính (tránh số tiền âm, giảm giá > 100%).
  - *Files:* `HoaDon.java`, `HoaDonChiTiet.java`, `DotGiamGia.java`, `PhieuGiamGia.java`, v.v.

### 🟠 GIAI ĐOẠN 2: ƯU TIÊN CAO (LÀM KỸ TRƯỚC)

- [ ] **T-09:** Chuyển đổi toàn bộ 12+ endpoints xóa/sửa bằng `GET` -> `POST` hoặc `DELETE` + Thêm CSRF token vào UI.
  - *Files:* `AdminBienTheController.java`, `AdminDanhMucController.java`, `AdminKhuyenMaiController.java`, `SanPhamYeuThichController.java`, `UserAddressController.java`
- [ ] **T-10:** Chuyển endpoint đăng xuất `GET /dang-xuat` sang `POST` + Bật bảo vệ CSRF.
  - *Files:* `UserDangNhapController.java`, `SecurityConfig.java`
- [ ] **T-11:** Cấu hình thuộc tính an toàn cho session cookie (`secure=true`, `http-only=true`, `same-site=Lax`).
  - *Files:* `application.properties`
- [ ] **T-12:** Validate định dạng IP đầu vào và chặn dải IP Private/Loopback trước khi gọi external API (Chống SSRF).
  - *Files:* `LocationService.java`
- [ ] **T-13:** Thêm kiểm tra độ mạnh mật khẩu mới khi người dùng thực hiện reset mật khẩu.
  - *Files:* `UserQuenMatKhauService.java`
- [ ] **T-14:** Chuyển đổi các quan hệ `@ManyToOne` và `@OneToOne` mặc định sang `FetchType.LAZY` để tránh truy vấn thừa thông tin nhạy cảm.
  - *Files:* Toàn bộ các Entity files trong `entity/`
- [ ] **T-15:** Chuẩn hóa thông báo lỗi chuyển hướng (Redirect Error handling) - Không hiển thị raw exception message ra URL.
  - *Files:* `AdminDanhMucController.java`, `AdminKhuyenMaiController.java`, v.v.
- [ ] **T-16:** Thêm Rate Limiting cho luồng Đăng ký tài khoản và Quên mật khẩu.
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
- [ ] **T-24:** Xử lý rò rỉ danh tính người dùng (User enumeration) tại trang đăng ký/khôi phục mật khẩu.
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
- [ ] **T-44:** Đồng bộ hóa cơ chế upload path động (sử dụng `app.upload.path` thay vì hardcode `Paths.get("uploads/product/")`).
  - *Files:* `AdminBienTheService.java`
- [ ] **T-45:** Tái cấu trúc Rate Limiter sử dụng Caffeine Cache có sẵn để tự động giải phóng bộ nhớ (eviction) và tránh memory leak.
  - *Files:* `LocationRestController.java`, `LoginRateLimiter.java`
- [ ] **T-46:** Tách logic bảo mật upload (MIME check, extension whitelist) thành class tiện ích dùng chung `FileStorageService`.
  - *Files:* `AdminSanPhamService.java`, `AdminBienTheService.java`
- [ ] **T-47:** Dọn dẹp các import trùng lặp trong file Interceptor.
  - *Files:* `AdminInterceptor.java`

### 🟢 GIAI ĐOẠN 4: PHÁT TRIỂN CHỨC NĂNG MỚI (LÀM SAU CÙNG)

- [ ] **T-33:** **Quản lý Đơn hàng online:** Thao tác xem, duyệt, xác nhận đơn hàng do khách đặt từ web.
- [ ] **T-34:** **Bình luận & Đánh giá:** Khách hàng đánh giá số sao, đính kèm ảnh thực tế.
- [ ] **T-35:** **Kiểm duyệt bình luận:** Bộ lọc tự động phát hiện và chuyển từ tục tĩu thành `***`.
- [ ] **T-36:** **Quản lý Bảo hành theo IMEI:** Lưu serial/IMEI sản phẩm, kiểm tra hạn bảo hành.
- [ ] **T-37:** **Quản lý Hoàn trả:** Xử lý yêu cầu hoàn tiền/đổi trả sản phẩm lỗi trong vòng 7 ngày.
- [ ] **T-38:** **Quản lý Tin tức / Blog:** Soạn thảo, đăng tải các bài review, hướng dẫn chọn vợt.
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
| **04/06/2026 11:05** | —                   | **Cập nhật kiến trúc** | Cập nhật tasks.md: bổ sung T-42 đến T-47 dựa trên kết quả phân tích cấu trúc, mã nguồn dư thừa và bất cập giữa Spring Security, Interceptor và Cấu hình upload. | Antigravity AI      |

---

### 💡 Hướng dẫn cập nhật trạng thái:

1. Khi bắt đầu làm Task `T-XX`, đổi trạng thái từ `[ ]` sang `[/]` và cập nhật bảng **Tổng quan tiến độ**.
2. Khi hoàn thành, đổi thành `[x]`, ghi nhận ngày giờ hoàn thành và lưu thông tin chi tiết vào **Nhật ký cập nhật**.
3. Luôn bảo toàn định dạng và liên kết file trong suốt quá trình phát triển.
