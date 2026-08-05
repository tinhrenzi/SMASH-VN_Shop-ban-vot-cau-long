# Sơ đồ Cấu trúc Dự án & Luồng Hoạt động (SMASH-VN)

Tài liệu này cung cấp cái nhìn chi tiết về cấu trúc thư mục hiện tại của dự án **SMASH-VN (Shop bán vợt cầu lông)** và mô tả chi tiết luồng hoạt động của các chức năng cốt lõi trong hệ thống.

---

## 1. Sơ đồ Cấu trúc Thư mục Dự án

Dưới đây là sơ đồ cấu trúc thư mục chính của dự án Maven Spring Boot:

```text
SMASH-VN_Shop-ban-vot-cau-long/
├── .env                         # Lưu trữ các biến môi trường cấu hình (DB, API Keys, SMTP, Webhook Secrets)
├── .env.example                 # Tệp mẫu khai báo biến môi trường chuẩn
├── pom.xml                      # Cấu hình Maven Build & Dependencies (Spring Boot, Security, Thymeleaf, JPA, Jsoup, Tika)
├── deploy.bat                   # Kịch bản tự động hóa Build & Deploy trên Windows
├── deploy.js                    # Script Node.js tự động kiểm tra cổng dịch vụ & triển khai
├── docs/                        # Thư mục tài liệu báo cáo kiến trúc tương tác & báo cáo kiểm thử
│   ├── project-structure.html   # Trang tài liệu kiến trúc tương tác trực quan & Call Flow Map
│   └── log.html                 # Báo cáo kết quả kiểm thử toàn diện
├── payment/                     # Script phụ trợ tích hợp cổng thanh toán trực tuyến
├── uploads/                     # Thư mục lưu trữ hình ảnh tải lên động (Sản phẩm, Đánh giá, Avatar, Banner)
└── src/
    ├── main/
    │   ├── java/com/smashvn/shop/   # Mã nguồn Backend (Java Spring Boot MVC)
    │   │   ├── SmashVnApplication.java # Lớp khởi chạy chính của ứng dụng
    │   │   │
    │   │   ├── config/              # Các cấu hình hệ thống
    │   │   │   ├── AdminInterceptor.java        # Kiểm tra phiên làm việc & quyền truy cập trang quản trị
    │   │   │   ├── AsyncConfig.java             # Cấu hình xử lý bất đồng bộ (@EnableAsync)
    │   │   │   ├── DotenvEnvironmentPostProcessor.java # Tự động nạp biến môi trường từ .env
    │   │   │   ├── GeminiHttpClientConfig.java   # Cấu hình HTTP Client kết nối Google Gemini AI API
    │   │   │   ├── GhnConfig.java               # Cấu hình kết nối API Giao Hàng Nhanh
    │   │   │   ├── PlaintextPasswordMigrator.java # Migration tự động băm mật khẩu cũ sang BCrypt khi boot
    │   │   │   ├── ProfessionalDataSeeder.java   # Khởi tạo dữ liệu mẫu phong phú khi CSDL trống
    │   │   │   ├── SecurityConfig.java          # Cấu hình Spring Security (URL Authorization, BCrypt, OAuth2)
    │   │   │   ├── SepayConfig.java             # Cấu hình tích hợp thanh toán ngân hàng SePay
    │   │   │   ├── ShopContactProperties.java   # Nạp thông tin liên hệ cửa hàng từ properties
    │   │   │   ├── SpringContextHelper.java     # Tiện ích truy xuất Spring Beans tĩnh
    │   │   │   ├── UploadPathVerifier.java      # Tự động tạo và kiểm tra thư mục uploads/
    │   │   │   ├── UploadSecurityFilter.java    # Bảo mật tệp tin upload tránh mã độc (Shell scripts)
    │   │   │   └── WebMvcConfig.java            # Cấu hình Spring MVC Interceptors & Resource Handlers
    │   │   │
    │   │   ├── constant/            # Các Enum hằng số và cấu hình thuộc tính sản phẩm
    │   │   │   ├── CategoryType.java            # Enum loại danh mục (Vợt, Giày, Quần áo, Phụ kiện...)
    │   │   │   ├── DanhMucIds.java              # ID danh mục cố định hệ thống
    │   │   │   └── SanPhamAttributeConfig.java  # Cấu hình thuộc tính động cho từng loại sản phẩm
    │   │   │
    │   │   ├── controller/          # Các tầng tiếp nhận và điều hướng Request (MVC & REST Controllers)
    │   │   │   ├── admin/           # Controllers dành cho Admin & Staff
    │   │   │   │   ├── AdminBienTheController.java  # Quản lý Biến thể sản phẩm (SKU, màu, size, tồn kho)
    │   │   │   │   ├── AdminBlogController.java     # Quản lý bài viết tin tức & kinh nghiệm cầu lông
    │   │   │   │   ├── AdminController.java         # Dashboard quản trị tổng quát
    │   │   │   │   ├── AdminDangNhapController.java # Đăng nhập/Đăng xuất Admin & Staff
    │   │   │   │   ├── AdminDanhGiaController.java  # Quản lý & kiểm duyệt đánh giá sản phẩm
    │   │   │   │   ├── AdminDanhMucController.java  # Quản lý danh mục sản phẩm (Cha - Con)
    │   │   │   │   ├── AdminKhuyenMaiController.java# Quản lý Phiếu giảm giá & Đợt giảm giá
    │   │   │   │   ├── AdminNhanVienController.java # Quản lý tài khoản & phân quyền nhân viên
    │   │   │   │   ├── AdminPaymentTransactionController.java # Tra cứu lịch sử giao dịch thanh toán
    │   │   │   │   ├── AdminPosController.java      # Bán hàng trực tiếp tại quầy (POS)
    │   │   │   │   ├── AdminSanPhamController.java  # Quản lý danh sách sản phẩm & thuộc tính
    │   │   │   │   ├── AdminThongKeController.java  # Thống kê doanh thu & biểu đồ bán hàng
    │   │   │   │   └── CommentModerationAdminController.java # Quản trị bình luận bị gắn cờ vi phạm
    │   │   │   ├── advice/          # Global Notification Advice
    │   │   │   │   └── GlobalNotificationAdvice.java # Tự động nạp giỏ hàng & thông báo vào Model
    │   │   │   ├── api/             # REST Controllers cung cấp API cho Client/Frontend
    │   │   │   │   ├── CategoryAttributeRestController.java # API lấy thuộc tính động theo danh mục
    │   │   │   │   ├── ChatbotProductsRestController.java # API cung cấp dữ liệu sản phẩm cho AI
    │   │   │   │   ├── ChatbotRestController.java   # API Chatbot tư vấn qua Google Gemini AI
    │   │   │   │   ├── GhnRestController.java       # Tích hợp địa giới & tính phí ship Giao Hàng Nhanh
    │   │   │   │   ├── LocationRestController.java  # Lấy danh mục Tỉnh/Huyện/Xã Việt Nam
    │   │   │   │   ├── NewsletterApiController.java # Đăng ký nhận email tin tức/khuyến mãi
    │   │   │   │   ├── SearchApiController.java     # Tìm kiếm nhanh & gợi ý từ khóa sản phẩm
    │   │   │   │   └── ShippingApiController.java   # Tính toán phí vận chuyển đơn hàng
    │   │   │   ├── blog/            # Controller bài viết tin tức & bình luận
    │   │   │   ├── home/            # Controller giao diện trang chủ, banner, khuyến mãi, liên hệ
    │   │   │   ├── order/           # Controller Giỏ hàng (Cart) & Thanh toán (Checkout)
    │   │   │   ├── payment/         # Controller nhận Webhook SePay IPN & giả lập giao dịch
    │   │   │   ├── product/         # Controller danh sách sản phẩm, lọc nâng cao, chi tiết & wishlist
    │   │   │   └── user/            # Controller hồ sơ khách hàng, sổ địa chỉ, lịch sử đơn hàng, OTP
    │   │   │
    │   │   ├── dao/                 # Tầng Data Access Object hỗ trợ các truy vấn dữ liệu thủ công
    │   │   ├── dto/                 # Data Transfer Objects trao đổi dữ liệu Frontend - Backend
    │   │   ├── entity/              # Các JPA Entities ánh xạ các bảng CSDL SQL Server (31+ tables)
    │   │   ├── exception/           # Xử lý ngoại lệ tập trung (GlobalExceptionHandler & Custom Exceptions)
    │   │   ├── migration/           # Kịch bản chuyển đổi dữ liệu tự động (EmployeeMigrationRunner)
    │   │   ├── repository/          # Tầng tương tác CSDL Spring Data JPA (31 Repositories)
    │   │   ├── scheduler/           # Tác vụ lập lịch ngầm tự động (CommentModerationScheduler)
    │   │   ├── security/            # Tầng bảo mật nâng cao chống SPAM & Brute-force
    │   │   │   ├── ForgotPasswordRateLimiter.java# Giới hạn tần suất gửi OTP quên mật khẩu
    │   │   │   ├── LoginRateLimiter.java        # Giới hạn tần suất đăng nhập sai
    │   │   │   └── RegisterRateLimiter.java     # Giới hạn tần suất đăng ký tài khoản
    │   │   │
    │   │   ├── service/             # Tầng xử lý Logic Nghiệp vụ chính của hệ thống
    │   │   │   ├── admin/           # Nghiệp vụ quản trị (Biến thể, Khách hàng, Khuyến mãi, POS, Thống kê)
    │   │   │   ├── api/             # Nghiệp vụ API giao hàng GHN & tính phí ship vùng miền
    │   │   │   ├── blog/            # Nghiệp vụ bài viết & tự động kiểm duyệt bình luận
    │   │   │   ├── common/          # Nghiệp vụ quản lý lưu trữ tệp tin (FileStorageService)
    │   │   │   ├── impl/            # Implementations cho Chatbot AI Gemini, Newsletter & Giá
    │   │   │   ├── order/           # Nghiệp vụ giỏ hàng, Guest Checkout & Đơn hàng
    │   │   │   ├── payment/         # Nghiệp vụ thanh toán chuyển khoản SePay IPN
    │   │   │   ├── product/         # Nghiệp vụ sản phẩm, thương hiệu, danh mục, đánh giá, tính khuyến mãi
    │   │   │   └── user/            # Nghiệp vụ người dùng, sổ địa chỉ, đổi mật khẩu, OTP
    │   │   │
    │   │   └── util/                # Các class tiện ích (ProfanityFilter, VoucherCalculator, SpecUtils...)
    │   │
    │   └── resources/
    │       ├── application.properties # File cấu hình hệ thống Spring Boot chính
    │       ├── static/              # Tài nguyên tĩnh (CSS, JS, Images)
    │       └── templates/           # Giao diện Thymeleaf HTML (Admin & Customer)
    └── test/                        # Mã nguồn kiểm thử Unit & Integration test
```

