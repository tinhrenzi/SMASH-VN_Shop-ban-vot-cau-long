# 04. BẢN ĐỒ TOÀN BỘ ENDPOINTS (ENDPOINT MAP)

Dưới đây là danh mục tổng hợp toàn bộ các Route Endpoints trong hệ thống SMASH-VN, được phân nhóm theo phân hệ chức năng:

---

## 1. PHÂN HỆ XÁC THỰC & NGƯỜI DÙNG (AUTHENTICATION & USER)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/user/dang-nhap` | `UserDangNhapController` | `hienThiTrangDangNhap` | N/A | Public | Hiển thị form đăng nhập |
| `POST`| `/user/dang-nhap` | `UserDangNhapController` | `xuLyDangNhap` | `UserDangNhapService` | Public | Xử lý đăng nhập username/password |
| `GET` | `/user/dang-ky` | `UserDangKyController` | `hienThiTrangDangKy` | N/A | Public | Hiển thị form đăng ký |
| `POST`| `/user/dang-ky` | `UserDangKyController` | `xuLyDangKy` | `UserDangKyService` | Public | Xử lý đăng ký tài khoản mới |
| `GET` | `/user/google-success`| `UserDangNhapController` | `googleLoginSuccess` | `UserDangNhapService` | Public | Callback đăng nhập Google OAuth2 |
| `GET` | `/user/dang-xuat` | `UserDangNhapController` | `dangXuat` | N/A | Public | Đăng xuất tài khoản |
| `GET` | `/user/quen-mat-khau`| `UserQuenMatKhauController`| `hienThiQuenMatKhau` | N/A | Public | Form yêu cầu khôi phục mật khẩu |
| `POST`| `/user/quen-mat-khau`| `UserQuenMatKhauController`| `xuLyQuenMatKhau` | `UserQuenMatKhauService` | Public | Gửi email chứa token đặt lại MK |
| `GET` | `/user/dat-lai-mat-khau`| `UserQuenMatKhauController`| `hienThiDatLaiMatKhau` | `UserQuenMatKhauService` | Public | Form đặt lại mật khẩu mới |
| `POST`| `/user/dat-lai-mat-khau`| `UserQuenMatKhauController`| `xuLyDatLaiMatKhau` | `UserQuenMatKhauService` | Public | Lưu mật khẩu mới vào DB |

---

## 2. PHÂN HỆ TRANG CHỦ & CỬA HÀNG (CATALOG & PRODUCTS)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service / Repository | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/` | `HomeController` | `hienThiTrangChu` | `SanPhamRepository`, `DotGiamGiaDAO` | Public | Trang chủ (Flash sale, New, Best Seller) |
| `GET` | `/shop` | `HomeController` | `hienThiCuaHang` | `SanPhamService`, `SanPhamSpecification` | Public | Trang cửa hàng, lọc đa tiêu chí, phân trang |
| `GET` | `/san-pham/{id}` | `SanPhamController` | `hienThiChiTietSanPham` | `SanPhamService`, `PricingService` | Public | Chi tiết sản phẩm & biến thể |
| `POST`| `/san-pham/{id}/danh-gia` | `SanPhamController` | `guiDanhGia` | `DanhGiaService`, `ProfanityFilter` | `ROLE_KH` | Gửi bài đánh giá & hình ảnh review |
| `POST`| `/api/yeu-thich/toggle/{id}` | `SanPhamYeuThichController` | `toggleWishlist` | `SanPhamYeuThichService` | `ROLE_KH` | Thêm / Bỏ sản phẩm yêu thích |
| `GET` | `/api/search/autocomplete` | `SearchApiController` | `autocomplete` | `SanPhamRepository.searchAutocomplete` | Public | Gợi ý nhanh khi người dùng gõ từ khóa |

---

