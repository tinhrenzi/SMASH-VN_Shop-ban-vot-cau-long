# 06. CHỈ MỤC FILE & METHOD NGHIỆP VỤ (FILE INDEX)

Tài liệu này đóng vai trò từ điển tra cứu nhanh: **"Chỉ cần biết tên file là biết file đó đang làm gì trong hệ thống, chứa những method nào và tương tác với những file nào"**.

---

## 1. DANH MỤC CONTROLLERS TRỌNG YẾU

### `CheckoutController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/order/CheckoutController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng:** Điều phối toàn bộ quy trình checkout, áp dụng voucher, mua ngay và đặt hàng.
- **Method quan trọng:**
  - `startCheckout()`: Bắt đầu checkout các món đã chọn trong giỏ hàng.
  - `buyNow()`: Bắt đầu checkout Mua Ngay 1 sản phẩm từ trang chi tiết.
  - `startCheckoutAll()`: Bắt đầu checkout toàn bộ giỏ hàng.
  - `viewCheckout()`: Render giao diện `checkout.html`, nạp sổ địa chỉ và tính phí ship GHN.
  - `submitCheckout()`: Tiếp nhận form đặt hàng, phân nhánh COD / SePay, xử lý khách vãng lai Guest.
  - `applyVoucher()`: Kiểm tra tính hợp lệ và trả về số tiền giảm của Voucher.
- **Gọi tới:** `GioHangService`, `CheckoutContextService`, `GuestCheckoutService`, `ShippingFeeCalculator`, `VoucherCalculator`.
- **Được gọi từ:** `cart.html`, `product-detail.html`, `checkout.html`, `checkout.js`.

### `GioHangController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/order/GioHangController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng:** Quản lý giỏ hàng kép (Thành viên lưu DB & Khách vãng lai lưu Session).
- **Method quan trọng:**
  - `hienThiGioHang()`: Render trang `cart.html`.
  - `xuLyThemVaoGio()`: Thêm sản phẩm vào giỏ (AJAX).
  - `capNhatSoLuong()`: Thay đổi số lượng (+ / -) và kiểm tra tồn kho.
  - `xoaSanPhamAjax()` / `xoaNhieuSanPhamAjax()`: Xóa 1 hoặc nhiều món khỏi giỏ.
  - `layDuLieuMiniCart()`: Trả về JSON cập nhật dropdown giỏ hàng trên Header.
- **Gọi tới:** `GioHangService`, `GuestCartService`.

### `AdminController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/admin/AdminController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng:** Quản lý danh sách đơn hàng, chuyển trạng thái, xử lý RMA đổi trả, quản lý kho hàng lỗi.
- **Method quan trọng:**
  - `hienThiDanhSachDonHang()`: Xem danh sách và lọc đơn hàng theo nhiều tiêu chí.
  - `getOrderDetailJson()`: Trả về chi tiết đơn hàng dạng JSON cho modal xem nhanh.
  - `cancelOrderUnpaid()`: Hủy đơn hàng chưa thanh toán (COD / Chờ TT).
  - `cancelOrderPaidWithRefund()`: Hủy đơn Online đã thanh toán và ghi nhận chứng từ hoàn tiền.
  - `approveReturn()`: Duyệt yêu cầu đổi trả và tự động tạo đơn thu hồi `GHN_RETURN`.
  - `confirmRestock()`: Xác nhận kiểm kho phân loại Hàng tốt (hoàn kho bán) vs Hàng lỗi (`soLuongSpLoi`).
  - `confirmRefund()`: Xác nhận hoàn tiền chuyển khoản cho khách đổi trả.
  - `confirmExchangeShipment()`: Xác nhận giao sản phẩm đổi mới qua `GHN_EXCHANGE`.
  - `hienThiKhoSanPhamLoi()` / `xemChiTietKhoLoi()` / `xuLyHangLoi()`: Quản lý và xử lý kho hàng lỗi.
- **Gọi tới:** `OrderViewService`, `InventoryLotService`, `GhnService`.

### `AdminPosController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/admin/AdminPosController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng:** Bán hàng tại quầy POS, quét barcode, quản lý đa tab hóa đơn chờ, in hóa đơn nhiệt K80.
- **Method quan trọng:**
  - `hienThiTrangPos()`: Render giao diện POS tại quầy.
  - `searchProducts()`: Tìm kiếm biến thể siêu nhanh theo Barcode / SKU / Tên.
  - `searchCustomers()`: Tìm kiếm khách hàng theo SĐT.
  - `checkVoucher()`: Áp dụng voucher giảm giá tại quầy.
  - `thanhToanPos()`: Thực hiện thanh toán tiền mặt / SePay QR, trừ kho FIFO ngay.
  - `inHoaDonPos()`: Render template in hóa đơn nhiệt K80.
  - `cancelPendingOrder()`: Hủy và giải phóng tab hóa đơn chờ.
- **Gọi tới:** `AdminPosService`, `AdminKhachHangService`.

