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
gioHangService.submitCodOrder():
├── Lưu HoaDon với trạng thái: OrderStatus = CHO_XAC_NHAN, PaymentStatus = PENDING
├── ⚠️ CHƯA TRỪ TỒN KHO NGAY LÚC ĐẶT: Tránh rủi ro spam/đặt đơn ảo giữ chỗ làm cạn kho.
├── Tạo vận đơn GHN chính thức qua GhnService.createShippingOrder() (Nhận mã ghnOrderCode)
├── Xóa các món đã mua khỏi giỏ hàng
└── Gửi email thông báo đặt hàng thành công tới khách hàng
↓
Trạng thái sau khi đặt:
- OrderStatus = CHO_XAC_NHAN ("cho_xac_nhan")
- PaymentStatus = PENDING ("pending")
- PaymentMethod = "COD"
- Tồn kho: CHƯA BỊ TRỪ (isStockDeductedState("cho_xac_nhan") == false)
↓
KHI NHÂN VIÊN / QUẢN LÝ BẤM "XÁC NHẬN ĐƠN HÀNG":
├── OrderViewService.updateOrderStatusByAdmin() / moveOrderToNextStatus()
├── Phát hiện chuyển từ 'cho_xac_nhan' (false) sang 'da_xac_nhan' (true)
├── MỚI CHÍNH THỨC GỌI: InventoryLotService.allocateFifo(items)
├── Trừ số lượng các lô nhập PhieuNhapChiTiet theo thứ tự ngày nhập (FIFO)
├── Trừ tồn kho kinh doanh SanPhamChiTiet.soLuongTon
└── Nếu thiếu hàng: Chặn chuyển trạng thái và báo lỗi INSUFFICIENT_STOCK
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
Khách quét mã QR chuyển khoản đúng số tiền và nội dung (VD: DHSVN20260819-140)
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
│   ├── Lưu PaymentTransaction với status = 'SUCCESS', gateway = 'SEPAY'
│   ├── Thay thế HoaDonChiTiet tạm bằng các bản ghi phân bổ theo lô thực tế
│   ├── Trừ số lượng còn lại của Voucher (Pessimistic Lock)
│   ├── Cập nhật HoaDon: OrderStatus = CHO_XAC_NHAN, TrangThaiThanhToan = 'DA_THANH_TOAN', PaymentStatus = 'paid'
│   └── Sau khi Commit: Gửi email hóa đơn và email kích hoạt tài khoản Guest
│
├── [Nếu THIẾU KHO (PAID_INSUFFICIENT_STOCK)]:
│   ├── Lưu PaymentTransaction với status = 'PAID_INSUFFICIENT_STOCK'
│   ├── Cập nhật HoaDon: OrderStatus = YEU_CAU_HUY, TrangThaiThanhToan = 'CHO_HOAN_TIEN', RefundStatus = PENDING
│   └── KHÔNG rollback nhận tiền -> Để Admin hoàn tiền thủ công cho khách
│
└── [Nếu LỆCH TIỀN (AMOUNT_MISMATCH)]:
    └── Lưu PaymentTransaction với status = 'AMOUNT_MISMATCH', gửi cảnh báo quản trị
```

---

## 3. LUỒNG ĐỔI TRẢ HÀNG, KIỂM KHO & HOÀN TIỀN (RMA FLOW)

```text
Khách hàng tại dash-my-order.html bấm "Yêu cầu đổi / trả hàng"
↓
Kiểm tra điều kiện: Đơn đã giao (DA_GIAO) trong vòng 7 ngày (OrderViewService.isWithinReturnWindow)
↓
Khách chọn loại yêu cầu (Trả hàng hoàn tiền / Đổi hàng mới), nhập lý do
và upload ảnh + video bằng chứng sản phẩm bị lỗi (FileStorageService kiểm tra MIME & dung lượng)
↓
Gửi POST /user/don-hang/tra-hang/{id} -> OrderViewService.yeuCauTraHang()
↓
Cập nhật ReturnStatus = REQUESTED ("CHO_DUYET")
↓
Admin xem yêu cầu tại /admin/don-hang -> Xem video/ảnh bằng chứng của khách
↓
Admin bấm "Duyệt yêu cầu":
OrderViewService.duyetYeuCauTraHangVaTaoDonGhn():
├── Gọi GhnService.createShippingOrder() tạo vận đơn thu hồi hàng tận nhà khách (nha_cung_cap = 'GHN_RETURN')
└── Cập nhật ReturnStatus = APPROVED ("DA_DUYET") -> CHO_THU_HOI
↓
Bưu tá GHN lấy hàng về kho Smash-VN -> GhnPollingScheduler / Admin cập nhật ReturnStatus = INSPECTING ("DANG_KIEM_TRA")
↓
Nhân viên kho mở kiện hàng và thực hiện Kiểm Kho (Inspection) tại modal:
├── [Trường hợp Hàng Nguyên Vẹn, Không Lỗi]:
│   └── Hoàn lại vào kho bán bình thường qua InventoryLotService.hoanKho() (ReturnInventoryStatus = RESTOCKED_GOOD)
│
└── [Trường hợp Hàng Bị Lỗi (Hư hỏng / Lỗi sản xuất)]:
    └── Nhập vào "Kho Sản Phẩm Lỗi" (Tăng soLuongSpLoi trong SanPhamChiTiet, ReturnInventoryStatus = QUARANTINED_FAULTY)
