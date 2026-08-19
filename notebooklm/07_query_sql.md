# 07. TRA CỨU QUERY, SQL & PHÂN TÍCH SẮP XẾP SẢN PHẨM (QUERY & SORTING)

---

## 1. SẮP XẾP THỨ TỰ HIỂN THỊ SẢN PHẨM (ORDER BY DEEP DIVE)

Đây là phân tích cực kỳ quan trọng giúp trả lời các câu hỏi: **"Sản phẩm đang được sắp xếp theo tiêu chí gì? ORDER BY nằm ở đâu? Muốn đổi thứ tự sắp xếp thì sửa file nào?"**

```
HTML View (th:each)
       │
       ▼
Model Attribute
       │
       ▼
Controller Method
       │
       ▼
Service Method
       │
       ▼
Repository / Criteria Specification
       │
       ▼
Mệnh đề ORDER BY trong SQL / JPQL / CriteriaBuilder
```

### 1.1. Bảng Tổng Hợp Quy Tắc Sắp Xếp Từng Màn Hình

| Màn Hình Hiển Thị | Model Attribute | File Khai Báo Logic | Tiêu Chí Sắp Xếp (ORDER BY) | Thứ Tự | Vị Trí Sửa Để Đổi Thứ Tự |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Cửa Hàng `/shop` (Mặc định)** | `page` (Page&lt;SanPham&gt;) | `SanPhamSpecification.java` | 1. Còn hàng trước (`hasStock DESC`)<br>2. ID mới nhất (`sp.id DESC`) | 1. `DESC`<br>2. `DESC` | `SanPhamSpecification.java` (Line 112 - 150) |
| **Cửa Hàng `/shop` (Giá tăng dần)** | `page` (`sort=price_asc`) | `SanPhamSpecification.java` | 1. Còn hàng trước (`hasStock DESC`)<br>2. Giá min (`MIN(spct.giaBan) ASC`)<br>3. `sp.id DESC` | 1. `DESC`<br>2. `ASC`<br>3. `DESC` | `SanPhamSpecification.java` (Line 126 - 135) |
| **Cửa Hàng `/shop` (Giá giảm dần)** | `page` (`sort=price_desc`) | `SanPhamSpecification.java` | 1. Còn hàng trước (`hasStock DESC`)<br>2. Giá min (`MIN(spct.giaBan) DESC`)<br>3. `sp.id DESC` | 1. `DESC`<br>2. `DESC`<br>3. `DESC` | `SanPhamSpecification.java` (Line 136 - 146) |
| **Trang Chủ (Sản phẩm mới)** | `newProducts` | `SanPhamRepository.java` | 1. Còn hàng trước (`hasStock DESC`)<br>2. ID mới nhất (`sp.id DESC`) | 1. `DESC`<br>2. `DESC` | `SanPhamRepository.findNewProducts` (Line 140) |
| **Trang Chủ (Bán chạy)** | `bestSellers` | `SanPhamRepository.java` | 1. Còn hàng trước (`hasStock DESC`)<br>2. Tổng đã bán (`SUM(hdct.soLuong) DESC`)<br>3. `sp.id DESC` | 1. `DESC`<br>2. `DESC`<br>3. `DESC` | `SanPhamRepository.findBestSellers` (Line 150) |
| **Trang Chủ (Nổi bật)** | `featuredProducts` | `SanPhamRepository.java` | 1. Còn hàng trước (`hasStock DESC`)<br>2. Lượt mua + Wishlist DESC<br>3. `sp.id DESC` | 1. `DESC`<br>2. `DESC`<br>3. `DESC` | `SanPhamRepository.findFeaturedProducts` (Line 162) |
| **Gợi ý Autocomplete** | `List<SanPham>` | `SanPhamRepository.java` | `sp.id DESC` | `DESC` | `SanPhamRepository.searchAutocomplete` (Line 112) |
| **Tìm kiếm cho Chatbot** | `List<SanPhamChiTiet>` | `SanPhamChiTietRepository.java` | 1. Tồn kho giảm dần (`spct.soLuongTon DESC`)<br>2. `spct.id DESC` | 1. `DESC`<br>2. `DESC` | `SanPhamChiTietRepository.searchForChatbot` (Line 85) |

