package com.smashvn.shop.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.smashvn.shop.dto.GeneralMetricsDTO;
import com.smashvn.shop.dto.ChartPointDTO;
import com.smashvn.shop.dto.TopProductDTO;
import com.smashvn.shop.dto.OrderStatusCountDTO;
import com.smashvn.shop.dto.TransactionHistoryDTO;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminThongKeService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
        if (raw == null) return "CASH";
        raw = raw.toUpperCase().trim();
        if (raw.contains("ZALOPAY")) return "ZALOPAY";
        if (raw.contains("SEPAY")) return "SEPAY";
        if (raw.contains("COD")) return "COD";
        if (raw.contains("CHUYEN_KHOAN") || raw.contains("CHUYỂN KHOẢN") || raw.contains("BANK") || raw.contains("TRANSFER") || raw.contains("BANKING")) {
            return "BANK_TRANSFER";
        }
        if (raw.contains("TIEN_MAT") || raw.contains("TIỀN MẶT") || raw.contains("CASH")) {
            return "CASH";
        }
        return "CASH";
    }

    private String standardizePaymentStatus(String status, String trangThaiDonHang) {
        if (status == null) return "PENDING";
        status = status.toUpperCase().trim();
        if ("PAID".equals(status) || "DA_THANH_TOAN".equals(status)) return "PAID";
        if ("CANCELLED".equals(status) || "DA_HUY".equals(status) || "da_huy".equalsIgnoreCase(trangThaiDonHang)) return "CANCELLED";
        if ("FAILED".equals(status) || "THAT_BAI".equals(status)) return "FAILED";
        if ("REFUNDED".equals(status) || "HOAN_TIEN".equals(status)) return "REFUNDED";
        if ("PENDING".equals(status) || "CHO_THANH_TOAN".equals(status)) return "PENDING";
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
        // 1. Chỉ số tổng hợp KPIs
        GeneralMetricsDTO rawMetrics = hoaDonRepository.getGeneralMetricsWithoutProductCount(start, end);
        Long totalSold = hoaDonChiTietRepository.getTotalProductsSold(start, end);
        GeneralMetricsDTO metrics = new GeneralMetricsDTO(
                rawMetrics.totalOrders(),
                rawMetrics.successfulOrders(),
                rawMetrics.cancelledOrders(),
                rawMetrics.totalRevenue(),
                rawMetrics.avgOrderValue(),
                totalSold
        );

        // Tỷ lệ hủy đơn
        double cancellationRate = 0.0;
        if (metrics.totalOrders() > 0) {
            cancellationRate = ((double) metrics.cancelledOrders() / metrics.totalOrders()) * 100.0;
        }

        // Khách hàng mới (Có đơn đầu tiên hoàn thành trong kỳ)
        Long newCustomersCount = hoaDonRepository.countNewCustomers(start, end);

        // 2. Tỷ lệ trạng thái đơn hàng
        List<OrderStatusCountDTO> statusCounts = hoaDonRepository.getOrderStatusDistribution(start, end);
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("cho_xac_nhan", 0L);
        statusMap.put("dang_giao", 0L);
        statusMap.put("da_giao", 0L);
        statusMap.put("da_huy", 0L);
        for (OrderStatusCountDTO osc : statusCounts) {
            String dbStatus = osc.status();
            if (dbStatus != null) {
                String normalizedStatus = switch (dbStatus.toLowerCase()) {
                    case "da_giao", "delivered" -> "da_giao";
                    case "da_huy", "cancelled" -> "da_huy";
                    case "dang_giao", "shipping", "dang_lay_hang" -> "dang_giao";
                    case "cho_xac_nhan", "processing", "da_xac_nhan" -> "cho_xac_nhan";
                    default -> dbStatus;
                };
                statusMap.put(normalizedStatus, statusMap.getOrDefault(normalizedStatus, 0L) + osc.count());
            }
        }

        // 3. Gom nhóm doanh thu cho biểu đồ
        String grouping = getGroupingType(start, end);
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();

        if ("HOUR".equals(grouping)) {
            List<ChartPointDTO> points = hoaDonRepository.getRevenueByHour(start, end);
            Map<Integer, BigDecimal> hourMap = points.stream()
                    .collect(Collectors.toMap(ChartPointDTO::getHour, ChartPointDTO::getRevenue));
            for (int i = 0; i < 24; i++) {
                chartLabels.add(String.format("%02dh", i));
                chartValues.add(hourMap.getOrDefault(i, BigDecimal.ZERO));
            }
        } else if ("DAY".equals(grouping)) {
            List<ChartPointDTO> points = hoaDonRepository.getRevenueByDay(start, end);
            Map<String, BigDecimal> dayMap = points.stream()
                    .collect(Collectors.toMap(
                            p -> String.format("%02d/%02d", p.getDay(), p.getMonth()),
                            ChartPointDTO::getRevenue
                    ));
            // Tạo tất cả các ngày trong khoảng
            LocalDateTime temp = start;
            while (!temp.isAfter(end)) {
                String dayKey = String.format("%02d/%02d", temp.getDayOfMonth(), temp.getMonthValue());
                chartLabels.add(dayKey);
                chartValues.add(dayMap.getOrDefault(dayKey, BigDecimal.ZERO));
                temp = temp.plusDays(1);
            }
        } else { // MONTH
            List<ChartPointDTO> points = hoaDonRepository.getRevenueByMonth(start, end);
            Map<String, BigDecimal> monthMap = points.stream()
                    .collect(Collectors.toMap(
                            p -> String.format("%02d/%d", p.getMonth(), p.getYear()),
                            ChartPointDTO::getRevenue
                    ));
            LocalDateTime temp = start;
            while (temp.isBefore(end) || temp.getYear() == end.getYear() && temp.getMonthValue() == end.getMonthValue()) {
                String monthKey = String.format("%02d/%d", temp.getMonthValue(), temp.getYear());
                chartLabels.add("Tháng " + temp.getMonthValue() + "/" + temp.getYear());
                chartValues.add(monthMap.getOrDefault(monthKey, BigDecimal.ZERO));
                temp = temp.plusMonths(1);
            }
        }

        // 4. Top 5 sản phẩm bán chạy nhất
        List<TopProductDTO> topProducts = hoaDonChiTietRepository.findBestSellingProducts(start, end, PageRequest.of(0, 5));

        // 5. Thống kê Online
        Long onlineTotal = hoaDonRepository.countOnlineTransactions(start, end);
        Long onlineSuccess = hoaDonRepository.countOnlineSuccessful(start, end);
        Long onlineFailed = hoaDonRepository.countOnlineFailed(start, end);
        Long onlinePending = hoaDonRepository.countOnlinePending(start, end);
        BigDecimal onlineRevenue = hoaDonRepository.sumOnlineRevenue(start, end);

        BigDecimal expectedRevenue = hoaDonRepository.sumExpectedRevenue(start, end);

        Map<String, Object> data = new HashMap<>();
        data.put("metrics", metrics);
        data.put("cancellationRate", cancellationRate);
        data.put("newCustomers", newCustomersCount);
        data.put("statusDistribution", statusMap);
        data.put("chartLabels", chartLabels);
        data.put("chartValues", chartValues);
        data.put("topProducts", topProducts);
        data.put("grouping", grouping);
        data.put("expectedRevenue", expectedRevenue != null ? expectedRevenue : BigDecimal.ZERO);
        data.put("actualRevenue", metrics.totalRevenue());
        
        // Put Online statistics
        data.put("onlineTotal", onlineTotal != null ? onlineTotal : 0L);
        data.put("onlineSuccess", onlineSuccess != null ? onlineSuccess : 0L);
        data.put("onlineFailed", onlineFailed != null ? onlineFailed : 0L);
        data.put("onlinePending", onlinePending != null ? onlinePending : 0L);
        data.put("onlineRevenue", onlineRevenue != null ? onlineRevenue : BigDecimal.ZERO);

        // 6. Lịch sử giao dịch (Shared Data Source)
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
                    "Doanh thu thực tế", "Doanh thu dự kiến", "Giá trị trung bình đơn hàng", 
                    "Tổng sản phẩm đã bán", "Khách hàng mới", "Tỷ lệ hủy đơn"
            };
            Object[] kpiValues = {
                    metrics.totalOrders(), metrics.successfulOrders(), metrics.cancelledOrders(),
                    stats.get("actualRevenue"), stats.get("expectedRevenue"), metrics.avgOrderValue(),
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
                    if (i == 5) { // Giá trị trung bình đơn (index 5)
                        valCell.setCellValue((Double) kpiValues[i]);
                        valCell.setCellStyle(currencyStyle);
                    } else if (i == 8) { // Tỷ lệ hủy đơn (index 8)
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