---

## 2. Luồng Hoạt động các Chức năng Chính

Dưới đây là sơ đồ và mô tả luồng hoạt động đối với các nghiệp vụ cốt lõi trong hệ thống.

### 2.1. Luồng Đăng ký & Đăng nhập (Auth Flow)

Hệ thống hỗ trợ 2 hình thức: đăng nhập thường và đăng nhập qua mạng xã hội (Google OAuth2).
Để bảo mật, hệ thống áp dụng `Rate Limiter` ngăn chặn brute-force.

```mermaid
sequenceDiagram
    actor User as Khách hàng
    participant System as Hệ thống (Spring Security)
    participant RateLimiter as Bộ lọc Rate Limiter
    participant DB as Database SQL Server

    User->>System: Gửi yêu cầu đăng nhập (Email/Password)
    System->>RateLimiter: Kiểm tra IP & Email đăng nhập
    alt Đạt giới hạn Spam (Rate limit exceeded)
        RateLimiter-->>System: Trả về lỗi chặn đăng nhập tạm thời
        System-->>User: Thông báo: Thử lại sau ít phút.
    else Cho phép đi tiếp
        System->>DB: Truy vấn thông tin tài khoản
        DB-->>System: Trả về tài khoản
        alt Sai mật khẩu
            System->>RateLimiter: Ghi nhận số lần đăng nhập sai (+1)
            System-->>User: Báo sai mật khẩu
        else Đúng mật khẩu
            System->>RateLimiter: Reset số lần đăng nhập sai (0)
            System->>System: Tạo Session lưu phiên làm việc
            System-->>User: Đăng nhập thành công, chuyển hướng về trang chủ
        end
    end
```