### `AdminBienTheController.java` & `AdminSanPhamController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/admin/`
- **Chức năng:** Quản lý danh sách sản phẩm cha, thêm sửa biến thể, nhập kho theo lô FIFO và xem lịch sử phiếu nhập.
- **Method quan trọng:**
  - `nhapKhoTheoLo()`: Tạo phiếu nhập `PhieuNhap` và lô chi tiết `PhieuNhapChiTiet`, tăng `soLuongTon`.
  - `layLichSuNhap()`: Lấy lịch sử các lô nhập của biến thể.
  - `xemChiTietPhieuNhap()`: Xem chi tiết toàn bộ phiếu nhập kho.
- **Gọi tới:** `AdminSanPhamService`, `AdminBienTheService`, `InventoryLotService`.

### `SepayIpnController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/payment/SepayIpnController.java`
- **Loại:** REST Controller
- **Chức năng:** Tiếp nhận Webhook IPN từ cổng thanh toán SePay và tra cứu giao dịch.
- **Method quan trọng:**
  - `handleSepayIpn()`: Nhận Webhook POST, kiểm tra IP whitelist, verify Apikey secret, xử lý thanh toán.
  - `queryTransaction()`: Trả về trạng thái giao dịch cho frontend polling.
- **Gọi tới:** `SepayGatewayService`, `SepayOrderPaymentService`.

### `ChatbotRestController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/api/ChatbotRestController.java`
- **Loại:** REST Controller
- **Chức năng:** Giao tiếp với Trợ lý ảo AI Gemini 2.0 Flash theo kiến trúc RAG.
- **Method quan trọng:**
  - `chat()`: Tiếp nhận tin nhắn, bóc tách giá/thương hiệu, truy vấn DB và gọi LLM.
  - `submitFeedback()`: Ghi nhận đánh giá Like/Dislike của khách.
- **Gọi tới:** `ChatbotServiceImpl`.

### `GhnRestController.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/controller/api/GhnRestController.java`
- **Loại:** REST Controller
- **Chức năng:** Cung cấp API danh mục địa giới hành chính GHN, Webhook bưu tá và tra cứu lộ trình.
- **Method quan trọng:**
  - `handleGhnWebhook()`: Nhận cập nhật trạng thái bưu tá realtime từ GHN.
  - `getProvinces()` / `getDistricts()` / `getWards()`: Lấy danh mục Tỉnh / Huyện / Xã.
  - `trackOrder()`: Tra cứu hành trình kiện hàng.
- **Gọi tới:** `GhnService`, `OrderViewService`.

---

## 2. DANH MỤC SERVICES TRỌNG YẾU

### `GioHangService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/order/GioHangService.java`
- **Chức năng:** Quản lý giỏ hàng database, khởi tạo đơn hàng từ checkout, tạo đơn COD, tạo đơn SePay pending.
- **Method quan trọng:**
  - `createOrderFromCheckout()`: Khóa biến thể, tính đơn giá, trừ voucher, lưu HoaDon.
  - `submitCodOrder()`: Khởi tạo đơn COD trạng thái `CHO_XAC_NHAN`, tạo đơn GHN, gửi mail.
  - `createSepayPendingOrder()`: Lưu đơn chờ quét QR, đăng ký Snapshot vào `PendingCheckoutRegistry`.

### `OrderViewService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/order/OrderViewService.java`
- **Chức năng:** Quản lý toàn bộ vòng đời đơn hàng, hủy đơn, hoàn kho, quy trình đổi trả hàng RMA, kiểm kho, hoàn tiền.
- **Method quan trọng:**
  - `cancelOrderUnpaid()`: Hủy đơn COD chưa thanh toán, chỉ hoàn kho nếu đơn đã ở `DA_XAC_NHAN`, hủy đơn GHN.
  - `cancelOrderPaidWithRefund()`: Hủy đơn online đã thanh toán, ghi nhận chứng từ hoàn tiền, hoàn kho FIFO, hủy đơn GHN, tạo transaction `ORDER_CANCEL_REFUND`.
  - `yeuCauTraHang()`: Tiếp nhận yêu cầu đổi trả kèm hình ảnh và video bằng chứng.
  - `duyetYeuCauTraHangVaTaoDonGhn()`: Tạo đơn thu hồi `GHN_RETURN`.
  - `xacNhanKiemKhoVaNhapKho()`: Phân loại hàng tốt (hoàn kho bán) vs hàng lỗi (nhập Kho Hàng Lỗi `soLuongSpLoi`).
  - `xacNhanHoanTienChoKhach()`: Xác nhận hoàn tiền, cập nhật `RefundStatus = COMPLETED`.
  - `xacNhanGiaoHangDoiMoiChoKhach()`: Tạo đơn `GHN_EXCHANGE` gửi hàng mới cho khách.

