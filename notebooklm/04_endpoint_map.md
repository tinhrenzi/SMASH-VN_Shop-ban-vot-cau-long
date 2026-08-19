# 04. BẢN ĐỒ TOÀN BỘ ENDPOINTS (ENDPOINT MAP)

Dưới đây là danh mục tổng hợp toàn bộ các Route Endpoints trong hệ thống SMASH-VN (120+ Endpoints), được phân nhóm khoa học theo từng phân hệ chức năng:

---

## 1. PHÂN HỆ XÁC THỰC & NGƯỜI DÙNG (AUTHENTICATION & USER)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/user/dang-nhap` | `UserDangNhapController` | `hienThiTrangDangNhap` | N/A | Public | Hiển thị form đăng nhập thành viên |
| `POST`| `/user/dang-nhap` | `UserDangNhapController` | `xuLyDangNhap` | `UserDangNhapService` | Public | Xử lý đăng nhập username/password, kiểm tra khóa |
| `GET` | `/user/dang-ky` | `UserDangKyController` | `hienThiTrangDangKy` | N/A | Public | Hiển thị form đăng ký tài khoản |
| `POST`| `/user/dang-ky` | `UserDangKyController` | `xuLyDangKy` | `UserDangKyService` | Public | Đăng ký tài khoản mới, mã hóa BCrypt, kiểm tra trùng lặp |
| `GET` | `/user/google-success`| `UserDangNhapController` | `googleLoginSuccess` | `UserDangNhapService` | Public | Callback xử lý đăng nhập 1-click Google OAuth2 |
| `GET` | `/user/dang-xuat` | `UserDangNhapController` | `dangXuat` | N/A | Public | Đăng xuất phiên làm việc của người dùng |
| `GET` | `/user/quen-mat-khau`| `UserQuenMatKhauController`| `hienThiQuenMatKhau` | N/A | Public | Form yêu cầu khôi phục mật khẩu |
| `POST`| `/user/quen-mat-khau`| `UserQuenMatKhauController`| `xuLyQuenMatKhau` | `UserQuenMatKhauService` | Public | Tạo token khôi phục và gửi link qua Email |
| `GET` | `/user/dat-lai-mat-khau`| `UserQuenMatKhauController`| `hienThiDatLaiMatKhau` | `UserQuenMatKhauService` | Public | Form đặt mật khẩu mới từ link email token |
| `POST`| `/user/dat-lai-mat-khau`| `UserQuenMatKhauController`| `xuLyDatLaiMatKhau` | `UserQuenMatKhauService` | Public | Xác thực token và cập nhật mật khẩu mới vào DB |

---

## 2. PHÂN HỆ TRANG CHỦ & CỬA HÀNG (CATALOG & PRODUCTS)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service / Repository | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/` | `HomeController` | `hienThiTrangChu` | `SanPhamRepository`, `DotGiamGiaDAO` | Public | Trang chủ (Flash Sale, Hàng mới, Bán chạy, Nổi bật, Blog) |
| `GET` | `/shop` | `HomeController` | `hienThiCuaHang` | `SanPhamService`, `SanPhamSpecification` | Public | Cửa hàng: lọc danh mục, thương hiệu, giá, thuộc tính, sắp xếp |
| `GET` | `/gioi-thieu` | `HomeController` | `hienThiGioiThieu` | N/A | Public | Trang giới thiệu về hệ thống SMASH-VN |
| `GET` | `/lien-he` | `HomeController` | `hienThiLienHe` | N/A | Public | Trang thông tin liên hệ và gửi góp ý |
| `GET` | `/san-pham/{id}` | `SanPhamController` | `hienThiChiTietSanPham` | `SanPhamService`, `PricingService` | Public | Chi tiết sản phẩm, danh sách biến thể, thuộc tính EAV, review |
| `POST`| `/san-pham/{id}/danh-gia` | `SanPhamController` | `guiDanhGia` | `DanhGiaService`, `FileStorageService` | `ROLE_KH` | Gửi bài review số sao kèm ảnh, kiểm duyệt từ ngữ cấm |
| `GET` | `/wishlist` | `SanPhamYeuThichController` | `viewWishlist` | `SanPhamYeuThichService` | `ROLE_KH` | Trang danh sách sản phẩm yêu thích của khách hàng |
| `POST`| `/api/yeu-thich/toggle/{id}` | `SanPhamYeuThichController` | `toggleWishlist` | `SanPhamYeuThichService` | `ROLE_KH` | Bật / Tắt sản phẩm yêu thích (AJAX) |
| `GET` | `/api/yeu-thich/count` | `SanPhamYeuThichController` | `getCount` | `SanPhamYeuThichService` | `ROLE_KH` | Lấy số lượng món trong Wishlist hiển thị trên Header |
| `GET` | `/api/search/autocomplete` | `SearchApiController` | `autocomplete` | `SanPhamRepository.searchAutocomplete` | Public | Gợi ý tìm kiếm nhanh top 8 sản phẩm khi gõ từ khóa |
| `GET` | `/api/categories/{id}/attributes`| `CategoryAttributeRestController`| `getAttributesByCategory`| `ThuocTinhService` | Public | Lấy danh mục thuộc tính động theo danh mục sản phẩm |

