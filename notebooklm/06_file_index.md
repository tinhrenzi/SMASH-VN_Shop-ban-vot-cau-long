# 06. CHỈ MỤC FILE & METHOD NGHIỆP VỤ (FILE INDEX)

Tài liệu này đóng vai trò từ điển tra cứu nhanh: **"Chỉ cần biết tên file là biết file đó đang làm gì trong hệ thống"**.

---

## 1. DANH MỤC CONTROLLERS TRỌNG YẾU

### `CheckoutController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/order/CheckoutController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng liên quan:** Điều phối toàn bộ quy trình checkout, áp dụng voucher, mua ngay và đặt hàng.
- **Method quan trọng:**
  - `startCheckout()`: Bắt đầu checkout các món đã chọn trong giỏ.
  - `buyNow()`: Bắt đầu checkout Mua Ngay 1 sản phẩm.
  - `viewCheckout()`: Hiển thị giao diện `checkout.html` và nạp dữ liệu phí ship.
  - `submitCheckout()`: Nhận form đặt hàng, validate, xử lý khách Guest, lưu đơn hàng.
  - `applyVoucher()`: Kiểm tra tính hợp lệ và trả về số tiền giảm của Voucher.
- **Gọi tới:** `GioHangService`, `CheckoutContextService`, `GuestCheckoutService`, `ShippingFeeCalculator`, `VoucherCalculator`.
- **Được gọi từ:** `cart.html`, `product-detail.html`, `checkout.html`, `checkout.js`.

### `GioHangController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/order/GioHangController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng liên quan:** Quản lý giỏ hàng thành viên DB và khách vãng lai Session.
- **Method quan trọng:**
  - `hienThiGioHang()`: Render trang `cart.html`.
  - `xuLyThemVaoGio()`: Thêm sản phẩm vào giỏ (AJAX).
  - `capNhatSoLuong()`: Thay đổi số lượng (+ / -) và kiểm tra tồn kho.
  - `xoaSanPhamAjax()` / `xoaNhieuSanPhamAjax()`: Xóa 1 hoặc nhiều món khỏi giỏ.
  - `layDuLieuMiniCart()`: Trả về JSON cập nhật dropdown giỏ hàng trên Header.
- **Gọi tới:** `GioHangService`, `GuestCartService`.

### `HomeController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/home/HomeController.java`
- **Loại:** Web MVC Controller
- **Chức năng liên quan:** Hiển thị trang chủ `index.html` và cửa hàng `shop.html`.
- **Method quan trọng:**
  - `hienThiTrangChu()`: Nạp sản phẩm Flash sale, New, Best Seller, Featured, Brands, Blogs.
  - `hienThiCuaHang()`: Nhận bộ lọc, gọi `SanPhamSpecification`, phân trang sản phẩm.
- **Gọi tới:** `SanPhamRepository`, `SanPhamService`, `DotGiamGiaDAO`, `ThuongHieuRepository`, `BlogService`.

### `SanPhamController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/product/SanPhamController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng liên quan:** Hiển thị chi tiết sản phẩm, tải biến thể động, gửi bài đánh giá.
- **Method quan trọng:**
  - `hienThiChiTietSanPham()`: Nạp thông tin sản phẩm, biến thể theo `CategoryType`, đợt giảm giá, đánh giá.
  - `guiDanhGia()`: Nhận bài review, lọc từ cấm, lưu ảnh upload, tính lại điểm trung bình.
- **Gọi tới:** `SanPhamService`, `PricingService`, `DanhGiaService`, `FileStorageService`.

### `SepayIpnController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/payment/SepayIpnController.java`
- **Loại:** REST Controller
- **Chức năng liên quan:** Tiếp nhận Webhook IPN từ cổng thanh toán SePay và tra cứu giao dịch.
- **Method quan trọng:**
  - `handleSepayIpn()`: Nhận Webhook POST, kiểm tra IP whitelist, verify Apikey secret.
  - `queryTransaction()`: Trả về trạng thái giao dịch cho frontend polling.
- **Gọi tới:** `SepayGatewayService`, `SepayOrderPaymentService`.