---

## 2. CHI TIẾT CÁC QUERY TRỌNG YẾU TRONG TOÀN BỘ REPOSITORIES

### 2.1. `SanPhamRepository.java`

#### Query 1: Lấy Sản Phẩm Mới Trang Chủ (`findNewProducts`)
- **Loại:** JPQL Query
- **JPQL:**
```sql
SELECT sp FROM SanPham sp
WHERE sp.trangThaiValue = true
ORDER BY CASE WHEN (SELECT SUM(spct.soLuongTon) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND spct.trangThaiValue = true) > 0 THEN 1 ELSE 0 END DESC,
         sp.id DESC
```
- **Ý nghĩa:** Lấy 14 sản phẩm mới nhất đang bán. Đưa sản phẩm còn hàng lên trước, sau đó xếp theo ID giảm dần (Sản phẩm tạo gần nhất lên đầu).

#### Query 2: Lấy Sản Phẩm Bán Chạy Trang Chủ (`findBestSellers`)
- **Loại:** JPQL Query
- **JPQL:**
```sql
SELECT sp FROM SanPham sp
WHERE sp.trangThaiValue = true
ORDER BY CASE WHEN (SELECT SUM(spct.soLuongTon) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND spct.trangThaiValue = true) > 0 THEN 1 ELSE 0 END DESC,
         (SELECT COALESCE(SUM(hdct.soLuong), 0) FROM HoaDonChiTiet hdct WHERE hdct.sanPhamChiTiet.sanPham = sp) DESC,
         sp.id DESC
```
- **Ý nghĩa:** Ưu tiên sản phẩm còn hàng ➔ Xếp theo tổng số lượng sản phẩm đã bán trong bảng `HoaDonChiTiet` từ cao xuống thấp ➔ ID giảm dần.

#### Query 3: Lấy Sản Phẩm Nổi Bật Trang Chủ (`findFeaturedProducts`)
- **Loại:** JPQL Query
- **JPQL:**
```sql
SELECT sp FROM SanPham sp
WHERE sp.trangThaiValue = true
ORDER BY CASE WHEN (SELECT SUM(spct.soLuongTon) FROM SanPhamChiTiet spct WHERE spct.sanPham = sp AND spct.trangThaiValue = true) > 0 THEN 1 ELSE 0 END DESC,
         ((SELECT COALESCE(SUM(hdct.soLuong), 0) FROM HoaDonChiTiet hdct WHERE hdct.sanPhamChiTiet.sanPham = sp) +
          (SELECT COUNT(w.id) FROM SanPhamYeuThich w WHERE w.sanPham = sp)) DESC,
         sp.id DESC
```
- **Ý nghĩa:** Ưu tiên còn hàng ➔ Xếp theo tổng điểm tương tác (Số lượng bán + Số lượt khách bấm tim yêu thích) giảm dần.

