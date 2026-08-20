package com.smashvn.shop;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.smashvn.shop.dto.order.GeneralMetricsDTO;
import com.smashvn.shop.dto.order.GrowthMetricDTO;
import com.smashvn.shop.dto.order.OperationalInsightDTO;
import com.smashvn.shop.dto.product.BrandRevenueDTO;
import com.smashvn.shop.dto.product.SlowMovingProductDTO;
import com.smashvn.shop.dto.product.TopProductDTO;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.admin.AdminThongKeService;
import com.smashvn.shop.service.admin.AdminThongKeService.OrderClassifier;
import com.smashvn.shop.service.admin.AdminThongKeService.RevenueClassification;

@SpringBootTest
public class AuditThongKeIntegrationTest {

    @Autowired
    private AdminThongKeService adminThongKeService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Test
    public void runFullAudit() throws Exception {
        System.out.println("================================================================================");
        System.out.println("                 SMASH-VN STATISTICS MODULE FULL AUDIT RUNNER                   ");
        System.out.println("================================================================================");

        // Audit Preset: "this_week"
        Map<String, LocalDateTime> rangeThisWeek = adminThongKeService.getDateRange("this_week", null, null);
        LocalDateTime startWeek = rangeThisWeek.get("start");
        LocalDateTime endWeek = rangeThisWeek.get("end");

        System.out.println("\n[AUDIT 2 & 3 & 7] TRACING ORDERS FOR PRESET 'this_week' (" + startWeek + " -> " + endWeek + ")");
        List<Object[]> rawOrdersWeek = hoaDonRepository.findAllOrdersInPeriod(startWeek, endWeek);
        System.out.println("Total raw orders in period: " + rawOrdersWeek.size());

        for (Object[] row : rawOrdersWeek) {
            Integer id = (Integer) row[0];
            String maHDSVN = (String) row[1];
            String customerName = (String) row[2];
            LocalDateTime ngayTao = (LocalDateTime) row[3];
            String paymentMethod = (String) row[4];
            String transactionType = (String) row[5];
            String paymentStatus = (String) row[6];
            String trangThaiThanhToan = (String) row[7];
            String trangThaiDonHang = (String) row[8];
            BigDecimal tongTien = (BigDecimal) row[12];
            RefundStatus refundStatus = (RefundStatus) row[13];

            String m1 = paymentMethod != null ? paymentMethod.toUpperCase().trim() : "";
            String m2 = transactionType != null ? transactionType.toUpperCase().trim() : "";
            boolean isCod = m1.contains("COD") || m2.contains("COD");
            RevenueClassification classification = OrderClassifier.classify(trangThaiDonHang, paymentStatus, trangThaiThanhToan, refundStatus, isCod);

            boolean isPendingRefundIncluded = (RefundStatus.PENDING == refundStatus);

            System.out.printf("  Order ID=%d | Code=%s | Customer=%s | Date=%s | Method=%s | PayStatus=%s | PayState=%s | OrderState=%s | RefundStatus=%s | Total=%s | Classification=%s | PendingRefund=%b%n",
                    id, maHDSVN, customerName, ngayTao, paymentMethod, paymentStatus, trangThaiThanhToan, trangThaiDonHang, refundStatus, tongTien, classification, isPendingRefundIncluded);
        }

        // Run full getStatisticsData for "this_week"
        Map<String, Object> statsWeek = adminThongKeService.getStatisticsData("this_week", startWeek, endWeek);
        GeneralMetricsDTO metricsWeek = (GeneralMetricsDTO) statsWeek.get("metrics");

        System.out.println("\n[DASHBOARD METRICS FOR 'this_week']");
        System.out.println("  totalOrders: " + statsWeek.get("totalOrders"));
        System.out.println("  successfulOrders: " + statsWeek.get("successfulOrders"));
        System.out.println("  cancelledOrders: " + statsWeek.get("cancelledOrders"));
        System.out.println("  processingOrders: " + statsWeek.get("processingOrders"));
        System.out.println("  actualRevenue: " + statsWeek.get("actualRevenue"));
        System.out.println("  expectedRevenue: " + statsWeek.get("expectedRevenue"));
        System.out.println("  refundedRevenue: " + statsWeek.get("refundedRevenue"));
        System.out.println("  pendingRefund: " + statsWeek.get("pendingRefund"));
        System.out.println("  productsSold: " + metricsWeek.totalProductsSold());

        // Audit Top Products
        @SuppressWarnings("unchecked")
        List<TopProductDTO> topProducts = (List<TopProductDTO>) statsWeek.get("topProducts");
        Double totalValidProductLineRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(startWeek, endWeek);
        System.out.println("\n[AUDIT 4 & 5] TOP PRODUCTS & REVENUE PERCENTAGE");
        System.out.println("  Total Valid Product-Line Revenue (Denominator): " + totalValidProductLineRevenue);
        for (TopProductDTO tp : topProducts) {
            System.out.printf("  Product ID=%d | Name=%s | Category=%s | SoldQty=%d | LineRevenue=%s | SharePct=%.2f%%%n",
                    tp.productId(), tp.productName(), tp.categoryName(), tp.soldQuantity(), tp.revenue(), tp.percentage());
        }

        // Audit Brands
        @SuppressWarnings("unchecked")
        List<BrandRevenueDTO> brandRevenues = (List<BrandRevenueDTO>) statsWeek.get("brandRevenues");
        System.out.println("\n[AUDIT 6] BRAND REVENUE & SHARE");
        double sumBrandRevenue = 0.0;
        double sumBrandPct = 0.0;
        for (BrandRevenueDTO br : brandRevenues) {
            double r = br.revenue() != null ? br.revenue().doubleValue() : 0.0;
            double p = br.percentage() != null ? br.percentage() : 0.0;
            sumBrandRevenue += r;
            sumBrandPct += p;
            System.out.printf("  Brand ID=%d | BrandName=%s | SoldQty=%d | Revenue=%s | SharePct=%.2f%%%n",
                    br.brandId(), br.brandName(), br.soldQuantity(), br.revenue(), br.percentage());
        }
        System.out.printf("  Sum Brand Revenue: %.2f | Sum Brand Pct: %.2f%%%n", sumBrandRevenue, sumBrandPct);

        // Audit Slow Moving Products
        @SuppressWarnings("unchecked")
        List<SlowMovingProductDTO> slowMoving = (List<SlowMovingProductDTO>) statsWeek.get("slowMovingProducts");
        System.out.println("\n[AUDIT 9] SLOW MOVING PRODUCTS (Top 5)");
        for (SlowMovingProductDTO sm : slowMoving) {
            System.out.printf("  Product ID=%d | Name=%s | Stock=%d | SoldInPeriod=%d | Warning=%s (%s)%n",
                    sm.productId(), sm.productName(), sm.stockQuantity(), sm.soldQuantity(), sm.warningBadge(), sm.warningLevel());
        }

        // Audit Insights
        @SuppressWarnings("unchecked")
        List<OperationalInsightDTO> insights = (List<OperationalInsightDTO>) statsWeek.get("insights");
        System.out.println("\n[AUDIT 10] OPERATIONAL INSIGHTS");
        for (OperationalInsightDTO ins : insights) {
            System.out.printf("  Type=[%s] | Title: %s | Message: %s%n", ins.type(), ins.title(), ins.message());
        }

        // Audit Growth
        @SuppressWarnings("unchecked")
        Map<String, GrowthMetricDTO> growthMap = (Map<String, GrowthMetricDTO>) statsWeek.get("growth");
        System.out.println("\n[AUDIT 8] GROWTH METRICS");
        if (growthMap != null) {
            for (Map.Entry<String, GrowthMetricDTO> entry : growthMap.entrySet()) {
                GrowthMetricDTO g = entry.getValue();
                System.out.printf("  Metric: %s | Current=%.2f | Previous=%.2f | Pct=%.2f%% | Formatted=%s | Dir=%s | isNew=%b%n",
                        entry.getKey(), g.currentValue(), g.previousValue(), g.percentageChange() != null ? g.percentageChange() : 0.0, g.formattedChange(), g.direction(), g.isNew());
            }
        } else {
            System.out.println("  Growth map is null (e.g. all_time)");
        }

        // Audit Excel Export
        System.out.println("\n[AUDIT 11] TESTING EXCEL EXPORT WORKBOOK GENERATION");
        byte[] excelBytes = adminThongKeService.exportToExcel(startWeek, endWeek);
        System.out.println("  Generated Excel size in bytes: " + excelBytes.length);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            int numberOfSheets = wb.getNumberOfSheets();
            System.out.println("  Number of sheets: " + numberOfSheets);
            for (int s = 0; s < numberOfSheets; s++) {
                Sheet sheet = wb.getSheetAt(s);
                System.out.println("    Sheet " + (s + 1) + ": " + sheet.getSheetName() + " (rows=" + sheet.getPhysicalNumberOfRows() + ")");
            }
        }

