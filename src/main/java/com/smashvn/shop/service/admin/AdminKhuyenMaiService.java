package com.smashvn.shop.service.admin;
import com.smashvn.shop.service.AuditService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminKhuyenMaiService {

    private final DotGiamGiaDAO dotGiamGiaDAO;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final SanPhamRepository sanPhamRepository;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==========================================
    // CAMPAIGN (ĐỢT GIẢM GIÁ) SERVICE METHODS
    // ==========================================
    @Transactional(readOnly = true)
    public List<DotGiamGia> getAllDotGiamGia() {
        return dotGiamGiaDAO.findAll();
    }

    @Transactional(readOnly = true)
    public DotGiamGia getDotGiamGiaById(Integer id) {
        return dotGiamGiaDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá id: " + id));
    }

    @Transactional
    public DotGiamGia createDotGiamGia(String tenChienDich, LocalDateTime start, LocalDateTime end,
            Integer phanTramGiam, String loaiGiamGia, List<Integer> productIds,
            Integer actingTaiKhoanId, String ipAddress) {
        // 1. Validation
        if (tenChienDich == null || tenChienDich.trim().isEmpty()) {
            throw new RuntimeException("Tên chiến dịch không được để trống!");
        }
        String cleanTen = tenChienDich.trim();
        String sanitizedTen = org.jsoup.Jsoup.clean(cleanTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new RuntimeException("Tên chiến dịch phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (!"Theo Phần Trăm".equals(loaiGiamGia) && !"Theo Khoảng".equals(loaiGiamGia)) {
            throw new RuntimeException("Loại giảm giá không hợp lệ! Chỉ cho phép 'Theo Phần Trăm' hoặc 'Theo Khoảng'.");
        }

        validateCampaignDates(start, end);
        if (phanTramGiam == null || phanTramGiam < 1 || phanTramGiam > 100) {
            throw new RuntimeException("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến 100%!");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm để áp dụng đợt giảm giá!");
        }

        // 2. Conflict overlap check
        checkCampaignOverlaps(productIds, start, end, null);

        // 3. Resolve acting employee
        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv == null) {
            throw new RuntimeException("Tài khoản đang thực hiện không có thông tin nhân viên!");
        }

        // 4. Save campaign entity first
        DotGiamGia dgg = new DotGiamGia();
        dgg.setTenChienDich(sanitizedTen);
        dgg.setNgayBatDau(start);
        dgg.setNgayKetThuc(end);
        dgg.setPhanTramGiam(phanTramGiam);
        dgg.setLoaiGiamGia(loaiGiamGia);
        dgg.setNhanVien(nv);
        dgg.setActive(true);

        // Load and associate products directly to campaign (Managed from DotGiamGia side)
        Set<SanPham> selectedProducts = new HashSet<>(sanPhamRepository.findAllById(productIds));
        dgg.setSanPhams(selectedProducts);

        DotGiamGia saved = dotGiamGiaDAO.save(dgg);

        // 5. Audit Log
        writeEditLog(actingTaiKhoanId, "DotGiamGia", saved.getId().longValue(), "INSERT",
                null, formatCampaignState(saved), ipAddress, "Tạo mới đợt giảm giá: " + sanitizedTen);

        return saved;
    }

    @Transactional
    public DotGiamGia updateDotGiamGia(Integer id, String tenChienDich, LocalDateTime start, LocalDateTime end,
            Integer phanTramGiam, String loaiGiamGia, List<Integer> productIds,
            Integer actingTaiKhoanId, String ipAddress) {
        DotGiamGia dgg = getDotGiamGiaById(id);

        // 1. Validation
        if (tenChienDich == null || tenChienDich.trim().isEmpty()) {
            throw new RuntimeException("Tên chiến dịch không được để trống!");
        }
        String cleanTen = tenChienDich.trim();
        String sanitizedTen = org.jsoup.Jsoup.clean(cleanTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new RuntimeException("Tên chiến dịch phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (!"Theo Phần Trăm".equals(loaiGiamGia) && !"Theo Khoảng".equals(loaiGiamGia)) {
            throw new RuntimeException("Loại giảm giá không hợp lệ! Chỉ cho phép 'Theo Phần Trăm' hoặc 'Theo Khoảng'.");
        }

        validateCampaignDates(start, end);
        if (phanTramGiam == null || phanTramGiam < 1 || phanTramGiam > 100) {
            throw new RuntimeException("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến 100%!");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm để áp dụng đợt giảm giá!");
        }

        // 2. Conflict overlap check
        checkCampaignOverlaps(productIds, start, end, id);

        String oldState = formatCampaignState(dgg);

        // 3. Update properties
        dgg.setTenChienDich(sanitizedTen);
        dgg.setNgayBatDau(start);
        dgg.setNgayKetThuc(end);
        dgg.setPhanTramGiam(phanTramGiam);
        dgg.setLoaiGiamGia(loaiGiamGia);

        // Load and assign products directly to campaign (Managed from DotGiamGia side)
        Set<SanPham> selectedProducts = new HashSet<>(sanPhamRepository.findAllById(productIds));
        dgg.getSanPhams().clear();
        dgg.getSanPhams().addAll(selectedProducts);

        DotGiamGia updated = dotGiamGiaDAO.save(dgg);

        // 4. Audit Log
        writeEditLog(actingTaiKhoanId, "DotGiamGia", updated.getId().longValue(), "UPDATE",
                oldState, formatCampaignState(updated), ipAddress, "Cập nhật đợt giảm giá: " + sanitizedTen);

        return updated;
    }

    @Transactional
    public void deactivateDotGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        DotGiamGia dgg = getDotGiamGiaById(id);
        String oldState = formatCampaignState(dgg);

        dgg.setActive(false);
        DotGiamGia saved = dotGiamGiaDAO.save(dgg);

        writeEditLog(actingTaiKhoanId, "DotGiamGia", id.longValue(), "UPDATE",
                oldState, formatCampaignState(saved), ipAddress, "Vô hiệu hóa đợt giảm giá: " + dgg.getTenChienDich());
    }

    @Transactional
    public void deleteDotGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        DotGiamGia dgg = getDotGiamGiaById(id);
        String oldState = formatCampaignState(dgg);

        dgg.setActive(false); // Soft delete
        DotGiamGia saved = dotGiamGiaDAO.save(dgg);

        writeEditLog(actingTaiKhoanId, "DotGiamGia", id.longValue(), "DELETE",
                oldState, formatCampaignState(saved), ipAddress, "Xóa logic đợt giảm giá: " + dgg.getTenChienDich());
    }

    private void validateCampaignDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new RuntimeException("Thời gian bắt đầu và kết thúc không được để trống!");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc!");
        }
    }

    public void checkCampaignOverlaps(List<Integer> productIds, LocalDateTime start, LocalDateTime end, Integer excludeCampaignId) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        List<DotGiamGia> allCampaigns = dotGiamGiaDAO.findAll();
        for (DotGiamGia campaign : allCampaigns) {
            if (!campaign.getActive()) {
                continue;
            }
            if (excludeCampaignId != null && campaign.getId().equals(excludeCampaignId)) {
                continue;
            }

            boolean overlaps = campaign.getNgayBatDau().isBefore(end) && campaign.getNgayKetThuc().isAfter(start);
            if (overlaps) {
                List<String> conflictedProductNames = new ArrayList<>();
                for (SanPham sp : campaign.getSanPhams()) {
                    if (productIds.contains(sp.getId())) {
                        conflictedProductNames.add(sp.getTenSanPham());
                    }
                }

                if (!conflictedProductNames.isEmpty()) {
                    throw new RuntimeException(String.format(
                            "Sản phẩm: %s đã được gán cho chiến dịch '%s' đang hoạt động trong khoảng %s - %s. Không được đè đợt giảm giá lên nhau!",
                            String.join(", ", conflictedProductNames),
                            campaign.getTenChienDich(),
                            campaign.getNgayBatDau().format(DATE_FORMATTER),
                            campaign.getNgayKetThuc().format(DATE_FORMATTER)
                    ));
                }
            }
        }
    }

    private String formatCampaignState(DotGiamGia dgg) {
        int productCount = dgg.getSanPhams() != null ? dgg.getSanPhams().size() : 0;
        return String.format("Tên: %s, %s Giảm: %d, Bắt đầu: %s, Kết thúc: %s, Số SP: %d, Active: %b",
                dgg.getTenChienDich(),
                "Theo Phần Trăm".equals(dgg.getLoaiGiamGia()) ? "Phần Trăm" : "Loại Khác",
                dgg.getPhanTramGiam(),
                dgg.getNgayBatDau().format(DATE_FORMATTER),
                dgg.getNgayKetThuc().format(DATE_FORMATTER),
                productCount,
                dgg.getActive());
    }

    // ==========================================
    // VOUCHER (PHIẾU GIẢM GIÁ) SERVICE METHODS
    // ==========================================
    @Transactional(readOnly = true)
    public List<PhieuGiamGia> getAllPhieuGiamGia() {
        return phieuGiamGiaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PhieuGiamGia getPhieuGiamGiaById(Integer id) {
        return phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá id: " + id));
    }

    @Transactional
    public PhieuGiamGia createPhieuGiamGia(String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia,
            Integer actingTaiKhoanId, String ipAddress) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new RuntimeException("Mã phiếu không được để trống!");
        }
        String uppercaseCode = maPhieu.trim().toUpperCase();

        // 1. Validation
        validateVoucherInputs(uppercaseCode, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia);

        // Check duplicate code
        if (phieuGiamGiaRepository.existsByMaPhieuIgnoreCase(uppercaseCode)) {
            throw new RuntimeException("Mã phiếu giảm giá '" + uppercaseCode + "' đã tồn tại trên hệ thống!");
        }

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv == null) {
            throw new RuntimeException("Tài khoản đang thực hiện không có thông tin nhân viên!");
        }

        // 2. Save
        PhieuGiamGia pgg = new PhieuGiamGia();
        pgg.setMaPhieu(uppercaseCode);
        pgg.setGiaTri(giaTri);
        pgg.setDonVi(donVi);
        pgg.setNgayBatDau(start);
        pgg.setNgayKetThuc(end);
        pgg.setSoLuongConLai(soLuongConLai);
        pgg.setGiaTriDonHangToiThieu(giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu);
        pgg.setLoaiGiamGia(loaiGiamGia);
        pgg.setNhanVien(nv);
        pgg.setActive(true);

        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        // 3. Audit Log
        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", saved.getId().longValue(), "INSERT",
                null, formatVoucherState(saved), ipAddress, "Tạo mới voucher: " + uppercaseCode);

        return saved;
    }

    @Transactional
    public PhieuGiamGia updatePhieuGiamGia(Integer id, String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia,
            Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);

        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new RuntimeException("Mã phiếu không được để trống!");
        }
        String uppercaseCode = maPhieu.trim().toUpperCase();

        // 1. Validation
        validateVoucherInputs(uppercaseCode, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia);

        // Check duplicate code excluding current
        if (phieuGiamGiaRepository.existsByMaPhieuIgnoreCaseAndIdNot(uppercaseCode, id)) {
            throw new RuntimeException("Mã phiếu giảm giá '" + uppercaseCode + "' đã được sử dụng bởi voucher khác!");
        }

        String oldState = formatVoucherState(pgg);

        // 2. Update properties
        pgg.setMaPhieu(uppercaseCode);
        pgg.setGiaTri(giaTri);
        pgg.setDonVi(donVi);
        pgg.setNgayBatDau(start);
        pgg.setNgayKetThuc(end);
        pgg.setSoLuongConLai(soLuongConLai);
        pgg.setGiaTriDonHangToiThieu(giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu);
        pgg.setLoaiGiamGia(loaiGiamGia);

        PhieuGiamGia updated = phieuGiamGiaRepository.save(pgg);

        // 3. Audit Log
        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", updated.getId().longValue(), "UPDATE",
                oldState, formatVoucherState(updated), ipAddress, "Cập nhật voucher: " + uppercaseCode);

        return updated;
    }

    @Transactional
    public void deactivatePhieuGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);
        String oldState = formatVoucherState(pgg);

        pgg.setActive(false);
        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", id.longValue(), "UPDATE",
                oldState, formatVoucherState(saved), ipAddress, "Vô hiệu hóa voucher: " + pgg.getMaPhieu());
    }

    @Transactional
    public void deletePhieuGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);
        String oldState = formatVoucherState(pgg);

        pgg.setActive(false); // Soft delete
        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", id.longValue(), "DELETE",
                oldState, formatVoucherState(saved), ipAddress, "Xóa logic voucher: " + pgg.getMaPhieu());
    }

    private void validateVoucherInputs(String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new RuntimeException("Mã phiếu không được để trống!");
        }
        if (!maPhieu.matches("^[A-Z0-9_]{2,50}$")) {
            throw new RuntimeException("Mã phiếu giảm giá không hợp lệ! Chỉ cho phép ký tự chữ in hoa, số và dấu gạch dưới từ 2 đến 50 ký tự.");
        }
        if (giaTri == null || giaTri.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0!");
        }
        if (donVi == null || donVi.trim().isEmpty()) {
            throw new RuntimeException("Đơn vị giảm giá không được để trống!");
        }
        if (!"%".equals(donVi) && !"VNĐ".equals(donVi)) {
            throw new RuntimeException("Đơn vị giảm giá không hợp lệ! Chỉ cho phép '%' hoặc 'VNĐ'.");
        }
        if ("%".equals(donVi) && giaTri.compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException("Nếu giảm theo phần trăm, giá trị không được vượt quá 100%!");
        }
        if (!"Giảm trực tiếp".equals(loaiGiamGia) && !"Giảm phần trăm".equals(loaiGiamGia)) {
            throw new RuntimeException("Phân loại voucher không hợp lệ! Chỉ cho phép 'Giảm trực tiếp' hoặc 'Giảm phần trăm'.");
        }
        if (start == null || end == null) {
            throw new RuntimeException("Hạn sử dụng (ngày bắt đầu và kết thúc) không được để trống!");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc!");
        }
        if (soLuongConLai == null || soLuongConLai < 0) {
            throw new RuntimeException("Số lượng phiếu không được âm!");
        }
        if (giaTriDonHangToiThieu == null || giaTriDonHangToiThieu.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Giá trị đơn hàng tối thiểu không được âm!");
        }
    }

    private String formatVoucherState(PhieuGiamGia pgg) {
        return String.format("Mã: %s, Giá trị: %s %s, Hạn dùng: %s - %s, Tối thiểu: %s, Số lượng: %d, Active: %b",
                pgg.getMaPhieu(),
                pgg.getGiaTri().toString(),
                pgg.getDonVi(),
                pgg.getNgayBatDau().format(DATE_FORMATTER),
                pgg.getNgayKetThuc().format(DATE_FORMATTER),
                pgg.getGiaTriDonHangToiThieu().toString(),
                pgg.getSoLuongConLai(),
                pgg.getActive());
    }

    // ==========================================
    // COMMON AUDIT LOG WRITER
    // ==========================================
    private void writeEditLog(Integer actingTaiKhoanId, String tenBang, Long idBanGhi, String hanhDong,
            String giaTriCu, String giaTriMoi, String ipAddress, String ghiChu) {
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, tenBang, idBanGhi, hanhDong, giaTriCu, giaTriMoi, ipAddress, ghiChu, actingUser.getVaiTro());
        }
    }
}
