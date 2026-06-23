package com.smashvn.shop.service.order;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.api.ShippingFeeCalculator;
import com.smashvn.shop.service.admin.AdminShippingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.smashvn.shop.entity.GioHang;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TrangThaiGioHang;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentMethod;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.repository.GioHangRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TrangThaiGioHangRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import java.time.LocalDateTime;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.smashvn.shop.util.VoucherCalculator;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.service.product.PriceSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GioHangService {

    public static final int MAX_CART_QUANTITY = 999;

    private final KhachHangRepository khachHangRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TrangThaiGioHangRepository trangThaiGioHangRepository;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final ShippingFeeCalculator shippingFeeCalculator;
    private final AdminShippingService adminShippingService;
    private final GhnService ghnService;
    private final SoDiaChiRepository soDiaChiRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PricingService pricingService;

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> themVaoGio(Integer idTaiKhoan, Integer idSanPhamChiTiet, Integer soLuong) {
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm thêm vào giỏ hàng phải lớn hơn 0.");
        }
        if (soLuong > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Số lượng sản phẩm thêm vào giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        if (idSanPhamChiTiet == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ.");
        }

        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            throw new RuntimeException("Tài khoản này chưa được cập nhật thông tin Khách Hàng!");
        }

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setKhachHang(khachHang);
            gioHang = gioHangRepository.save(gioHang);
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), idSanPhamChiTiet);
        SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (spct.getSanPham() == null || "ngung_ban".equals(spct.getSanPham().getTrangThai())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Sản phẩm này đã ngừng bán!");
        }

        // --- BÀI TOÁN VALIDATE TỒN KHO THỰC TẾ ---
        // 1. Xem trong giỏ của khách đã có bao nhiêu chiếc này rồi?
        int soLuongHienCo = (chiTiet != null && chiTiet.getSoLuong() != null) ? chiTiet.getSoLuong() : 0;

        // 2. Tính tổng số lượng khách sẽ có nếu thêm thành công
        long tongYeuCauLong = (long) soLuongHienCo + soLuong;
        if (tongYeuCauLong > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Tổng số lượng sản phẩm này trong giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        int tongYeuCau = (int) tongYeuCauLong;

        // 3. Khóa chặn: Nếu tổng yêu cầu vượt quá kho
        if (spct.getSoLuongTon() < tongYeuCau) {
            if (soLuongHienCo > 0) {
                throw new RuntimeException("Bạn đã có " + soLuongHienCo + " sản phẩm này trong giỏ. Không thể thêm vượt quá số lượng tồn kho (" + spct.getSoLuongTon() + ")!");
            } else {
                throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + spct.getSoLuongTon() + " sản phẩm.");
            }
        }

        // --- XỬ LÝ LƯU VÀO GIỎ ---
        if (chiTiet != null) {
            chiTiet.setSoLuong(tongYeuCau); // Đã có thì cộng dồn
        } else {
            chiTiet = new GioHangChiTiet(); // Chưa có thì tạo mới
            chiTiet.setGioHang(gioHang);
            chiTiet.setSanPhamChiTiet(spct);
            chiTiet.setSoLuong(soLuong);

            TrangThaiGioHang trangThai = trangThaiGioHangRepository.findById(1)
                    .orElseGet(() -> trangThaiGioHangRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("Lỗi cấu hình CSDL: Không tìm thấy ID trạng thái 1")));
            chiTiet.setTrangThai(trangThai);
        }

        gioHangChiTietRepository.save(chiTiet);

        // Đóng gói dữ liệu trả về cho Modal JS
        Map<String, Object> result = new HashMap<>();
        result.put("tenSanPham", spct.getSanPham().getTenSanPham());
        result.put("phanLoai", spct.getMauSac() + " | " + spct.getTrongLuong());
        result.put("giaBan", pricingService.calculateCurrentSellingPrice(spct));
        result.put("hinhAnh", spct.getHinhAnhSanPham());
        result.put("soLuongThem", soLuong);

        return result;
    }

    public List<GioHangChiTiet> layDanhSachSanPhamTrongGio(Integer idTaiKhoan) {
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            return new ArrayList<>();
        }
        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            return new ArrayList<>();
        }
        return gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
    }

    // SỬA ĐỔI 2: Gom toàn bộ logic tính tiền, gom JSON của Mini Cart vào Service
    public Map<String, Object> layDuLieuMiniCart(Integer idTaiKhoan) {
        List<GioHangChiTiet> danhSach = layDanhSachSanPhamTrongGio(idTaiKhoan);

        BigDecimal tongTien = BigDecimal.ZERO;
        int tongSoLuong = 0;
        List<Map<String, Object>> danhSachMini = new ArrayList<>();

        for (GioHangChiTiet item : danhSach) {
            SanPham sp = item.getSanPhamChiTiet().getSanPham();
            int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
            String trangThai = sp.getTrangThai();

            boolean hopLe = tonKho > 0 && (trangThai == null || trangThai.equals("dang_ban")) && item.getSoLuong() != null && item.getSoLuong() > 0;
            PriceSnapshot priceSnapshot = pricingService.buildPriceSnapshot(item.getSanPhamChiTiet());

            if (hopLe) {
                BigDecimal gia = priceSnapshot.giaBanSauGiam();
                BigDecimal soLuong = new BigDecimal(item.getSoLuong());
                tongTien = tongTien.add(gia.multiply(soLuong));
                tongSoLuong += item.getSoLuong();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("idChiTiet", item.getId());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("hinhAnh", item.getSanPhamChiTiet().getHinhAnhSanPham());
            map.put("giaBan", priceSnapshot.giaBanSauGiam());
            map.put("giaGoc", priceSnapshot.giaNiemYet());
            map.put("phanTramGiam", priceSnapshot.phanTramGiam());
            map.put("soLuong", item.getSoLuong());
            map.put("idSanPham", sp.getId());
            map.put("mauSac", item.getSanPhamChiTiet().getMauSac());
            map.put("trongLuong", item.getSanPhamChiTiet().getTrongLuong());
            danhSachMini.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("trangThai", "ok");
        response.put("tongSoLuong", tongSoLuong);
        response.put("tongTien", tongTien);
        response.put("danhSach", danhSachMini);

        return response;
    }

    @org.springframework.transaction.annotation.Transactional
    public void xoaSanPhamKhoiGio(Integer idGioHangChiTiet, Integer idTaiKhoan) {
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            throw new RuntimeException("Tài khoản không hợp lệ!");
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findById(idGioHangChiTiet)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        // Chống IDOR: Xác minh sản phẩm giỏ hàng thuộc về người dùng đang thao tác
        if (!chiTiet.getGioHang().getKhachHang().getId().equals(khachHang.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xóa sản phẩm này khỏi giỏ hàng!");
        }

        gioHangChiTietRepository.delete(chiTiet);
    }

    @org.springframework.transaction.annotation.Transactional
    public void capNhatSoLuong(Integer idGioHangChiTiet, Integer soLuongMoi, Integer idTaiKhoan) {
        if (soLuongMoi == null || soLuongMoi <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ hàng phải lớn hơn 0.");
        }
        if (soLuongMoi > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        if (idGioHangChiTiet == null) {
            throw new IllegalArgumentException("Chi tiết giỏ hàng không hợp lệ.");
        }

        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            throw new RuntimeException("Tài khoản không hợp lệ!");
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findById(idGioHangChiTiet)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        // Chống IDOR: Xác minh sản phẩm giỏ hàng thuộc về người dùng đang thao tác
        if (!chiTiet.getGioHang().getKhachHang().getId().equals(khachHang.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền cập nhật giỏ hàng này!");
        }

        // Chống sửa vượt quá tồn kho (H-7)
        if (chiTiet.getSanPhamChiTiet().getSoLuongTon() < soLuongMoi) {
            throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + chiTiet.getSanPhamChiTiet().getSoLuongTon() + " sản phẩm.");
        }
        chiTiet.setSoLuong(soLuongMoi);
        gioHangChiTietRepository.save(chiTiet);
    }

    @org.springframework.transaction.annotation.Transactional
    public void xoaTatCa(Integer idTaiKhoan) {
        List<GioHangChiTiet> danhSach = layDanhSachSanPhamTrongGio(idTaiKhoan);
        gioHangChiTietRepository.deleteAll(danhSach);
    }

    @org.springframework.transaction.annotation.Transactional
    public void cleanPendingOrders(Integer idTaiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            return;
        }
        List<HoaDon> existingOrders = hoaDonRepository.findByKhachHang_Id(kh.getId());
        for (HoaDon oldOrder : existingOrders) {
            if ("cho_thanh_toan".equals(oldOrder.getTrangThaiDonHang()) && 
                "pending".equalsIgnoreCase(oldOrder.getPaymentStatus())) {
                List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_Id(oldOrder.getId());
                if (txs != null && !txs.isEmpty()) {
                    paymentTransactionRepository.deleteAll(txs);
                }
                List<HoaDonChiTiet> details = hoaDonChiTietRepository.findByHoaDon_Id(oldOrder.getId());
                hoaDonChiTietRepository.deleteAll(details);
                // Xóa lịch sử trạng thái đơn hàng (được tạo bởi database triggers/legacy flow) trước để tránh lỗi Foreign Key
                hoaDonRepository.deleteOrderStatusHistoryByOrderId(oldOrder.getId());
                hoaDonRepository.delete(oldOrder);
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public HoaDon createOrder(Integer idTaiKhoan, String hoTenNhan, String sdtNhan, String diaChiNhan,
                              Integer idDonViVanChuyen, String phuongThucThanhToan, String ghiChu,
                              Integer ghnToDistrictId, String ghnToWardCode, Integer ghnProvinceId,
                              Integer idDiaChiLuu, String voucherCode) {
        String cleanGhiChu = sanitizeInput(ghiChu);
        if (cleanGhiChu != null && cleanGhiChu.length() > 500) {
            log.warn("[SECURITY_ALERT] Invalid note length for user: {}", idTaiKhoan);
            throw new IllegalArgumentException("Ghi chú đơn hàng tối đa 500 ký tự.");
        }

        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            throw new RuntimeException("Tài khoản chưa được cập nhật thông tin Khách Hàng!");
        }

        String finalHoTenNhan;
        String finalSdtNhan;
        String finalDiaChiNhan;
        Integer finalDistrictId = ghnToDistrictId;
        String finalWardCode = ghnToWardCode;
        Integer finalProvinceId = ghnProvinceId;

        if (idDiaChiLuu != null) {
            // Reload & validate inside same transaction
            SoDiaChi soDiaChi = soDiaChiRepository.findById(idDiaChiLuu)
                    .orElseThrow(() -> new IllegalArgumentException("Địa chỉ đã lưu không tồn tại."));
            if (!soDiaChi.getKhachHang().getId().equals(kh.getId())) {
                log.warn("[SECURITY_ALERT] Customer ID {} attempted to use unauthorized address ID {}", kh.getId(), idDiaChiLuu);
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền sử dụng địa chỉ này.");
            }

            // Resolve GHN mapping on server
            GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddress(soDiaChi);
            if (mapping == null || mapping.getDistrictId() == null || mapping.getWardCode() == null) {
                throw new IllegalArgumentException("Địa chỉ đã lưu của bạn chưa được chuẩn hóa địa chỉ GHN. Vui lòng cập nhật sổ địa chỉ hoặc chọn \"Nhập địa chỉ mới\".");
            }

            finalHoTenNhan = soDiaChi.getHoNguoiNhan() + " " + soDiaChi.getTenNguoiNhan();
            finalSdtNhan = soDiaChi.getSdtNguoiNhan();
            finalDiaChiNhan = soDiaChi.getDiaChiCuThe() + ", " + soDiaChi.getTinhThanh() + ", " + soDiaChi.getQuocGia();
            finalDistrictId = mapping.getDistrictId();
            finalWardCode = mapping.getWardCode();
            finalProvinceId = mapping.getProvinceId();
        } else {
            finalHoTenNhan = sanitizeInput(hoTenNhan);
            finalSdtNhan = sanitizeInput(sdtNhan);
            finalDiaChiNhan = sanitizeInput(diaChiNhan);

            if (finalHoTenNhan == null || finalHoTenNhan.isEmpty()) {
                throw new IllegalArgumentException("Họ và tên người nhận không được để trống.");
            }
            if (finalHoTenNhan.length() < 2 || finalHoTenNhan.length() > 100) {
                throw new IllegalArgumentException("Họ và tên người nhận phải từ 2 đến 100 ký tự.");
            }

            if (finalSdtNhan == null || finalSdtNhan.isEmpty()) {
                throw new IllegalArgumentException("Số điện thoại không được để trống.");
            }
            if (!finalSdtNhan.matches("^(\\+84|0)(3|5|7|8|9)[0-9]{8}$")) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 0 hoặc +84).");
            }

            if (finalDiaChiNhan == null || finalDiaChiNhan.isEmpty()) {
                throw new IllegalArgumentException("Địa chỉ nhận hàng không được để trống.");
            }
            if (finalDiaChiNhan.length() < 5 || finalDiaChiNhan.length() > 255) {
                throw new IllegalArgumentException("Địa chỉ nhận hàng phải từ 5 đến 255 ký tự.");
            }
        }

        // Clean up previous unpaid/pending orders for this customer to prevent duplicate display in Order History
        cleanPendingOrders(idTaiKhoan);

        List<GioHangChiTiet> cartItems = layDanhSachSanPhamTrongGio(idTaiKhoan);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        // Validate items eligibility (stock & status) with lock
        BigDecimal tamTinh = BigDecimal.ZERO;
        for (GioHangChiTiet item : cartItems) {
            SanPhamChiTiet lockedSpct = sanPhamChiTietRepository.findByIdWithLock(item.getSanPhamChiTiet().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            SanPham sp = lockedSpct.getSanPham();
            int tonKho = lockedSpct.getSoLuongTon();
            String trangThai = sp.getTrangThai();
            boolean hopLe = item.getSoLuong() != null && item.getSoLuong() > 0 && tonKho >= item.getSoLuong() && (trangThai == null || "dang_ban".equals(trangThai));
            if (!hopLe) {
                throw new RuntimeException("Sản phẩm '" + sp.getTenSanPham() + "' không đủ hàng tồn kho hoặc đã ngưng kinh doanh!");
            }
            BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(lockedSpct);
            tamTinh = tamTinh.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
        }

        // Load carrier using cached list
        DonViVanChuyen dvvc = adminShippingService.getAllCarriers().stream()
                .filter(c -> c.getId().equals(idDonViVanChuyen))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị vận chuyển"));

        // Server-side validation: enforce GHN location selection if GHN carrier is selected
        String carrierName = dvvc.getTenDonVi() != null ? dvvc.getTenDonVi().toUpperCase() : "";
        boolean isGhn = carrierName.contains("GIAO HÀNG NHANH") || carrierName.contains("GHN");
        if (isGhn) {
            if (finalDistrictId == null || finalWardCode == null || finalWardCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã (GHN) để sử dụng đơn vị vận chuyển Giao Hàng Nhanh.");
            }
        }

        BigDecimal phiShip = shippingFeeCalculator.calculateFee(dvvc, finalDistrictId, finalDiaChiNhan);

        // Voucher discount calculation
        BigDecimal giamGia = BigDecimal.ZERO;
        PhieuGiamGia appliedVoucher = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            PhieuGiamGia voucher = phieuGiamGiaRepository.findByMaPhieuWithLock(voucherCode.trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));
            if (!voucher.getActive()) {
                throw new IllegalArgumentException("Mã giảm giá không còn hoạt động.");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(voucher.getNgayBatDau())) {
                throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian áp dụng.");
            }
            if (now.isAfter(voucher.getNgayKetThuc())) {
                throw new IllegalArgumentException("Mã giảm giá đã hết hạn sử dụng.");
            }
            if (voucher.getSoLuongConLai() <= 0) {
                throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
            }
            if (tamTinh.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá này.");
            }

            giamGia = VoucherCalculator.calculateVoucherDiscount(tamTinh, voucher);

            voucher.setSoLuongConLai(voucher.getSoLuongConLai() - 1);
            phieuGiamGiaRepository.save(voucher);
            appliedVoucher = voucher;
        }

        BigDecimal totalAmount = tamTinh.subtract(giamGia).add(phiShip);

        // Find or create Payment Method
        String ptttName = "COD".equalsIgnoreCase(phuongThucThanhToan) ? "COD" : "SePay";
        List<PhuongThucThanhToan> allPttt = phuongThucThanhToanDAO.findAll();
        PhuongThucThanhToan pttt = allPttt.stream()
                .filter(p -> ptttName.equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan newP = new PhuongThucThanhToan();
                    newP.setTenPhuongThuc(ptttName);
                    return phuongThucThanhToanDAO.save(newP);
                });

        // Create HoaDon
        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(dvvc);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(totalAmount);
        hd.setPhiVanChuyen(phiShip);
        hd.setPhieuGiamGia(appliedVoucher);
        
        hd.setSoTienGiamVoucher(giamGia);
        if (appliedVoucher != null) {
            hd.setMaVoucherApDung(appliedVoucher.getMaPhieu());
            hd.setTenVoucherApDung("Voucher " + appliedVoucher.getMaPhieu());
            String limitDesc = appliedVoucher.getGiaTriGiamToiDa() != null ? " (Giảm tối đa " + appliedVoucher.getGiaTriGiamToiDa() + "đ)" : "";
            hd.setMoTaVoucherSnapshot("Giảm " + appliedVoucher.getGiaTri() + ("%".equals(appliedVoucher.getDonVi()) ? "%" : "đ") + limitDesc + " cho đơn hàng từ " + appliedVoucher.getGiaTriDonHangToiThieu() + "đ");
        } else {
            hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        }
        
        // Generate secure maDonHang: DHSVN + YYYYMMDDHHMMSS + - + 6 UUID characters
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuidStr = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        hd.setMaDonHang("DHSVN" + dateStr + "-" + uuidStr);
        
        if ("COD".equalsIgnoreCase(ptttName)) {
            hd.setTrangThaiDonHang(OrderStatus.CHO_XAC_NHAN.getValue()); // "cho_xac_nhan"
            hd.setPaymentMethod(PaymentMethod.COD.getValue()); // "cod"
        } else {
            hd.setTrangThaiDonHang(OrderStatus.CHO_THANH_TOAN.getValue()); // "cho_thanh_toan"
            hd.setPaymentMethod(PaymentMethod.SEPAY.getValue()); // "sepay"
        }
        
        hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        hd.setDiaChiNhan(finalDiaChiNhan);
        hd.setSdtNhan(finalSdtNhan);
        hd.setGhiChu(cleanGhiChu);
        hd.setPaymentStatus(PaymentStatus.PENDING.getValue()); // "pending"

        // Lưu thông tin địa chỉ GHN để tạo đơn vận chuyển sau này
        if (finalDistrictId != null) {
            hd.setGhnToDistrictId(finalDistrictId);
        }
        if (finalWardCode != null && !finalWardCode.isBlank()) {
            hd.setGhnToWardCode(finalWardCode);
        }

        hd = hoaDonRepository.save(hd);

        // Create HoaDonChiTiet
        for (GioHangChiTiet item : cartItems) {
            SanPhamChiTiet spct = item.getSanPhamChiTiet();
            SanPhamChiTiet lockedSpct = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));



            PriceSnapshot priceSnapshot = pricingService.buildPriceSnapshot(lockedSpct);

            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setHoaDon(hd);
            hdct.setSanPhamChiTiet(lockedSpct);
            hdct.setSoLuong(item.getSoLuong());
            hdct.setDonGia(priceSnapshot.giaBanSauGiam());
            hdct.setGiaNiemYet(priceSnapshot.giaNiemYet());
            hdct.setPhanTramGiam(priceSnapshot.phanTramGiam());
            hdct.setSoTienGiamSanPham(priceSnapshot.soTienGiamSanPham());
            hdct.setTenDotGiamGia(priceSnapshot.tenDotGiamGia());
            hdct.setIdDotGiamGia(priceSnapshot.idDotGiamGia());
            
            hdct.setTenSanPhamSnapshot(lockedSpct.getSanPham().getTenSanPham());
            
            String sku = null;
            try {
                java.lang.reflect.Method getSkuMethod = lockedSpct.getClass().getMethod("getSku");
                sku = (String) getSkuMethod.invoke(lockedSpct);
            } catch (Exception e) {
                // ignore
            }
            if (sku == null || sku.isBlank()) {
                sku = "SKU-" + lockedSpct.getSanPham().getId() + "-" + lockedSpct.getId();
            }
            hdct.setSkuSnapshot(sku);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Màu sắc: ").append(lockedSpct.getMauSac() != null ? lockedSpct.getMauSac() : "N/A");
            if (lockedSpct.getTrongLuong() != null && !lockedSpct.getTrongLuong().trim().isEmpty()) {
                sb.append(" | Trọng lượng: ").append(lockedSpct.getTrongLuong());
            }
            if (lockedSpct.getMucCang() != null && !lockedSpct.getMucCang().trim().isEmpty()) {
                sb.append(" | Mức căng: ").append(lockedSpct.getMucCang());
            }
            hdct.setThuocTinhSnapshot(sb.toString());
            
            if (lockedSpct.getSanPham().getThuongHieu() != null) {
                hdct.setThuongHieuSnapshot(lockedSpct.getSanPham().getThuongHieu().getTenThuongHieu());
            }
            if (lockedSpct.getSanPham().getDanhMuc() != null) {
                hdct.setDanhMucSnapshot(lockedSpct.getSanPham().getDanhMuc().getTenDanhMuc());
            }

            hoaDonChiTietRepository.save(hdct);
        }

        // For COD orders: clear cart + tạo đơn GHN
        if ("COD".equalsIgnoreCase(ptttName)) {
            xoaTatCa(idTaiKhoan);

            // Tạo đơn vận chuyển GHN (nếu có thông tin địa chỉ GHN)
            if (finalDistrictId != null && finalWardCode != null && !finalWardCode.isBlank()) {
                final HoaDon finalHd = hd;
                final List<HoaDonChiTiet> savedItems = hoaDonChiTietRepository.findByHoaDon_Id(finalHd.getId());
                try {
                    String ghnCode = ghnService.createShippingOrder(finalHd, savedItems, finalDistrictId, finalWardCode);
                    if (ghnCode != null) {
                        finalHd.setGhnOrderCode(ghnCode);
                        finalHd.setGhnStatus("ready_to_pick");
                        finalHd.setGhnToDistrictId(finalDistrictId);
                        finalHd.setGhnToWardCode(finalWardCode);
                        hd = hoaDonRepository.save(finalHd);
                        log.info("[GHN] Tạo đơn vận chuyển thành công cho HoaDon #{}: {}", finalHd.getId(), ghnCode);
                    }
                } catch (Exception e) {
                    log.error("[GHN] Lỗi tạo đơn GHN cho HoaDon #{}: {}", finalHd.getId(), e.getMessage());
                    // Không throw – lỗi GHN không làm hỏng đơn hàng
                }
            }
        }

        return hd;
    }
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Safelist.none()).trim();
    }
}
