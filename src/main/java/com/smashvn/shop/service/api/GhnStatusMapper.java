package com.smashvn.shop.service.api;

import org.springframework.stereotype.Component;

/**
 * Lớp tiện ích ánh xạ trạng thái GHN sang trạng thái đơn hàng nội bộ. Được dùng
 * chung bởi cả Webhook (GhnRestController) và Scheduler (GhnPollingScheduler).
 */
@Component
public class GhnStatusMapper {

    /**
     * Ánh xạ trạng thái GHN sang trạng thái đơn hàng nội bộ.
     *
     * @param ghnStatus mã trạng thái từ GHN (ví dụ: "picked", "delivering",
     * "delivered"...)
     * @return trạng thái nội bộ tương ứng ("cho_xac_nhan", "dang_giao",
     * "da_giao", "da_huy") hoặc null nếu trạng thái không được nhận diện
     */
    public String mapToInternalStatus(String ghnStatus) {
        if (ghnStatus == null) {
            return null;
        }

        switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick":
            case "picking":
                return "dang_lay_hang";

            case "money_collect_picking":
            case "picked":
            case "storing":
            case "sorting":
            case "transporting":
            case "delivering":
            case "money_collect_delivering":
                return "dang_giao";

            case "delivered":
                return "da_giao";

            case "cancel":
            case "exception":
            case "lost":
            case "damage":
            case "return":
                return "da_huy";

            default:
                return null;
        }
    }

    /**
     * Kiểm tra xem trạng thái GHN có phải là trạng thái kết thúc (terminal) hay
     * không. Trạng thái kết thúc: delivered, cancel, exception, lost, damage,
     * return.
     */
    public boolean isTerminalGhnStatus(String ghnStatus) {
        if (ghnStatus == null) {
            return false;
        }

        switch (ghnStatus.toLowerCase()) {
            case "delivered":
            case "cancel":
            case "exception":
            case "lost":
            case "damage":
            case "return":
                return true;
            default:
                return false;
        }
    }
}