---

### 2.2. Luồng Giỏ hàng & Đặt hàng (Cart & Checkout Flow)

Hệ thống hỗ trợ song song hai cơ chế: **Khách hàng đã đăng nhập** và **Khách vãng lai (Guest Checkout)**.

```mermaid
flowchart TD
    Start([Khách hàng tiến hành Checkout]) --> AuthCheck{Đã đăng nhập?}
    
    AuthCheck -- Có --> GetUserCart[Lấy Giỏ hàng từ CSDL] --> FillInfo[Hiển thị thông tin & Địa chỉ có sẵn]
    
    AuthCheck -- Không --> GetGuestCart[Lấy Giỏ hàng từ Cookie/Session] --> RequestInfo[Yêu cầu nhập Họ tên, SĐT, Email]
    
    RequestInfo --> CheckEmail{Email đã tồn tại?}
    CheckEmail -- Đã có tài khoản ACTIVE --> ForceLogin[Yêu cầu đăng nhập hoặc dùng Email khác]
    CheckEmail -- Tài khoản vãng lai GUEST --> CheckGuestPurchases{Số lần mua của Guest >= 3?}
    CheckGuestPurchases -- Đúng --> BlockGuest[Bắt buộc kích hoạt tài khoản bằng mật khẩu trước khi mua tiếp]
    CheckGuestPurchases -- Sai --> AutoLink[Liên kết đơn hàng với TK vãng lai hiện tại]
    CheckEmail -- Chưa tồn tại --> AutoReg[Tự động tạo tài khoản GUEST mới]

    AutoReg & AutoLink & FillInfo --> SelectShip[Chọn địa chỉ nhận hàng]
    
    SelectShip --> API_GHN[Gửi yêu cầu đến API Giao Hàng Nhanh]
    API_GHN --> CalcShipFee[Tính phí vận chuyển dựa trên vị trí địa lý]
    
    CalcShipFee --> ApplyVoucher[Áp dụng phiếu giảm giá & kiểm tra điều kiện tối thiểu]
    
    ApplyVoucher --> PaymentMethod{Chọn PT Thanh toán}
    
    PaymentMethod -- COD --> CreateOrderCOD[Tạo đơn hàng trạng thái: CHỜ XÁC NHẬN] --> ClearCart[Xóa giỏ hàng] --> EndSuccess([Đặt hàng thành công])
    
    PaymentMethod -- Chuyển khoản ngân hàng / Ví điện tử --> CreateOrderPending[Tạo đơn hàng trạng thái: CHỜ THANH TOÁN] --> PayRedirect[Chuyển hướng đến cổng thanh toán trực tuyến]
```