↓
Rẽ nhánh xử lý tiếp theo:
├── [Nếu là Yêu Cầu ĐỔI HÀNG MỚI]:
│   Admin bấm "Xác nhận gửi đổi" -> OrderViewService.xacNhanGiaoHangDoiMoiChoKhach()
│   (Trừ kho biến thể đổi mới -> Tạo đơn GHN mới GHN_EXCHANGE gửi khách -> ReturnStatus = EXCHANGED)
│
└── [Nếu là Yêu Cầu TRẢ HÀNG HOÀN TIỀN]:
    Admin chuyển khoản trả tiền cho khách -> Nhập mã giao dịch & upload chứng từ
    OrderViewService.xacNhanHoanTienChoKhach()
    (Cập nhật RefundStatus = COMPLETED, TrangThaiThanhToan = 'REFUNDED', ReturnStatus = 'HOAN_TIEN_THANH_CONG')
```

---

## 4. LUỒNG BÁN HÀNG TẠI QUẦY (POS COUNTER FLOW)

```text
Thu ngân mở giao diện /admin/pos
↓
Quét mã Barcode SKU hoặc gõ tìm kiếm sản phẩm
↓
AdminPosController.searchProducts() -> AdminPosService.timKiemBienTheChoPos()
↓
Thêm sản phẩm vào tab Hóa Đơn Chờ (Hỗ trợ tối đa 10 tab hóa đơn song song)
↓
Tìm kiếm khách hàng thành viên theo SĐT hoặc bấm tạo nhanh khách hàng mới
↓
Chọn hình thức thanh toán:
├── [Thanh toán Tiền Mặt (Cash)]:
│   AdminPosService.thanhToanHoaDonPos()
│   (Trừ kho FIFO ngay lập tức -> Cập nhật OrderStatus = DA_GIAO, PaymentStatus = PAID)
│
└── [Thanh toán SePay QR tại quầy]:
    Hệ thống sinh mã QR Vietcombank với mã hóa đơn HDSVN...
    Khách quét mã chuyển khoản -> SePay IPN báo về -> Tự động hoàn thành đơn (DA_GIAO, PAID)