---

## 3. PHÂN HỆ GIỎ HÀNG & CHECKOUT (CART & ORDER PLACEMENT)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/gio-hang` | `GioHangController` | `hienThiGioHang` | `GioHangService`, `GuestCartService` | Public | Hiển thị trang giỏ hàng `cart.html` |
| `POST`| `/gio-hang/them` | `GioHangController` | `xuLyThemVaoGio` | `GioHangService`, `GuestCartService` | Public | Thêm sản phẩm vào giỏ hàng (AJAX JSON) |
| `POST`| `/gio-hang/cap-nhat` | `GioHangController` | `capNhatSoLuong` | `GioHangService`, `GuestCartService` | Public | Tăng/Giảm số lượng món, kiểm tra tồn kho |
| `POST`| `/gio-hang/api/xoa/{id}` | `GioHangController` | `xoaSanPhamAjax` | `GioHangService`, `GuestCartService` | Public | Xóa 1 sản phẩm khỏi giỏ |
| `POST`| `/gio-hang/api/xoa-nhieu` | `GioHangController` | `xoaNhieuSanPhamAjax` | `GioHangService`, `GuestCartService` | Public | Xóa nhiều sản phẩm được tích chọn |
| `GET` | `/gio-hang/api/mini-cart` | `GioHangController` | `layDuLieuMiniCart` | `GioHangService`, `GuestCartService` | Public | Trả về dữ liệu JSON cập nhật Mini Cart trên Header |
| `POST`| `/checkout/start` | `CheckoutController` | `startCheckout` | `CheckoutContextService` | Public | Bắt đầu checkout danh sách món đã chọn trong giỏ |
| `POST`| `/checkout/buy-now` | `CheckoutController` | `buyNow` | `CheckoutContextService` | Public | Bắt đầu checkout nhanh cho luồng Mua Ngay 1 món |
| `POST`| `/checkout/start-all` | `CheckoutController` | `startCheckoutAll` | `CheckoutContextService` | Public | Bắt đầu checkout toàn bộ sản phẩm trong giỏ |
| `GET` | `/checkout` | `CheckoutController` | `viewCheckout` | `CheckoutContextService`, `ShippingFeeCalculator` | Public | Render trang đặt hàng `checkout.html` |
| `POST`| `/checkout/submit` | `CheckoutController` | `submitCheckout` | `GioHangService`, `InventoryLotService`, `GhnService` | Public | **Xác nhận đặt hàng chính thức (COD / SePay QR)** |
| `POST`| `/api/voucher/apply` | `CheckoutController` | `applyVoucher` | `VoucherCalculator`, `PhieuGiamGiaRepository` | Public | Kiểm tra điều kiện và tính số tiền giảm của Voucher |
| `GET` | `/checkout/success` | `CheckoutController` | `orderSuccess` | `OrderViewService` | Public | Trang hiển thị thông báo đặt hàng thành công |
| `POST`| `/api/shipping/calculate-fee` | `ShippingApiController` | `calculateFee` | `ShippingFeeCalculator` | Public | Tính toán phí vận chuyển GHN thời gian thực |