> [!NOTE]
> Khi **Khách vãng lai** đặt hàng thành công lần đầu tiên, hệ thống sẽ tự động gửi một email bất đồng bộ kích hoạt tài khoản (`autoRegisterGuest` -> `sendOrderAndAccountNotification`). Khách hàng có thể truy cập đường link trong email để thiết lập mật khẩu và nâng cấp lên tài khoản `ACTIVE` chính thức.

---

### 2.3. Luồng Thanh toán Trực tuyến (Payment Gateway Webhook/IPN Flow)

Hệ thống hỗ trợ tích hợp sâu với **ZaloPay** (Ví điện tử) và **SePay** (Tự động nhận chuyển khoản ngân hàng qua IPN Webhook).

#### 2.3.1. Luồng thanh toán tự động qua chuyển khoản Ngân hàng (SePay IPN Webhook)

```mermaid
sequenceDiagram
    actor User as Khách hàng
    participant Bank as Ngân hàng
    participant SePay as Hệ thống SePay.vn
    participant Webhook as SepayIpnController (Spring Boot)
    participant Service as SepayGatewayService
    participant DB as Database SQL Server

    User->>Bank: Quét mã QR / Chuyển khoản với nội dung (mã hóa đơn)
    Bank->>SePay: Ghi nhận số dư thay đổi thành công
    SePay->>Webhook: Gửi thông báo Webhook IPN (Raw JSON payload)
    
    Webhook->>Webhook: 1. Kiểm tra Whitelist IP (bảo mật nguồn tin)
    Webhook->>Webhook: 2. Xác thực API Key (Authorization Header)
    
    alt Không hợp lệ
        Webhook-->>SePay: Trả về HTTP 403 Forbidden / 401 Unauthorized
    else Hợp lệ
        Webhook->>Service: Xử lý giao dịch thanh toán
        Service->>DB: Kiểm tra xem ID giao dịch (TransactionId) đã được xử lý chưa (Tránh trùng lặp)
        
        alt Giao dịch đã xử lý trước đó
            Service-->>Webhook: Trả về trạng thái "Already processed"
            Webhook-->>SePay: Trả về HTTP 200 OK (Ngăn SePay gửi lại)
        else Giao dịch mới
            Service->>DB: Tìm đơn hàng (HoaDon) tương ứng từ nội dung chuyển khoản (Memo)
            alt Không tìm thấy đơn hàng hoặc sai số tiền
                Service->>DB: Ghi log lỗi giao dịch lỗi / thanh toán sai số tiền
                Service-->>Webhook: Ném lỗi InvalidPaymentException
            else Tìm thấy đơn hàng & đúng số tiền
                Service->>DB: 1. Cập nhật trạng thái đơn hàng -> ĐÃ THANH TOÁN
                Service->>DB: 2. Cập nhật trạng thái thanh toán đơn hàng -> ĐÃ THANH TOÁN
                Service->>DB: 3. Lưu thông tin lịch sử giao dịch vào bảng PaymentTransaction
                Service-->>Webhook: Xử lý thành công
                Webhook-->>SePay: Trả về HTTP 200 OK
            end
        end
    end
```