↓
Nhấn nút "In hóa đơn" -> Xuất mẫu in hóa đơn nhiệt K80 (/admin/pos/print/{id})
```

---

## 5. LUỒNG HỦY ĐƠN HÀNG CHƯA THANH TOÁN (UNPAID CANCEL FLOW - COD)

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

---

## 6. LUỒNG HỦY ĐƠN ONLINE ĐÃ THANH TOÁN & XÁC NHẬN HOÀN TIỀN (PAID CANCEL & REFUND FLOW)

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
├── Hoàn kho FIFO qua InventoryLotService.hoanKho(items) (Chống hoàn kho 2 lần bằng cờ daNhapKhoHoan)
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

## 7. LUỒNG NHẬP KHO LÔ HÀNG & PHÂN BỔ FIFO (INVENTORY LOT IMPORT & FIFO FLOW)

```text
Admin mở form Sản phẩm -> Quản lý biến thể -> Nhập kho theo lô
↓
Nhập thông tin: Mã phiếu nhập (hoặc tự sinh PN-YYYYMMDD-XXXX), Nhà cung cấp, Số lượng nhập, Đơn giá nhập, Ghi chú
↓
Gửi POST /admin/san-pham/{id}/bien-the/nhap-lo
↓
InventoryLotService.nhapKho() [Transactional]:
├── Tạo bản ghi PhieuNhap (Mã, NCC, Tổng tiền, Ngày nhập, Nhân viên tạo)
├── Tạo bản ghi PhieuNhapChiTiet:
│   ├── id_san_pham_chi_tiet = biến thể được chọn
│   ├── so_luong_nhap = số lượng
│   ├── don_gia_nhap = giá nhập
│   ├── so_luong_ton_lo = số lượng ban đầu
│   └── ngay_nhap = thời gian hiện tại
└── Tăng tồn kho kinh doanh SanPhamChiTiet.soLuongTon += so_luong_nhap
↓
Khi có đơn hàng cần xuất kho:
InventoryLotService.allocateFifo(items) [Transactional]:
├── Khóa SanPham cha theo ID ASC để chống Deadlock
├── Với từng biến thể, quét PhieuNhapChiTiet có so_luong_ton_lo > 0 theo ngay_nhap ASC
├── Trừ lần lượt so_luong_ton_lo của từng lô cho tới khi đủ số lượng
└── Giảm SanPhamChiTiet.soLuongTon tương ứng
```

---

## 8. LUỒNG QUẢN LÝ & XỬ LÝ KHO SẢN PHẨM LỖI (FAULTY INVENTORY FLOW)

```text
Sản phẩm lỗi phát sinh từ quy trình RMA kiểm kho (ReturnInventoryStatus = QUARANTINED_FAULTY)
↓
Hệ thống cộng dồn vào cột SanPhamChiTiet.soLuongSpLoi (Hoàn toàn cách ly khỏi soLuongTon)
↓
Admin truy cập /admin/kho-san-pham-loi
↓
Xem danh sách các biến thể có soLuongSpLoi > 0 (xếp giảm dần)
↓
Bấm "Xem chi tiết" (/admin/kho-san-pham-loi/{id}):
├── Xem nguồn gốc: Đơn hàng trả nào sinh ra món lỗi, lý do lỗi, bằng chứng ảnh/video của khách
└── Xem lịch sử các lần xử lý trước đó từ bảng EditLog
↓
Admin chọn hành động xử lý:
├── [1. Xuất trả Nhà Cung Cấp (XUAT_TRA_NCC)]: Trả lại NCC để đổi mới/hoàn tiền
├── [2. Thanh lý thu hồi vốn (THANH_LY)]: Bán thanh lý giá rẻ
└── [3. Tiêu hủy (TIEU_HUY)]: Hư hỏng nặng không thể phục hồi
↓
Nhập số lượng xử lý, lý do, người nhận/ghi chú -> Gửi POST /admin/kho-san-pham-loi/{id}/xu-ly
↓
InventoryLotService.xuLyHangLoi() [Transactional]:
├── Giảm SanPhamChiTiet.soLuongSpLoi -= số lượng xử lý
└── Ghi nhật ký vào EditLog (Hành động, Số lượng, Lý do, Nhân viên thực hiện, Thời gian)
```

---

## 9. LUỒNG KHÁCH HÀNG VÃNG LAI (GUEST CHECKOUT FLOW)

```text
Khách vãng lai chưa đăng nhập vào trang checkout.html và đặt hàng
↓
Gửi POST /checkout/submit
↓
GuestCheckoutService.processGuestCheckout():
├── Kiểm tra Email & SĐT trong cơ sở dữ liệu
├── Nếu chưa có tài khoản: Tự động khởi tạo TaiKhoan (Status = GUEST, Role = ROLE_KH) & KhachHang
├── Nếu đã có tài khoản: Kiểm tra số lần mua khách Guest (Giới hạn tối đa 3 lần mua chưa kích hoạt)
├── Sinh Token đặt mật khẩu (TokenKhoiPhuc) có thời hạn 24 giờ
└── Gửi Email thông báo đặt hàng kèm link kích hoạt và tạo mật khẩu
↓
Khách bấm vào link trong Email -> Nhập mật khẩu mới -> Tài khoản chuyển sang AccountStatus = ACTIVE
```

---

## 10. LUỒNG TRỢ LÝ ẢO AI GEMINI RAG (CHATBOT RAG FLOW)

```text
Khách mở khung chat AI trên giao diện Web -> Gõ câu hỏi (VD: "Tìm vợt Yonex công thủ toàn diện tầm 2 triệu")
↓
Gửi POST /api/chatbot/chat
↓
ChatbotServiceImpl.chat():
├── 1. Phân tích ngữ nghĩa & thực thể câu hỏi:
│   ├── Nhận diện thương hiệu (Yonex, Victor, Lining...)
│   ├── Nhận diện danh mục (Vợt, Giày, Áo, Balo...)
│   └── Phân tích khoảng giá qua VietnamesePriceParser (MinPrice, MaxPrice)
│
├── 2. Truy xuất dữ liệu thực tế (Retrieval Phase):
│   └── Gọi SanPhamChiTietRepository.searchForChatbot(keyword, brand, category, minPrice, maxPrice)
│       (Lấy danh sách các biến thể thực tế đang CÒN HÀNG trong database)
│
├── 3. Tổng hợp ngữ cảnh & Gọi LLM (Generation Phase):
│   ├── Nạp dữ liệu sản phẩm tìm được vào System Prompt
│   ├── Gửi request tới Google Gemini 2.0 Flash qua giao thức OpenAI-compatible REST API
│   └── Nhận phản hồi văn bản tự nhiên, chuyên môn cao từ Gemini
│
└── 4. Trả về kết quả:
    └── Trả về JSON gồm text câu trả lời + danh sách thẻ sản phẩm gợi ý (Card UI kèm giá, ảnh và link mua)
```

---
*Tài liệu Luồng nghiệp vụ hoàn chỉnh của dự án SMASH-VN.*