---

## 4. PHÂN HỆ CỔNG THANH TOÁN SEPAY (SEPAY PAYMENT GATEWAY)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST`| `/api/payment/sepay/ipn` | `SepayIpnController` | `handleSepayIpn` | `SepayGatewayService`, `SepayOrderPaymentService` | SePay Secret | **Webhook IPN nhận thông báo tiền về từ SePay** |
| `GET` | `/api/payment/sepay/query/{code}` | `SepayIpnController` | `queryTransaction` | `SepayGatewayService` | Public | Frontend Polling kiểm tra trạng thái đơn hàng đã thanh toán |
| `GET` | `/admin/sepay-simulate` | `SepaySimulationController`| `showSimulationPage`| N/A | `ROLE_QL` | Giao diện giả lập bắn Webhook SePay môi trường Dev |
| `POST`| `/api/payment/sepay/simulate` | `SepaySimulationController`| `simulatePayment` | `SepayGatewayService` | `ROLE_QL` | Thực thi giả lập Webhook SePay gửi dữ liệu test |

---

## 5. PHÂN HỆ KHÁCH HÀNG & DASHBOARD (USER DASHBOARD & RMA)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/dashboard` | `UserDashboardController` | `hienThiDashboard` | `UserDashboardService`, `OrderViewService` | `ROLE_KH` | Trang quản lý tài khoản cá nhân, đơn hàng, sổ địa chỉ |
| `GET` | `/user/don-hang/{id}` | `UserDashboardController` | `xemChiTietDonHang` | `OrderViewService` | `ROLE_KH` | Xem chi tiết timeline và hóa đơn đơn mua |
| `POST`| `/user/don-hang/huy/{id}` | `UserDashboardController` | `huyDonHang` | `OrderViewService.huyDonHang` | `ROLE_KH` | Khách tự hủy đơn hàng đang ở trạng thái chờ |
| `POST`| `/user/don-hang/tra-hang/{id}` | `UserDashboardController` | `guiYeuCauTraHang` | `OrderViewService`, `FileStorageService` | `ROLE_KH` | **Gửi yêu cầu Đổi/Trả hàng trong 7 ngày (Upload ảnh + video)** |
| `POST`| `/user/cap-nhat-thong-tin` | `UserDashboardController` | `capNhatThongTin` | `UserDashboardService` | `ROLE_KH` | Cập nhật họ tên, số điện thoại, giới tính, ngày sinh |
| `POST`| `/user/doi-mat-khau` | `UserDashboardController` | `doiMatKhau` | `UserDashboardService` | `ROLE_KH` | Đổi mật khẩu đăng nhập tài khoản |
| `POST`| `/user/address/create` | `UserAddressController` | `createAddress` | `UserAddressService` | `ROLE_KH` | Thêm mới địa chỉ nhận hàng vào sổ địa chỉ |
| `POST`| `/user/address/update/{id}` | `UserAddressController` | `updateAddress` | `UserAddressService` | `ROLE_KH` | Cập nhật thông tin địa chỉ trong sổ |
| `POST`| `/user/address/delete/{id}` | `UserAddressController` | `deleteAddress` | `UserAddressService` | `ROLE_KH` | Xóa địa chỉ khỏi sổ |
| `POST`| `/user/address/set-default/{id}`| `UserAddressController` | `setDefaultAddress`| `UserAddressService` | `ROLE_KH` | Đặt địa chỉ làm địa chỉ mặc định khi thanh toán |

---