### `InventoryLotService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/inventory/InventoryLotService.java`
- **Chức năng:** Thuật toán phân bổ tồn kho FIFO 2 giai đoạn, nhập kho theo lô, quản lý và xử lý kho hàng lỗi.
- **Method quan trọng:**
  - `nhapKho()`: Tạo `PhieuNhap` và `PhieuNhapChiTiet`, cập nhật `soLuongTon`.
  - `allocateFifo()`: Khóa sản phẩm cha theo ID ASC, lập kế hoạch phân bổ, trừ `soLuongTonLo` theo `ngayNhap ASC`.
  - `hoanKho()`: Hoàn lại số lượng tồn kho vào các lô ban đầu khi hủy đơn hoặc kiểm kho hàng tốt.
  - `layDanhSachKhoSanPhamLoi()` / `layChiTietKhoSanPhamLoi()`: Tra cứu hàng lỗi.
  - `xuLyHangLoi()`: Xử lý xuất trả NCC, thanh lý hoặc tiêu hủy, ghi nhật ký `EditLog`.

### `SepayOrderPaymentService.java` & `SepayGatewayService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/payment/`
- **Chức năng:** Xử lý xác nhận thanh toán SePay Webhook IPN trong 1 Transaction kín.
- **Method quan trọng:**
  - `handleIpn()`: Verify chữ ký, Idempotency guard, kiểm tra số tiền.
  - `xuLyThanhToanSePay()`: Phân bổ tồn kho FIFO thực tế, cập nhật đơn hàng sang ĐÃ THANH TOÁN, trừ voucher, xử lý thiếu kho `PAID_INSUFFICIENT_STOCK`.

### `GhnService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/api/GhnService.java`
- **Chức năng:** Tích hợp toàn bộ HTTP API của Giao Hàng Nhanh v2.
- **Method quan trọng:**
  - `calculateShipFee()`: Gọi API tính phí vận chuyển theo bưu cục và khối lượng.
  - `createShippingOrder()`: Gọi API tạo vận đơn giao hàng chính thức (`GHN`).
  - `createReturnOrder()`: Gọi API tạo đơn thu hồi đổi trả (`GHN_RETURN`).
  - `trackOrder()`: Tra cứu hành trình vận đơn bưu tá realtime.
  - `cancelOrder()`: Hủy vận đơn GHN khi đơn hàng bị hủy.

### `AdminPosService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/admin/AdminPosService.java`
- **Chức năng:** Xử lý nghiệp vụ POS bán hàng tại quầy, quản lý giỏ hàng tại quầy, thanh toán tiền mặt/QR, xuất hóa đơn K80.

### `AdminThongKeService.java`
- **Đường dẫn:** `src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java`
- **Chức năng:** Phân loại doanh thu thực tế vs tạm tính vs giảm trừ/hoàn tiền, vẽ biểu đồ Chart.js và xuất file Excel POI chuyên nghiệp.

---

## 3. DANH MỤC REPOSITORIES TRỌNG YẾU

### `SanPhamRepository.java`
- `findNewProducts(Pageable)`: Lấy sản phẩm mới (Còn hàng trước, `sp.id DESC`).
- `findBestSellers(Pageable)`: Lấy sản phẩm bán chạy (Còn hàng trước, `SUM(hdct.soLuong) DESC`).
- `findFeaturedProducts(Pageable)`: Lấy sản phẩm nổi bật (Còn hàng trước, `(SUM(hdct) + COUNT(wishlist)) DESC`).
- `searchAutocomplete(keyword, Pageable)`: Gợi ý tìm kiếm nhanh top 8 sản phẩm.
- `findByIdWithLock(id)`: Khóa `PESSIMISTIC_WRITE` trên SanPham cha khi phân bổ FIFO.

### `SanPhamChiTietRepository.java`
- `findByIdWithLock(id)`: Khóa `PESSIMISTIC_WRITE` trên biến thể khi kiểm tra tồn kho.
- `searchForChatbot(...)`: Truy vấn đa tiêu chí kết hợp thuộc tính EAV cho Trợ lý ảo AI.
- `findBySoLuongSpLoiGreaterThanOrderBySoLuongSpLoiDesc()`: Lấy danh sách biến thể có hàng lỗi.

### `HoaDonRepository.java`
- `findByMaDonHang(maDonHang)`: Tra cứu hóa đơn theo mã đơn.
- `findByIdWithLock(id)`: Khóa `PESSIMISTIC_WRITE` trên đơn hàng khi nhận Webhook IPN hoặc Hủy đơn.
- `findActiveShippingOrders(Pageable)`: Native query tìm đơn GHN cần Polling đồng bộ trạng thái.
- `findRawTransactionsInPeriod(...)`: Lấy danh sách giao dịch phục vụ thống kê doanh thu.

### `PhieuNhapRepository.java` & `PhieuNhapChiTietRepository.java`
- `findBySanPhamChiTietIdAndSoLuongTonLoGreaterThanOrderByNgayNhapAsc()`: Lấy các lô còn hàng xếp theo ngày nhập tăng dần phục vụ xuất kho FIFO.

### `PaymentTransactionRepository.java`
- `findByTransactionId(maGiaoDich)`: Tra cứu giao dịch đảm bảo Idempotency.
- `findByHoaDonIdOrderByNgayTaoDesc(hoaDonId)`: Lấy lịch sử toàn bộ giao dịch thanh toán & hoàn tiền của đơn hàng.

---
*Tài liệu Chỉ mục File hoàn chỉnh của dự án SMASH-VN.*
