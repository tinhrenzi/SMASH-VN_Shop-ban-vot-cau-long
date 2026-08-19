# 01. TỔNG QUAN HỆ THỐNG SMASH-VN

## 1. Mục Đích Hệ Thống
**SMASH-VN** là nền tảng thương mại điện tử chuyên nghiệp và giải pháp bán hàng đa kênh (Omnichannel) phục vụ kinh doanh vợt cầu lông, giày dép, trang phục và phụ kiện thể thao cầu lông. Hệ thống kết hợp đồng bộ giữa:
- **Cửa hàng trực tuyến (E-Commerce Web):** Trải nghiệm mua sắm nhanh, tra cứu sản phẩm đa tiêu chí, giỏ hàng kép (Thành viên DB & Khách vãng lai Session), checkout tối ưu, thanh toán SePay QR tự động và theo dõi đơn hàng thời gian thực.
- **Bán hàng tại quầy (POS - Point of Sale):** Giao diện quét mã vạch SKU tốc độ cao cho nhân viên thu ngân, hỗ trợ đa tab hóa đơn chờ, tạo nhanh tài khoản khách hàng, thanh toán tiền mặt/SePay QR và in hóa đơn nhiệt K80.
- **Quản trị chuỗi cung ứng & Kho hàng:** Quản lý lô hàng nhập theo nguyên tắc FIFO (Nhập trước xuất trước), quản lý tách biệt Kho Hàng Lỗi (Defective Inventory) và xử lý đổi trả hàng (RMA).
- **Trợ lý Ảo AI Chatbot:** Tích hợp mô hình ngôn ngữ Google Gemini 2.0 Flash với kỹ thuật RAG truy xuất tồn kho và giá bán thực tế để tư vấn lối chơi và thông số vợt cho khách hàng.

---

## 2. Công Nghệ Sử Dụng

```
[Frontend Client]          Thymeleaf Server-Side Rendering + HTML5 + CSS3 + Vanilla JavaScript + AJAX + Chart.js
         │
[Security Layer]           Spring Security 6.x + CSRF Protection + BCrypt + Caffeine Cache Rate Limiters
         │
[Backend Framework]        Java 21 (LTS) + Spring Boot 4.0.6 (Spring MVC, Spring Data JPA, Spring Mail)
         │
[Database & Migration]     Microsoft SQL Server 2022 + Flyway Migration
         │
[Utility & Helpers]        Jsoup 1.18.1 (Anti-XSS) + Apache Tika 2.9.2 (MIME Verification) + Apache POI 5.2.5 (Excel)
         │
[External Gateways]        SePay Payment QR + Giao Hàng Nhanh (GHN) API + Google OAuth2 + Gemini 2.0 Flash AI
```

---

## 3. Kiến Trúc Tổng Thể (Layered Monolith Architecture)

Hệ thống được tổ chức theo kiến trúc phân tầng kinh điển (Layered Architecture):

1. **Client Tier (Trình duyệt):** Gửi các HTTP Request (GET/POST) tải trang Thymeleaf hoặc các cuộc gọi REST/AJAX không tải lại trang.
2. **Security & Filter Tier:**
   - `SecurityConfig.java`: Kiểm soát xác thực, lọc CSRF, áp dụng phân quyền URL.
   - `AdminInterceptor.java`: Kiểm tra tính hợp lệ của Session và trạng thái khóa tài khoản trong Caffeine Cache.
   - `UploadSecurityFilter.java`: Chặn thực thi mã độc trong thư mục tài nguyên `/uploads/`.
   - `RateLimiters`: Chống tấn công dò quét Brute-Force vào form Đăng nhập, Đăng ký và Quên mật khẩu.
3. **Controller Tier (Lớp Điều phối):**
   - Web MVC Controllers: Điều hướng view Thymeleaf, nạp `Model` attributes.
   - REST Controllers: Tiếp nhận và trả về dữ liệu chuẩn JSON (`@RestController`).
4. **Service Tier (Lớp Nghiệp Vụ Cốt Lõi):**
   - Quản lý Transactions (`@Transactional`), khóa chống xung đột (`@Lock(PESSIMISTIC_WRITE)`).
   - Xử lý thuật toán tồn kho FIFO, tính toán khuyến mãi, tích hợp cổng thanh toán và vận chuyển.
