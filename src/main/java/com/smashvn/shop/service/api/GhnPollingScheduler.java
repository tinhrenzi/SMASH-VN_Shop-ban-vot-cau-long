package com.smashvn.shop.service.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.OrderViewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler Polling định kỳ gọi API GHN để đồng bộ trạng thái đơn hàng.
 * Đây là cơ chế dự phòng (fallback) cho Webhook, đảm bảo đơn hàng
 * luôn được cập nhật ngay cả khi Webhook bị mất hoặc chưa được cấu hình.
 *
 * Cơ chế:
 * - Xoay vòng trang (Page Rotation): mỗi chu kỳ quét 1 trang (batch-size đơn)
 * - Chống chạy chồng: AtomicBoolean + fixedDelay
 * - Try-catch từng đơn: 1 đơn lỗi không ảnh hưởng các đơn khác
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GhnPollingScheduler {

    private final HoaDonRepository hoaDonRepository;
    private final GhnService ghnService;
    private final GhnStatusMapper ghnStatusMapper;
    private final OrderViewService orderViewService;

    @Value("${ghn.polling.enabled:true}")
    private boolean pollingEnabled;

    @Value("${ghn.polling.batch-size:50}")
    private int batchSize;

    private int currentPage = 0;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Quét trạng thái đơn hàng từ GHN theo chu kỳ.
     * Mặc định: mỗi 10 phút (600.000ms), có thể cấu hình qua ghn.polling.interval-ms.
     */
    @Scheduled(fixedDelayString = "${ghn.polling.interval-ms:600000}")
    public void pollGhnOrderStatuses() {
        if (!pollingEnabled) return;

        // Khóa chống chạy chồng (defense-in-depth, fixedDelay đã tự chống rồi)
        if (!running.compareAndSet(false, true)) {
            log.warn("[GHN_POLLING] Chu kỳ trước chưa hoàn tất, bỏ qua lần này.");
            return;
        }

        try {
            List<HoaDon> orders = hoaDonRepository
                    .findActiveShippingOrders(PageRequest.of(currentPage, batchSize));

            // Xoay vòng trang: nếu trang hiện tại ít hơn batch → reset về trang 0
            if (orders.size() < batchSize) {
                currentPage = 0;
            } else {
                currentPage++;
            }

            if (orders.isEmpty()) {
                log.info("[GHN_POLLING] Không có đơn hàng nào cần kiểm tra.");
                return;
            }

            log.info("[GHN_POLLING] Bắt đầu quét trang {}, {} đơn hàng",
                    currentPage == 0 ? 0 : currentPage - 1, orders.size());

            int updated = 0, skipped = 0, errors = 0;

            for (HoaDon hd : orders) {
                try {
                    String orderCode = hd.getGhnOrderCode();
                    if (orderCode == null || orderCode.isBlank()) {
                        skipped++;
                        continue;
                    }

                    Map<String, Object> trackingData = ghnService.trackOrder(orderCode);
                    if (trackingData == null) {
                        log.warn("[GHN_POLLING] trackOrder trả về null cho đơn #{} (orderCode={})",
                                hd.getId(), orderCode);
                        errors++;
                        continue;
                    }

                    String ghnStatus = (String) trackingData.get("status");
                    String currentGhnStatus = hd.getGhnStatus();

                    // Chỉ cập nhật khi trạng thái GHN thực sự thay đổi
                    if (ghnStatus != null && !ghnStatus.equalsIgnoreCase(currentGhnStatus)) {
                        String internalStatus = ghnStatusMapper.mapToInternalStatus(ghnStatus);
                        if (internalStatus != null) {
                            orderViewService.applyShippingStatus(hd.getId(), internalStatus, ghnStatus);
                            log.info("[GHN_POLLING] Cập nhật đơn #{}: {} → {} (GHN: {} → {})",
                                    hd.getId(), hd.getTrangThaiDonHang(), internalStatus,
                                    currentGhnStatus, ghnStatus);
                            updated++;
                        } else {
                            skipped++;
                        }
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    log.warn("[GHN_POLLING] Lỗi kiểm tra đơn #{}: {}", hd.getId(), e.getMessage());
                    errors++;
                }
            }

            log.info("[GHN_POLLING] Hoàn tất: {} cập nhật, {} bỏ qua, {} lỗi", updated, skipped, errors);

        } finally {
            running.set(false); // Luôn mở khóa, kể cả khi có exception
        }
    }
}