## 6. PHÂN HỆ QUẢN TRỊ ADMIN - ĐƠN HÀNG, RMA & KHO HÀNG LỖI

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/admin/don-hang` | `AdminController` | `hienThiDanhSachDonHang` | `OrderViewService` | `QL, NV` | Quản lý danh sách đơn hàng, lọc đa tiêu chí |
| `GET` | `/admin/don-hang/detail-json` | `AdminController` | `getOrderDetailJson` | `OrderViewService` | `QL, NV` | Trả về chi tiết đơn hàng dạng JSON cho modal |
| `POST`| `/admin/don-hang/next-status` | `AdminController` | `moveOrderToNextStatus` | `OrderViewService` | `QL, NV` | Chuyển đơn sang trạng thái tiếp theo theo quy trình |
| `POST`| `/admin/don-hang/cancel-unpaid` | `AdminController` | `cancelOrderUnpaid` | `OrderViewService.cancelOrderUnpaid` | `QL, NV` | **Hủy đơn hàng chưa thanh toán (COD / Chờ TT)** |
| `POST`| `/admin/don-hang/cancel-paid-refund`| `AdminController`| `cancelOrderPaidWithRefund`| `OrderViewService.cancelOrderPaidWithRefund`| `QL, NV` | **Hủy đơn Online đã thanh toán & Xác nhận hoàn tiền (Upload chứng từ)** |
| `POST`| `/admin/don-hang/approve-return`| `AdminController` | `approveReturn` | `OrderViewService.duyetYeuCauTraHangVaTaoDonGhn`| `QL, NV` | **Duyệt đổi trả & tạo đơn thu hồi GHN_RETURN** |
| `POST`| `/admin/don-hang/reject-return` | `AdminController` | `rejectReturn` | `OrderViewService.tuChoiYeuCauTraHang` | `QL, NV` | Từ chối yêu cầu đổi trả của khách hàng |
| `POST`| `/admin/don-hang/confirm-restock`| `AdminController` | `confirmRestock` | `OrderViewService.xacNhanKiemKhoVaNhapKho` | `QL, NV` | **Xác nhận kiểm kho (Phân loại Hàng tốt vs Hàng lỗi `soLuongSpLoi`)** |
| `POST`| `/admin/don-hang/confirm-refund` | `AdminController` | `confirmRefund` | `OrderViewService.xacNhanHoanTienChoKhach` | `QL, NV` | **Xác nhận chuyển khoản hoàn tiền cho đơn trả hàng** |
| `POST`| `/admin/don-hang/confirm-exchange-shipment`| `AdminController`| `confirmExchangeShipment`| `OrderViewService.xacNhanGiaoHangDoiMoiChoKhach`| `QL, NV`| Xác nhận gửi sản phẩm đổi mới qua GHN_EXCHANGE |
| `GET` | `/admin/kho-san-pham-loi` | `AdminController` | `hienThiKhoSanPhamLoi` | `InventoryLotService` | `QL, NV` | Giao diện quản lý danh sách sản phẩm bị lỗi |
| `GET` | `/admin/kho-san-pham-loi/{id}` | `AdminController` | `xemChiTietKhoLoi` | `InventoryLotService` | `QL, NV` | Xem nguồn gốc đơn hàng lỗi và lịch sử xử lý |
| `POST`| `/admin/kho-san-pham-loi/{id}/xu-ly`| `AdminController`| `xuLyHangLoi` | `InventoryLotService.xuLyHangLoi` | `QL, NV` | **Xử lý hàng lỗi (Xuất trả NCC, Thanh lý, Tiêu hủy)** |
| `GET` | `/admin/khach-hang` | `AdminController` | `danhSachKhachHang` | `AdminKhachHangService` | `QL, NV` | Quản lý danh sách tài khoản khách hàng |
| `POST`| `/admin/khach-hang/them` | `AdminController` | `themKhachHang` | `AdminKhachHangService` | `QL, NV` | Tạo tài khoản khách hàng thủ công |
| `POST`| `/admin/khach-hang/sua/{id}` | `AdminController` | `suaKhachHang` | `AdminKhachHangService` | `QL, NV` | Sửa thông tin tài khoản khách hàng |

---

## 7. PHÂN HỆ QUẢN TRỊ ADMIN - SẢN PHẨM, LÔ NHẬP KHO & KHUYẾN MÃI

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/admin/san-pham` | `AdminSanPhamController` | `danhSachSanPham` | `AdminSanPhamService` | `QL, NV` | Quản lý danh sách sản phẩm |
| `GET` | `/admin/san-pham/them` | `AdminSanPhamController` | `formThemSanPham` | `AdminSanPhamService` | `QL, NV` | Form thêm mới sản phẩm cha |
| `POST`| `/admin/san-pham/them` | `AdminSanPhamController` | `themSanPham` | `AdminSanPhamService` | `QL, NV` | Lưu sản phẩm cha mới vào DB |
| `GET` | `/admin/san-pham/sua/{id}` | `AdminSanPhamController` | `formSuaSanPham` | `AdminSanPhamService` | `QL, NV` | Form chỉnh sửa sản phẩm cha và quản lý biến thể con |
| `POST`| `/admin/san-pham/sua/{id}` | `AdminSanPhamController` | `suaSanPham` | `AdminSanPhamService` | `QL, NV` | Cập nhật thông tin sản phẩm cha |
| `POST`| `/admin/san-pham/{id}/bien-the/them`| `AdminBienTheController` | `themBienThe` | `AdminBienTheService` | `QL, NV` | Thêm mới biến thể sản phẩm |
| `POST`| `/admin/san-pham/{id}/bien-the/sua/{idBt}`| `AdminBienTheController`| `suaBienThe` | `AdminBienTheService` | `QL, NV` | Cập nhật giá bán, SKU, thuộc tính biến thể |
| `POST`| `/admin/san-pham/{id}/bien-the/nhap-lo`| `AdminBienTheController`| `nhapKhoTheoLo` | `InventoryLotService.nhapKho` | `QL, NV` | **Nhập hàng theo lô FIFO (Tạo PhieuNhap & PhieuNhapChiTiet)** |
| `GET` | `/admin/san-pham/{id}/bien-the/{idBt}/lich-su-nhap`| `AdminBienTheController`| `layLichSuNhap`| `InventoryLotService`| `QL, NV`| Lấy lịch sử các lô nhập của biến thể (JSON) |
| `GET` | `/admin/san-pham/phieu-nhap/{idPn}`| `AdminSanPhamController`| `xemChiTietPhieuNhap`| `InventoryLotService`| `QL, NV`| Xem chi tiết toàn bộ phiếu nhập kho |
| `GET` | `/admin/danh-muc` | `AdminDanhMucController` | `danhSachDanhMuc` | `DanhMucService`, `ThuongHieuService` | `QL, NV` | Quản lý danh mục, thương hiệu, thuộc tính |
| `POST`| `/admin/danh-muc/them` | `AdminDanhMucController` | `themDanhMuc` | `DanhMucService` | `QL, NV` | Thêm danh mục sản phẩm mới |
| `POST`| `/admin/danh-muc/sua/{id}` | `AdminDanhMucController` | `suaDanhMuc` | `DanhMucService` | `QL, NV` | Cập nhật tên, icon danh mục |
| `POST`| `/admin/danh-muc/thuong-hieu/them`| `AdminDanhMucController` | `themThuongHieu` | `ThuongHieuService` | `QL, NV` | Thêm thương hiệu mới |
| `GET` | `/admin/khuyen-mai` | `AdminController` | `hienThiKhuyenMai` | `AdminKhuyenMaiService` | `QL, NV` | Danh sách đợt giảm giá & phiếu giảm giá |
| `GET` | `/admin/khuyen-mai/dot-giam-gia/them`| `AdminKhuyenMaiController`| `formThemDotGiamGia`| `AdminKhuyenMaiService`| `QL, NV`| Form tạo đợt giảm giá trực tiếp |
| `POST`| `/admin/khuyen-mai/dot-giam-gia/them`| `AdminKhuyenMaiController`| `themDotGiamGia` | `AdminKhuyenMaiService`| `QL, NV`| Lưu đợt giảm giá mới |
| `GET` | `/admin/khuyen-mai/phieu-giam-gia/them`| `AdminKhuyenMaiController`| `formThemPhieuGiamGia`| `AdminKhuyenMaiService`| `QL, NV`| Form tạo phiếu giảm giá / Voucher |
| `POST`| `/admin/khuyen-mai/phieu-giam-gia/them`| `AdminKhuyenMaiController`| `themPhieuGiamGia`| `AdminKhuyenMaiService`| `QL, NV`| Lưu voucher mới vào DB |

