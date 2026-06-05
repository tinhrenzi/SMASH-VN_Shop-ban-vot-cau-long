package com.smashvn.shop.service;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZaloPayScheduler {

    private final HoaDonRepository hoaDonRepository;
    private final AuditService auditService;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds (1 minute)
    @Transactional
    public void cleanupExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<HoaDon> expiredOrders = hoaDonRepository.findPendingZaloPayOrdersOlderThan(cutoff);

        if (!expiredOrders.isEmpty()) {
            log.info("ZaloPay Scheduler: Found {} pending orders older than 30 minutes to expire.", expiredOrders.size());
        }

        for (HoaDon hd : expiredOrders) {
            try {
                hd.setPaymentStatus("EXPIRED");
                hd.setTrangThaiThanhToan("HUY");
                hoaDonRepository.save(hd);

                auditService.log(
                        null,
                        "HoaDon",
                        Long.valueOf(hd.getId()),
                        "ZALOPAY_EXPIRE",
                        "PENDING",
                        "EXPIRED",
                        "127.0.0.1",
                        "Auto expired by scheduler (exceeded 30-minute payment timeout window)",
                        "SYSTEM"
                );
                log.info("ZaloPay Scheduler: Successfully transitioned order #{} to EXPIRED", hd.getId());
            } catch (Exception e) {
                log.error("ZaloPay Scheduler: Error transitioning order #{} to EXPIRED: ", hd.getId(), e);
            }
        }
    }
}