### `AdminPosController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/admin/AdminPosController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng liên quan:** Bán hàng tại quầy POS, quét mã vạch, hóa đơn chờ, in hóa đơn nhiệt K80.
- **Method quan trọng:**
  - `hienThiTrangPos()`: Hiển thị giao diện POS.
  - `timKiemSanPhamAjax()`: Tìm biến thể siêu nhanh theo Barcode / SKU.
  - `thanhToanPos()`: Thực hiện thanh toán tiền mặt / SePay QR tại quầy.
  - `inHoaDonPos()`: Render template in hóa đơn nhiệt.
- **Gọi tới:** `AdminPosService`.

### `AdminController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/admin/AdminController.java`
- **Loại:** Web MVC Controller
- **Chức năng liên quan:** Quản lý đơn hàng, đổi trạng thái, xử lý RMA đổi trả, quản lý kho hàng lỗi.
- **Method quan trọng:**
  - `hienThiDanhSachDonHang()`: Xem danh sách và lọc đơn hàng.
  - `capNhatTrangThaiDonHang()`: Chuyển trạng thái đơn hàng theo state machine.
  - `duyetTraHang()`: Duyệt yêu cầu đổi trả & tạo đơn thu hồi GHN.
  - `kiemKhoTraHang()`: Xác nhận kiểm kho (Hàng tốt vs Hàng lỗi).
  - `hoanTien()`: Xác nhận hoàn tất chuyển khoản tiền cho khách.
  - `hienThiKhoSanPhamLoi()` / `xuLyHangLoi()`: Quản lý và xử lý hàng lỗi.
- **Gọi tới:** `OrderViewService`, `InventoryLotService`.

### `AdminThongKeController.java`
- **Path:** `src/main/java/com/smashvn/shop/controller/admin/AdminThongKeController.java`
- **Loại:** Web MVC & REST Controller
- **Chức năng liên quan:** Báo cáo doanh thu, biểu đồ tài chính và xuất file Excel POI.
- **Method quan trọng:**
  - `hienThiTrangThongKe()`: Nạp số liệu thống kê ngày/tuần/tháng/năm.
  - `xuatExcel()`: Tạo và tải file Excel `.xlsx`.
- **Gọi tới:** `AdminThongKeService`.

---

## 2. DANH MỤC SERVICES TRỌNG YẾU

### `GioHangService.java`
- **Path:** `src/main/java/com/smashvn/shop/service/order/GioHangService.java`
- **Chức năng:** Quản lý giỏ hàng database, khởi tạo đơn hàng từ checkout, tạo đơn COD, tạo đơn SePay pending.
- **Method quan trọng:**
  - `createOrderFromCheckout()`: Khóa biến thể, tính đơn giá, trừ voucher, lưu HoaDon.
  - `submitCodOrder()`: Trừ kho FIFO ngay, tạo đơn GHN, gửi mail.
  - `createSepayPendingOrder()`: Lưu đơn chờ quét QR, đăng ký Snapshot vào PendingCheckoutRegistry.
  - `themVaoGioHang()` / `capNhatSoLuong()` / `xoaKhoiGioHang()`: CRUD giỏ hàng thành viên.

### `OrderViewService.java`
- **Path:** `src/main/java/com/smashvn/shop/service/order/OrderViewService.java`
- **Chức năng:** Quản lý toàn bộ vòng đời đơn hàng, hủy đơn, hoàn kho, quy trình đổi trả hàng, kiểm kho, hoàn tiền.
- **Method quan trọng:**
  - `updateOrderStatusByAdmin()`: Cập nhật trạng thái và hoàn kho nếu hủy đơn.
  - `huyDonHang()`: Khách hàng tự hủy đơn.
  - `yeuCauTraHang()`: Tiếp nhận yêu cầu đổi trả và lưu ảnh bằng chứng.
  - `duyetYeuCauTraHangVaTaoDonGhn()`: Tạo đơn thu hồi GHN_RETURN.
  - `xacNhanKiemKhoVaNhapKho()`: Phân loại hàng tốt (hoàn kho bán) vs hàng lỗi (nhập Kho Hàng Lỗi).
  - `xacNhanHoanTienChoKhach()`: Cập nhật RefundStatus=COMPLETED, không hoàn kho lặp lại.
  - `xacNhanGiaoHangDoiMoiChoKhach()`: Tạo đơn GHN_EXCHANGE gửi hàng mới.