---

## 8. PHÂN HỆ BÁN HÀNG TẠI QUẦY (POS COUNTER)

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/admin/pos` | `AdminPosController` | `hienThiTrangPos` | `AdminPosService` | `QL, NV` | Giao diện POS bán hàng tại quầy |
| `GET` | `/admin/pos/search-products` | `AdminPosController` | `searchProducts` | `AdminPosService.timKiemBienTheChoPos` | `QL, NV` | Quét Barcode/SKU tìm biến thể siêu tốc |
| `GET` | `/admin/pos/search-customers`| `AdminPosController` | `searchCustomers` | `AdminKhachHangService` | `QL, NV` | Tìm kiếm khách hàng theo SĐT |
| `GET` | `/admin/pos/check-voucher` | `AdminPosController` | `checkVoucher` | `AdminPosService.kiemTraVoucherPos` | `QL, NV` | Kiểm tra và áp dụng voucher tại quầy |
| `POST`| `/admin/pos/checkout` | `AdminPosController` | `thanhToanPos` | `AdminPosService.thanhToanHoaDonPos` | `QL, NV` | **Thanh toán hóa đơn quầy (Tiền mặt / SePay QR, Trừ kho FIFO)** |
| `GET` | `/admin/pos/print/{id}` | `AdminPosController` | `inHoaDonPos` | `AdminPosService` | `QL, NV` | Xuất bản in hóa đơn nhiệt chuẩn K80 |
| `POST`| `/admin/pos/cancel-pending-order/{id}`| `AdminPosController`| `cancelPendingOrder`| `AdminPosService.huyDonHangChoPos`| `QL, NV`| Hủy bỏ tab hóa đơn chờ |

---

## 9. PHÂN HỆ QUẢN TRỊ ADMIN - BÁO CÁO, NHÂN VIÊN, GIAO DỊCH & CMS

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/admin/thong-ke` | `AdminThongKeController` | `hienThiTrangThongKe` | `AdminThongKeService` | `ROLE_QL` | Dashboard biểu đồ doanh thu, cơ cấu sản phẩm bán chạy |
| `GET` | `/admin/thong-ke/api` | `AdminThongKeController` | `layDuLieuThongKeApi`| `AdminThongKeService` | `ROLE_QL` | Lấy dữ liệu thống kê JSON cho biểu đồ Chart.js |
| `GET` | `/admin/thong-ke/export` | `AdminThongKeController` | `xuatExcelBaoCao` | `AdminThongKeService.generateExcelReport` | `ROLE_QL` | Xuất file Excel báo cáo doanh thu chi tiết |
| `GET` | `/admin/nhan-vien` | `AdminNhanVienController` | `danhSachNhanVien` | `AdminNhanVienService` | `ROLE_QL` | Quản lý danh sách tài khoản nhân viên |
| `POST`| `/admin/nhan-vien/them` | `AdminNhanVienController` | `themNhanVien` | `AdminNhanVienService` | `ROLE_QL` | Tạo tài khoản nhân viên mới |
| `POST`| `/admin/nhan-vien/sua/{id}` | `AdminNhanVienController` | `suaNhanVien` | `AdminNhanVienService` | `ROLE_QL` | Chỉnh sửa thông tin/chức vụ nhân viên |
| `POST`| `/admin/nhan-vien/toggle/{id}` | `AdminNhanVienController` | `toggleTrangThai` | `AdminNhanVienService` | `ROLE_QL` | Khóa / Mở khóa tài khoản nhân viên |
| `GET` | `/admin/transactions` | `AdminPaymentTransactionController`| `listTransactions`| `PaymentTransactionRepository`| `ROLE_QL`| Quản lý danh sách giao dịch SePay & Hoàn tiền |
| `GET` | `/admin/transactions/export/excel`| `AdminPaymentTransactionController`| `exportExcel`| `AdminPaymentTransactionController`| `ROLE_QL`| Xuất lịch sử giao dịch ra file Excel |
| `GET` | `/admin/moderation/keywords` | `CommentModerationAdminController`| `listKeywords` | `CommentModerationService` | `ROLE_QL` | Cấu hình từ điển từ khóa cấm/thô tục |
| `POST`| `/admin/moderation/keywords/add`| `CommentModerationAdminController`| `addKeyword` | `CommentModerationService` | `ROLE_QL` | Thêm từ khóa cấm mới |
| `POST`| `/admin/moderation/keywords/delete/{id}`| `CommentModerationAdminController`| `deleteKeyword`| `CommentModerationService`| `ROLE_QL`| Xóa từ khóa cấm |
| `GET` | `/admin/danh-gia` | `AdminDanhGiaController` | `danhSachDanhGia` | `DanhGiaService` | `QL, NV` | Quản lý và duyệt bài đánh giá của khách hàng |
| `POST`| `/admin/danh-gia/an-binh-luan/{id}`| `AdminDanhGiaController`| `anBinhLuan` | `DanhGiaService` | `QL, NV` | Ẩn bình luận vi phạm |
| `GET` | `/admin/blog` | `AdminBlogController` | `listBlogs` | `BlogService` | `ROLE_QL` | Quản lý danh sách bài viết blog |
| `POST`| `/admin/blog/add` | `AdminBlogController` | `addBlog` | `BlogService` | `ROLE_QL` | Thêm bài viết mới (Nội dung CKEditor) |
| `POST`| `/admin/blog/publish/{id}` | `AdminBlogController` | `publishBlog` | `BlogService` | `ROLE_QL` | Duyệt xuất bản bài viết |