## 3. PHÂN HỆ GIỎ HÀNG & CHECKOUT (CART & ORDER PLACEMENT)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/gio-hang` | `GioHangController` | `hienThiGioHang` | `GioHangService`, `GuestCartService` | Public | Hiển thị trang giỏ hàng `cart.html` |
| `POST`| `/gio-hang/them` | `GioHangController` | `xuLyThemVaoGio` | `GioHangService`, `GuestCartService` | Public | Thêm sản phẩm vào giỏ (AJAX JSON) |
| `POST`| `/gio-hang/cap-nhat` | `GioHangController` | `capNhatSoLuong` | `GioHangService`, `GuestCartService` | Public | Cập nhật số lượng món (+ / -) |
| `POST`| `/gio-hang/api/xoa/{id}` | `GioHangController` | `xoaSanPhamAjax` | `GioHangService`, `GuestCartService` | Public | Xóa 1 sản phẩm khỏi giỏ |
| `POST`| `/gio-hang/api/xoa-nhieu` | `GioHangController` | `xoaNhieuSanPhamAjax` | `GioHangService`, `GuestCartService` | Public | Xóa nhiều sản phẩm được chọn |
| `GET` | `/gio-hang/api/mini-cart` | `GioHangController` | `layDuLieuMiniCart` | `GioHangService`, `GuestCartService` | Public | Lấy dữ liệu dropdown Mini Cart |
| `POST`| `/checkout/start` | `CheckoutController` | `startCheckout` | `CheckoutContextService` | Public | Bắt đầu checkout các món đã tích chọn |
| `POST`| `/checkout/buy-now` | `CheckoutController` | `buyNow` | `CheckoutContextService` | Public | Bắt đầu checkout Mua Ngay 1 món |
| `POST`| `/checkout/start-all` | `CheckoutController` | `startCheckoutAll` | `CheckoutContextService` | Public | Bắt đầu checkout toàn bộ giỏ hàng |
| `GET` | `/checkout` | `CheckoutController` | `viewCheckout` | `CheckoutContextService`, `ShippingFeeCalculator` | Public | Hiển thị trang đặt hàng `checkout.html` |
| `POST`| `/checkout/submit` | `CheckoutController` | `submitCheckout` | `GioHangService`, `InventoryLotService`, `GhnService` | Public | **Xác nhận đặt hàng chính thức** |
| `POST`| `/api/voucher/apply` | `CheckoutController` | `applyVoucher` | `VoucherCalculator`, `PhieuGiamGiaRepository` | Public | Kiểm tra và áp dụng mã voucher |

---

## 4. PHÂN HỆ CỔNG THANH TOÁN (SEPAY PAYMENT GATEWAY)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST`| `/api/payment/sepay/ipn` | `SepayIpnController` | `handleSepayIpn` | `SepayGatewayService`, `SepayOrderPaymentService` | SePay Secret | **Webhook IPN nhận tiền SePay** |
| `GET` | `/api/payment/sepay/query/{code}` | `SepayIpnController` | `queryTransaction` | `SepayGatewayService` | Public | Polling trạng thái thanh toán đơn hàng |
| `POST`| `/api/payment/sepay/simulate` | `SepaySimulationController`| `simulatePayment` | `SepayGatewayService` | `ROLE_QL` | Giả lập Webhook SePay môi trường Dev |

---

## 5. PHÂN HỆ KHÁCH HÀNG & DASHBOARD (USER DASHBOARD & RMA)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/dashboard` | `UserDashboardController` | `hienThiDashboard` | `UserDashboardService`, `OrderViewService` | `ROLE_KH` | Trang cá nhân, đơn mua, sổ địa chỉ |
| `POST`| `/user/don-hang/huy/{id}` | `UserDashboardController` | `huyDonHang` | `OrderViewService.huyDonHang` | `ROLE_KH` | Khách tự hủy đơn hàng chờ xác nhận |
| `POST`| `/user/don-hang/tra-hang/{id}` | `UserDashboardController` | `guiYeuCauTraHang` | `OrderViewService.yeuCauTraHang` | `ROLE_KH` | Gửi yêu cầu Đổi/Trả hàng trong 7 ngày |
| `POST`| `/user/address/create` | `UserAddressController` | `createAddress` | `UserAddressService` | `ROLE_KH` | Thêm địa chỉ mới vào sổ địa chỉ |
| `POST`| `/user/address/set-default/{id}` | `UserAddressController` | `setDefaultAddress`| `UserAddressService` | `ROLE_KH` | Đặt địa chỉ làm mặc định |

---

