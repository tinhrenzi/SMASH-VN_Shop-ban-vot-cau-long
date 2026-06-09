package com.smashvn.shop.service;

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
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GioHangService {

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

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> themVaoGio(Integer idTaiKhoan, Integer idSanPhamChiTiet, Integer soLuong) {
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
        // Sử dụng findByIdWithLock để khóa dòng sản phẩm chi tiết bằng Pessimistic Write (Chống Overselling)
        SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // --- BÀI TOÁN VALIDATE TỒN KHO THỰC TẾ ---
        // 1. Xem trong giỏ của khách đã có bao nhiêu chiếc này rồi?
        int soLuongHienCo = (chiTiet != null && chiTiet.getSoLuong() != null) ? chiTiet.getSoLuong() : 0;

        // 2. Tính tổng số lượng khách sẽ có nếu thêm thành công
        int tongYeuCau = soLuongHienCo + soLuong;

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
                    .orElseThrow(() -> new RuntimeException("Lỗi cấu hình CSDL: Không tìm thấy ID trạng thái 1"));
            chiTiet.setTrangThai(trangThai);
        }

        gioHangChiTietRepository.save(chiTiet);

        // Đóng gói dữ liệu trả về cho Modal JS
        Map<String, Object> result = new HashMap<>();
        result.put("tenSanPham", spct.getSanPham().getTenSanPham());
        result.put("phanLoai", spct.getMauSac() + " | " + spct.getTrongLuong());
        result.put("giaBan", spct.getGiaBan());
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

            boolean hopLe = tonKho > 0 && (trangThai == null || trangThai.equals("dang_ban"));

            if (hopLe) {
                BigDecimal gia = item.getSanPhamChiTiet().getGiaBan();
                BigDecimal soLuong = new BigDecimal(item.getSoLuong());
                tongTien = tongTien.add(gia.multiply(soLuong));
                tongSoLuong += item.getSoLuong();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("idChiTiet", item.getId());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("hinhAnh", item.getSanPhamChiTiet().getHinhAnhSanPham());
            map.put("giaBan", item.getSanPhamChiTiet().getGiaBan());
            map.put("soLuong", item.getSoLuong());
            map.put("idSanPham", sp.getId());
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

        if (soLuongMoi > 0) {
            // Chống sửa vượt quá tồn kho (H-7)
            if (chiTiet.getSanPhamChiTiet().getSoLuongTon() < soLuongMoi) {
                throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + chiTiet.getSanPhamChiTiet().getSoLuongTon() + " sản phẩm.");
            }
            chiTiet.setSoLuong(soLuongMoi);
            gioHangChiTietRepository.save(chiTiet);
        } else {
            gioHangChiTietRepository.delete(chiTiet);
        }
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
                hoaDonRepository.delete(oldOrder);
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public HoaDon createOrder(Integer idTaiKhoan, String hoTenNhan, String sdtNhan, String diaChiNhan,
                              Integer idDonViVanChuyen, String phuongThucThanhToan, String ghiChu,
                              Integer ghnToDistrictId, String ghnToWardCode) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            throw new RuntimeException("Tài khoản chưa được cập nhật thông tin Khách Hàng!");
        }

        // Clean up previous unpaid/pending orders for this customer to prevent duplicate display in Order History
        cleanPendingOrders(idTaiKhoan);

        List<GioHangChiTiet> cartItems = layDanhSachSanPhamTrongGio(idTaiKhoan);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        // Validate items eligibility (stock & status)
        BigDecimal tamTinh = BigDecimal.ZERO;
        for (GioHangChiTiet item : cartItems) {
            SanPham sp = item.getSanPhamChiTiet().getSanPham();
            int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
            String trangThai = sp.getTrangThai();
            boolean hopLe = tonKho >= item.getSoLuong() && (trangThai == null || "dang_ban".equals(trangThai));
            if (!hopLe) {
                throw new RuntimeException("Sản phẩm '" + sp.getTenSanPham() + "' không đủ hàng tồn kho hoặc đã ngưng kinh doanh!");
            }
            tamTinh = tamTinh.add(item.getSanPhamChiTiet().getGiaBan().multiply(new BigDecimal(item.getSoLuong())));
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
            if (ghnToDistrictId == null || ghnToWardCode == null || ghnToWardCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã (GHN) để sử dụng đơn vị vận chuyển Giao Hàng Nhanh.");
            }
        }

        BigDecimal phiShip = shippingFeeCalculator.calculateFee(dvvc, diaChiNhan);
        BigDecimal totalAmount = tamTinh.add(phiShip);

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
        hd.setDiaChiNhan(diaChiNhan);
        hd.setSdtNhan(sdtNhan);
        hd.setGhiChu(ghiChu);
        hd.setPaymentStatus(PaymentStatus.PENDING.getValue()); // "pending"

        // Lưu thông tin địa chỉ GHN để tạo đơn vận chuyển sau này
        if (ghnToDistrictId != null) {
            hd.setGhnToDistrictId(ghnToDistrictId);
        }
        if (ghnToWardCode != null && !ghnToWardCode.isBlank()) {
            hd.setGhnToWardCode(ghnToWardCode);
        }

        hd = hoaDonRepository.save(hd);

        // Create HoaDonChiTiet
        for (GioHangChiTiet item : cartItems) {
            SanPhamChiTiet spct = item.getSanPhamChiTiet();

            // For COD orders: deduct stock immediately
            if ("COD".equalsIgnoreCase(ptttName)) {
                // Acquire pessimistic write lock & deduct stock
                SanPhamChiTiet lockedSpct = sanPhamChiTietRepository.findByIdWithLock(spct.getId())
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
                if (lockedSpct.getSoLuongTon() < item.getSoLuong()) {
                    throw new RuntimeException("Sản phẩm '" + lockedSpct.getSanPham().getTenSanPham() + "' không đủ hàng tồn kho!");
                }
                lockedSpct.setSoLuongTon(lockedSpct.getSoLuongTon() - item.getSoLuong());
                sanPhamChiTietRepository.save(lockedSpct);
            }

            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setHoaDon(hd);
            hdct.setSanPhamChiTiet(spct);
            hdct.setSoLuong(item.getSoLuong());
            hdct.setDonGia(spct.getGiaBan());
            hoaDonChiTietRepository.save(hdct);
        }

        // For COD orders: clear cart + tạo đơn GHN
        if ("COD".equalsIgnoreCase(ptttName)) {
            xoaTatCa(idTaiKhoan);

            // Tạo đơn vận chuyển GHN (nếu có thông tin địa chỉ GHN)
            if (ghnToDistrictId != null && ghnToWardCode != null && !ghnToWardCode.isBlank()) {
                final HoaDon finalHd = hd;
                final List<HoaDonChiTiet> savedItems = hoaDonChiTietRepository.findByHoaDon_Id(finalHd.getId());
                try {
                    String ghnCode = ghnService.createShippingOrder(finalHd, savedItems, ghnToDistrictId, ghnToWardCode);
                    if (ghnCode != null) {
                        finalHd.setGhnOrderCode(ghnCode);
                        finalHd.setGhnStatus("ready_to_pick");
                        finalHd.setGhnToDistrictId(ghnToDistrictId);
                        finalHd.setGhnToWardCode(ghnToWardCode);
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
}
