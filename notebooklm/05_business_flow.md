# 05. LUỒNG NGHIỆP VỤ CỐT LÕI (BUSINESS FLOWS)

---

## 1. LUỒNG ĐẶT HÀNG THANH TOÁN KHI NHẬN HÀNG (COD FLOW)

```text
Khách xem giỏ hàng (cart.html) HOẶC bấm Mua ngay (product-detail.html)
↓
Gửi POST /checkout/start HOẶC POST /checkout/buy-now
↓
CheckoutController.startCheckout() sinh token Context và lưu vào Session
↓
Chuyển hướng sang giao diện checkout.html
↓
Khách nhập địa chỉ (Tỉnh/Quận/Phường), chọn đơn vị vận chuyển GHN, nhập mã Voucher
↓
Hệ thống tự động gọi GHN API tính phí giao hàng thời gian thực (ShippingFeeCalculator)
↓
Khách chọn radio "Thanh toán khi nhận hàng (COD)" và bấm "Đặt hàng"
↓
Gửi Ajax POST /checkout/submit
↓
CheckoutController.submitCheckout()
↓
Xử lý khách vãng lai: Tự động cấp tài khoản GUEST, sinh Token đặt mật khẩu gửi về Email
↓
Gọi GioHangService.createOrderFromCheckout():
├── Khóa từng biến thể SanPhamChiTiet bằng Pessimistic Write Lock
├── Tính lại đơn giá niêm yết và giá khuyến mãi (PricingServiceImpl)
├── Khóa và trừ số lượng Voucher còn lại (PhieuGiamGiaRepository)
└── Khởi tạo HoaDon và các bản ghi HoaDonChiTiet kèm Snapshots
↓
GioHangService.submitCodOrder():
├── Không trừ kho ngay lúc đặt; chỉ trừ kho FIFO khi Nhân viên bấm 'Xác nhận đơn hàng' qua InventoryLotService.allocateFifo()
├── Tạo vận đơn GHN chính thức qua GhnService.createShippingOrder() (Nhận mã ghnOrderCode)
├── Xóa các món đã mua khỏi giỏ hàng
└── Gửi email thông báo đặt hàng thành công tới khách hàng
↓
Trạng thái cuối:
- OrderStatus = CHO_XAC_NHAN ("cho_xac_nhan")
- PaymentStatus = PENDING ("pending")
- PaymentMethod = "COD"
- Đã trừ tồn kho khả dụng
```

---

## 2. LUỒNG ĐẶT HÀNG THANH TOÁN ONLINE SEPAY (SEPAY QR FLOW)

```text
Khách tại checkout.html chọn "Chuyển khoản ngân hàng qua mã QR (SePay)"
↓
Bấm "Đặt hàng" -> Gửi POST /checkout/submit
↓
CheckoutController.submitCheckout() -> GioHangService.createSepayPendingOrder()
↓
Khởi tạo HoaDon với trạng thái chờ:
- OrderStatus = CHO_THANH_TOAN ("cho_thanh_toan")
- PaymentStatus = PENDING ("pending")
- PaymentMethod = "SEPAY"
- CHƯA trừ tồn kho
↓
Lưu Execution Snapshot vào PendingCheckoutRegistry (hạn 30 phút)
↓
Xóa các món khỏi giỏ hàng -> Trả về thông tin tài khoản Vietcombank & mã QR cho frontend
↓
Khách quét mã QR chuyển khoản đúng số tiền và nội dung (DHSVN20260819-140)
↓
SePay nhận biến động số dư -> Bắn Webhook POST /api/payment/sepay/ipn
↓
SepayIpnController.handleSepayIpn()
├── Kiểm tra IP Whitelist
├── Kiểm tra Header Authorization: Apikey {secret}
└── Chuyển sang SepayGatewayService.handleIpn()
↓
SepayGatewayService.handleIpn():
├── Kiểm tra Idempotency chống trùng lặp theo transactionId
├── Khớp mã đơn hàng và so sánh số tiền chuyển khoản == order.getTongTien()
└── Chuyển sang SepayOrderPaymentService.xuLyThanhToanSePay()
↓
SepayOrderPaymentService.xuLyThanhToanSePay() [Transactional]:
├── Khóa HoaDon bằng findByIdWithLock(orderId)
├── Gọi InventoryLotService.allocateFifo(items)
│
├── [Nếu ĐỦ KHO (SUCCESS)]:
│   ├── Lưu PaymentTransaction với status = 'SUCCESS'
│   ├── Thay thế HoaDonChiTiet tạm bằng các bản ghi phân bổ theo lô thực tế
│   ├── Trừ số lượng còn lại của Voucher (Pessimistic Lock)
│   ├── Cập nhật HoaDon: OrderStatus = CHO_XAC_NHAN, TrangThaiThanhToan = 'DA_THANH_TOAN', PaymentStatus = 'paid'
│   └── Sau khi Commit: Gửi email hóa đơn và email kích hoạt tài khoản Guest
│
└── [Nếu THIẾU KHO (PAID_INSUFFICIENT_STOCK)]:
    ├── Lưu PaymentTransaction với status = 'PAID_INSUFFICIENT_STOCK'
    ├── Cập nhật HoaDon: OrderStatus = YEU_CAU_HUY, TrangThaiThanhToan = 'CHO_HOAN_TIEN', RefundStatus = PENDING
    └── KHÔNG rollback nhận tiền -> Để Admin hoàn tiền thủ công cho khách
```

