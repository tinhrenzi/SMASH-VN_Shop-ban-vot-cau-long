# 01. TỔNG QUAN HỆ THỐNG SMASH-VN

## 1. Mục Đích Hệ Thống
**SMASH-VN** là nền tảng thương mại điện tử chuyên nghiệp và giải pháp bán hàng đa kênh (Omnichannel) phục vụ kinh doanh vợt cầu lông, giày dép, trang phục và phụ kiện thể thao cầu lông. Hệ thống kết hợp đồng bộ chặt chẽ giữa:
- **Cửa hàng trực tuyến (E-Commerce Web):** Trải nghiệm mua sắm mượt mà, tra cứu và lọc sản phẩm đa tiêu chí (Criteria Specification), giỏ hàng kép (Thành viên lưu DB & Khách vãng lai lưu Session), tokenized checkout context, thanh toán SePay QR Vietcombank tự động xác thực và theo dõi đơn hàng thời gian thực qua GHN.
- **Bán hàng tại quầy (POS - Point of Sale):** Giao diện quét mã vạch SKU/Barcode tốc độ cao cho thu ngân, hỗ trợ đa tab hóa đơn chờ (tối đa 10 hóa đơn song song), tìm kiếm hoặc tạo nhanh hồ sơ khách hàng, thanh toán tiền mặt/SePay QR và in hóa đơn nhiệt K80 chuẩn thương mại.
- **Quản trị Chuỗi cung ứng & Kho hàng FIFO:** Quản lý nhập hàng theo phiếu nhập và lô nhập (`PhieuNhap`, `PhieuNhapChiTiet`), phân bổ xuất kho theo nguyên tắc FIFO (Nhập trước xuất trước), quản lý tách biệt Kho Hàng Lỗi (`soLuongSpLoi` trong `SanPhamChiTiet`) và xử lý xuất trả NCC / thanh lý / tiêu hủy.
- **Quy trình Đổi trả hàng (RMA & Refunds):** Tiếp nhận yêu cầu đổi trả trong vòng 7 ngày hỗ trợ đính kèm bằng chứng ảnh và video (tối đa 50MB), tự động sinh vận đơn thu hồi tận nhà `GHN_RETURN`, kiểm kho phân loại hàng tốt (hoàn kho bán) vs hàng lỗi (chuyển kho lỗi), rẽ nhánh đổi mới (`GHN_EXCHANGE`) hoặc hoàn tiền.
- **Trợ lý Ảo AI Chatbot:** Tích hợp mô hình ngôn ngữ Google Gemini 2.0 Flash với kỹ thuật RAG (Retrieval-Augmented Generation) truy xuất trực tiếp dữ liệu tồn kho, giá bán và thông số kỹ thuật thực tế từ database để tư vấn lối chơi và sản phẩm phù hợp.

---

## 2. Ngăn Xếp Công Nghệ Sử Dụng (Technology Stack)

```
[Frontend Client Tier]        Thymeleaf Server-Side Rendering (SSR) + HTML5 + CSS3 (Bootstrap/Tailwind-style)
                              + Vanilla JavaScript + Fetch / AJAX + Chart.js + CKEditor 5
          │
[Security & Cache Tier]       Spring Security 6.x + CSRF Protection + BCrypt Password Encoding
                              + Caffeine Cache (Session & Rate Limiters) + AdminInterceptor
          │
[Backend Application Tier]    Java 21 (LTS) + Spring Boot 4.0.6 (Spring MVC, Spring Data JPA, Spring Mail)
                              + Lombok + Spring Validation
          │
[Database & Migration Tier]   Microsoft SQL Server 2022 + Hibernate 6.x ORM + Flyway Migration
          │
[Libraries & Utilities]       Jsoup 1.18.1 (Anti-XSS Sanitizer) + Apache Tika 2.9.2 (MIME & Video Verification)
                              + Apache POI 5.2.5 (Excel Export/Import)
          │
[External Gateways & APIs]    SePay Payment Gateway (Vietcombank QR IPN Webhook)
                              + Giao Hàng Nhanh (GHN v2 API)
                              + Google OAuth2 Authentication (1-Click Login)
                              + Google Gemini 2.0 Flash AI API (RAG Engine)
```

---

## 3. Kiến Trúc Tổng Thể (Layered Monolith Architecture)

Hệ thống được tổ chức theo kiến trúc phân tầng kinh điển (Layered Monolith), đảm bảo tính module hóa, bảo trì cao và độc lập giữa các tầng:

1. **Client Tier (Giao diện người dùng):** 
   - Gửi các HTTP GET/POST Request nhận HTML kết xuất từ Thymeleaf template engine.
   - Các thao tác tương tác nhanh (Thêm giỏ hàng, cập nhật số lượng, áp dụng voucher, gợi ý autocomplete, lọc danh mục, chat AI, POS barcode) được thực hiện qua các cuộc gọi REST/AJAX không tải lại trang.
