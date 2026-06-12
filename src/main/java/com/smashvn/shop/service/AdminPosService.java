package com.smashvn.shop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPosService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final AuditService auditService;
    private final SanPhamRepository sanPhamRepository;

    // Tìm kiếm biến thể sản phẩm đang bán kèm lọc danh mục & thương hiệu
    public List<SanPhamChiTiet> searchActiveVariants(String query, Integer idDanhMuc, Integer idThuongHieu) {
        List<SanPhamChiTiet> all = sanPhamChiTietRepository.findAll();
        return all.stream()
                .filter(v -> "dang_ban".equals(v.getSanPham().getTrangThai()))
                .filter(v -> idDanhMuc == null || idDanhMuc == -1 || (v.getSanPham().getDanhMuc() != null && v.getSanPham().getDanhMuc().getId().equals(idDanhMuc)))
                .filter(v -> idThuongHieu == null || idThuongHieu == -1 || (v.getSanPham().getThuongHieu() != null && v.getSanPham().getThuongHieu().getId().equals(idThuongHieu)))
                .filter(v -> {
                    if (query == null || query.trim().isEmpty()) {
                        return true;
                    }
                    String lowerQuery = query.toLowerCase().trim();
                    return v.getSanPham().getTenSanPham().toLowerCase().contains(lowerQuery)
                            || v.getMauSac().toLowerCase().contains(lowerQuery)
                            || v.getTrongLuong().toLowerCase().contains(lowerQuery)
                            || v.getMucCang().toLowerCase().contains(lowerQuery)
                            || (v.getSanPham().getDanhMuc() != null && v.getSanPham().getDanhMuc().getTenDanhMuc().toLowerCase().contains(lowerQuery))
                            || (v.getSanPham().getThuongHieu() != null && v.getSanPham().getThuongHieu().getTenThuongHieu().toLowerCase().contains(lowerQuery));
                })
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm khách hàng — tương thích multi-role (dùng flag la_khach_hang =
     * true). Loại trừ tài khoản Khách Lẻ nội bộ (guest@smashvn.com).
     */
    public List<KhachHang> searchCustomers(String query) {
        List<KhachHang> customers = khachHangRepository.findByLaKhachHangTrue()
                .stream()
                .filter(c -> !"guest@smashvn.com".equals(c.getTaiKhoan().getEmail()))
                .collect(Collectors.toList());

        if (query == null || query.trim().isEmpty()) {
            return customers;
        }
        String lowerQuery = query.toLowerCase().trim();
        return customers.stream()
                .filter(c -> c.getHoKh().toLowerCase().contains(lowerQuery)
                || c.getTenKh().toLowerCase().contains(lowerQuery)
                || c.getSoDienThoaiKh().contains(lowerQuery)
                || c.getTaiKhoan().getEmail().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // Lấy thông tin voucher và xác thực
    public PhieuGiamGia checkVoucher(String maVoucher, BigDecimal tongTien) {
        if (maVoucher == null || maVoucher.trim().isEmpty()) {
            return null;
        }
        PhieuGiamGia voucher = phieuGiamGiaRepository.findByMaPhieu(maVoucher.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher: " + maVoucher));

        if (!voucher.getActive()) {
            throw new RuntimeException("Voucher đã ngưng hoạt động.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) {
            throw new RuntimeException("Voucher chưa bắt đầu sử dụng.");
        }
        if (voucher.getNgayKetThuc() != null && now.isAfter(voucher.getNgayKetThuc())) {
            throw new RuntimeException("Voucher đã hết hạn sử dụng.");
        }
        if (voucher.getSoLuongConLai() != null && voucher.getSoLuongConLai() <= 0) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng.");
        }
        if (tongTien.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher: " + voucher.getGiaTriDonHangToiThieu() + " đ");
        }
        return voucher;
    }

    // DTO cho POS item
    public static class PosItem {

        public Integer idSanPhamChiTiet;
        public Integer soLuong;
    }

    /**
     * Hàm thanh toán POS trong transaction an toàn.
     *
     * @param phuongThucPos TIEN_MAT | CHUYEN_KHOAN (phương thức thanh toán POS)
     * @param ghiChu Ghi chú hóa đơn (nullable)
     */
    @Transactional
    public HoaDon thanhToanPos(Integer idKhachHang, String maVoucher, List<PosItem> items,
            String phuongThucPos, String maGiaoDich, String ghiChu,
            Integer idNhanVienTaiKhoan, String clientIp) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm nào!");
        }

        // 1. Xác định nhân viên thực hiện
        TaiKhoan nvTk = taiKhoanRepository.findById(idNhanVienTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Nhân viên thực hiện giao dịch không hợp lệ!"));

        NhanVien nhanVien = nhanVienRepository.findByTaiKhoanId(nvTk.getId());

        // 2. Xác định khách hàng (Nếu không có, dùng tài khoản Khách Lẻ mặc định)
        KhachHang khachHang;
        if (idKhachHang == null || idKhachHang == -1) {
            TaiKhoan guestTk = taiKhoanRepository.findByEmail("guest@smashvn.com");
            if (guestTk == null) {
                TaiKhoan tk = new TaiKhoan();
                tk.setEmail("guest@smashvn.com");
                tk.setMatKhau("GUEST_NO_PASSWORD");
                tk.setVaiTro("KH");
                tk.setTrangThai("hoat_dong");
                guestTk = taiKhoanRepository.save(tk);
            }
            final TaiKhoan guestTkFinal = guestTk;
            khachHang = khachHangRepository.findByTaiKhoan_Email("guest@smashvn.com").orElseGet(() -> {
                KhachHang kh = new KhachHang();
                kh.setTaiKhoan(guestTkFinal);
                kh.setHoKh("Khách");
                kh.setTenKh("Lẻ");
                kh.setSoDienThoaiKh("0000000000");
                kh.setNhanBanTin(false);
                kh.setLaTaiKhoanNoiBo(false);
                return khachHangRepository.save(kh);
            });
        } else {
            khachHang = khachHangRepository.findById(idKhachHang)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng trong hệ thống!"));
        }

        // 3. Ánh xạ phương thức thanh toán POS → PhuongThucThanhToan (giữ FK cho tương thích)
        String tenPhuongThucCan = "CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? "chuyển khoản" : "tiền mặt";
        List<PhuongThucThanhToan> allPttt = phuongThucThanhToanDAO.findAll();
        PhuongThucThanhToan pttt;
        if (allPttt.isEmpty()) {
            PhuongThucThanhToan defaultPttt = new PhuongThucThanhToan();
            defaultPttt.setTenPhuongThuc("CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? "Chuyển khoản" : "Tiền mặt");
            pttt = phuongThucThanhToanDAO.save(defaultPttt);
        } else {
            pttt = allPttt.stream()
                    .filter(p -> p.getTenPhuongThuc().toLowerCase().contains(tenPhuongThucCan))
                    .findFirst()
                    .orElse(allPttt.get(0));
        }

        // 4. Lấy đơn vị vận chuyển (Bán tại quầy)
        DonViVanChuyen dvvc = null;
        List<DonViVanChuyen> allDvvc = donViVanChuyenDAO.findAll();
        if (allDvvc.isEmpty()) {
            DonViVanChuyen defaultDvvc = new DonViVanChuyen();
            defaultDvvc.setTenDonVi("Mua tại quầy");
            defaultDvvc.setHotline("000000");
            dvvc = donViVanChuyenDAO.save(defaultDvvc);
        } else {
            dvvc = allDvvc.stream()
                    .filter(d -> d.getTenDonVi().toLowerCase().contains("quầy") || d.getTenDonVi().toLowerCase().contains("chỗ"))
                    .findFirst()
                    .orElse(allDvvc.get(0));
        }

        // 5. Khóa và kiểm tra tồn kho các sản phẩm chi tiết bằng Pessimistic Write
        BigDecimal tongTienHang = BigDecimal.ZERO;
        List<HoaDonChiTiet> listCt = new java.util.ArrayList<>();

        for (PosItem item : items) {
            if (item.soLuong == null || item.soLuong <= 0) {
                throw new RuntimeException("Số lượng sản phẩm không hợp lệ!");
            }
            SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(item.idSanPhamChiTiet)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm ID: " + item.idSanPhamChiTiet));

            if (!"dang_ban".equals(spct.getSanPham().getTrangThai())) {
                throw new RuntimeException("Sản phẩm '" + spct.getSanPham().getTenSanPham() + "' hiện đã ngưng kinh doanh!");
            }

            if (spct.getSoLuongTon() < item.soLuong) {
                throw new RuntimeException("Sản phẩm '" + spct.getSanPham().getTenSanPham() + " [" + spct.getMauSac() + "]' không đủ hàng tồn kho! Còn lại: " + spct.getSoLuongTon());
            }

            // Trừ tồn kho
            spct.setSoLuongTon(spct.getSoLuongTon() - item.soLuong);
            sanPhamChiTietRepository.save(spct);

            // Tính tiền
            BigDecimal itemTotal = spct.getGiaBan().multiply(new BigDecimal(item.soLuong));
            tongTienHang = tongTienHang.add(itemTotal);

            // Lưu tạm chi tiết
            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setSanPhamChiTiet(spct);
            hdct.setSoLuong(item.soLuong);
            hdct.setDonGia(spct.getGiaBan());
            listCt.add(hdct);
        }

        // 6. Xử lý Voucher và Khóa Pessimistic Write voucher
        PhieuGiamGia phieu = null;
        BigDecimal giamGia = BigDecimal.ZERO;

        if (maVoucher != null && !maVoucher.trim().isEmpty()) {
            phieu = phieuGiamGiaRepository.findByMaPhieuWithLock(maVoucher.trim())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher: " + maVoucher));

            if (!phieu.getActive()) {
                throw new RuntimeException("Voucher đã ngưng hoạt động.");
            }
            LocalDateTime now = LocalDateTime.now();
            if (phieu.getNgayBatDau() != null && now.isBefore(phieu.getNgayBatDau())) {
                throw new RuntimeException("Voucher chưa bắt đầu.");
            }
            if (phieu.getNgayKetThuc() != null && now.isAfter(phieu.getNgayKetThuc())) {
                throw new RuntimeException("Voucher đã hết hạn.");
            }
            if (phieu.getSoLuongConLai() != null && phieu.getSoLuongConLai() <= 0) {
                throw new RuntimeException("Voucher đã hết lượt sử dụng.");
            }
            if (tongTienHang.compareTo(phieu.getGiaTriDonHangToiThieu()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu của voucher (" + phieu.getGiaTriDonHangToiThieu() + " đ)");
            }

            // Tính tiền giảm
            if ("%".equals(phieu.getDonVi())) {
                BigDecimal percent = phieu.getGiaTri().divide(new BigDecimal("100"));
                giamGia = tongTienHang.multiply(percent);
            } else {
                giamGia = phieu.getGiaTri();
            }

            if (giamGia.compareTo(tongTienHang) > 0) {
                giamGia = tongTienHang;
            }

            // Trừ lượt sử dụng voucher
            phieu.setSoLuongConLai(phieu.getSoLuongConLai() - 1);
            phieuGiamGiaRepository.save(phieu);
        }

        BigDecimal tongTienCuoi = tongTienHang.subtract(giamGia);
        if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) {
            tongTienCuoi = BigDecimal.ZERO;
        }

        // 7. Tạo hóa đơn — trạng thái DA_THANH_TOAN vì nhân viên đã xác nhận nhận tiền
        HoaDon hd = new HoaDon();
        hd.setKhachHang(khachHang);
        hd.setNhanVien(nhanVien);
        hd.setPhuongThucThanhToan(pttt);
        hd.setPhieuGiamGia(phieu);
        hd.setDonViVanChuyen(dvvc);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(tongTienCuoi);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setTrangThaiDonHang("da_giao");           // Bán tại quầy → hoàn thành ngay
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");   // Nhân viên đã xác nhận
        hd.setDiaChiNhan("Bán tại quầy");
        hd.setSdtNhan(khachHang.getSoDienThoaiKh() != null && !khachHang.getSoDienThoaiKh().trim().isEmpty()
                ? khachHang.getSoDienThoaiKh()
                : "0000000000");
        hd.setGhiChu(ghiChu);
        hd.setMaGiaoDich(maGiaoDich);
        hd.setNguoiXacNhanThanhToan(nhanVien != null ? nhanVien.getHoTenNv() : "Nhân viên hệ thống");
        hd.setThoiGianXacNhan(LocalDateTime.now());

        hd = hoaDonRepository.save(hd);

        // 8. Lưu các chi tiết hóa đơn
        for (HoaDonChiTiet ct : listCt) {
            ct.setHoaDon(hd);
            hoaDonChiTietRepository.save(ct);
        }

        // 9. Ghi nhật ký kiểm toán
        String ptLabel = "CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? "Chuyển khoản" : "Tiền mặt";
        auditService.log(nvTk.getId(), "HoaDon", Long.valueOf(hd.getId()), "INSERT", null, null, clientIp,
                "Thanh toán POS - " + ptLabel + " - Tổng tiền: " + tongTienCuoi + " đ (Mã: HD-" + hd.getId() + ")", nvTk.getVaiTro());

        return hd;
    }
}