---

### 2.4. Luồng Bán hàng tại Quầy (POS System Flow)

Dành cho Nhân viên và Quản lý tại cửa hàng để tạo nhanh hóa đơn bán hàng trực tiếp cho khách.

```mermaid
flowchart TD
    StartPOS([Nhân viên mở giao diện POS]) --> SearchProduct[Tìm kiếm sản phẩm/biến thể theo từ khóa hoặc mã]
    SearchProduct --> AddToPOSCart[Thêm sản phẩm vào giỏ POS]
    AddToPOSCart --> EnterCustomer[Nhập thông tin khách hàng - tùy chọn]
    EnterCustomer --> CheckVoucher[Áp dụng voucher trực tiếp tại quầy nếu có]
    CheckVoucher --> PayAtCounter{Phương thức thanh toán}
    
    PayAtCounter -- Tiền mặt --> CompletePOSOrder[Tạo đơn hàng hoàn tất & Trừ trực tiếp số lượng tồn kho]
    PayAtCounter -- Chuyển khoản QR --> GenerateQR[Hiển thị mã QR động SePay/ZaloPay để quét]
    
    GenerateQR --> WaitForPayment[Nhân viên chờ hệ thống cập nhật thanh toán tự động qua Webhook hoặc bấm kiểm tra]
    WaitForPayment --> CompletePOSOrder
    
    CompletePOSOrder --> PrintReceipt[Mở pop-up in hóa đơn trực tiếp cho khách] --> EndPOS([Hoàn tất giao dịch tại quầy])
```

---

### 2.5. Luồng Kiểm duyệt Đánh giá & Bình luận (Comment & Review Moderation Flow)

Để tránh spam và chứa ngôn từ phản cảm, các bình luận của bài viết Blog hoặc đánh giá sản phẩm sẽ đi qua bộ lọc kiểm duyệt tự động trước khi lưu vào CSDL.

```mermaid
flowchart TD
    UserComment([Người dùng gửi Bình luận / Đánh giá]) --> FilterProfanity{Đi qua bộ lọc ProfanityFilter}
    
    FilterProfanity -- Có chứa từ tục tĩu --x BlockComment[Từ chối bình luận ngay lập tức và cảnh báo người dùng]
    
    FilterProfanity -- Không chứa từ tục tĩu --> SearchKeywords{Chứa từ khóa kiểm duyệt thủ công?}
    
    SearchKeywords -- Có chứa từ khóa nhạy cảm --> SavePending[Lưu bình luận với trạng thái CHỜ KIỂM DUYỆT]
    SavePending --> AdminPanel[Hiển thị tại danh sách bình luận chờ duyệt của Admin]
    AdminPanel --> AdminAction{Admin duyệt?}
    AdminAction -- Phê duyệt --> SetActive[Trạng thái: HOẠT ĐỘNG - hiển thị lên website]
    AdminAction -- Từ chối --> DeleteComment[Xóa bỏ bình luận]
    
    SearchKeywords -- Hoàn toàn sạch --> SetActive
```

---

### 2.6. Luồng Chatbot Gemini AI tư vấn khách hàng (AI Support Chatbot Flow)

Hệ thống tích hợp một chatbot tư vấn dựa trên mô hình Gemini AI để hỗ trợ khách hàng nhanh chóng.

```mermaid
sequenceDiagram
    actor User as Khách hàng
    participant UI as Giao diện Chatbot (Client)
    participant Controller as ChatRestController
    participant Service as ChatService
    participant GeminiAPI as Google Gemini API

    User->>UI: Nhập câu hỏi (Ví dụ: "Vợt Nanoflare 1000Z giá bao nhiêu?")
    UI->>Controller: Gửi nội dung tin nhắn và lịch sử trò chuyện (ConversationId)
    Controller->>Service: Xử lý ngữ cảnh chat
    Service->>Service: Tự động đính kèm thông tin cửa hàng / Hướng dẫn trả lời (System Prompt)
    Service->>GeminiAPI: Gọi API Gemini (gemini-1.5-flash) kèm theo Prompt và lịch sử chat
    GeminiAPI-->>Service: Trả về nội dung phản hồi dạng Text
    Service->>Controller: Lưu tin nhắn của khách hàng & Chatbot vào cơ sở dữ liệu (ChatMessage)
    Controller-->>UI: Trả về nội dung JSON phản hồi cho khách hàng
    UI-->>User: Hiển thị câu trả lời mượt mà trên khung Chat
```

---

*Tài liệu được tạo tự động bởi Antigravity để phản ánh chính xác mã nguồn dự án hiện tại.*