2. **Security & Interceptor Tier:**
   - `SecurityConfig.java`: Kiểm soát phân quyền URL theo vai trò (RBAC), kích hoạt bảo vệ CSRF, chặn truy cập trái phép.
   - `AdminInterceptor.java`: Kiểm tra tính hợp lệ của phiên đăng nhập Admin/Nhân viên, xác thực tài khoản không bị khóa thông qua Caffeine Cache.
   - `UploadSecurityFilter.java`: Chặn thực thi các file mã nguồn nguy hiểm (.jsp, .php, .exe, .sh) trong thư mục tài nguyên `/uploads/`.
   - `RateLimiters`: Chống tấn công dò quét Brute-Force mật khẩu trên các form Đăng nhập, Đăng ký và Quên mật khẩu.
3. **Controller Tier (Lớp Điều phối):**
   - Web MVC Controllers (35 Controllers): Điều hướng view, binding dữ liệu form, kiểm tra validation và nạp `Model` attributes.
   - REST API Controllers: Tiếp nhận và trả về dữ liệu chuẩn JSON cho các dịch vụ AJAX, Webhook và tích hợp bên ngoài.
4. **Service Tier (Lớp Nghiệp Vụ Cốt Lõi - 43 Services):**
   - Đảm bảo tính toàn vẹn dữ liệu thông qua Transaction Management (`@Transactional`).
   - Áp dụng cơ chế khóa chống xung đột đồng thời (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
   - Xử lý thuật toán phân bổ tồn kho FIFO 2 giai đoạn, tính giá động (DotGiamGia), kiểm tra hạn và khấu trừ Voucher, tích hợp cổng thanh toán SePay và bưu cục GHN.
5. **Repository Tier (Lớp Truy Xuất Dữ Liệu - 33 Repositories):**
   - Kế thừa Spring Data JPA `JpaRepository` & `JpaSpecificationExecutor`.
   - Kết hợp linh hoạt giữa JPQL Query, Native SQL Query, Correlated Subqueries và JPA Criteria Specification.
6. **Database Tier:**
   - Microsoft SQL Server 2022 lưu trữ toàn bộ 42 bảng thực thể nghiệp vụ, khóa ngoại và chỉ mục tối ưu hóa hiệu năng.

---

## 4. Danh Mục 13 Phân Hệ Nghiệp Vụ Chính

1. **Module Xác thực & Người dùng (Auth & User):** Đăng nhập username/password, Đăng ký kèm mã hóa BCrypt, Đăng nhập 1-click Google OAuth2, Quên mật khẩu qua email token, Sổ địa chỉ giao hàng (`SoDiaChi`) tích hợp mã tỉnh/huyện/xã GHN.
2. **Module Sản phẩm & Danh mục (Catalog & EAV):** Quản lý Danh mục cha con, Thương hiệu, Sản phẩm cha (`SanPham`), Biến thể con (`SanPhamChiTiet`), Thuộc tính động EAV (`ThuocTinh`, `DanhMucThuocTinh`, `SanPhamChiTietThuocTinh`), Bộ lọc Specification đa tiêu chí và Đánh giá sao kèm ảnh.
3. **Module Giỏ hàng & Đặt hàng (Cart & Tokenized Checkout):** Giỏ hàng Database cho Member, Giỏ hàng Session cho Guest, Tokenized Checkout Context chống double submit, Luồng Mua Ngay 1 sản phẩm, Tính phí ship GHN realtime.
4. **Module Khuyến mãi & Giảm giá (Discounts & Vouchers):** 
   - Đợt giảm giá (`DotGiamGia`): Chiến dịch giảm giá trực tiếp theo % trên sản phẩm.
   - Phiếu giảm giá (`PhieuGiamGia`): Voucher mã code với quy tắc đơn tối thiểu, giảm tối đa, số lượng giới hạn và khóa Pessimistic chống vượt hạn mức.
5. **Module Cổng Thanh Toán (Payment Gateways):**
   - Thanh toán COD khi nhận hàng.
   - Thanh toán trực tuyến SePay Vietcombank QR tự động xác thực qua Webhook IPN, cơ chế Idempotency chống trùng lặp, xử lý các trường hợp lệch tiền, thiếu kho (`PAID_INSUFFICIENT_STOCK`) và thanh toán trễ sau hủy.
6. **Module Dịch vụ Vận chuyển (Shipping & GHN):** Tích hợp Giao Hàng Nhanh API v2 (Tính phí bưu cục, tạo vận đơn chính thức `GHN`, tạo đơn thu hồi `GHN_RETURN`, tạo đơn gửi đổi `GHN_EXCHANGE`, background scheduler đồng bộ trạng thái bưu tá).
7. **Module Quản lý Đơn hàng (Order Lifecycle Management):** Vòng đời 9 trạng thái đơn hàng, duyệt đơn, hủy đơn chưa thanh toán (không hoàn kho nếu chưa xác nhận, hoàn kho FIFO nếu đã xác nhận), hủy đơn online đã thanh toán & xác nhận hoàn tiền kèm chứng từ.
8. **Module Đổi trả & Hoàn tiền (RMA & Refunds):** Kiểm tra hạn 7 ngày, hỗ trợ khách tải lên ảnh và video bằng chứng lỗi, tạo đơn thu hồi bưu tá, nhân viên kiểm kho phân loại Hàng tốt (hoàn kho bán) vs Hàng lỗi (chuyển Kho Hàng Lỗi), hoàn tiền hoặc đổi mới.
9. **Module Quản trị Kho Hàng FIFO & Kho Hàng Lỗi (Inventory):** Quản lý lô nhập hàng (`PhieuNhap`, `PhieuNhapChiTiet`), thuật toán phân bổ xuất kho FIFO 2 giai đoạn, quản lý riêng biệt Kho Sản Phẩm Lỗi (`soLuongSpLoi`) và xuất xử lý hàng lỗi có ghi vết `EditLog`.
10. **Module Bán hàng tại quầy (Counter POS):** Quét mã vạch SKU/Barcode, quản lý đa tab hóa đơn chờ (lên đến 10 tab song song), tìm kiếm/tạo nhanh khách hàng, thanh toán tiền mặt/SePay QR tại quầy và xuất bản in hóa đơn nhiệt K80.
11. **Module Thống kê & Báo cáo (Analytics & POI Excel):** Doanh thu bán lẻ thực tế, Doanh thu tạm tính, Khoản giảm trừ/hoàn tiền, Biểu đồ Chart.js theo ngày/tuần/tháng/năm và xuất báo cáo Excel chuyên nghiệp qua Apache POI.
12. **Module Kiểm duyệt & Quản trị Nội dung (Moderation & CMS):** Bộ lọc từ khóa cấm/thô tục (Profanity Filter), Scheduler quét tự động vi phạm, khóa tài khoản vi phạm bình luận, Quản lý bài viết Blog và tác vụ dọn rác 90 ngày.
13. **Module Trợ lý Ảo AI Chatbot & Marketing:** Trợ lý ảo Gemini 2.0 Flash RAG trả lời tự nhiên dựa trên dữ liệu sản phẩm trong DB, Form đăng ký/hủy bản tin khuyến mãi Newsletter.

---

## 5. Các Nhóm Người Dùng & Phân Quyền (RBAC Actors)

- **Quản lý (ROLE_QL / Admin):** Quản trị viên cấp cao nhất. Toàn quyền hệ thống, độc quyền xem báo cáo thống kê doanh thu (`/admin/thong-ke/**`), quản lý tài khoản nhân viên (`/admin/nhan-vien/**`), duyệt xuất bản/xóa bài viết blog, cấu hình từ khóa kiểm duyệt nội dung, xem lịch sử giao dịch SePay.
- **Nhân viên (ROLE_NV / Staff):** Nhân viên bán hàng & vận hành kho. Truy cập POS tại quầy (`/admin/pos/**`), xử lý và chuyển trạng thái đơn hàng (`/admin/don-hang/**`), quản lý sản phẩm & danh mục (`/admin/san-pham/**`, `/admin/danh-muc/**`), quản lý khách hàng, duyệt RMA đổi trả, kiểm kho phân loại hàng và quản lý kho hàng lỗi (`/admin/kho-san-pham-loi/**`).
- **Khách hàng thành viên (ROLE_KH / Member):** Khách hàng đã đăng ký tài khoản. Quản lý hồ sơ cá nhân, sổ địa chỉ, danh sách sản phẩm yêu thích (Wishlist), lịch sử đơn hàng, gửi đánh giá nhận xét kèm ảnh, gửi yêu cầu đổi trả hàng trong 7 ngày kèm video/ảnh bằng chứng.
- **Khách hàng vãng lai (GUEST):** Khách mua hàng nhanh không cần đăng nhập trước. Hệ thống tự động khởi tạo tài khoản GUEST, giới hạn tối đa 3 lần mua và gửi email chứa token đặt mật khẩu để kích hoạt tài khoản chính thức.

---

## 6. Vòng Đời Xử Lý Request Tổng Quát (Request Lifecycle)

```
[HTTP Request từ Client]
           │
           ▼
[SecurityFilterChain] ───────► (Kiểm tra CSRF, Phân quyền URL, Rate Limiting, Upload Security)
           │
           ▼
[AdminInterceptor] ──────────► (Xác thực phiên Admin/Staff, kiểm tra tài khoản bị khóa trong Cache)
           │
           ▼
[Controller Layer] ──────────► (Đọc tham số/DTO, Validate dữ liệu Bean Validation, gọi Service)
           │
           ▼
[Service Layer] ─────────────► (Mở Transaction `@Transactional`, áp dụng Pessimistic Lock,
           │                   thực thi thuật toán FIFO, tính giá, gọi Webhook/External API)
           ▼
[Repository Layer] ──────────► (Thực thi JPA Methods, JPQL Queries, Native SQL, CriteriaBuilder)
           │
           ▼
[SQL Server Database] ───────► (Thao tác dữ liệu ACID, trả về tập kết quả)
           │
           ▼
[Response to Client] ────────► [Thymeleaf SSR HTML View] HOẶC [JSON Response REST API]
```

---

## 7. Các Hệ Thống Tích Hợp Bên Ngoài

1. **Cổng thanh toán SePay:** Cổng thanh toán tự động Vietcombank QR. Tiếp nhận Webhook IPN tại `POST /api/payment/sepay/ipn`, xác thực chữ ký `Authorization: Apikey` và IP Whitelist.
2. **Giao Hàng Nhanh (GHN v2 API):** Tính phí giao hàng realtime theo khối lượng và địa chỉ bưu cục, tự động sinh mã vận đơn, tạo đơn thu hồi bưu tá lấy hàng tận nơi (`GHN_RETURN`), đồng bộ trạng thái đơn hàng tự động.
3. **Google OAuth2:** Dịch vụ đăng nhập 1-click qua tài khoản Google.
4. **Google Gemini 2.0 Flash:** API AI thông minh hỗ trợ tư vấn sản phẩm và giải đáp kỹ thuật cầu lông theo kiến trúc RAG.
5. **Geolocation & IP Detection:** Tự động định vị Tỉnh/Thành phố của khách hàng theo địa chỉ IP để gợi ý địa chỉ giao hàng gần nhất.

---

## 8. Cấu Trúc Thư Mục Quan Trọng Trong Dự Án

```
src/main/java/com/smashvn/shop/
├── config/             # Cấu hình Spring Security, Interceptor, SePay, GHN, Gemini AI, Caffeine Cache
├── controller/
│   ├── admin/          # 13 Admin Controllers (Đơn hàng, POS, Sản phẩm, Biến thể, Kho lỗi, Thống kê, Blog...)
│   ├── advice/         # Controller Advices toàn cục (@ControllerAdvice, Flash messages)
│   ├── api/            # 8 REST API Controllers (GHN, Location, Chatbot, Search, Newsletter, Shipping...)
│   ├── blog/           # Blog public controller
│   ├── home/           # Home & Shop catalog controller
│   ├── order/          # GioHangController, CheckoutController
│   ├── payment/        # SepayIpnController, SepaySimulationController
│   ├── product/        # SanPhamController, SanPhamYeuThichController
│   └── user/           # DangNhap, DangKy, QuenMatKhau, Dashboard, Address controllers
├── dto/                # Data Transfer Objects (Checkout, POS, Inventory, Chatbot, Shipping, Analytics)
├── entity/             # 32 JPA Entities + 12 Enums + Entity Listeners
├── exception/          # Custom Exceptions (GhnUnsupportedRouteException, GhnCreateIndeterminateException...)
├── repository/         # 33 Spring Data JPA Repositories
├── scheduler/          # Background Schedulers (CommentModerationScheduler, GhnPollingScheduler...)
├── service/            # 43 Interfaces & Service Implementations
└── specification/      # SanPhamSpecification (Dynamic JPA Criteria Search & Sorting)

src/main/resources/
├── static/             # CSS, JavaScript (app.js, checkout.js, pos.js, cart.js), Hình ảnh, Vendor assets
├── templates/          # Thymeleaf HTML Templates
│   ├── admin/          # Giao diện Quản trị (donhang-list, pos, sanpham-edit, kho-san-pham-loi, thongke...)
│   ├── layout/         # Header, Footer, Sidebar, Modals components
│   └── ...             # Giao diện Public (index, shop, product-detail, cart, checkout, dashboard, blog...)
├── application.properties # Tham số cấu hình toàn bộ hệ thống (DB, Mail, SePay, GHN, Gemini, Uploads)
└── db/migration/       # Các file Flyway SQL migration
```

---
*Tài liệu Tổng quan Hệ thống thuộc bộ hồ sơ kỹ thuật SMASH-VN.*