        System.out.println("\n================================================================================");
        System.out.println("                         AUDIT COMPLETE SUCCESS                                 ");
        System.out.println("================================================================================");
    }

    @Autowired
    private com.smashvn.shop.repository.SanPhamRepository sanPhamRepository;

    @Autowired
    private com.smashvn.shop.repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private com.smashvn.shop.repository.ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void inspectDatabaseForDemoPlan() {
        System.out.println("================================================================================");
        System.out.println("                 DATABASE INSPECTION FOR DEMO SEED PLAN                         ");
        System.out.println("================================================================================");

        System.out.println("\n--- BRANDS (ThuongHieu) ---");
        List<Map<String, Object>> brands = jdbcTemplate.queryForList("SELECT * FROM ThuongHieu");
        for (var b : brands) {
            System.out.println("  Brand: " + b);
        }

        System.out.println("\n--- PAYMENT METHODS (PhuongThucThanhToan) ---");
        List<Map<String, Object>> pttts = jdbcTemplate.queryForList("SELECT * FROM PhuongThucThanhToan");
        for (var p : pttts) {
            System.out.println("  PTTT: " + p);
        }

        System.out.println("\n--- CUSTOMERS (KhachHang) ---");
        List<Map<String, Object>> customers = jdbcTemplate.queryForList("SELECT * FROM KhachHang");
        System.out.println("  Total customers: " + customers.size());
        for (var c : customers) {
            System.out.println("  Customer: " + c);
        }

        System.out.println("\n--- PRODUCTS & VARIANTS ---");
        List<Map<String, Object>> products = jdbcTemplate.queryForList(
            "SELECT sp.id AS product_id, sp.ten_san_pham, th.ten_thuong_hieu, dm.ten_danh_muc, sp.trang_thai, " +
            "spct.id AS spct_id, spct.so_luong_ton, spct.gia_ban, spct.trang_thai AS spct_status " +
            "FROM SanPham sp " +
            "LEFT JOIN ThuongHieu th ON th.id = sp.id_thuong_hieu " +
            "LEFT JOIN DanhMuc dm ON dm.id = sp.id_danh_muc " +
            "LEFT JOIN SanPhamChiTiet spct ON spct.id_san_pham = sp.id " +
            "WHERE sp.trang_thai = 1 " +
            "ORDER BY th.ten_thuong_hieu, sp.id, spct.id"
        );
        for (var p : products) {
            System.out.println("  Product/Variant: " + p);
        }

        System.out.println("\n--- ALL EXISTING ORDERS (HoaDon) ---");
        List<Map<String, Object>> allOrders = jdbcTemplate.queryForList(
            "SELECT hd.id, hd.ngay_tao, hd.tong_tien, hd.trang_thai_don_hang, hd.trang_thai_thanh_toan, " +
            "kh.ho_ten_kh, pttt.ten_phuong_thuc " +
            "FROM HoaDon hd " +
            "LEFT JOIN KhachHang kh ON kh.id = hd.id_khach_hang " +
            "LEFT JOIN PhuongThucThanhToan pttt ON pttt.id = hd.id_phuong_thuc_thanh_toan " +
            "ORDER BY hd.id"
        );
        System.out.println("  Total orders in DB: " + allOrders.size());
        for (var hd : allOrders) {
            System.out.println("  Order: " + hd);
        }

        System.out.println("\n--- EXISTING ORDER ITEMS (HoaDonChiTiet) ---");
        List<Map<String, Object>> allItems = jdbcTemplate.queryForList(
            "SELECT hdct.id, hdct.id_hoa_don, hdct.id_san_pham_chi_tiet, hdct.so_luong, hdct.don_gia, sp.ten_san_pham " +
            "FROM HoaDonChiTiet hdct " +
            "LEFT JOIN SanPhamChiTiet spct ON spct.id = hdct.id_san_pham_chi_tiet " +
            "LEFT JOIN SanPham sp ON sp.id = spct.id_san_pham " +
            "ORDER BY hdct.id_hoa_don, hdct.id"
        );
        for (var item : allItems) {
            System.out.println("  Item: " + item);
        }
        System.out.println("================================================================================");
    }

    @Test
    public void testDateRangeAndQueryBaseline() {
        System.out.println("================================================================================");
        System.out.println("                 PHASE 0 & PHASE 1: DATE RANGE & BASELINE TEST                  ");
        System.out.println("================================================================================");

        // Test Preset: "last_30_days"
        Map<String, LocalDateTime> range30 = adminThongKeService.getDateRange("last_30_days", null, null);
        LocalDateTime start30 = range30.get("start");
        LocalDateTime end30 = range30.get("end");

        Map<String, LocalDateTime> prevRange30 = adminThongKeService.getPreviousDateRange("last_30_days", start30, end30);
        LocalDateTime prevStart30 = prevRange30.get("start");
        LocalDateTime prevEnd30 = prevRange30.get("end");

        long currentDays30 = java.time.temporal.ChronoUnit.DAYS.between(start30.toLocalDate(), end30.toLocalDate()) + 1;
        long prevDays30 = java.time.temporal.ChronoUnit.DAYS.between(prevStart30.toLocalDate(), prevEnd30.toLocalDate()) + 1;

        System.out.println("\n[PHASE 0: DATE RANGE VERIFICATION]");
        System.out.printf("  Preset 'last_30_days': Current = %s -> %s (%d days)%n", start30, end30, currentDays30);
        System.out.printf("                         Previous = %s -> %s (%d days)%n", prevStart30, prevEnd30, prevDays30);

        org.junit.jupiter.api.Assertions.assertEquals(30, currentDays30, "Current days for last_30_days must be exactly 30");
        org.junit.jupiter.api.Assertions.assertEquals(30, prevDays30, "Previous days for last_30_days must be exactly 30");

        // Query Stats for last_30_days
        Map<String, Object> stats30 = adminThongKeService.getStatisticsData("last_30_days", start30, end30);
        @SuppressWarnings("unchecked")
        List<String> chartLabels = (List<String>) stats30.get("chartLabels");
        System.out.printf("  chartLabels.size() = %d (Expected = 30)%n", chartLabels.size());
        org.junit.jupiter.api.Assertions.assertEquals(30, chartLabels.size(), "chartLabels size must be exactly 30");

        GeneralMetricsDTO metrics30 = (GeneralMetricsDTO) stats30.get("metrics");

        System.out.println("\n[PHASE 1: CURRENT BASELINE FOR 'last_30_days' (21/07 -> 19/08)]");
        System.out.println("  totalOrders: " + stats30.get("totalOrders"));
        System.out.println("  successfulOrders: " + stats30.get("successfulOrders"));
        System.out.println("  cancelledOrders: " + stats30.get("cancelledOrders"));
        System.out.println("  processingOrders: " + stats30.get("processingOrders"));
        System.out.println("  actualRevenue: " + stats30.get("actualRevenue"));
        System.out.println("  AOV: " + metrics30.avgOrderValue());
        System.out.println("  newCustomers: " + stats30.get("newCustomers"));
        System.out.println("  productsSold: " + metrics30.totalProductsSold());
        System.out.println("  expectedRevenue: " + stats30.get("expectedRevenue"));
        System.out.println("  refundedRevenue: " + stats30.get("refundedRevenue"));
        System.out.println("  pendingRefund: " + stats30.get("pendingRefund"));

        Double totalValidProductRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(start30, end30);
        System.out.println("  productRevenue (totalValidProductLineRevenue): " + totalValidProductRevenue);

        // Previous period baseline
        Map<String, Object> statsPrev30 = adminThongKeService.getStatisticsData("last_30_days", prevStart30, prevEnd30);
        GeneralMetricsDTO metricsPrev30 = (GeneralMetricsDTO) statsPrev30.get("metrics");

        System.out.println("\n[PHASE 1: PREVIOUS BASELINE FOR 'last_30_days' (21/06 -> 20/07)]");
        System.out.println("  prev totalOrders: " + statsPrev30.get("totalOrders"));
        System.out.println("  prev successfulOrders: " + statsPrev30.get("successfulOrders"));
        System.out.println("  prev cancelledOrders: " + statsPrev30.get("cancelledOrders"));
        System.out.println("  prev processingOrders: " + statsPrev30.get("processingOrders"));
        System.out.println("  prev actualRevenue: " + statsPrev30.get("actualRevenue"));
        System.out.println("  prev AOV: " + metricsPrev30.avgOrderValue());
        System.out.println("  prev newCustomers: " + statsPrev30.get("newCustomers"));
        System.out.println("  prev productsSold: " + metricsPrev30.totalProductsSold());

        System.out.println("================================================================================");
    }

    @Test
    public void testDryRunSeedAndRollback() throws Exception {
        System.out.println("================================================================================");
        System.out.println("                 DRY RUN: SEED -> AUDIT ASSERTIONS -> ROLLBACK                  ");
        System.out.println("================================================================================");

        // Read seed and rollback SQL scripts
        String seedSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-seed.sql"), java.nio.charset.StandardCharsets.UTF_8);
        String rollbackSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-rollback.sql"), java.nio.charset.StandardCharsets.UTF_8);

        // 1. Execute Seed
        System.out.println("\n[DRY RUN STEP 1] Executing demo-statistics-seed.sql...");
        jdbcTemplate.execute(seedSql);

        try {
            // 2. Verify Insert Counts
            int tkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_cust_%'", Integer.class);
            int khCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KhachHang kh JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int dcCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SoDiaChi dc JOIN KhachHang kh ON kh.id = dc.id_khach_hang JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int hdCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon WHERE ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);
            int hdctCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDonChiTiet hdct JOIN HoaDon hd ON hd.id = hdct.id_hoa_don WHERE hd.ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);

            System.out.println("  Inserted TaiKhoan: " + tkCount + " (Expected: 18)");
            System.out.println("  Inserted KhachHang: " + khCount + " (Expected: 18)");
            System.out.println("  Inserted SoDiaChi: " + dcCount + " (Expected: 18)");
            System.out.println("  Inserted HoaDon: " + hdCount + " (Expected: 41)");
            System.out.println("  Inserted HoaDonChiTiet: " + hdctCount + " (Expected: 41)");

            org.junit.jupiter.api.Assertions.assertEquals(18, tkCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, khCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, dcCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdctCount);

            // 3. Query Statistics for "last_30_days"
            Map<String, LocalDateTime> range30 = adminThongKeService.getDateRange("last_30_days", null, null);
            LocalDateTime startCurr = range30.get("start");
            LocalDateTime endCurr = range30.get("end");

            Map<String, LocalDateTime> prevRange30 = adminThongKeService.getPreviousDateRange("last_30_days", startCurr, endCurr);
            LocalDateTime startPrev = prevRange30.get("start");
            LocalDateTime endPrev = prevRange30.get("end");

            Map<String, Object> statsCurr = adminThongKeService.getStatisticsData("last_30_days", startCurr, endCurr);
            Map<String, Object> statsPrev = adminThongKeService.getStatisticsData("last_30_days", startPrev, endPrev);

            GeneralMetricsDTO mCurr = (GeneralMetricsDTO) statsCurr.get("metrics");
            GeneralMetricsDTO mPrev = (GeneralMetricsDTO) statsPrev.get("metrics");

            System.out.println("\n[DRY RUN STEP 2: KPI VERIFICATION]");
            System.out.println("  Previous Revenue: " + statsPrev.get("actualRevenue") + " (Expected: 61386000.00)");
            System.out.println("  Current Revenue: " + statsCurr.get("actualRevenue") + " (Expected: 88263800.00)");
            System.out.println("  Previous Total Orders: " + statsPrev.get("totalOrders") + " (Expected: 20)");
            System.out.println("  Current Total Orders: " + statsCurr.get("totalOrders") + " (Expected: 28)");
            System.out.println("  Current Success Orders: " + statsCurr.get("successfulOrders") + " (Expected: 22)");
            System.out.println("  Current Processing Orders: " + statsCurr.get("processingOrders") + " (Expected: 4)");
            System.out.println("  Current Cancelled Orders: " + statsCurr.get("cancelledOrders") + " (Expected: 2)");
            System.out.println("  Previous New Customers: " + statsPrev.get("newCustomers") + " (Expected: 8)");
            System.out.println("  Current New Customers: " + statsCurr.get("newCustomers") + " (Expected: 11)");
            System.out.println("  Current Products Sold: " + mCurr.totalProductsSold() + " (Expected: 26)");
            System.out.println("  Pending Refund: " + statsCurr.get("pendingRefund") + " (Expected: 4806400.00)");

            Double totalValidProductRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(startCurr, endCurr);
            System.out.println("  Current Product Revenue: " + totalValidProductRevenue + " (Expected: 86121000.0)");

            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("61386000.00"), statsPrev.get("actualRevenue"));
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("88263800.00"), statsCurr.get("actualRevenue"));
            org.junit.jupiter.api.Assertions.assertEquals(20L, statsPrev.get("totalOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(28L, statsCurr.get("totalOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(22L, statsCurr.get("successfulOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(4L, statsCurr.get("processingOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(2L, statsCurr.get("cancelledOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(8L, statsPrev.get("newCustomers"));
            org.junit.jupiter.api.Assertions.assertEquals(11L, statsCurr.get("newCustomers"));
            org.junit.jupiter.api.Assertions.assertEquals(26L, mCurr.totalProductsSold());
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("4806400.00"), statsCurr.get("pendingRefund"));
            org.junit.jupiter.api.Assertions.assertEquals(86121000.0, totalValidProductRevenue);

            // Audit Top Products
            @SuppressWarnings("unchecked")
            List<TopProductDTO> topProducts = (List<TopProductDTO>) statsCurr.get("topProducts");
            System.out.println("\n[DRY RUN STEP 3: TOP PRODUCTS]");
            for (TopProductDTO tp : topProducts) {
                System.out.printf("  Product ID=%d | Name=%s | Category=%s | SoldQty=%d | LineRevenue=%s | SharePct=%.2f%%%n",
                        tp.productId(), tp.productName(), tp.categoryName(), tp.soldQuantity(), tp.revenue(), tp.percentage());
            }

            // Audit Brand Revenue
            @SuppressWarnings("unchecked")
            List<BrandRevenueDTO> brandRevenues = (List<BrandRevenueDTO>) statsCurr.get("brandRevenues");
            System.out.println("\n[DRY RUN STEP 4: BRAND REVENUES]");
            double sumBrandRevenue = 0.0;
            for (BrandRevenueDTO br : brandRevenues) {
                double r = br.revenue() != null ? br.revenue().doubleValue() : 0.0;
                sumBrandRevenue += r;
                System.out.printf("  Brand ID=%d | BrandName=%s | SoldQty=%d | Revenue=%s | SharePct=%.2f%%%n",
                        br.brandId(), br.brandName(), br.soldQuantity(), br.revenue(), br.percentage());
            }
            System.out.printf("  Sum Brand Revenue: %.2f (Expected: 86121000.00)%n", sumBrandRevenue);
            org.junit.jupiter.api.Assertions.assertEquals(86121000.0, sumBrandRevenue, 0.01);

        } finally {
            // 4. Rollback
            System.out.println("\n[DRY RUN STEP 5] Executing demo-statistics-rollback.sql...");
            jdbcTemplate.execute(rollbackSql);

            int remainingDemoTk = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_%'", Integer.class);
            int remainingOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon", Integer.class);

            System.out.println("  Remaining Demo Accounts: " + remainingDemoTk + " (Expected: 0)");
            System.out.println("  Remaining Total Orders: " + remainingOrders + " (Expected: 7 original orders)");

            org.junit.jupiter.api.Assertions.assertEquals(0, remainingDemoTk);
            org.junit.jupiter.api.Assertions.assertEquals(7, remainingOrders);
            System.out.println("\n==> [DRY RUN SUCCESS] All assertions PASS and DB was cleanly rolled back to 7 original orders!");
        }
        System.out.println("================================================================================");
    }
}
