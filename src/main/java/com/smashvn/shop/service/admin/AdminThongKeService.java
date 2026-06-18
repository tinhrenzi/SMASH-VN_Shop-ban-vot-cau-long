package com.smashvn.shop.service.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.smashvn.shop.dto.order.GeneralMetricsDTO;
import com.smashvn.shop.dto.product.TopProductDTO;
import com.smashvn.shop.dto.payment.TransactionHistoryDTO;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminThongKeService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public enum RevenueClassification {
        ACTUAL_REVENUE,
        ACTUAL_REVENUE_REVERSAL,
        PROJECTED_REVENUE,
        EXCLUDED
    }

    public static class OrderClassifier {

        public static RevenueClassification classify(
                String trangThaiDonHang,
                String paymentStatus,
                String trangThaiThanhToan,
                RefundStatus refundStatus) {

            String status = trangThaiDonHang != null ? trangThaiDonHang.toLowerCase() : "";
            String pStatus = paymentStatus != null ? paymentStatus.toLowerCase() : "";
            String tStatus = trangThaiThanhToan != null ? trangThaiThanhToan.toUpperCase() : "";

            // 1. Actual Revenue: only successful/delivered orders
            if ("da_giao".equals(status) || "hoan_thanh".equals(status)) {
                // If it is refunded, it is a reversal
                if ("refunded".equals(pStatus) || "REFUNDED".equals(tStatus) || RefundStatus.COMPLETED == refundStatus) {
                    return RevenueClassification.ACTUAL_REVENUE_REVERSAL;
                }
                return RevenueClassification.ACTUAL_REVENUE;
            }

            // 2. Projected Revenue: successfully paid but not delivered or cancelled
            boolean isPaid = "paid".equals(pStatus) || "DA_THANH_TOAN".equals(tStatus);
            boolean isProjectedStatus = "cho_xac_nhan".equals(status)
                    || "da_xac_nhan".equals(status)
                    || "dang_lay_hang".equals(status)
                    || "dang_giao".equals(status);
            boolean isRefunded = "refunded".equals(pStatus) || "REFUNDED".equals(tStatus) || RefundStatus.COMPLETED == refundStatus;

            if (isPaid && isProjectedStatus && !isRefunded) {
                return RevenueClassification.PROJECTED_REVENUE;
            }

            return RevenueClassification.EXCLUDED;
        }
    }

    // Xử lý Lấy khoảng thời gian từ bộ lọc nhanh (Preset)
    public Map<String, LocalDateTime> getDateRange(String preset, String startDateStr, String endDateStr) {
        LocalDateTime start;
        LocalDateTime end;
        ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);

        switch (preset != null ? preset.toLowerCase() : "") {
            case "today":
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "this_week":
                // Thứ 2 tuần này
                LocalDate monday = now.toLocalDate().with(DayOfWeek.MONDAY);
                start = monday.atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "this_month":
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "this_year":
                start = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "last_30_days":
                start = now.toLocalDate().minusDays(30).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "all_time":
                start = LocalDate.of(2000, 1, 1).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "custom":
                if (startDateStr != null && !startDateStr.isEmpty() && endDateStr != null && !endDateStr.isEmpty()) {
                    start = LocalDate.parse(startDateStr).atStartOfDay();
                    end = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);
                } else {
                    // Mặc định 30 ngày qua
                    start = now.toLocalDate().minusDays(30).atStartOfDay();
                    end = now.toLocalDate().atTime(LocalTime.MAX);
                }
                break;
            default:
                // Mặc định 30 ngày qua
                start = now.toLocalDate().minusDays(30).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
        }

        Map<String, LocalDateTime> range = new HashMap<>();
        range.put("start", start);
        range.put("end", end);
        return range;
    }

    // Xác định kiểu gom nhóm biểu đồ
    public String getGroupingType(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        long days = duration.toDays();
        if (days <= 1) {
            return "HOUR";
        } else if (days <= 90) {
            return "DAY";
        } else {
            return "MONTH";
        }
    }

    private String standardizePaymentMethod(String raw) {
        if (raw == null) {
            return "CASH";
        }
        raw = raw.toUpperCase().trim();
        if (raw.contains("ZALOPAY")) {
            return "ZALOPAY";
        }
        if (raw.contains("SEPAY")) {
            return "SEPAY";
        }
        if (raw.contains("COD")) {
            return "COD";
        }
        if (raw.contains("CHUYEN_KHOAN") || raw.contains("CHUYỂN KHOẢN") || raw.contains("BANK") || raw.contains("TRANSFER") || raw.contains("BANKING")) {
            return "BANK_TRANSFER";
        }
        if (raw.contains("TIEN_MAT") || raw.contains("TIỀN MẶT") || raw.contains("CASH")) {
            return "CASH";
        }
        return "CASH";
    }

    private String standardizePaymentStatus(String status, String trangThaiDonHang) {
        if (status == null) {
            return "PENDING";
        }
        status = status.toUpperCase().trim();
        if ("PAID".equals(status) || "DA_THANH_TOAN".equals(status)) {
            return "PAID";
        }
        if ("CANCELLED".equals(status) || "DA_HUY".equals(status) || "da_huy".equalsIgnoreCase(trangThaiDonHang)) {
            return "CANCELLED";
        }
        if ("FAILED".equals(status) || "THAT_BAI".equals(status)) {
            return "FAILED";
        }
        if ("REFUNDED".equals(status) || "HOAN_TIEN".equals(status)) {
            return "REFUNDED";
        }
        if ("PENDING".equals(status) || "CHO_THANH_TOAN".equals(status)) {
            return "PENDING";
        }
        return "PENDING";
    }

    public List<TransactionHistoryDTO> getTransactionHistory(LocalDateTime start, LocalDateTime end, Integer limit) {
        org.springframework.data.domain.Pageable pageable = limit != null ? PageRequest.of(0, limit) : org.springframework.data.domain.Pageable.unpaged();
        List<Object[]> rawList = hoaDonRepository.findRawTransactionsInPeriod(start, end, pageable);
        List<TransactionHistoryDTO> dtos = new ArrayList<>();
        for (Object[] row : rawList) {
            Integer rawId = (Integer) row[0];
            String ho = (String) row[1];
            String ten = (String) row[2];
            LocalDateTime ngayTao = (LocalDateTime) row[3];
            String pm = (String) row[4];
            String tenPttt = (String) row[5];
            String ps = (String) row[6];
            String tttt = (String) row[7];
            String ttdh = (String) row[8];
            String tid = (String) row[9];
            String appTransId = (String) row[10];
            String maGd = (String) row[11];
            BigDecimal amount = (BigDecimal) row[12];

            Long id = rawId != null ? rawId.longValue() : 0L;
            String invoiceCode = "HD-" + id;

            String customerName = "Khách vãng lai";
            if (ho != null || ten != null) {
                customerName = ((ho != null ? ho : "") + " " + (ten != null ? ten : "")).trim();
                if (customerName.isEmpty()) {
                    customerName = "Khách vãng lai";
                }
            }

            String rawMethod = pm != null && !pm.trim().isEmpty() ? pm : tenPttt;
            String paymentMethod = standardizePaymentMethod(rawMethod);

            String rawStatus = ps != null && !ps.trim().isEmpty() ? ps : tttt;
            String paymentStatus = standardizePaymentStatus(rawStatus, ttdh);

            String transactionId = tid != null && !tid.trim().isEmpty() ? tid : appTransId;
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = maGd;
            }
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = "-";
            }

            dtos.add(new TransactionHistoryDTO(
                    id,
                    invoiceCode,
                    customerName,
                    ngayTao,
                    paymentMethod,
                    paymentStatus,
                    transactionId,
                    amount != null ? amount : BigDecimal.ZERO
            ));
        }
        return dtos;
    }

    // Cache kết quả thống kê 30 giây (được cấu hình bằng Caffeine TTL ở application.properties)
    @Cacheable(value = "thongke", key = "#start.toString() + '-' + #end.toString()")
    public Map<String, Object> getStatisticsData(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rawOrders = hoaDonRepository.findAllOrdersInPeriod(start, end);

        long totalOrders = rawOrders.size();
        long successfulOrders = 0;
        long cancelledOrders = 0;
        BigDecimal actualRevenue = BigDecimal.ZERO;
        BigDecimal expectedRevenue = BigDecimal.ZERO;
        BigDecimal refundedRevenue = BigDecimal.ZERO; // New Metric: total value of refunded orders
        BigDecimal pendingRefund = BigDecimal.ZERO;

        List<BigDecimal> successfulOrderAmounts = new ArrayList<>();

        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("cho_xac_nhan", 0L);
        statusMap.put("dang_giao", 0L);
        statusMap.put("da_giao", 0L);
        statusMap.put("da_huy", 0L);

        Map<String, BigDecimal> groupedRevenue = new HashMap<>();
        String grouping = getGroupingType(start, end);

        long onlineTotal = 0;
        long onlineSuccess = 0;
        long onlineFailed = 0;
        long onlinePending = 0;
        BigDecimal onlineRevenue = BigDecimal.ZERO;

        for (Object[] row : rawOrders) {
            LocalDateTime ngayTao = (LocalDateTime) row[3];
            String paymentMethod = (String) row[4];
            String paymentStatus = (String) row[6];
            String trangThaiThanhToan = (String) row[7];
            String trangThaiDonHang = (String) row[8];
            BigDecimal tongTien = (BigDecimal) row[12];
            RefundStatus refundStatus = (RefundStatus) row[13];

            if (tongTien == null) {
                tongTien = BigDecimal.ZERO;
            }

            // Centralized classification
            RevenueClassification classification = OrderClassifier.classify(trangThaiDonHang, paymentStatus, trangThaiThanhToan, refundStatus);

            BigDecimal revenueContribution = BigDecimal.ZERO;
            if (classification == RevenueClassification.ACTUAL_REVENUE) {
                actualRevenue = actualRevenue.add(tongTien);
                successfulOrders++;
                successfulOrderAmounts.add(tongTien);
                revenueContribution = tongTien;
            } else if (classification == RevenueClassification.ACTUAL_REVENUE_REVERSAL) {
                actualRevenue = actualRevenue.subtract(tongTien);
                refundedRevenue = refundedRevenue.add(tongTien); // Add to new metric
                revenueContribution = tongTien.negate();
            } else if (classification == RevenueClassification.PROJECTED_REVENUE) {
                expectedRevenue = expectedRevenue.add(tongTien);
            }

            // If the order has actual revenue contribution (regular or reversal), add to chart grouping
            if (revenueContribution.compareTo(BigDecimal.ZERO) != 0) {
                if ("HOUR".equals(grouping)) {
                    String key = String.valueOf(ngayTao.getHour());
                    groupedRevenue.put(key, groupedRevenue.getOrDefault(key, BigDecimal.ZERO).add(revenueContribution));
                } else if ("DAY".equals(grouping)) {
                    String key = String.format("%02d/%02d", ngayTao.getDayOfMonth(), ngayTao.getMonthValue());
                    groupedRevenue.put(key, groupedRevenue.getOrDefault(key, BigDecimal.ZERO).add(revenueContribution));
                } else { // MONTH
                    String key = String.format("%02d/%d", ngayTao.getMonthValue(), ngayTao.getYear());
                    groupedRevenue.put(key, groupedRevenue.getOrDefault(key, BigDecimal.ZERO).add(revenueContribution));
                }
            }

            // Count cancelled orders
            String status = trangThaiDonHang != null ? trangThaiDonHang.toLowerCase() : "";
            if ("da_huy".equals(status)) {
                cancelledOrders++;
            }

            // Count pending refund
            if (RefundStatus.PENDING == refundStatus) {
                pendingRefund = pendingRefund.add(tongTien);
            }

            // Status map distribution
            String normalizedStatus = switch (status) {
                case "da_giao", "delivered" ->
                    "da_giao";
                case "da_huy", "cancelled" ->
                    "da_huy";
                case "dang_giao", "shipping", "dang_lay_hang" ->
                    "dang_giao";
                case "cho_xac_nhan", "processing", "da_xac_nhan" ->
                    "cho_xac_nhan";
                default ->
                    status;
            };
            if (statusMap.containsKey(normalizedStatus)) {
                statusMap.put(normalizedStatus, statusMap.get(normalizedStatus) + 1);
            }

            // Online stats
            if (paymentMethod != null) {
                String pm = paymentMethod.toLowerCase();
                if (pm.contains("zalopay") || pm.contains("sepay")) {
                    onlineTotal++;

                    // Online Success KPI: strictly requires gateway-confirmed payment_status='paid'.
                    // trang_thai_thanh_toan='DA_THANH_TOAN' alone is NOT sufficient because it can be set
                    // manually by admins via the override endpoint and would otherwise inflate this metric.
                    String pStatus = paymentStatus != null ? paymentStatus.toUpperCase() : "";
                    String tStatus = trangThaiThanhToan != null ? trangThaiThanhToan.toUpperCase() : "";
                    if ("PAID".equals(pStatus)) {
                        onlineSuccess++;
                        // Revenue: only count if delivered AND gateway-confirmed
                        if ("da_giao".equalsIgnoreCase(trangThaiDonHang) || "hoan_thanh".equalsIgnoreCase(trangThaiDonHang)) {
                            if ("REFUNDED".equals(pStatus) || "REFUNDED".equals(tStatus) || RefundStatus.COMPLETED == refundStatus) {
                                onlineRevenue = onlineRevenue.subtract(tongTien);
                            } else {
                                onlineRevenue = onlineRevenue.add(tongTien);
                            }
                        }
                    } else if ("FAILED".equals(pStatus) || "THAT_BAI".equalsIgnoreCase(pStatus) || "FAILED".equals(tStatus)) {
                        onlineFailed++;
                    } else if ("PENDING".equals(pStatus) || "PENDING".equals(tStatus) || "CHO_THANH_TOAN".equals(tStatus)) {
                        onlinePending++;
                    } else {
                        onlinePending++;
                    }
                }
            }
        }

        // Avg order value
        double avgOrderValue = 0.0;
        if (!successfulOrderAmounts.isEmpty()) {
            BigDecimal sum = successfulOrderAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgOrderValue = sum.doubleValue() / successfulOrderAmounts.size();
        }

        // Tỷ lệ hủy đơn
        double cancellationRate = 0.0;
        if (totalOrders > 0) {
            cancellationRate = ((double) cancelledOrders / totalOrders) * 100.0;
        }

        // Khách hàng mới (Có đơn đầu tiên hoàn thành trong kỳ)
        Long newCustomersCount = hoaDonRepository.countNewCustomers(start, end);

        // Chart labels & values
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();

        if ("HOUR".equals(grouping)) {
            for (int i = 0; i < 24; i++) {
                chartLabels.add(String.format("%02dh", i));
                chartValues.add(groupedRevenue.getOrDefault(String.valueOf(i), BigDecimal.ZERO));
            }
        } else if ("DAY".equals(grouping)) {
            LocalDateTime temp = start;
            while (!temp.isAfter(end)) {
                String dayKey = String.format("%02d/%02d", temp.getDayOfMonth(), temp.getMonthValue());
                chartLabels.add(dayKey);
                chartValues.add(groupedRevenue.getOrDefault(dayKey, BigDecimal.ZERO));
                temp = temp.plusDays(1);
            }
        } else { // MONTH
            LocalDateTime temp = start;
            while (temp.isBefore(end) || temp.getYear() == end.getYear() && temp.getMonthValue() == end.getMonthValue()) {
                String monthKey = String.format("%02d/%d", temp.getMonthValue(), temp.getYear());
                chartLabels.add("Tháng " + temp.getMonthValue() + "/" + temp.getYear());
                chartValues.add(groupedRevenue.getOrDefault(monthKey, BigDecimal.ZERO));
                temp = temp.plusMonths(1);
            }
        }

        // Top 5 products
        List<TopProductDTO> topProducts = hoaDonChiTietRepository.findBestSellingProducts(start, end, PageRequest.of(0, 5));

        // General DTO
        GeneralMetricsDTO metrics = new GeneralMetricsDTO(
                totalOrders,
                successfulOrders,
                cancelledOrders,
                actualRevenue,
                avgOrderValue,
                hoaDonChiTietRepository.getTotalProductsSold(start, end)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("metrics", metrics);
        data.put("cancellationRate", cancellationRate);
        data.put("newCustomers", newCustomersCount != null ? newCustomersCount : 0L);
        data.put("statusDistribution", statusMap);
        data.put("chartLabels", chartLabels);
        data.put("chartValues", chartValues);
        data.put("topProducts", topProducts);
        data.put("grouping", grouping);
        data.put("expectedRevenue", expectedRevenue);
        data.put("actualRevenue", actualRevenue);
        data.put("refundedRevenue", refundedRevenue); // New Metric: Refund Revenue
        data.put("pendingRefund", pendingRefund);

        // Put Online statistics
        data.put("onlineTotal", onlineTotal);
        data.put("onlineSuccess", onlineSuccess);
        data.put("onlineFailed", onlineFailed);
        data.put("onlinePending", onlinePending);
        data.put("onlineRevenue", onlineRevenue);

        // Transaction history
        Long totalTransactions = hoaDonRepository.countTransactionsInPeriod(start, end);
        List<TransactionHistoryDTO> transactions = getTransactionHistory(start, end, 100);
        data.put("totalTransactions", totalTransactions != null ? totalTransactions : 0L);
        data.put("displayedTransactions", transactions.size());
        data.put("transactions", transactions);

        return data;
    }

    // Xuất báo cáo thống kê ra file Excel
    public byte[] exportToExcel(LocalDateTime start, LocalDateTime end) throws IOException {
        Map<String, Object> stats = getStatisticsData(start, end);
        GeneralMetricsDTO metrics = (GeneralMetricsDTO) stats.get("metrics");
        Double cancellationRate = (Double) stats.get("cancellationRate");
        Long newCustomers = (Long) stats.get("newCustomers");
        @SuppressWarnings("unchecked")
        Map<String, Long> statusMap = (Map<String, Long>) stats.get("statusDistribution");
        @SuppressWarnings("unchecked")
        List<String> chartLabels = (List<String>) stats.get("chartLabels");
        @SuppressWarnings("unchecked")
        List<BigDecimal> chartValues = (List<BigDecimal>) stats.get("chartValues");
        @SuppressWarnings("unchecked")
        List<TopProductDTO> topProducts = (List<TopProductDTO>) stats.get("topProducts");

        try (Workbook workbook = new XSSFWorkbook()) {
            // Tạo kiểu chữ và Style cho Header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            // Kiểu định dạng tiền tệ VND
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0\" đ\""));

            // ----------------------------------------------------
            // SHEET 1: CHỈ SỐ TỔNG QUAN & TRẠNG THÁI ĐƠN HÀNG
            // ----------------------------------------------------
            Sheet kpiSheet = workbook.createSheet("KPIs & Trạng Thái Đơn Hàng");
            kpiSheet.setColumnWidth(0, 8000);
            kpiSheet.setColumnWidth(1, 5000);

            Row r0 = kpiSheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("KPIs Chỉ Số Tổng Quan");
            c0.setCellStyle(headerStyle);
            kpiSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

            String[] kpiNames = {
                "Tổng số đơn hàng", "Đơn hàng thành công", "Đơn hàng đã hủy",
                "Doanh thu thực tế", "Doanh thu dự kiến", "Doanh thu đã hoàn", "Giá trị trung bình đơn hàng",
                "Tổng sản phẩm đã bán", "Khách hàng mới", "Tỷ lệ hủy đơn"
            };
            Object[] kpiValues = {
                metrics.totalOrders(), metrics.successfulOrders(), metrics.cancelledOrders(),
                stats.get("actualRevenue"), stats.get("expectedRevenue"), stats.get("refundedRevenue"), metrics.avgOrderValue(),
                metrics.totalProductsSold(), newCustomers, cancellationRate
            };

            for (int i = 0; i < kpiNames.length; i++) {
                Row row = kpiSheet.createRow(i + 1);
                row.createCell(0).setCellValue(kpiNames[i]);
                Cell valCell = row.createCell(1);
                if (kpiValues[i] instanceof Long) {
                    valCell.setCellValue((Long) kpiValues[i]);
                } else if (kpiValues[i] instanceof Integer) {
                    valCell.setCellValue((Integer) kpiValues[i]);
                } else if (kpiValues[i] instanceof BigDecimal) {
                    valCell.setCellValue(((BigDecimal) kpiValues[i]).doubleValue());
                    valCell.setCellStyle(currencyStyle);
                } else if (kpiValues[i] instanceof Double) {
                    if (kpiNames[i].contains("trung bình")) {
                        valCell.setCellValue((Double) kpiValues[i]);
                        valCell.setCellStyle(currencyStyle);
                    } else if (kpiNames[i].contains("Tỷ lệ")) {
                        valCell.setCellValue((Double) kpiValues[i] / 100.0);
                        CellStyle pctStyle = workbook.createCellStyle();
                        pctStyle.setDataFormat(format.getFormat("0.0%"));
                        valCell.setCellStyle(pctStyle);
                    } else {
                        valCell.setCellValue((Double) kpiValues[i]);
                    }
                }
            }

            // Dòng trống và vẽ bảng Trạng thái Đơn hàng
            int startRowStatus = kpiNames.length + 3;
            Row rStatusHeader = kpiSheet.createRow(startRowStatus);
            Cell cStatusHeader = rStatusHeader.createCell(0);
            cStatusHeader.setCellValue("Trạng Thái Đơn Hàng");
            cStatusHeader.setCellStyle(headerStyle);
            kpiSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRowStatus, startRowStatus, 0, 1));

            String[] statuses = {"Chờ xác nhận", "Đang giao", "Đã giao", "Đã hủy"};
            String[] statusKeys = {"cho_xac_nhan", "dang_giao", "da_giao", "da_huy"};

            for (int i = 0; i < statuses.length; i++) {
                Row row = kpiSheet.createRow(startRowStatus + 1 + i);
                row.createCell(0).setCellValue(statuses[i]);
                row.createCell(1).setCellValue(statusMap.getOrDefault(statusKeys[i], 0L));
            }

            // Dòng trống và vẽ bảng Thống kê Online
            int startRowZp = startRowStatus + statuses.length + 3;
            Row rZpHeader = kpiSheet.createRow(startRowZp);
            Cell cZpHeader = rZpHeader.createCell(0);
            cZpHeader.setCellValue("Thống Kê Thanh Toán Online");
            cZpHeader.setCellStyle(headerStyle);
            kpiSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRowZp, startRowZp, 0, 1));

            String[] zpLabels = {
                "Tổng số giao dịch Online", "Giao dịch thành công",
                "Giao dịch thất bại", "Giao dịch đang chờ", "Doanh thu Online"
            };
            Object[] zpValues = {
                stats.get("onlineTotal"), stats.get("onlineSuccess"),
                stats.get("onlineFailed"), stats.get("onlinePending"), stats.get("onlineRevenue")
            };

            for (int i = 0; i < zpLabels.length; i++) {
                Row row = kpiSheet.createRow(startRowZp + 1 + i);
                row.createCell(0).setCellValue(zpLabels[i]);
                Cell valCell = row.createCell(1);
                if (zpValues[i] instanceof Long) {
                    valCell.setCellValue((Long) zpValues[i]);
                } else if (zpValues[i] instanceof BigDecimal) {
                    valCell.setCellValue(((BigDecimal) zpValues[i]).doubleValue());
                    valCell.setCellStyle(currencyStyle);
                }
            }

            // ----------------------------------------------------
            // SHEET 2: DOANH THU THEO THỜI GIAN
            // ----------------------------------------------------
            Sheet revSheet = workbook.createSheet("Doanh Thu Theo Thời Gian");
            revSheet.setColumnWidth(0, 6000);
            revSheet.setColumnWidth(1, 5000);

            Row rRevHeader = revSheet.createRow(0);
            Cell cRevTime = rRevHeader.createCell(0);
            cRevTime.setCellValue("Thời Gian");
            cRevTime.setCellStyle(headerStyle);

            Cell cRevAmount = rRevHeader.createCell(1);
            cRevAmount.setCellValue("Doanh Thu");
            cRevAmount.setCellStyle(headerStyle);

            for (int i = 0; i < chartLabels.size(); i++) {
                Row row = revSheet.createRow(i + 1);
                row.createCell(0).setCellValue(chartLabels.get(i));
                Cell cellVal = row.createCell(1);
                cellVal.setCellValue(chartValues.get(i).doubleValue());
                cellVal.setCellStyle(currencyStyle);
            }

            // ----------------------------------------------------
            // SHEET 3: TOP SẢN PHẨM BÁN CHẠY
            // ----------------------------------------------------
            Sheet prodSheet = workbook.createSheet("Top Sản Phẩm Bán Chạy");
            prodSheet.setColumnWidth(0, 2000);
            prodSheet.setColumnWidth(1, 10000);
            prodSheet.setColumnWidth(2, 5000);
            prodSheet.setColumnWidth(3, 3000);
            prodSheet.setColumnWidth(4, 5000);

            Row rProdHeader = prodSheet.createRow(0);
            String[] prodHeaders = {"Rank", "Tên Sản Phẩm", "Danh Mục", "Số Lượng Bán", "Doanh Thu"};
            for (int i = 0; i < prodHeaders.length; i++) {
                Cell cell = rProdHeader.createCell(i);
                cell.setCellValue(prodHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < topProducts.size(); i++) {
                TopProductDTO p = topProducts.get(i);
                Row row = prodSheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(p.productName());
                row.createCell(2).setCellValue(p.categoryName());
                row.createCell(3).setCellValue(p.soldQuantity());
                Cell cellRev = row.createCell(4);
                cellRev.setCellValue(p.revenue().doubleValue());
                cellRev.setCellStyle(currencyStyle);
            }

            // ----------------------------------------------------
            // SHEET 4: LỊCH SỬ GIAO DỊCH
            // ----------------------------------------------------
            Sheet txSheet = workbook.createSheet("Lịch Sử Giao Dịch");
            txSheet.setColumnWidth(0, 3000);
            txSheet.setColumnWidth(1, 7000);
            txSheet.setColumnWidth(2, 6000);
            txSheet.setColumnWidth(3, 5000);
            txSheet.setColumnWidth(4, 8000);
            txSheet.setColumnWidth(5, 5000);
            txSheet.setColumnWidth(6, 5000);

            Row rTxHeader = txSheet.createRow(0);
            String[] txHeaders = {
                "Mã Hóa Đơn", "Khách Hàng", "Thời Gian",
                "Phương Thức", "Mã Giao Dịch", "Số Tiền", "Trạng Thái Thanh Toán"
            };
            for (int i = 0; i < txHeaders.length; i++) {
                Cell cell = rTxHeader.createCell(i);
                cell.setCellValue(txHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            @SuppressWarnings("unchecked")
            List<TransactionHistoryDTO> transactionsList = (List<TransactionHistoryDTO>) stats.get("transactions");
            if (transactionsList != null) {
                DateTimeFormatter excelDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                for (int i = 0; i < transactionsList.size(); i++) {
                    TransactionHistoryDTO tx = transactionsList.get(i);
                    Row row = txSheet.createRow(i + 1);

                    // Mã Hóa Đơn (String)
                    row.createCell(0).setCellValue(tx.invoiceCode());

                    // Khách Hàng (String)
                    row.createCell(1).setCellValue(tx.customerName());

                    // Thời Gian (String)
                    String formattedTime = tx.transactionTime() != null ? excelDateFormatter.format(tx.transactionTime()) : "";
                    row.createCell(2).setCellValue(formattedTime);

                    // Phương Thức (String)
                    row.createCell(3).setCellValue(tx.paymentMethod());

                    // Mã Giao Dịch (String)
                    row.createCell(4).setCellValue(tx.transactionId());

                    // Số Tiền (Numeric)
                    Cell cellAmt = row.createCell(5);
                    cellAmt.setCellValue(tx.amount() != null ? tx.amount().doubleValue() : 0.0);
                    cellAmt.setCellStyle(currencyStyle);

                    // Trạng Thái Thanh Toán (String)
                    row.createCell(6).setCellValue(tx.paymentStatus());
                }
            }

            // Ghi workbook vào byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
