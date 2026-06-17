package com.smashvn.shop.service.admin;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.exception.PromotionValidationException;
import com.smashvn.shop.util.PromotionValidationConstants;

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
            throw new PromotionValidationException("Tên chiến dịch không được để trống!");
        }
        String cleanTen = tenChienDich.trim();
        String sanitizedTen = org.jsoup.Jsoup.clean(cleanTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new PromotionValidationException("Tên chiến dịch phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (!"Theo Phần Trăm".equals(loaiGiamGia) && !"Theo Khoảng".equals(loaiGiamGia)) {
            throw new PromotionValidationException("Loại giảm giá không hợp lệ! Chỉ cho phép 'Theo Phần Trăm' hoặc 'Theo Khoảng'.");
        }

        validateCampaignDates(start, end);
        if (phanTramGiam == null || phanTramGiam < 1 || phanTramGiam > PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT) {
            throw new PromotionValidationException("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến " + PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT + "%!");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new PromotionValidationException("Vui lòng chọn ít nhất một sản phẩm để áp dụng đợt giảm giá!");
        }

        // 2. Conflict overlap check
        checkCampaignOverlaps(productIds, start, end, null);

        // 3. Resolve acting employee
        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv == null) {
            throw new PromotionValidationException("Tài khoản đang thực hiện không có thông tin nhân viên!");
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
            throw new PromotionValidationException("Tên chiến dịch không được để trống!");
        }
        String cleanTen = tenChienDich.trim();
        String sanitizedTen = org.jsoup.Jsoup.clean(cleanTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new PromotionValidationException("Tên chiến dịch phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (!"Theo Phần Trăm".equals(loaiGiamGia) && !"Theo Khoảng".equals(loaiGiamGia)) {
            throw new PromotionValidationException("Loại giảm giá không hợp lệ! Chỉ cho phép 'Theo Phần Trăm' hoặc 'Theo Khoảng'.");
        }

        validateCampaignDates(start, end);
        if (phanTramGiam == null || phanTramGiam < 1 || phanTramGiam > PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT) {
            throw new PromotionValidationException("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến " + PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT + "%!");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new PromotionValidationException("Vui lòng chọn ít nhất một sản phẩm để áp dụng đợt giảm giá!");
        }

        // 2. Conflict overlap check
        checkCampaignOverlaps(productIds, start, end, id);

        String oldState = formatCampaignState(dgg);

        // Sync both sides of the ManyToMany relationship
        Set<SanPham> selectedProducts = new HashSet<>(sanPhamRepository.findAllById(productIds));
        
        // Remove this campaign from products that are no longer selected
        for (SanPham sp : dgg.getSanPhams()) {
            if (!selectedProducts.contains(sp)) {
                if (sp.getCacDotGiamGia() != null) {
                    sp.getCacDotGiamGia().remove(dgg);
                }
            }
        }
        
        // Add this campaign to products that are newly selected
        for (SanPham sp : selectedProducts) {
            if (!dgg.getSanPhams().contains(sp)) {
                if (sp.getCacDotGiamGia() == null) {
                    sp.setCacDotGiamGia(new HashSet<>());
                }
                sp.getCacDotGiamGia().add(dgg);
            }
        }

        // 3. Update properties
        dgg.setTenChienDich(sanitizedTen);
        dgg.setNgayBatDau(start);
        dgg.setNgayKetThuc(end);
        dgg.setPhanTramGiam(phanTramGiam);
        dgg.setLoaiGiamGia(loaiGiamGia);
        dgg.setActive(true); // Automatically reactivate campaign on edit/save

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
            throw new PromotionValidationException("Thời gian bắt đầu và kết thúc không được để trống!");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new PromotionValidationException("Ngày bắt đầu phải trước ngày kết thúc!");
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
                    throw new PromotionValidationException(String.format(
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
            BigDecimal giaTriGiamToiDa,
            Integer actingTaiKhoanId, String ipAddress) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new PromotionValidationException("Mã phiếu không được để trống!");
        }
        String uppercaseCode = maPhieu.trim().toUpperCase();

        // 1. Validation
        validateVoucherInputs(uppercaseCode, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, false);

        // Check duplicate code
        if (phieuGiamGiaRepository.existsByMaPhieuIgnoreCase(uppercaseCode)) {
            throw new PromotionValidationException("Mã phiếu giảm giá '" + uppercaseCode + "' đã tồn tại trên hệ thống!");
        }

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv == null) {
            throw new PromotionValidationException("Tài khoản đang thực hiện không có thông tin nhân viên!");
        }

        // Cap only applies to percentage vouchers; force null for fixed-amount
        BigDecimal resolvedCap = "%".equals(donVi) ? giaTriGiamToiDa : null;

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
        pgg.setGiaTriGiamToiDa(resolvedCap);
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
            BigDecimal giaTriGiamToiDa,
            Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);

        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new PromotionValidationException("Mã phiếu không được để trống!");
        }
        String uppercaseCode = maPhieu.trim().toUpperCase();

        // 1. Validation
        validateVoucherInputs(uppercaseCode, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, true);

        // Check duplicate code excluding current
        if (phieuGiamGiaRepository.existsByMaPhieuIgnoreCaseAndIdNot(uppercaseCode, id)) {
            throw new PromotionValidationException("Mã phiếu giảm giá '" + uppercaseCode + "' đã được sử dụng bởi voucher khác!");
        }

        String oldState = formatVoucherState(pgg);

        // Cap only applies to percentage vouchers; force null for fixed-amount
        BigDecimal resolvedCap = "%".equals(donVi) ? giaTriGiamToiDa : null;

        // 2. Update properties
        pgg.setMaPhieu(uppercaseCode);
        pgg.setGiaTri(giaTri);
        pgg.setDonVi(donVi);
        pgg.setNgayBatDau(start);
        pgg.setNgayKetThuc(end);
        pgg.setSoLuongConLai(soLuongConLai);
        pgg.setGiaTriDonHangToiThieu(giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu);
        pgg.setLoaiGiamGia(loaiGiamGia);
        pgg.setGiaTriGiamToiDa(resolvedCap);
        pgg.setActive(true); // Automatically reactivate voucher on edit/save

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

    private void checkNotDecimal(BigDecimal bd, String errorMessage) {
        if (bd != null && bd.stripTrailingZeros().scale() > 0) {
            throw new PromotionValidationException(errorMessage);
        }
    }

    private void validateVoucherInputs(String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia, BigDecimal giaTriGiamToiDa, boolean isUpdate) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new PromotionValidationException("Mã phiếu không được để trống!");
        }
        if (!maPhieu.matches("^[A-Z0-9_]{2,50}$")) {
            throw new PromotionValidationException("Mã phiếu giảm giá không hợp lệ! Chỉ cho phép ký tự chữ in hoa, số và dấu gạch dưới từ 2 đến 50 ký tự.");
        }
        if (donVi == null || donVi.trim().isEmpty()) {
            throw new PromotionValidationException("Đơn vị giảm giá không được để trống!");
        }
        if (!"%".equals(donVi) && !"VND".equals(donVi)) {
            throw new PromotionValidationException("Đơn vị giảm giá không hợp lệ! Chỉ cho phép '%' hoặc 'VND'.");
        }
        if (!"Giảm trực tiếp".equals(loaiGiamGia) && !"Giảm phần trăm".equals(loaiGiamGia)) {
            throw new PromotionValidationException("Phân loại voucher không hợp lệ! Chỉ cho phép 'Giảm trực tiếp' hoặc 'Giảm phần trăm'.");
        }

        // Cross-field consistency validations
        if ("%".equals(donVi) && !"Giảm phần trăm".equals(loaiGiamGia)) {
            throw new PromotionValidationException("Đơn vị '%' và loại giảm giá phải là 'Giảm phần trăm'!");
        }
        if ("VND".equals(donVi) && !"Giảm trực tiếp".equals(loaiGiamGia)) {
            throw new PromotionValidationException("Đơn vị 'VND' và loại giảm giá phải là 'Giảm trực tiếp'!");
        }

        // Value check
        if (giaTri == null) {
            throw new PromotionValidationException("Giá trị giảm giá không được để trống!");
        }
        if ("%".equals(donVi)) {
            checkNotDecimal(giaTri, "Giá trị giảm % phải là số nguyên, không được là số thập phân!");
            if (giaTri.compareTo(BigDecimal.ZERO) < 0) {
                throw new PromotionValidationException("Giá trị giảm % không được là số âm!");
            }
            if (giaTri.compareTo(BigDecimal.ZERO) == 0) {
                throw new PromotionValidationException("Giá trị giảm giá phải lớn hơn 0!");
            }
            if (giaTri.compareTo(new BigDecimal(PromotionValidationConstants.MAX_VOUCHER_PERCENT)) > 0) {
                throw new PromotionValidationException("Giá trị giảm % không được vượt quá " + PromotionValidationConstants.MAX_VOUCHER_PERCENT + "%!");
            }
        } else {
            checkNotDecimal(giaTri, "Giá trị giảm tiền (VND) phải là số nguyên, không được là số thập phân!");
            if (giaTri.compareTo(BigDecimal.ZERO) < 0) {
                throw new PromotionValidationException("Giá trị giảm tiền (VND) không được là số âm!");
            }
            if (giaTri.compareTo(BigDecimal.ZERO) == 0) {
                throw new PromotionValidationException("Giá trị giảm giá phải lớn hơn 0!");
            }
            if (giaTri.compareTo(PromotionValidationConstants.MAX_VND_VALUE) > 0) {
                throw new PromotionValidationException("Giá trị giảm tiền không được vượt quá 100,000,000 VNĐ!");
            }
        }

        if (start == null || end == null) {
            throw new PromotionValidationException("Hạn sử dụng (ngày bắt đầu và kết thúc) không được để trống!");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new PromotionValidationException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        // Quantity check
        if (soLuongConLai == null) {
            throw new PromotionValidationException("Số lượng voucher không được để trống!");
        }
        if (isUpdate) {
            if (soLuongConLai < 0) {
                throw new PromotionValidationException("Số lượng voucher không được là số âm!");
            }
        } else {
            if (soLuongConLai <= 0) {
                throw new PromotionValidationException("Số lượng voucher phải lớn hơn 0!");
            }
        }
        if (soLuongConLai > PromotionValidationConstants.MAX_QUANTITY) {
            throw new PromotionValidationException("Số lượng voucher không được vượt quá " + PromotionValidationConstants.MAX_QUANTITY + "!");
        }

        // Minimum order value check
        if (giaTriDonHangToiThieu == null) {
            throw new PromotionValidationException("Giá trị đơn hàng tối thiểu không được để trống!");
        }
        checkNotDecimal(giaTriDonHangToiThieu, "Giá trị đơn hàng tối thiểu phải là số nguyên, không được là số thập phân!");
        if (giaTriDonHangToiThieu.compareTo(BigDecimal.ZERO) < 0) {
            throw new PromotionValidationException("Giá trị đơn hàng tối thiểu không được là số âm!");
        }
        if (giaTriDonHangToiThieu.compareTo(PromotionValidationConstants.MAX_VND_VALUE) > 0) {
            throw new PromotionValidationException("Giá trị đơn hàng tối thiểu không được vượt quá 100,000,000 VNĐ!");
        }

        // Cap validation
        if ("%".equals(donVi)) {
            if (giaTriGiamToiDa == null) {
                throw new PromotionValidationException("Giá trị giảm tối đa là bắt buộc đối với voucher giảm theo phần trăm!");
            }
            checkNotDecimal(giaTriGiamToiDa, "Giá trị giảm tối đa phải là số nguyên, không được là số thập phân!");
            if (giaTriGiamToiDa.compareTo(BigDecimal.ZERO) < 0) {
                throw new PromotionValidationException("Giá trị giảm tối đa không được là số âm!");
            }
            if (giaTriGiamToiDa.compareTo(BigDecimal.ZERO) == 0) {
                throw new PromotionValidationException("Giá trị giảm tối đa phải lớn hơn 0!");
            }
            if (giaTriGiamToiDa.compareTo(PromotionValidationConstants.MAX_VND_VALUE) > 0) {
                throw new PromotionValidationException("Giá trị giảm tối đa không được vượt quá 100,000,000 VNĐ!");
            }
        } else {
            if (giaTriGiamToiDa != null) {
                throw new PromotionValidationException("Voucher giảm trực tiếp (VND) không được có giá trị giảm tối đa!");
            }
        }
    }

    private String formatVoucherState(PhieuGiamGia pgg) {
        String capStr = (pgg.getGiaTriGiamToiDa() == null) ? "Không giới hạn" : pgg.getGiaTriGiamToiDa().toPlainString();
        return String.format("Mã: %s, Giá trị: %s %s, Hạn dùng: %s - %s, Tối thiểu: %s, Tối đa: %s, Số lượng: %d, Active: %b",
                pgg.getMaPhieu(),
                pgg.getGiaTri().toString(),
                pgg.getDonVi(),
                pgg.getNgayBatDau().format(DATE_FORMATTER),
                pgg.getNgayKetThuc().format(DATE_FORMATTER),
                pgg.getGiaTriDonHangToiThieu().toString(),
                capStr,
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