### `SepayOrderPaymentService.java`
- **Path:** `src/main/java/com/smashvn/shop/service/payment/SepayOrderPaymentService.java`
- **Chức năng:** Xử lý xác nhận thanh toán SePay Webhook IPN trong 1 Transaction kín.
- **Method quan trọng:**
  - `xuLyThanhToanSePay()`: Phân bổ tồn kho FIFO thực tế, cập nhật trạng thái đơn sang ĐÃ THANH TOÁN, trừ voucher, xử lý trường hợp thiếu kho `PAID_INSUFFICIENT_STOCK`.

### `InventoryLotService.java`
- **Path:** `src/main/java/com/smashvn/shop/service/inventory/InventoryLotService.java`
- **Chức năng:** Thuật toán phân bổ tồn kho FIFO 2 giai đoạn, nhập kho, quản lý kho hàng lỗi.
- **Method quan trọng:**
  - `allocateFifo()`: Khóa sản phẩm cha theo ID ASC, lập kế hoạch phân bổ, trừ `soLuongTon` của từng lô.
  - `hoanKho()`: Hoàn lại số lượng tồn kho vào các lô ban đầu khi hủy đơn.
  - `layDanhSachKhoSanPhamLoi()` / `layChiTietKhoSanPhamLoi()`: Tra cứu hàng lỗi.
  - `xuLyHangLoi()`: Xử lý xuất trả NCC, thanh lý hoặc tiêu hủy.

### `PricingServiceImpl.java`
- **Path:** `src/main/java/com/smashvn/shop/service/product/PricingServiceImpl.java`
- **Chức năng:** Tính toán giá bán thực tế sau đợt giảm giá (DotGiamGia) và tạo PriceSnapshot.
- **Method quan trọng:**
  - `calculateCurrentSellingPrice()`: Tính giá sau khi áp dụng chiến dịch giảm giá.
  - `buildPriceSnapshot()`: Tạo đối tượng snapshot lưu vết vào hóa đơn chi tiết.

### `GhnService.java`
- **Path:** `src/main/java/com/smashvn/shop/service/api/GhnService.java`
- **Chức năng:** Tích hợp toàn bộ HTTP API của Giao Hàng Nhanh.
- **Method quan trọng:**
  - `calculateShipFee()`: Gọi API tính phí vận chuyển theo địa chỉ và khối lượng.
  - `createShippingOrder()`: Gọi API tạo vận đơn giao hàng chính thức.
  - `createReturnOrder()`: Gọi API tạo đơn thu hồi đổi trả.
  - `trackOrder()`: Tra cứu trạng thái vận đơn bưu tá.

### `AdminThongKeService.java`
- **Path:** `src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java`
- **Chức năng:** Phân loại doanh thu thực tế vs tạm tính vs giảm trừ, vẽ biểu đồ và xuất Excel POI.
- **Method quan trọng:**
  - `getDateRange()`: Xử lý bộ lọc thời gian preset (hôm nay, tuần này, tháng này...).
  - `generateExcelReport()`: Xây dựng file Excel `.xlsx` với định dạng chuyên nghiệp.

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
- `findByMaDonHang(maDonHang)`: Tra cứu hóa đơn theo mã.
- `findByIdWithLock(id)`: Khóa `PESSIMISTIC_WRITE` trên đơn hàng khi nhận Webhook IPN.
- `findActiveShippingOrders(Pageable)`: Native query tìm đơn GHN cần Polling đồng bộ trạng thái.
- `findRawTransactionsInPeriod(...)`: Lấy danh sách giao dịch phục vụ thống kê doanh thu.

### `PhieuGiamGiaRepository.java`
- `findByMaPhieu(maPhieu)`: Tra cứu voucher theo mã code.
- `findByMaPhieuWithLock(maPhieu)`: Khóa `PESSIMISTIC_WRITE` khi trừ số lượng voucher còn lại.

---
*Tài liệu Chỉ mục File hoàn chỉnh của dự án SMASH-VN.*
