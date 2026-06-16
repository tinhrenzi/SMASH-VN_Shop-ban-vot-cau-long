package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    @Override
    public BigDecimal calculateCurrentSellingPrice(SanPhamChiTiet spct) {
        if (spct == null) {
            return BigDecimal.ZERO;
        }
        return buildPriceSnapshot(spct).giaBanSauGiam();
    }

    @Override
    public PriceSnapshot buildPriceSnapshot(SanPhamChiTiet spct) {
        BigDecimal giaNiemYet = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
        
        SanPham sp = spct.getSanPham();
        if (sp == null || sp.getCacDotGiamGia() == null || sp.getCacDotGiamGia().isEmpty()) {
            return new PriceSnapshot(giaNiemYet, giaNiemYet, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }

        // Tìm chương trình khuyến mại duy nhất đang hoạt động theo đúng quy tắc cấu hình
        Optional<DotGiamGia> activeDggOpt = sp.getCacDotGiamGia().stream()
            .filter(dgg -> Boolean.TRUE.equals(dgg.getActive()) && "ACTIVE".equals(dgg.getDynamicStatus()))
            .findFirst();

        if (activeDggOpt.isEmpty()) {
            return new PriceSnapshot(giaNiemYet, giaNiemYet, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }

        DotGiamGia dgg = activeDggOpt.get();
        BigDecimal phanTramGiam = BigDecimal.valueOf(dgg.getPhanTramGiam());
        
        // Số tiền giảm cụ thể = gia_niem_yet * phan_tram_giam / 100
        BigDecimal soTienGiam = giaNiemYet.multiply(phanTramGiam)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            
        BigDecimal giaBanSauGiam = giaNiemYet.subtract(soTienGiam);
        if (giaBanSauGiam.compareTo(BigDecimal.ZERO) < 0) {
            giaBanSauGiam = BigDecimal.ZERO;
        }

        return new PriceSnapshot(
            giaNiemYet,
            giaBanSauGiam.setScale(2, RoundingMode.HALF_UP),
            phanTramGiam.setScale(2, RoundingMode.HALF_UP),
            soTienGiam.setScale(2, RoundingMode.HALF_UP),
            dgg.getTenChienDich(),
            dgg.getId()
        );
    }

    @Override
    public BigDecimal calculateLineTotal(SanPhamChiTiet spct, Integer quantity) {
        if (spct == null || quantity == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal price = calculateCurrentSellingPrice(spct);
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