5. **Repository Tier (Lớp Truy Xuất Dữ Liệu):**
   - Spring Data JPA, Hibernate ORM, JPQL Queries, Native SQL Queries và JPA Criteria Specification.
6. **Database Tier:**
   - Microsoft SQL Server 2022 lưu trữ toàn bộ thực thể nghiệp vụ.

---

## 4. Các Module Nghiệp Vụ Chính

1. **Module Xác thực & Người dùng (Auth & User):** Đăng nhập, Đăng ký, Quên mật khẩu token qua Email, Đăng nhập Google OAuth2, Sổ địa chỉ giao hàng.
2. **Module Sản phẩm & Danh mục (Catalog):** Quản lý Danh mục, Thương hiệu, Sản phẩm cha, Biến thể con, Thuộc tính động EAV, Bộ lọc nâng cao Specification, Đánh giá & Rating sao.
3. **Module Giỏ hàng & Checkout (Cart & Checkout):** Giỏ hàng Database cho Member, Giỏ hàng Session cho Guest, Tokenized Checkout Context, Mua ngay, Tính phí ship GHN.
4. **Module Khuyến mãi & Giảm giá (Discounts & Vouchers):** Đợt giảm giá (DotGiamGia - chiến dịch giá bán trực tiếp) và Phiếu giảm giá (PhieuGiamGia - Voucher mã code).
5. **Module Thanh toán (Payment):** Thanh toán khi nhận hàng (COD), Cổng thanh toán trực tuyến SePay Vietcombank QR tự động qua Webhook IPN.
6. **Module Vận chuyển (Shipping & GHN):** Tích hợp Giao Hàng Nhanh API (Tính phí bưu cục, tạo vận đơn, thu hồi đổi trả, scheduler đồng bộ trạng thái bưu tá).
7. **Module Quản lý Đơn hàng (Order Management):** Vòng đời đơn hàng, hủy đơn, hoàn tồn kho, duyệt đơn.
8. **Module Đổi trả & Hoàn tiền (RMA & Refunds):** Kiểm tra hạn 7 ngày, tải bằng chứng ảnh/video, tạo đơn thu hồi GHN, kiểm kho (Hàng tốt vs Hàng lỗi), xác nhận hoàn tiền.
9. **Module Quản trị Kho hàng & Tồn kho FIFO (Inventory):** Lô nhập hàng (PhieuNhap, PhieuNhapChiTiet), Thuật toán phân bổ FIFO 2 giai đoạn, Quản lý Kho Hàng Lỗi (`soLuongSpLoi`).
10. **Module POS Bán hàng tại quầy (Counter POS):** Quét mã vạch SKU, đa tab hóa đơn chờ, thanh toán tiền mặt/SePay, in hóa đơn nhiệt K80.
11. **Module Thống kê & Báo cáo (Analytics):** Doanh thu thực tế, Doanh thu tạm tính, Khoản giảm trừ, Biểu đồ Chart.js, Xuất file Excel POI.
12. **Module Kiểm duyệt & Blog (Moderation & CMS):** Lọc từ khóa thô tục, Scheduler quét vi phạm, Tự động khóa bình luận, Quản lý bài viết Blog và tác vụ xóa rác 90 ngày.
13. **Module AI Chatbot & Marketing:** Trợ lý ảo Gemini 2.0 Flash RAG, Đăng ký/Hủy đăng ký bản tin Newsletter.

---

## 5. Các Nhóm Người Dùng (User Roles & Actors)