## 6. PHÂN HỆ QUẢN TRỊ ADMIN (ADMINISTRATION & POS)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/admin/pos` | `AdminPosController` | `hienThiTrangPos` | `AdminPosService` | `QL, NV` | Giao diện POS bán hàng tại quầy |
| `POST`| `/admin/pos/thanh-toan` | `AdminPosController` | `thanhToanPos` | `AdminPosService.thanhToanHoaDonPos` | `QL, NV` | Thanh toán hóa đơn POS tại quầy |
| `GET` | `/admin/pos/in-hoa-don/{id}` | `AdminPosController` | `inHoaDonPos` | `AdminPosService` | `QL, NV` | In hóa đơn nhiệt K80 |
| `GET` | `/admin/don-hang` | `AdminController` | `hienThiDanhSachDonHang` | `OrderViewService` | `QL, NV` | Quản lý danh sách đơn hàng |
| `GET` | `/admin/don-hang/detail-json` | `AdminController` | `getOrderDetailJson` | `OrderViewService` | `QL, NV` | Lấy chi tiết đơn hàng dạng JSON |
| `POST`| `/admin/don-hang/next-status` | `AdminController` | `moveOrderToNextStatus` | `OrderViewService` | `QL, NV` | Chuyển đơn sang trạng thái tiếp theo |
| `POST`| `/admin/don-hang/cancel-unpaid` | `AdminController` | `cancelOrderUnpaid` | `OrderViewService.cancelOrderUnpaid` | `QL, NV` | **Hủy đơn hàng chưa thanh toán (COD)** |
| `POST`| `/admin/don-hang/cancel-paid-refund` | `AdminController` | `cancelOrderPaidWithRefund` | `OrderViewService.cancelOrderPaidWithRefund` | `QL, NV` | **Hủy đơn Online đã thanh toán & Xác nhận hoàn tiền** |
| `POST`| `/admin/don-hang/update-status`| `AdminController`| `capNhatTrangThaiDonHang`| `OrderViewService` | `QL, NV` | Chuyển đổi trạng thái đơn hàng (Tương thích) |
| `POST`| `/admin/don-hang/approve-return`| `AdminController`| `approveReturn` | `OrderViewService.duyetYeuCauTraHangVaTaoDonGhn`| `QL, NV` | Duyệt đổi trả & tạo đơn thu hồi GHN |
| `POST`| `/admin/don-hang/confirm-restock` | `AdminController` | `confirmRestock` | `OrderViewService.xacNhanKiemKhoVaNhapKho` | `QL, NV` | Xác nhận kiểm kho (Hàng tốt vs Hàng lỗi) |
| `POST`| `/admin/don-hang/confirm-refund` | `AdminController` | `confirmRefund` | `OrderViewService.xacNhanHoanTienChoKhach` | `QL, NV` | Xác nhận hoàn tiền cho đơn trả hàng |
| `POST`| `/admin/don-hang/confirm-exchange-shipment` | `AdminController` | `confirmExchangeShipment` | `OrderViewService.xacNhanGiaoHangDoiMoiChoKhach` | `QL, NV` | Xác nhận giao sản phẩm đổi mới |
| `GET` | `/admin/thong-ke` | `AdminThongKeController` | `hienThiTrangThongKe` | `AdminThongKeService` | `ROLE_QL` | Dashboard biểu đồ doanh thu & báo cáo |
| `GET` | `/admin/thong-ke/xuat-excel` | `AdminThongKeController` | `xuatExcel` | `AdminThongKeService.generateExcelReport` | `ROLE_QL` | Xuất báo cáo doanh thu Excel POI |
| `GET` | `/admin/kho-san-pham-loi` | `AdminController` | `hienThiKhoSanPhamLoi` | `InventoryLotService` | `QL, NV` | Quản lý danh sách hàng lỗi |
| `POST`| `/admin/kho-san-pham-loi/xu-ly` | `AdminController` | `xuLyHangLoi` | `InventoryLotService.xuLyHangLoi` | `QL, NV` | Xử lý hàng lỗi (Trả NCC/Thanh lý/Hủy) |
| `GET` | `/admin/nhan-vien` | `AdminNhanVienController` | `danhSachNhanVien` | `AdminNhanVienService` | `ROLE_QL` | Quản lý danh sách nhân viên |
| `GET` | `/admin/moderation/keywords` | `CommentModerationAdminController`| `listKeywords` | `CommentModerationService` | `ROLE_QL` | Cấu hình từ khóa kiểm duyệt nội dung |

---

## 7. PHÂN HỆ REST APIS NGOÀI (EXTERNAL SERVICES & CHATBOT)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST`| `/api/ghn/webhook` | `GhnRestController` | `handleGhnWebhook` | `OrderViewService` | Public | Webhook cập nhật trạng thái bưu tá GHN |
| `GET` | `/api/ghn/provinces` | `GhnRestController` | `getProvinces` | `GhnService` | Public | Lấy danh mục 63 tỉnh/thành GHN |
| `GET` | `/api/ghn/districts/{id}` | `GhnRestController` | `getDistricts` | `GhnService` | Public | Lấy danh mục quận/huyện GHN |
| `GET` | `/api/ghn/wards/{id}` | `GhnRestController` | `getWards` | `GhnService` | Public | Lấy danh mục phường/xã GHN |
| `POST`| `/api/chatbot/chat` | `ChatbotRestController` | `chat` | `ChatbotServiceImpl` (Gemini 2.0 Flash) | Public | Gửi tin nhắn hỏi đáp Trợ lý ảo AI |
| `POST`| `/api/newsletter/subscribe` | `NewsletterApiController` | `subscribe` | `NewsletterServiceImpl` | Public | Đăng ký nhận bản tin khuyến mãi |

---
*Tài liệu Endpoint Map hoàn chỉnh của dự án SMASH-VN.*