---

## 3. LUỒNG ĐỔI TRẢ HÀNG, KIỂM KHO & HOÀN TIỀN (RMA FLOW)

```text
Khách hàng tại dash-my-order.html bấm "Yêu cầu đổi / trả hàng"
↓
Kiểm tra điều kiện: Đơn đã giao (DA_GIAO) trong vòng 7 ngày (OrderViewService.isWithinReturnWindow)
↓
Khách chọn lý do, nhập mô tả và upload ảnh/video bằng chứng lỗi sản phẩm
↓
Gửi POST /user/don-hang/tra-hang/{id} -> OrderViewService.yeuCauTraHang()
↓
Cập nhật ReturnStatus = REQUESTED ("CHO_DUYET")
↓
Admin xem yêu cầu tại /admin/don-hang/{id} -> Bấm "Duyệt yêu cầu"
↓
OrderViewService.duyetYeuCauTraHangVaTaoDonGhn():
├── Gọi GhnService.createShippingOrder() tạo vận đơn thu hồi hàng tận nhà khách (GHN_RETURN)
└── Cập nhật ReturnStatus = APPROVED ("DA_DUYET") -> CHO_THU_HOI
↓
Bưu tá GHN lấy hàng về kho Smash VN -> GhnPollingScheduler cập nhật ReturnStatus = INSPECTING ("DANG_KIEM_TRA")
↓
Nhân viên kho mở kiện hàng và thực hiện Kiểm Kho (Inspection):
├── [Trường hợp Hàng Nguyên Vẹn, Không Lỗi]:
│   └── Hoàn lại vào kho bán bình thường qua InventoryLotService.hoanKho()
│
└── [Trường hợp Hàng Bị Lỗi (Hư hỏng / Lỗi sản xuất)]:
    └── Nhập vào "Kho Sản Phẩm Lỗi" (Tăng soLuongSpLoi trong SanPhamChiTiet, không đưa vào bán lại)
↓
Rẽ nhánh xử lý tiếp:
├── [Nếu là Yêu Cầu ĐỔI HÀNG MỚI]:
│   OrderViewService.xacNhanGiaoHangDoiMoiChoKhach()
│   (Trừ kho biến thể đổi mới -> Tạo đơn GHN mới gửi khách -> ReturnStatus = EXCHANGED)
│
└── [Nếu là Yêu Cầu TRẢ HÀNG HOÀN TIỀN]:
    Admin chuyển khoản trả tiền cho khách -> Nhập mã giao dịch
    OrderViewService.xacNhanHoanTienChoKhach()
    (Cập nhật RefundStatus = COMPLETED, TrangThaiThanhToan = 'REFUNDED', ReturnStatus = 'HOAN_TIEN_THANH_CONG')
```

---

## 4. LUỒNG BÁN HÀNG TẠI QUẦY (POS COUNTER FLOW)

```text
Thu ngân mở giao diện /admin/pos
↓
Quét mã Barcode SKU hoặc tìm kiếm sản phẩm
↓
AdminPosController.timKiemSanPhamAjax() -> AdminPosService.timKiemBienTheChoPos()
↓
Thêm sản phẩm vào tab Hóa Đơn Chờ (Hỗ trợ tối đa 10 tab hóa đơn song song)
↓
Tìm kiếm khách hàng thành viên theo SĐT hoặc tạo nhanh khách hàng mới
↓
Chọn hình thức thanh toán:
├── [Thanh toán Tiền Mặt (Cash)]:
│   AdminPosService.thanhToanHoaDonPos()
│   (Trừ kho FIFO ngay lập tức -> Cập nhật DA_GIAO, DA_THANH_TOAN)
│
└── [Thanh toán SePay QR tại quầy]:
    Hệ thống sinh mã QR Vietcombank với mã hóa đơn HDSVN...
    Khách chuyển khoản -> SePay IPN báo về -> Tự động hoàn thành đơn (DA_GIAO)
↓
Nhấn nút "In hóa đơn" -> Xuất mẫu in hóa đơn nhiệt K80 (/admin/pos/in-hoa-don/{id})
```

---

## 5. LUỒNG HỦY ĐƠN HÀNG (ORDER CANCELLATION FLOW)