- **Quản lý (ROLE_QL / Admin):** Quản trị viên cấp cao nhất. Toàn quyền truy cập hệ thống, độc quyền xem báo cáo thống kê doanh thu (`/admin/thong-ke/**`), quản lý tài khoản nhân viên (`/admin/nhan-vien/**`), duyệt xuất bản/xóa blog, cấu hình từ khóa kiểm duyệt.
- **Nhân viên (ROLE_NV / Staff):** Nhân viên bán hàng & vận hành. Truy cập POS tại quầy (`/admin/pos/**`), xử lý đơn hàng (`/admin/don-hang/**`), quản lý sản phẩm & danh mục (`/admin/san-pham/**`), quản lý khách hàng, theo dõi giao dịch và quản lý kho hàng lỗi (`/admin/kho-san-pham-loi/**`).
- **Khách hàng thành viên (ROLE_KH / Member):** Khách hàng có tài khoản. Quản lý hồ sơ, sổ địa chỉ, lịch sử đơn hàng, gửi đánh giá, yêu cầu đổi trả trong vòng 7 ngày.
- **Khách hàng vãng lai (GUEST):** Mua hàng nhanh không cần mật khẩu ban đầu. Hệ thống tự động cấp tài khoản GUEST và gửi email tạo mật khẩu. Giới hạn mua tối đa 3 lần.

---

## 6. Luồng Xử Lý Request Tổng Quát (Request Lifecycle)

```
[HTTP Request]
       │
       ▼
[SecurityFilterChain] ── (Kiểm tra CSRF, Auth, Role, Rate Limit, Upload Security)
       │
       ▼
[AdminInterceptor] ──── (Kiểm tra trạng thái hoạt động của tài khoản trong Cache)
       │
       ▼
[Controller Layer] ──── (Đọc Request, Validate dữ liệu, gọi Service, binding Model / JSON)
       │
       ▼
[Service Layer] ─────── (Khởi tạo Transaction, áp dụng Pessimistic Lock, xử lý nghiệp vụ)
       │
       ▼
[Repository Layer] ──── (Thực thi JPA / JPQL / Criteria / Native SQL)
       │
       ▼
[SQL Server Database] ─ (Lưu trữ và trả về kết quả)
       │
       ▼
[Response to Client] ── (Render Thymeleaf HTML View HOẶC Trả về JSON REST API)
```

---

## 7. Các Hệ Thống Tích Hợp Bên Ngoài

1. **SePay Gateway:** Cổng thanh toán tự động Vietcombank QR. Tiếp nhận Webhook IPN tại `POST /api/payment/sepay/ipn`, xác thực chữ ký `Authorization: Apikey` và IP Whitelist.
2. **Giao Hàng Nhanh (GHN):** Hệ thống bưu cục vận chuyển. Tính phí giao hàng realtime, tự động phát sinh mã vận đơn, tạo đơn thu hồi bưu tá lấy hàng tận nơi, tra cứu lộ trình.
3. **Google OAuth2:** Dịch vụ đăng nhập 1-click qua tài khoản Google.
4. **Google Gemini 2.0 Flash:** API AI thông minh hỗ trợ tư vấn sản phẩm và giải đáp kỹ thuật cầu lông.
5. **Geolocation & IP Detection:** Tự động định vị Tỉnh/Thành phố của khách hàng theo IP để gợi ý địa chỉ giao hàng.

---

## 8. Cấu Trúc Thư Mục Quan Trọng Trong Dự Án

- `src/main/java/com/smashvn/shop/config/`: Chứa toàn bộ cấu hình bảo mật, interceptor, SePay, GHN, Gemini.
- `src/main/java/com/smashvn/shop/controller/`: Chứa 35 Controllers tiếp nhận request.
- `src/main/java/com/smashvn/shop/service/`: Chứa 37 Services thực thi logic nghiệp vụ cốt lõi.
- `src/main/java/com/smashvn/shop/repository/`: Chứa 33 Repositories tương tác dữ liệu.
- `src/main/java/com/smashvn/shop/entity/`: Chứa 32 Entities ánh xạ cơ sở dữ liệu.
- `src/main/java/com/smashvn/shop/specification/`: Chứa `SanPhamSpecification.java` lọc sản phẩm động.
- `src/main/java/com/smashvn/shop/scheduler/`: Chứa các background schedulers.
- `src/main/resources/templates/`: Chứa toàn bộ giao diện Thymeleaf HTML (Public & Admin).
- `src/main/resources/application.properties`: File cấu hình toàn bộ tham số hệ thống.

---
*Tài liệu thuộc bộ hồ sơ kỹ thuật SMASH-VN. Tra cứu chi tiết tại các file tiếp theo.*