---

## 10. PHÂN HỆ REST APIS NGOÀI, GHN & TRỢ LÝ ẢO AI CHATBOT

| HTTP | URL Pattern | Controller | Method Xử Lý | Service Được Gọi | Quyền Hạn | Chức Năng Chi Tiết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST`| `/api/ghn/webhook` | `GhnRestController` | `handleGhnWebhook` | `OrderViewService`, `GhnStatusMapper` | Public | Webhook tiếp nhận trạng thái bưu tá từ GHN |
| `GET` | `/api/ghn/provinces` | `GhnRestController` | `getProvinces` | `GhnService.getProvinces` | Public | Lấy danh mục 63 Tỉnh/Thành phố từ GHN |
| `GET` | `/api/ghn/districts/{id}` | `GhnRestController` | `getDistricts` | `GhnService.getDistricts` | Public | Lấy danh mục Quận/Huyện theo Tỉnh |
| `GET` | `/api/ghn/wards/{id}` | `GhnRestController` | `getWards` | `GhnService.getWards` | Public | Lấy danh mục Phường/Xã theo Huyện |
| `GET` | `/api/ghn/track-order/{code}` | `GhnRestController` | `trackOrder` | `GhnService.trackOrder` | Public | Tra cứu hành trình bưu tá GHN theo mã vận đơn |
| `POST`| `/api/chatbot/chat` | `ChatbotRestController` | `chat` | `ChatbotServiceImpl` (Gemini 2.0 Flash) | Public | **Gửi tin nhắn hỏi đáp Trợ lý ảo AI (Kiến trúc RAG)** |
| `POST`| `/api/chatbot/feedback` | `ChatbotRestController` | `submitFeedback` | `ChatbotServiceImpl` | Public | Khách gửi feedback Like/Dislike câu trả lời AI |
| `GET` | `/api/chatbot/products/recommend`| `ChatbotProductsRestController`| `getRecommendations`| `SanPhamService` | Public | Lấy thẻ sản phẩm gợi ý đính kèm tin nhắn chat |
| `GET` | `/api/location/detect` | `LocationRestController` | `detectLocation` | `LocationService` | Public | Tự động định vị Tỉnh/Thành theo IP khách |
| `POST`| `/api/newsletter/subscribe` | `NewsletterApiController` | `subscribe` | `NewsletterServiceImpl` | Public | Đăng ký nhận bản tin khuyến mãi |
| `POST`| `/api/newsletter/unsubscribe` | `NewsletterApiController` | `unsubscribe` | `NewsletterServiceImpl` | Public | Hủy đăng ký nhận bản tin khuyến mãi |

---
*Tài liệu Endpoint Map hoàn chỉnh của dự án SMASH-VN.*