### 5.1. Luồng Hủy Đơn Chưa Thanh Toán (COD / Chờ thanh toán)
```text
Admin/Nhân viên bấm "HỦY ĐƠN" trên giao diện Admin
↓
Mở Modal Bước 1: Chọn lý do hủy (dropdown lý do phổ biến + ghi chú nếu chọn "Khác")
↓
Bấm "Tiếp tục" -> Backend nhận diện: Đơn hàng CHƯA THANH TOÁN (COD / Chờ TT)
↓
Hiển thị Bước 2A: Xác nhận đơn chưa thanh toán, không phát sinh hoàn tiền
↓
Bấm "Xác nhận hủy đơn" -> Gửi POST /admin/don-hang/cancel-unpaid
↓
OrderViewService.cancelOrderUnpaid() [Transactional]:
├── Lock HoaDon bằng findByIdWithLock(id)
├── Kiểm tra trạng thái đơn: Chặn nếu đơn đã hủy hoặc đã giao / bàn giao GHN
├── Enforce Backend: Chặn nếu đơn thực tế đã thanh toán (bắt buộc qua luồng hoàn tiền)
├── Kiểm tra trừ kho:
│   ├── Nếu đơn ở CHO_XAC_NHAN (chưa trừ kho): KHÔNG hoàn kho
│   └── Nếu đơn đã ở DA_XAC_NHAN (đã trừ kho): Gọi InventoryLotService.hoanKho()
├── Khôi phục lượt sử dụng Voucher nếu có (PhieuGiamGiaRepository, chống hoàn 2 lần)
├── Hủy vận đơn GHN nếu đã sinh mã qua GhnService.cancelOrder(ghnOrderCode)
├── Cập nhật HoaDon: OrderStatus = DA_HUY, PaymentStatus = 'CANCELLED', TrangThaiThanhToan = 'HUY', LyDoHuy = ...
└── Ghi AuditLog lịch sử trạng thái
```

### 5.2. Luồng Hủy Đơn Online Đã Thanh Toán & Xác Nhận Hoàn Tiền (Online Paid Cancel & Refund Flow)
```text
Admin/Nhân viên bấm "HỦY ĐƠN" trên giao diện Admin
↓
Mở Modal Bước 1: Chọn lý do hủy (dropdown lý do phổ biến + ghi chú nếu chọn "Khác")
↓
Bấm "Tiếp tục" -> Backend nhận diện: ĐƠN HÀNG ĐÃ ĐƯỢC THANH TOÁN (Online Paid)
↓
Hiển thị Bước 2B:
├── Cảnh báo: "ĐƠN HÀNG ĐÃ ĐƯỢC THANH TOÁN"
├── Card thông tin thanh toán gốc: Mã đơn, Khách, Gateway, Số tiền đã trả, Mã GD, TG thanh toán, Lý do hủy
└── Form xác nhận hoàn tiền: PTTT hoàn (Chuyển khoản/Tiền mặt), Số tiền hoàn, Mã GD hoàn, Ghi chú, Upload ảnh chứng từ
↓
Bấm "Xác nhận hoàn tiền & Hủy đơn" -> Gửi POST (multipart) /admin/don-hang/cancel-paid-refund
↓
OrderViewService.cancelOrderPaidWithRefund() [Transactional]:
├── Lock HoaDon bằng findByIdWithLock(id)
├── Idempotency Guard: Chặn nếu đơn đã DA_HUY và có PaymentTransaction REFUND_SUCCESS
├── Enforce Backend: Bắt buộc đơn phải có isOrderPaid() == true
├── Validate: Số tiền hoàn <= Tổng tiền khách trả, Phương thức hoàn, Mã GD hoàn
├── Hoàn kho FIFO qua InventoryLotService.hoanKho(items) (Chống hoàn kho 2 lần bằng daNhapKhoHoan)
├── Khôi phục lượt sử dụng Voucher nếu có (chống hoàn 2 lần)
├── Hủy vận đơn GHN nếu có qua GhnService.cancelOrder(ghnOrderCode)
├── Tạo PaymentTransaction mới:
│   ├── Gateway = 'ORDER_CANCEL_REFUND'
│   ├── Status = 'REFUND_SUCCESS'
│   ├── Amount = Số tiền hoàn
│   ├── TransactionId = Mã GD hoàn
│   └── RawPayload = JSON chi tiết (lyDoHuy, ptttHoan, anhChungTu, nguoiThucHien, thoiGian)
│   (Giao dịch thanh toán gốc ban đầu được giữ nguyên hoàn toàn!)
├── Cập nhật HoaDon:
│   ├── OrderStatus = DA_HUY
│   ├── PaymentStatus = REFUNDED
│   ├── TrangThaiThanhToan = 'REFUNDED'
│   ├── RefundStatus = COMPLETED
│   └── Lưu thông tin chi tiết hoàn tiền (phuongThucHoan, soTienHoan, maGiaoDichHoan, anhChungTuHoan)
├── Ghi AuditLog lịch sử trạng thái chi tiết
└── Loại bỏ hoàn toàn khỏi Doanh thu bán hàng (AdminThongKeService phân loại EXCLUDED / Sales Revenue = 0)
```

---
*Tài liệu Luồng nghiệp vụ hoàn chỉnh của dự án SMASH-VN.*