#### Query 4: Khóa Sản Phẩm Cha Chống Deadlock (`findByIdWithLock`)
- **Loại:** JPQL + `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- **JPQL:** `SELECT s FROM SanPham s WHERE s.id = :id`
- **Ý nghĩa:** Khóa độc quyền bản ghi sản phẩm cha theo thứ tự ID ASC trong thuật toán phân bổ FIFO để ngăn chặn xung đột transaction.

---

### 2.2. `SanPhamChiTietRepository.java`

#### Query 1: Tìm Kiếm Cho Trợ Lý Ảo Chatbot AI (`searchForChatbot`)
- **Loại:** JPQL Fetch Join
- **JPQL:**
```sql
SELECT DISTINCT spct FROM SanPhamChiTiet spct
JOIN FETCH spct.sanPham sp
LEFT JOIN FETCH sp.danhMuc dm
LEFT JOIN FETCH sp.thuongHieu th
LEFT JOIN spct.sanPhamChiTietThuocTinhs att
WHERE sp.trangThaiValue = true AND spct.trangThaiValue = true AND spct.soLuongTon > 0
  AND (:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(COALESCE(sp.moTa, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(COALESCE(att.giaTri, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
  AND (:brandName IS NULL OR REPLACE(LOWER(th.tenThuongHieu), '-', '') LIKE REPLACE(LOWER(CONCAT('%', :brandName, '%')), '-', ''))
  AND (:categoryName IS NULL OR LOWER(dm.tenDanhMuc) LIKE LOWER(CONCAT('%', :categoryName, '%')))
  AND (:minPrice IS NULL OR spct.giaBan >= :minPrice)
  AND (:maxPrice IS NULL OR spct.giaBan <= :maxPrice)
ORDER BY spct.soLuongTon DESC, spct.id DESC
```
- **Ý nghĩa:** Tìm kiếm biến thể phù hợp nhất còn hàng theo từ khóa người dùng trò chuyện, hỗ trợ phân giải giá, danh mục, màu sắc và trọng lượng.

#### Query 2: Lấy Danh Sách Biến Thể Có Hàng Lỗi
- **Method:** `findBySoLuongSpLoiGreaterThanOrderBySoLuongSpLoiDesc(Integer soLuong)`
- **Loại:** Spring Data Derived Query
- **Ý nghĩa:** `findBySoLuongSpLoiGreaterThan` (Lọc biến thể có `soLuongSpLoi > 0`), `OrderBySoLuongSpLoiDesc` (Xếp giảm dần theo số lượng lỗi).

---

### 2.3. `HoaDonRepository.java`

#### Query 1: Lấy Đơn Hàng Cần Đồng Bộ Vận Chuyển GHN (`findActiveShippingOrders`)
- **Loại:** Native SQL (SQL Server)
- **SQL:**
```sql
SELECT hd.*,
    (SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC) AS ghnOrderCode,
    (SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN_RETURN' ORDER BY t.id DESC) AS ghnReturnOrderCode,
    (SELECT TOP 1 t.trang_thai FROM TichHopVanChuyen t WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC) AS ghnStatus
FROM HoaDon hd
WHERE hd.trang_thai_don_hang IN ('cho_xac_nhan', 'dang_lay_hang', 'dang_giao')
  AND EXISTS (
      SELECT 1 FROM TichHopVanChuyen t
      WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN' AND t.ma_van_don IS NOT NULL
  )
ORDER BY hd.ngay_tao ASC
```
- **Ý nghĩa:** Dùng cho `GhnPollingScheduler` quét các đơn đang vận chuyển để gọi GHN Track API cập nhật trạng thái tự động.

#### Query 2: Thống Kê Giao Dịch Doanh Thu Trong Kỳ (`findRawTransactionsInPeriod`)
- **Loại:** JPQL Query
- **JPQL:**
```sql
SELECT hd.id, hd.khachHang.hoTenKh, null, hd.ngayTao, null, hd.phuongThucThanhToan.tenPhuongThuc, null, hd.trangThaiThanhToan, hd.trangThaiDonHang, null, null, null, hd.tongTien, hd.nhanVien.id
FROM HoaDon hd
LEFT JOIN hd.khachHang LEFT JOIN hd.phuongThucThanhToan
WHERE (LOWER(hd.trangThaiThanhToan) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.trangThaiThanhToan) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.trangThaiThanhToan) = 'cancelled' AND hd.ngayThanhToan IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh'))
  AND hd.ngayTao BETWEEN :start AND :end
ORDER BY hd.ngayTao ASC
```

---

### 2.4. `PhieuGiamGiaRepository.java`

#### Query 1: Khóa Pessimistic Write Trừ Số Lượng Voucher (`findByMaPhieuWithLock`)
- **Loại:** JPQL + `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- **JPQL:** `SELECT p FROM PhieuGiamGia p WHERE p.maPhieu = :maPhieu`
- **Ý nghĩa:** Khóa độc quyền bản ghi Voucher trong suốt quá trình transaction đặt hàng, ngăn chặn nhiều khách cùng dùng hết voucher tại cùng 1 mili-giây.

---
*Tài liệu Query & SQL hoàn chỉnh của dự án SMASH-VN.*
