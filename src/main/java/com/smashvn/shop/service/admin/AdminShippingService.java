package com.smashvn.shop.service.admin;
import com.smashvn.shop.service.AuditService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminShippingService {

    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;

    @Cacheable(value = "shipping-carriers")
    public List<DonViVanChuyen> getAllCarriers() {
        return donViVanChuyenDAO.findAll();
    }

    @Transactional
    @CacheEvict(value = "shipping-carriers", allEntries = true)
    public void updateShippingFee(Integer carrierId, BigDecimal phiLocal, BigDecimal phiNationwide, Long version, Integer actingTaiKhoanId, String clientIp) {
        // 1. Service-Level Authorization
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        
        if (!Boolean.TRUE.equals(actingUser.getLaQuanLy())) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này. Chỉ Quản lý mới được chỉnh sửa phí vận chuyển.");
        }

        // 2. Validation Centralization
        validateShippingFee(phiLocal, phiNationwide);

        // 3. Load entity
        DonViVanChuyen dv = donViVanChuyenDAO.findById(carrierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển."));

        if (version != null && !version.equals(dv.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(DonViVanChuyen.class, carrierId);
        }

        BigDecimal oldLocal = dv.getPhiLocal();
        BigDecimal oldNationwide = dv.getPhiNationwide();
        Long versionBefore = dv.getVersion();

        // 4. Update values
        dv.setPhiLocal(phiLocal);
        dv.setPhiNationwide(phiNationwide);

        // 5. Save entity
        dv = donViVanChuyenDAO.saveAndFlush(dv);

        Long versionAfter = dv.getVersion();

        // 6. Audit Log Enhancement
        String oldState = String.format("phiLocal=%s, phiNationwide=%s", oldLocal, oldNationwide);
        String newState = String.format("phiLocal=%s, phiNationwide=%s", phiLocal, phiNationwide);
        
        String ghiChu = String.format("[SHIPPING_FEE_UPDATED] carrierId=%d, carrierName=%s, versionBefore=%s, versionAfter=%s, actingUserId=%d, actingUsername=%s, clientIp=%s, timestamp=%s",
                dv.getId(), dv.getTenDonVi(),
                versionBefore != null ? versionBefore.toString() : "0",
                versionAfter != null ? versionAfter.toString() : "0",
                actingUser.getId(), actingUser.getEmail(),
                clientIp, LocalDateTime.now().toString());

        auditService.log(
                actingTaiKhoanId,
                "DonViVanChuyen",
                Long.valueOf(dv.getId()),
                "UPDATE",
                oldState,
                newState,
                clientIp,
                ghiChu,
                actingUser.getVaiTro() != null ? actingUser.getVaiTro() : "QUAN_LY"
        );
    }

    private void validateShippingFee(BigDecimal phiLocal, BigDecimal phiNationwide) {
        if (phiLocal == null || phiNationwide == null) {
            throw new IllegalArgumentException("Phí vận chuyển không được để trống.");
        }
        if (phiLocal.compareTo(BigDecimal.ZERO) < 0 || phiNationwide.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí vận chuyển không được âm.");
        }
        BigDecimal maxLimit = BigDecimal.valueOf(10000000);
        if (phiLocal.compareTo(maxLimit) > 0 || phiNationwide.compareTo(maxLimit) > 0) {
            throw new IllegalArgumentException("Phí vận chuyển không được vượt quá 10,000,000 đ.");
        }
    }

    @Transactional
    @CacheEvict(value = "shipping-carriers", allEntries = true)
    public void updateGhnConfig(Integer id, String token, String clientId, String diaChiKho, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        
        if (!Boolean.TRUE.equals(actingUser.getLaQuanLy())) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này. Chỉ Quản lý mới được chỉnh sửa cấu hình kết nối.");
        }

        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token không được để trống.");
        }
        String trimmedToken = token.trim();
        if (trimmedToken.length() < 2 || trimmedToken.length() > 255) {
            throw new IllegalArgumentException("Token phải có độ dài từ 2 đến 255 ký tự.");
        }

        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID không được để trống.");
        }
        String trimmedClientId = clientId.trim();
        if (trimmedClientId.length() < 2 || trimmedClientId.length() > 100) {
            throw new IllegalArgumentException("Client ID phải có độ dài từ 2 đến 100 ký tự.");
        }

        if (diaChiKho == null || diaChiKho.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ kho không được để trống.");
        }
        String trimmedDiaChiKho = diaChiKho.trim();
        String sanitizedDiaChiKho = org.jsoup.Jsoup.clean(trimmedDiaChiKho, org.jsoup.safety.Safelist.none());
        if (sanitizedDiaChiKho.length() < 5 || sanitizedDiaChiKho.length() > 255) {
            throw new IllegalArgumentException("Địa chỉ kho phải có độ dài từ 5 đến 255 ký tự.");
        }

        DonViVanChuyen dv = donViVanChuyenDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển."));

        String oldState = String.format("token=%s, clientId=%s, diaChiKho=%s",
                com.smashvn.shop.util.ValidationUtils.maskSecret(dv.getToken()),
                com.smashvn.shop.util.ValidationUtils.maskSecret(dv.getClientId()),
                dv.getDiaChiKho());

        dv.setToken(trimmedToken);
        dv.setClientId(trimmedClientId);
        dv.setDiaChiKho(sanitizedDiaChiKho);
        dv = donViVanChuyenDAO.saveAndFlush(dv);

        String newState = String.format("token=%s, clientId=%s, diaChiKho=%s",
                com.smashvn.shop.util.ValidationUtils.maskSecret(trimmedToken),
                com.smashvn.shop.util.ValidationUtils.maskSecret(trimmedClientId),
                sanitizedDiaChiKho);

        String ghiChu = String.format("[GHN_CONFIG_UPDATED] id=%d, actingUserId=%d, actingUsername=%s, clientIp=%s, timestamp=%s",
                dv.getId(), actingUser.getId(), actingUser.getEmail(), clientIp, LocalDateTime.now().toString());

        auditService.log(
                actingTaiKhoanId,
                "DonViVanChuyen",
                Long.valueOf(dv.getId()),
                "UPDATE",
                oldState,
                newState,
                clientIp,
                ghiChu,
                actingUser.getVaiTro() != null ? actingUser.getVaiTro() : "QUAN_LY"
        );
    }
}
