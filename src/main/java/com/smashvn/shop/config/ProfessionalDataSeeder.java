package com.smashvn.shop.config;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.smashvn.shop.dao.HinhAnhSanPhamDAO;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DanhMucThuocTinh;
import com.smashvn.shop.entity.HinhAnhSanPham;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.DanhMucThuocTinhRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamChiTietThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Professional Data Seeder for SMASH-VN development environment.
 *
 * Placeholder evidence (binary audit 2026-08-03):
 *   18 files, each exactly 84 bytes, PNG 110x10 RGBA,
 *   chunks: IHDR[13] IDAT[27] IEND[0],
 *   all share SHA-256: 01f477834dbdd8d5b38739af3b68c12c75f6e723f824349adde2c5d24a95ceb9
 *
 * Expected filesystem counts (chot audit):
 *   totalFilesFound       = 169
 *   placeholdersSkipped   = 18
 *   validFileImages       = 151
 *   rootUnmappedSkipped   = 33
 *   seedableImages        = 118
 *   productGroups         = 71
 */
// @Component - Tạm thời vô hiệu hóa seeder để chuẩn bị cho dữ liệu demo
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class ProfessionalDataSeeder implements CommandLineRunner {

    // Known SHA-256 of the 18 placeholder files (110x10 RGBA PNG, 84 bytes each)
    private static final String KNOWN_PLACEHOLDER_SHA256 =
        "01f477834dbdd8d5b38739af3b68c12c75f6e723f824349adde2c5d24a95ceb9";
    private static final int KNOWN_PLACEHOLDER_SIZE = 84;

    @Value("${app.seed.dry-run:true}")
    private boolean dryRun;

    @Value("${app.seed.product-image-root:uploads/product}")
    private String productImageRoot;

    @Value("${app.seed.include-commerce:false}")
    private boolean includeCommerce;

    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ThuocTinhRepository thuocTinhRepository;
    private final DanhMucThuocTinhRepository danhMucThuocTinhRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietThuocTinhRepository sanPhamChiTietThuocTinhRepository;
    private final HinhAnhSanPhamDAO hinhAnhSanPhamDAO;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    private static final List<String> KNOWN_BRANDS = Arrays.asList(
        "Yonex", "Li-Ning", "Victor", "Mizuno", "Kawasaki", "Kamito", "Apacs", "VS"
    );

    private static final List<String> NINE_KNOWN_FOLDERS = Arrays.asList(
        "Balo", "C\u01b0\u1edbc", "Gi\u00e0y", "Li-Ning", "Qu\u1ea5n c\u00e1n", "Qu\u1ea7n", "T\u00fai X\u00e1ch", "Yonex", "\u00c1o"
    );

    // Expected filesystem counts (chot audit)
    private static final int EXPECTED_TOTAL_FILES     = 169;
    private static final int EXPECTED_PLACEHOLDERS    = 18;
    private static final int EXPECTED_VALID_IMAGES    = 151;
    private static final int EXPECTED_ROOT_UNMAPPED   = 33;
    private static final int EXPECTED_SEEDABLE_IMAGES = 118;
    private static final int EXPECTED_PRODUCT_GROUPS  = 71;
    private static final int EXPECTED_UNSUPPORTED     = 0;
    private static final int EXPECTED_INVALID_HEADER  = 0;

    @Override
    public void run(String... args) throws Exception {
        log.info("=================================================");
        log.info("      STARTING PROFESSIONAL DATA SEEDER         ");
        log.info("=================================================");
        log.info("SEED MODE              : {}", dryRun ? "DRY_RUN" : "COMMIT");
        log.info("IMAGE ROOT             : {}", productImageRoot);
        log.info("INCLUDE COMMERCE       : {}", includeCommerce);

        if (includeCommerce) {
            throw new IllegalStateException("Commerce seed is not implemented and remains blocked");
        }

        if (!dryRun) {
            log.info("Dry-run is false. Executing full catalog commit in real transaction...");
            executeCommitInRealTransaction();
            return;
        }

        scanAndReportDryRun();
    }

    // =========================================================================
    //  DRY-RUN
    // =========================================================================

    private void scanAndReportDryRun() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            try {
                SeedPlan plan = buildSeedPlan();
                logDryRunReport(plan);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // =========================================================================
    //  COMMIT
    // =========================================================================

    private void executeCommitInRealTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            try {
                SeedContext context = new SeedContext();
                SeedPlan plan = buildSeedPlan();
                validatePreflightCommit(plan);
                seedAccounts(context, plan);
                seedEmployeeAndCustomerProfiles(context, plan);
                seedCategories(context, plan);
                seedBrands(context, plan);
                seedAttributes(context, plan);
                seedCategoryAttributes(context, plan);
                seedProductsAndVariants(context, plan);
                seedProductImages(context, plan);
                flushCommitChanges();
                validateCommitResult(context, plan);
                logCommitReport(context, plan);
            } catch (RuntimeException e) {
                log.error("[COMMIT_ERROR] Failure during database seed transaction. Rolling back...", e);
                status.setRollbackOnly();
                throw e;
            } catch (Exception e) {
                log.error("[COMMIT_ERROR] Unexpected error during database seed transaction. Rolling back...", e);
                status.setRollbackOnly();
                throw new RuntimeException(e);
            }
        });
    }

    // =========================================================================
    //  PREFLIGHT
    // =========================================================================

    private void validatePreflightCommit(SeedPlan plan) {
        if (includeCommerce) {
            throw new IllegalStateException("Commerce seed is not implemented and remains blocked");
        }
        if (plan.totalFilesFound != EXPECTED_TOTAL_FILES) {
            throw new IllegalStateException("Preflight failed: totalFilesFound=" + plan.totalFilesFound + ", expected=" + EXPECTED_TOTAL_FILES);
        }
        if (plan.placeholdersSkipped != EXPECTED_PLACEHOLDERS) {
            throw new IllegalStateException("Preflight failed: placeholdersSkipped=" + plan.placeholdersSkipped + ", expected=" + EXPECTED_PLACEHOLDERS);
        }
        if (plan.validFileImages != EXPECTED_VALID_IMAGES) {
            throw new IllegalStateException("Preflight failed: validFileImages=" + plan.validFileImages + ", expected=" + EXPECTED_VALID_IMAGES);
        }
        if (plan.rootUnmappedSkipped != EXPECTED_ROOT_UNMAPPED) {
            throw new IllegalStateException("Preflight failed: rootUnmappedSkipped=" + plan.rootUnmappedSkipped + ", expected=" + EXPECTED_ROOT_UNMAPPED);
        }
        if (plan.seedableImages.size() != EXPECTED_SEEDABLE_IMAGES) {
            throw new IllegalStateException("Preflight failed: seedableImages=" + plan.seedableImages.size() + ", expected=" + EXPECTED_SEEDABLE_IMAGES);
        }
        if (plan.productGroups.size() != EXPECTED_PRODUCT_GROUPS) {
            throw new IllegalStateException("Preflight failed: productGroups=" + plan.productGroups.size() + ", expected=" + EXPECTED_PRODUCT_GROUPS);
        }
        if (plan.unsupportedExtensionSkipped != EXPECTED_UNSUPPORTED) {
            throw new IllegalStateException("Preflight failed: unsupportedExtensionSkipped=" + plan.unsupportedExtensionSkipped + ", expected=0");
        }
        if (plan.invalidHeaderSkipped != EXPECTED_INVALID_HEADER) {
            throw new IllegalStateException("Preflight failed: invalidHeaderSkipped=" + plan.invalidHeaderSkipped + ", expected=0");
        }
        if (plan.scanErrors != 0) {
            throw new IllegalStateException("Preflight failed: scanErrors=" + plan.scanErrors);
        }
        if (plan.accountConflicts != 0) {
            throw new IllegalStateException("Preflight failed: accountConflicts=" + plan.accountConflicts);
        }
        if (plan.profileConflicts != 0) {
            throw new IllegalStateException("Preflight failed: profileConflicts=" + plan.profileConflicts);
        }
        if (plan.masterDataConflicts != 0) {
            throw new IllegalStateException("Preflight failed: masterDataConflicts=" + plan.masterDataConflicts);
        }
        if (plan.catAttrConflicts != 0) {
            throw new IllegalStateException("Preflight failed: catAttrConflicts=" + plan.catAttrConflicts);
        }
        if (plan.productConflicts != 0) {
            throw new IllegalStateException("Preflight failed: productConflicts=" + plan.productConflicts);
        }
        if (plan.variantConflicts != 0) {
            throw new IllegalStateException("Preflight failed: variantConflicts=" + plan.variantConflicts);
        }
        if (plan.imageConflicts != 0) {
            throw new IllegalStateException("Preflight failed: imageConflicts=" + plan.imageConflicts);
        }
        if (plan.groupKeyConflicts != 0) {
            throw new IllegalStateException("Preflight failed: groupKeyConflicts=" + plan.groupKeyConflicts);
        }
        log.info("[PREFLIGHT_OK] All preflight preconditions passed successfully.");
    }

    // =========================================================================
    //  BUILD SEED PLAN
    // =========================================================================

    private SeedPlan buildSeedPlan() throws Exception {
        SeedPlan plan = new SeedPlan();
        auditAccounts(plan);
        scanFilesystem(plan);
        plan.productGroups = groupImageFiles(plan.seedableImages, plan);
        verifyGroupKeyUniqueness(plan);
        auditMasterData(plan);
        auditDbIdempotency(plan);
        return plan;
    }

    // =========================================================================
    //  STEP 1: ACCOUNT AUDIT
    // =========================================================================

    private void auditAccounts(SeedPlan plan) {
        List<TestAccountSpec> accountSpecs = getTestAccountSpecs();
        for (TestAccountSpec spec : accountSpecs) {
            TaiKhoan existing = taiKhoanRepository.findByUsername(spec.username);
            if (existing == null) {
                plan.accountsWouldInsert++;
                plan.profilesWouldInsert++;
            } else {
                boolean hasConflict = false;
                if (!spec.role.equalsIgnoreCase(existing.getVaiTro())) {
                    log.error("[ACCOUNT_ROLE_CONFLICT] User '{}' has role '{}' in DB, expected '{}'",
                        spec.username, existing.getVaiTro(), spec.role);
                    hasConflict = true;
                }
                if (existing.getMatKhau() == null) {
                    log.error("[ACCOUNT_PASSWORD_CONFLICT] User '{}' has null password in DB.", spec.username);
                    hasConflict = true;
                } else if (!passwordEncoder.matches("123456", existing.getMatKhau())) {
                    log.error("[ACCOUNT_PASSWORD_CONFLICT] User '{}' password does not match '123456'.", spec.username);
                    hasConflict = true;
                }
                if (existing.getTrangThaiTaiKhoan() != AccountStatus.ACTIVE) {
                    log.error("[ACCOUNT_STATUS_CONFLICT] User '{}' is not ACTIVE.", spec.username);
                    hasConflict = true;
                }
                if (hasConflict) {
                    plan.accountConflicts++;
                } else {
                    plan.accountsExisting++;
                    auditProfile(plan, spec, existing);
                }
            }
            plan.accountSpecs.add(spec);
        }
    }

    private void auditProfile(SeedPlan plan, TestAccountSpec spec, TaiKhoan existing) {
        NhanVien nv = nhanVienRepository.findByTaiKhoanId(existing.getId());
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(existing.getId());

        if (spec.role.equals("QL") || spec.role.equals("NV")) {
            if (nv == null && kh == null) {
                plan.profilesWouldInsert++;
            } else if (nv != null && kh == null) {
                plan.profilesExisting++;
            } else { // kh != null, bất kể nv có tồn tại hay không
                log.error("[PROFILE_ROLE_CONFLICT] User '{}' with role QL/NV has a KhachHang profile.", spec.username);
                plan.profileConflicts++;
            }
        } else if (spec.role.equals("KH")) {
            if (kh == null && nv == null) {
                plan.profilesWouldInsert++;
            } else if (kh != null && nv == null) {
                plan.profilesExisting++;
            } else { // nv != null, bất kể kh có tồn tại hay không
                log.error("[PROFILE_ROLE_CONFLICT] User '{}' with role KH has a NhanVien profile.", spec.username);
                plan.profileConflicts++;
            }
        }
    }

    // =========================================================================
    //  STEP 2: FILESYSTEM SCAN
    // =========================================================================

    private void scanFilesystem(SeedPlan plan) throws Exception {
        Path rootPath = Paths.get(productImageRoot).toAbsolutePath().normalize();
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            log.error("Image root directory does not exist or is not a directory: {}", rootPath);
            plan.scanErrors++;
            return;
        }
        try (Stream<Path> stream = Files.walk(rootPath)) {
            List<Path> allPaths = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            plan.totalFilesFound = allPaths.size();
            for (Path path : allPaths) {
                String fileName = path.getFileName().toString();
                long fileSize = Files.size(path);
                String relPathStr = rootPath.relativize(path).toString().replace('\\', '/');

                String ext = getExtension(fileName).toLowerCase(Locale.ROOT);
                if (!Arrays.asList(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                    plan.unsupportedExtensionSkipped++;
                    continue;
                }
                if (!isValidImageHeader(path)) {
                    plan.invalidHeaderSkipped++;
                    continue;
                }
                if (isKnownEmptyPngPlaceholder(path, fileSize)) {
                    plan.placeholdersSkipped++;
                    continue;
                }
                plan.validFileImages++;

                if (relPathStr.indexOf('/') < 0) {
                    plan.rootUnmappedSkipped++;
                    continue;
                }

                String[] parts = relPathStr.split("/");
                String topDir = parts[0];
                boolean knownFolder = NINE_KNOWN_FOLDERS.stream().anyMatch(f -> normalizeKey(f).equals(normalizeKey(topDir)));
                if (!knownFolder) {
                    log.error("[FOLDER_SCAN_CONFLICT] Unknown top-level folder detected: {}", topDir);
                    plan.scanErrors++;
                    continue;
                }
                plan.seedableImages.add(new ImageFileSpec(relPathStr, path, fileSize, fileName));
            }
        } catch (Exception e) {
            log.error("Error during filesystem walk: ", e);
            plan.scanErrors++;
        }
    }

    // =========================================================================
    //  STEP 3+4: GROUP KEY UNIQUENESS
    // =========================================================================

    private void verifyGroupKeyUniqueness(SeedPlan plan) {
        Map<String, String> naturalToStable = new LinkedHashMap<>();
        for (ProductGroupSpec spec : plan.productGroups.values()) {
            String naturalKey = normalizeKey(spec.productName) + "::" + normalizeKey(spec.brandName);
            if (naturalToStable.containsKey(naturalKey)) {
                String existingStable = naturalToStable.get(naturalKey);
                if (!existingStable.equals(spec.stableGroupKey)) {
                    log.error("[GROUP_KEY_CONFLICT] Stable keys '{}' and '{}' both map to natural key '{}'",
                        existingStable, spec.stableGroupKey, naturalKey);
                    plan.groupKeyConflicts++;
                    plan.productConflicts++;
                }
            } else {
                naturalToStable.put(naturalKey, spec.stableGroupKey);
            }
        }
    }

    // =========================================================================
    //  STEP 5: MASTER DATA AUDIT
    // =========================================================================

    private void auditMasterData(SeedPlan plan) {
        List<String> targetCats = Arrays.asList(
            "V\u1ee3t C\u1ea7u L\u00f4ng", "Trang Ph\u1ee5c", "Balo", "Gi\u00e0y C\u1ea7u L\u00f4ng",
            "C\u01b0\u1edbc C\u1ea7u L\u00f4ng", "T\u00fai C\u1ea7u L\u00f4ng", "Ph\u1ee5 Ki\u1ec7n C\u1ea7u L\u00f4ng"
        );
        List<DanhMuc> dbCats = danhMucRepository.findAll();
        Map<String, List<DanhMuc>> groupedCats = dbCats.stream()
            .collect(Collectors.groupingBy(d -> normalizeKey(d.getTenDanhMuc())));
        for (String cat : targetCats) {
            String norm = normalizeKey(cat);
            List<DanhMuc> matches = groupedCats.getOrDefault(norm, List.of());
            if (matches.isEmpty()) {
                plan.masterWouldInsert++;
            } else if (matches.size() == 1) {
                if (!Boolean.TRUE.equals(matches.get(0).getTrangThai())) {
                    log.error("[MASTER_DATA_CONFLICT] DanhMuc '{}' exists but is inactive", cat);
                    plan.masterDataConflicts++;
                } else {
                    plan.masterExisting++;
                }
            } else {
                log.error("[MASTER_DATA_CONFLICT] Multiple DanhMuc entries for '{}'", norm);
                plan.masterDataConflicts++;
            }
        }

        List<String> targetBrands = Arrays.asList(
            "Yonex", "Li-Ning", "Victor", "Mizuno", "Kawasaki", "Kamito", "Apacs", "VS", "Kh\u00e1c"
        );
        List<ThuongHieu> dbBrands = thuongHieuRepository.findAll();
        Map<String, List<ThuongHieu>> groupedBrands = dbBrands.stream()
            .collect(Collectors.groupingBy(t -> normalizeKey(t.getTenThuongHieu())));
        for (String brand : targetBrands) {
            String norm = normalizeKey(brand);
            List<ThuongHieu> matches = groupedBrands.getOrDefault(norm, List.of());
            if (matches.isEmpty()) {
                plan.masterWouldInsert++;
            } else if (matches.size() == 1) {
                if (!Boolean.TRUE.equals(matches.get(0).getTrangThai())) {
                    log.error("[MASTER_DATA_CONFLICT] ThuongHieu '{}' exists but is inactive", brand);
                    plan.masterDataConflicts++;
                } else {
                    plan.masterExisting++;
                }
            } else {
                log.error("[MASTER_DATA_CONFLICT] Multiple ThuongHieu entries for '{}'", norm);
                plan.masterDataConflicts++;
            }
        }

        List<String> targetAttrs = Arrays.asList(
            "M\u00e0u s\u1eafc", "Size", "Tr\u1ecdng l\u01b0\u1ee3ng", "S\u1ee9c c\u0103ng", "Ch\u1ea5t li\u1ec7u",
            "Ki\u1ec3u d\u00e1ng", "K\u00edch th\u01b0\u1edbc", "\u0110\u01b0\u1eddng k\u00ednh", "Chi\u1ec1u d\u00e0i"
        );
        List<ThuocTinh> dbAttrs = thuocTinhRepository.findAll();
        Map<String, List<ThuocTinh>> groupedAttrs = dbAttrs.stream()
            .collect(Collectors.groupingBy(t -> normalizeKey(t.getTenThuocTinh())));
        for (String attr : targetAttrs) {
            String norm = normalizeKey(attr);
            List<ThuocTinh> matches = groupedAttrs.getOrDefault(norm, List.of());
            if (matches.isEmpty()) {
                plan.masterWouldInsert++;
            } else if (matches.size() == 1) {
                if (!Boolean.TRUE.equals(matches.get(0).getTrangThai())) {
                    log.error("[MASTER_DATA_CONFLICT] ThuocTinh '{}' exists but is inactive", attr);
                    plan.masterDataConflicts++;
                } else {
                    plan.masterExisting++;
                }
            } else {
                log.error("[MASTER_DATA_CONFLICT] Multiple ThuocTinh entries for '{}'", norm);
                plan.masterDataConflicts++;
            }
        }

        auditCatAttrLinks(plan);
    }

    private void auditCatAttrLinks(SeedPlan plan) {
        Map<String, List<String>> catAttrMappings = getTargetCategoryAttributeMappings();
        List<DanhMucThuocTinh> allLinks = danhMucThuocTinhRepository.findAll();
        for (Map.Entry<String, List<String>> entry : catAttrMappings.entrySet()) {
            String catName = entry.getKey();
            for (String attrName : entry.getValue()) {
                long activeMatches = allLinks.stream().filter(l ->
                    l.getDanhMuc() != null
                    && normalizeKey(l.getDanhMuc().getTenDanhMuc()).equals(normalizeKey(catName))
                    && l.getThuocTinh() != null
                    && normalizeKey(l.getThuocTinh().getTenThuocTinh()).equals(normalizeKey(attrName))
                    && Boolean.TRUE.equals(l.getTrangThai())).count();
                long inactiveMatches = allLinks.stream().filter(l ->
                    l.getDanhMuc() != null
                    && normalizeKey(l.getDanhMuc().getTenDanhMuc()).equals(normalizeKey(catName))
                    && l.getThuocTinh() != null
                    && normalizeKey(l.getThuocTinh().getTenThuocTinh()).equals(normalizeKey(attrName))
                    && !Boolean.TRUE.equals(l.getTrangThai())).count();
                if (activeMatches == 0 && inactiveMatches == 0) {
                    plan.catAttrWouldInsert++;
                } else if (activeMatches == 1 && inactiveMatches == 0) {
                    plan.catAttrExisting++;
                } else {
                    log.error("[CAT_ATTR_CONFLICT] Cat-attr pair [{} x {}]: active={}, inactive={}",
                        catName, attrName, activeMatches, inactiveMatches);
                    plan.catAttrConflicts++;
                }
            }
        }
    }

    // =========================================================================
    //  STEP 6: DB IDEMPOTENCY AUDIT
    // =========================================================================

    private void auditDbIdempotency(SeedPlan plan) {
        List<SanPham> allSanPhams = sanPhamRepository.findAll();
        List<HinhAnhSanPham> allDbImages = hinhAnhSanPhamDAO.findAll();
        Map<String, List<HinhAnhSanPham>> urlToImages = allDbImages.stream()
            .filter(img -> img.getUrlHinhAnh() != null)
            .collect(Collectors.groupingBy(img -> normalizeImageUrlKey(img.getUrlHinhAnh())));

        for (ProductGroupSpec group : plan.productGroups.values()) {
            List<SanPham> matchingProducts = allSanPhams.stream().filter(sp ->
                sp.getTenSanPham() != null
                && normalizeKey(sp.getTenSanPham()).equals(normalizeKey(group.productName))
                && sp.getThuongHieu() != null
                && normalizeKey(sp.getThuongHieu().getTenThuongHieu()).equals(normalizeKey(group.brandName))
            ).collect(Collectors.toList());

            SanPham existingSp = null;
            if (matchingProducts.isEmpty()) {
                plan.productsWouldInsert++;
            } else if (matchingProducts.size() == 1) {
                existingSp = matchingProducts.get(0);
                plan.productsExisting++;
                if (existingSp.getDanhMuc() == null
                        || !normalizeKey(existingSp.getDanhMuc().getTenDanhMuc()).equals(normalizeKey(group.categoryName))) {
                    log.error("[PRODUCT_CATEGORY_CONFLICT] Product '{}' category mismatch: found='{}', expected='{}'",
                        group.productName,
                        existingSp.getDanhMuc() != null ? existingSp.getDanhMuc().getTenDanhMuc() : null,
                        group.categoryName);
                    plan.productConflicts++;
                }
            } else {
                log.error("[PRODUCT_CONFLICT] Multiple products for name='{}' brand='{}'", group.productName, group.brandName);
                plan.productConflicts++;
            }

            SanPhamChiTiet baseVariant = null;
            if (existingSp != null) {
                List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPham_Id(existingSp.getId());
                List<SanPhamChiTiet> baseVariants = new ArrayList<>();
                for (SanPhamChiTiet v : variants) {
                    List<SanPhamChiTietThuocTinh> atts = sanPhamChiTietThuocTinhRepository.findBySanPhamChiTiet_Id(v.getId());
                    if (atts == null || atts.isEmpty()) baseVariants.add(v);
                }
                if (baseVariants.isEmpty()) {
                    plan.variantsWouldInsert++;
                } else if (baseVariants.size() == 1) {
                    baseVariant = baseVariants.get(0);
                    plan.variantsExisting++;
                } else {
                    log.error("[VARIANT_BASE_CONFLICT] Multiple BASE variants for product '{}'", group.productName);
                    plan.variantConflicts++;
                }
            } else {
                plan.variantsWouldInsert++;
            }

            final SanPhamChiTiet finalBaseVariant = baseVariant;
            List<HinhAnhSanPham> baseImages = new ArrayList<>();
            if (finalBaseVariant != null) {
                baseImages = allDbImages.stream()
                    .filter(img -> img.getSanPhamChiTiet() != null
                        && img.getSanPhamChiTiet().getId().equals(finalBaseVariant.getId()))
                    .collect(Collectors.toList());
                long mainCount = 0;
                List<Integer> orders = new ArrayList<>();
                for (HinhAnhSanPham img : baseImages) {
                    if (Boolean.TRUE.equals(img.getLaAnhChinh())) mainCount++;
                    if (img.getThuTu() == null || img.getThuTu() <= 0) {
                        log.error("[IMAGE_ORDER_INVALID] Image ID {} invalid thuTu: {}", img.getId(), img.getThuTu());
                        plan.imageConflicts++;
                    } else {
                        if (orders.contains(img.getThuTu())) {
                            log.error("[IMAGE_ORDER_DUPLICATE] Image ID {} duplicate thuTu: {}", img.getId(), img.getThuTu());
                            plan.imageConflicts++;
                        }
                        orders.add(img.getThuTu());
                    }
                }
                if (!baseImages.isEmpty()) {
                    if (mainCount == 0) {
                        log.error("[IMAGE_MAIN_CONFLICT] BASE variant {} has images but no main.", finalBaseVariant.getId());
                        plan.imageConflicts++;
                    } else if (mainCount > 1) {
                        log.error("[IMAGE_MAIN_CONFLICT] BASE variant {} has multiple main images.", finalBaseVariant.getId());
                        plan.imageConflicts++;
                    }
                }
            }

            for (ImageFileSpec imgSpec : group.images) {
                String urlKey = normalizeImageUrlKey(imgSpec.relPath);
                List<HinhAnhSanPham> dbImgs = urlToImages.getOrDefault(urlKey, List.of());
                if (dbImgs.isEmpty()) {
                    plan.imagesWouldInsert++;
                } else if (dbImgs.size() == 1) {
                    HinhAnhSanPham dbImg = dbImgs.get(0);
                    if (finalBaseVariant != null
                            && dbImg.getSanPhamChiTiet() != null
                            && dbImg.getSanPhamChiTiet().getId().equals(finalBaseVariant.getId())) {
                        plan.imagesExisting++;
                    } else {
                        log.error("[IMAGE_OWNERSHIP_CONFLICT] Image '{}' belongs to variant {}, expected BASE {}",
                            imgSpec.relPath,
                            dbImg.getSanPhamChiTiet() != null ? dbImg.getSanPhamChiTiet().getId() : null,
                            finalBaseVariant != null ? finalBaseVariant.getId() : "null");
                        plan.imageConflicts++;
                    }
                } else {
                    log.error("[IMAGE_DUPLICATE_CONFLICT] Multiple records for URL '{}'", imgSpec.relPath);
                    plan.imageConflicts++;
                }
            }
        }
    }

    // =========================================================================
    //  COMMIT PHASES
    // =========================================================================

    private void seedAccounts(SeedContext context, SeedPlan plan) {
        for (TestAccountSpec spec : plan.accountSpecs) {
            TaiKhoan existing = taiKhoanRepository.findByUsername(spec.username);
            if (existing == null) {
                TaiKhoan tk = new TaiKhoan();
                tk.setUsername(spec.username);
                tk.setMatKhau(passwordEncoder.encode("123456"));
                tk.setVaiTro(spec.role);
                tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
                tk.setNgayTao(LocalDateTime.now());
                tk.setSoLanMuaThanhCong(0);
                tk.setSoLanNhacNhoViPham(0);
                TaiKhoan saved = taiKhoanRepository.save(tk);
                context.accounts.put(spec.username, saved);
                context.accountsInserted++;
                log.info("[COMMIT_ACCOUNT] Saved: username={}, role={}", saved.getUsername(), saved.getVaiTro());
            } else {
                context.accounts.put(spec.username, existing);
                context.accountsReused++;
            }
        }
    }

    private void seedEmployeeAndCustomerProfiles(SeedContext context, SeedPlan plan) {
        TaiKhoan adminTk = context.accounts.get("admin");
        if (adminTk != null) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(adminTk.getId());
            if (nv == null) {
                nv = new NhanVien();
                nv.setTaiKhoan(adminTk);
                nv.setHoTen("Qu\u1ea3n tr\u1ecb h\u1ec7 th\u1ed1ng");
                nv.setChucVu("Qu\u1ea3n l\u00fd");
                nv.setSoDienThoaiNv("0900000001");
                nv.setNgayTao(LocalDateTime.now());
                nv = nhanVienRepository.save(nv);
                context.profilesInserted++;
                log.info("[COMMIT_PROFILE] Saved NhanVien admin: {}", nv.getHoTen());
            } else {
                context.profilesReused++;
            }
            context.adminNhanVien = nv;
        }
        TaiKhoan nv01Tk = context.accounts.get("nhanvien01");
        if (nv01Tk != null) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(nv01Tk.getId());
            if (nv == null) {
                nv = new NhanVien();
                nv.setTaiKhoan(nv01Tk);
                nv.setHoTen("Nguy\u1ec5n V\u0103n Nh\u00e2n");
                nv.setChucVu("Nh\u00e2n vi\u00ean b\u00e1n h\u00e0ng");
                nv.setSoDienThoaiNv("0900000002");
                nv.setNgayTao(LocalDateTime.now());
                nhanVienRepository.save(nv);
                context.profilesInserted++;
                log.info("[COMMIT_PROFILE] Saved NhanVien nv01: {}", nv.getHoTen());
            } else {
                context.profilesReused++;
            }
        }
        for (int i = 1; i <= 12; i++) {
            String uname = String.format("khachhang%02d", i);
            TaiKhoan khTk = context.accounts.get(uname);
            if (khTk != null) {
                KhachHang kh = khachHangRepository.findByTaiKhoan_Id(khTk.getId());
                if (kh == null) {
                    kh = new KhachHang();
                    kh.setTaiKhoan(khTk);
                    kh.setHoTenKh("Kh\u00e1ch H\u00e0ng Demo " + i);
                    kh.setSoDienThoaiKh(String.format("098%07d", i));
                    kh.setNgayTao(LocalDateTime.now());
                    khachHangRepository.save(kh);
                    context.profilesInserted++;
                    log.info("[COMMIT_PROFILE] Saved KhachHang: {}", kh.getHoTenKh());
                } else {
                    context.profilesReused++;
                }
            }
        }
    }

    private void seedCategories(SeedContext context, SeedPlan plan) {
        List<String> categoryNames = Arrays.asList(
            "V\u1ee3t C\u1ea7u L\u00f4ng", "Trang Ph\u1ee5c", "Balo", "Gi\u00e0y C\u1ea7u L\u00f4ng",
            "C\u01b0\u1edbc C\u1ea7u L\u00f4ng", "T\u00fai C\u1ea7u L\u00f4ng", "Ph\u1ee5 Ki\u1ec7n C\u1ea7u L\u00f4ng"
        );
        List<DanhMuc> existingCategories = danhMucRepository.findAll();
        for (String catName : categoryNames) {
            List<DanhMuc> matches = existingCategories.stream()
                .filter(d -> d.getTenDanhMuc() != null && normalizeKey(d.getTenDanhMuc()).equals(normalizeKey(catName)))
                .collect(Collectors.toList());
            DanhMuc dm;
            if (matches.isEmpty()) {
                dm = new DanhMuc();
                dm.setTenDanhMuc(catName);
                dm.setTrangThai(true);
                dm = danhMucRepository.save(dm);
                context.categoriesInserted++;
                log.info("[COMMIT_CATEGORY] Saved DanhMuc: {}", dm.getTenDanhMuc());
            } else if (matches.size() == 1) {
                dm = matches.get(0);
                context.categoriesReused++;
            } else {
                throw new IllegalStateException("[COMMIT_ERROR] Multiple DanhMuc found for: " + catName);
            }
            context.categories.put(catName, dm);
        }
    }

    private void seedBrands(SeedContext context, SeedPlan plan) {
        List<String> brandNames = Arrays.asList(
            "Yonex", "Li-Ning", "Victor", "Mizuno", "Kawasaki", "Kamito", "Apacs", "VS", "Kh\u00e1c"
        );
        List<ThuongHieu> existingBrands = thuongHieuRepository.findAll();
        for (String bName : brandNames) {
            String canonical = normalizeKey(bName).equals("lining") ? "Li-Ning" : bName;
            List<ThuongHieu> matches = existingBrands.stream()
                .filter(b -> b.getTenThuongHieu() != null && normalizeKey(b.getTenThuongHieu()).equals(normalizeKey(canonical)))
                .collect(Collectors.toList());
            ThuongHieu th;
            if (matches.isEmpty()) {
                th = new ThuongHieu();
                th.setTenThuongHieu(canonical);
                th.setTrangThai(true);
                th = thuongHieuRepository.save(th);
                existingBrands.add(th);
                context.brandsInserted++;
                log.info("[COMMIT_BRAND] Saved ThuongHieu: {}", th.getTenThuongHieu());
            } else if (matches.size() == 1) {
                th = matches.get(0);
                context.brandsReused++;
            } else {
                throw new IllegalStateException("[COMMIT_ERROR] Multiple ThuongHieu found for: " + canonical);
            }
            context.brands.put(canonical, th);
        }
    }

    private void seedAttributes(SeedContext context, SeedPlan plan) {
        List<String> attrNames = Arrays.asList(
            "M\u00e0u s\u1eafc", "Size", "Tr\u1ecdng l\u01b0\u1ee3ng", "S\u1ee9c c\u0103ng", "Ch\u1ea5t li\u1ec7u",
            "Ki\u1ec3u d\u00e1ng", "K\u00edch th\u01b0\u1edbc", "\u0110\u01b0\u1eddng k\u00ednh", "Chi\u1ec1u d\u00e0i"
        );
        List<ThuocTinh> existingAttrs = thuocTinhRepository.findAll();
        for (String aName : attrNames) {
            List<ThuocTinh> matches = existingAttrs.stream()
                .filter(t -> t.getTenThuocTinh() != null && normalizeKey(t.getTenThuocTinh()).equals(normalizeKey(aName)))
                .collect(Collectors.toList());
            ThuocTinh tt;
            if (matches.isEmpty()) {
                tt = new ThuocTinh();
                tt.setTenThuocTinh(aName);
                tt.setTrangThai(true);
                tt = thuocTinhRepository.save(tt);
                context.attributesInserted++;
                log.info("[COMMIT_ATTRIBUTE] Saved ThuocTinh: {}", tt.getTenThuocTinh());
            } else if (matches.size() == 1) {
                tt = matches.get(0);
                context.attributesReused++;
            } else {
                throw new IllegalStateException("[COMMIT_ERROR] Multiple ThuocTinh found for: " + aName);
            }
            context.attributes.put(aName, tt);
        }
    }

    private void seedCategoryAttributes(SeedContext context, SeedPlan plan) {
        Map<String, List<String>> mappings = getTargetCategoryAttributeMappings();
        List<DanhMucThuocTinh> existingLinks = danhMucThuocTinhRepository.findAll();
        for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
            DanhMuc dm = context.categories.get(entry.getKey());
            if (dm == null) throw new IllegalStateException("[COMMIT_ERROR] Category not resolved: " + entry.getKey());
            for (String attrName : entry.getValue()) {
                ThuocTinh tt = context.attributes.get(attrName);
                if (tt == null) throw new IllegalStateException("[COMMIT_ERROR] Attribute not resolved: " + attrName);
                boolean exists = existingLinks.stream().anyMatch(l ->
                    l.getDanhMuc() != null && dm.getId().equals(l.getDanhMuc().getId())
                    && l.getThuocTinh() != null && tt.getId().equals(l.getThuocTinh().getId())
                    && Boolean.TRUE.equals(l.getTrangThai()));
                if (!exists) {
                    DanhMucThuocTinh dmtt = new DanhMucThuocTinh();
                    dmtt.setDanhMuc(dm);
                    dmtt.setThuocTinh(tt);
                    dmtt.setTrangThai(true);
                    danhMucThuocTinhRepository.save(dmtt);
                    existingLinks.add(dmtt);
                    context.linksInserted++;
                    log.info("[COMMIT_CAT_ATTR] Linked '{}' with '{}'", dm.getTenDanhMuc(), tt.getTenThuocTinh());
                } else {
                    context.linksReused++;
                }
            }
        }
    }

    private void seedProductsAndVariants(SeedContext context, SeedPlan plan) {
        if (context.adminNhanVien == null) {
            throw new IllegalStateException("NhanVien admin must be persisted before seeding products!");
        }
        for (ProductGroupSpec group : plan.productGroups.values()) {
            DanhMuc dm = context.categories.get(group.categoryName);
            ThuongHieu th = context.brands.get(group.brandName);
            if (dm == null) throw new IllegalStateException("[COMMIT_ERROR] Category not resolved for: " + group.productName);
            if (th == null) throw new IllegalStateException("[COMMIT_ERROR] Brand not resolved for: " + group.productName);

            List<SanPham> existingList = sanPhamRepository.findByThuongHieuId(th.getId()).stream()
                .filter(sp -> sp.getTenSanPham() != null
                    && normalizeKey(sp.getTenSanPham()).equals(normalizeKey(group.productName)))
                .collect(Collectors.toList());

            SanPham sp;
            if (existingList.isEmpty()) {
                sp = new SanPham();
                sp.setTenSanPham(group.productName);
                sp.setDanhMuc(dm);
                sp.setThuongHieu(th);
                sp.setNhanVien(context.adminNhanVien);
                sp.setTrangThaiValue(true);
                sp.setSoDanhGia(0);
                sp.setDiemTrungBinh(0.0);
                sp.setMoTa("D\u1eef li\u1ec7u s\u1ea3n ph\u1ea9m m\u1eabu cho m\u00f4i tr\u01b0\u1eddng ph\u00e1t tri\u1ec3n: " + group.productName);
                sp.setNgayTao(LocalDateTime.now());
                sp.setNgayCapNhat(LocalDateTime.now());
                sp = sanPhamRepository.save(sp);
                context.productsInserted++;
                log.info("[COMMIT_PRODUCT] Saved SanPham: ID={}, name={}", sp.getId(), sp.getTenSanPham());
            } else if (existingList.size() == 1) {
                sp = existingList.get(0);
                context.productsReused++;
            } else {
                throw new IllegalStateException("[COMMIT_ERROR] Multiple products found for name='" + group.productName + "'");
            }
            context.products.put(group.stableGroupKey, sp);

            List<SanPhamChiTiet> existingVariants = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
            List<SanPhamChiTiet> baseVariants = existingVariants.stream().filter(spct -> {
                List<SanPhamChiTietThuocTinh> atts = sanPhamChiTietThuocTinhRepository.findBySanPhamChiTiet_Id(spct.getId());
                return atts == null || atts.isEmpty();
            }).collect(Collectors.toList());

            SanPhamChiTiet spct;
            if (baseVariants.isEmpty()) {
                int seedHash = group.stableGroupKey.hashCode();
                int soLuongTon = 10 + Math.floorMod(seedHash, 31);
                BigDecimal giaBan = calculateDemoPrice(group.categoryName, seedHash);
                BigDecimal giaNhap = giaBan.multiply(new BigDecimal("0.75")).setScale(0, RoundingMode.HALF_UP);
                spct = new SanPhamChiTiet();
                spct.setSanPham(sp);
                spct.setGiaBan(giaBan);
                spct.setGiaNhap(giaNhap);
                spct.setSoLuongTon(soLuongTon);
                spct.setTrangThaiValue(true);
                spct.setNgayTao(LocalDateTime.now());
                spct.setNgayCapNhat(LocalDateTime.now());
                spct = sanPhamChiTietRepository.save(spct);
                context.variantsInserted++;
                log.info("[COMMIT_VARIANT] Saved SanPhamChiTiet: ID={}, Product={}", spct.getId(), sp.getTenSanPham());
            } else if (baseVariants.size() == 1) {
                spct = baseVariants.get(0);
                context.variantsReused++;
            } else {
                throw new IllegalStateException("[COMMIT_ERROR] Multiple BASE variants for product: " + group.productName);
            }
            context.productVariants.put(group.stableGroupKey, spct);
        }
    }

    private void seedProductImages(SeedContext context, SeedPlan plan) {
        List<HinhAnhSanPham> allDbImages = hinhAnhSanPhamDAO.findAll();
        Map<String, HinhAnhSanPham> normalizedUrlIndex = new LinkedHashMap<>();
        for (HinhAnhSanPham img : allDbImages) {
            if (img.getUrlHinhAnh() != null) {
                String key = normalizeImageUrlKey(img.getUrlHinhAnh());
                if (key != null) {
                    if (normalizedUrlIndex.containsKey(key)) {
                        throw new IllegalStateException("[COMMIT_ERROR] Duplicate normalized URL in DB: " + key);
                    }
                    normalizedUrlIndex.put(key, img);
                }
            }
        }
        for (ProductGroupSpec group : plan.productGroups.values()) {
            SanPhamChiTiet spct = context.productVariants.get(group.stableGroupKey);
            if (spct == null) throw new IllegalStateException("[COMMIT_ERROR] Missing variant for key: " + group.stableGroupKey);

            List<HinhAnhSanPham> existingImages = allDbImages.stream()
                .filter(img -> img.getSanPhamChiTiet() != null
                    && img.getSanPhamChiTiet().getId().equals(spct.getId()))
                .collect(Collectors.toList());

            boolean baseHasMain = existingImages.stream().anyMatch(img -> Boolean.TRUE.equals(img.getLaAnhChinh()));
            int maxExistingOrder = existingImages.stream()
                .map(HinhAnhSanPham::getThuTu).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
            int nextOrder = maxExistingOrder + 1;

            for (int i = 0; i < group.images.size(); i++) {
                ImageFileSpec imgSpec = group.images.get(i);
                String urlKey = normalizeImageUrlKey(imgSpec.relPath);
                if (urlKey == null) throw new IllegalStateException("[COMMIT_ERROR] Null URL key in group: " + group.productName);

                HinhAnhSanPham existing = normalizedUrlIndex.get(urlKey);
                if (existing == null) {
                    HinhAnhSanPham hasp = new HinhAnhSanPham();
                    hasp.setSanPhamChiTiet(spct);
                    hasp.setUrlHinhAnh(imgSpec.relPath);
                    if (!baseHasMain && i == 0) {
                        hasp.setLaAnhChinh(true);
                        baseHasMain = true;
                    } else {
                        hasp.setLaAnhChinh(false);
                    }
                    hasp.setThuTu(nextOrder++);
                    hinhAnhSanPhamDAO.save(hasp);
                    normalizedUrlIndex.put(urlKey, hasp);
                    context.imagesInserted++;
                    log.info("[COMMIT_IMAGE] Saved: URL={}, main={}, order={}",
                        hasp.getUrlHinhAnh(), hasp.getLaAnhChinh(), hasp.getThuTu());
                } else {
                    if (existing.getSanPhamChiTiet() == null || !existing.getSanPhamChiTiet().getId().equals(spct.getId())) {
                        throw new IllegalStateException("[COMMIT_ERROR] Image URL '" + imgSpec.relPath
                            + "' belongs to variant "
                            + (existing.getSanPhamChiTiet() != null ? existing.getSanPhamChiTiet().getId() : "null")
                            + " but expected BASE variant ID " + spct.getId());
                    }
                    context.imagesReused++;
                }
            }
        }
    }

    private void flushCommitChanges() {
        sanPhamRepository.flush();
        sanPhamChiTietRepository.flush();
        hinhAnhSanPhamDAO.flush();
    }

    // =========================================================================
    //  POST-FLUSH VALIDATION
    // =========================================================================

    private void validateCommitResult(SeedContext context, SeedPlan plan) {
        if (context.accounts.size() != 14) {
            throw new IllegalStateException("[VALIDATION_FAILED] Expected 14 accounts, found " + context.accounts.size());
        }
        for (TestAccountSpec spec : plan.accountSpecs) {
            TaiKhoan tk = taiKhoanRepository.findByUsername(spec.username);
            if (tk == null || !spec.role.equalsIgnoreCase(tk.getVaiTro()) || tk.getTrangThaiTaiKhoan() != AccountStatus.ACTIVE) {
                throw new IllegalStateException("[VALIDATION_FAILED] Account verification failed for: " + spec.username);
            }
            if (!passwordEncoder.matches("123456", tk.getMatKhau())) {
                throw new IllegalStateException("[VALIDATION_FAILED] Password mismatch for: " + spec.username);
            }
        }
        NhanVien adminNv = nhanVienRepository.findByTaiKhoanId(context.accounts.get("admin").getId());
        NhanVien staffNv = nhanVienRepository.findByTaiKhoanId(context.accounts.get("nhanvien01").getId());
        if (adminNv == null || staffNv == null) {
            throw new IllegalStateException("[VALIDATION_FAILED] NhanVien resolve failed.");
        }
        for (int i = 1; i <= 12; i++) {
            String uname = String.format("khachhang%02d", i);
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(context.accounts.get(uname).getId());
            if (kh == null) throw new IllegalStateException("[VALIDATION_FAILED] KhachHang profile failed: " + uname);
        }
        if (khachHangRepository.findByTaiKhoan_Id(context.accounts.get("admin").getId()) != null) {
            throw new IllegalStateException("[VALIDATION_FAILED] Admin has an illegal KhachHang profile.");
        }
        if (khachHangRepository.findByTaiKhoan_Id(context.accounts.get("nhanvien01").getId()) != null) {
            throw new IllegalStateException("[VALIDATION_FAILED] Nhanvien01 has an illegal KhachHang profile.");
        }
        for (int i = 1; i <= 12; i++) {
            String uname = String.format("khachhang%02d", i);
            if (nhanVienRepository.findByTaiKhoanId(context.accounts.get(uname).getId()) != null) {
                throw new IllegalStateException("[VALIDATION_FAILED] " + uname + " has an illegal NhanVien profile.");
            }
        }
        if (context.categories.size() != 7 || context.brands.size() != 9 || context.attributes.size() != 9) {
            throw new IllegalStateException("[VALIDATION_FAILED] Master data sizes failed.");
        }
        for (DanhMuc dm : context.categories.values()) {
            if (!Boolean.TRUE.equals(dm.getTrangThai())) {
                throw new IllegalStateException("[VALIDATION_FAILED] DanhMuc not active: " + dm.getTenDanhMuc());
            }
        }
        for (ThuongHieu th : context.brands.values()) {
            if (!Boolean.TRUE.equals(th.getTrangThai())) {
                throw new IllegalStateException("[VALIDATION_FAILED] ThuongHieu not active: " + th.getTenThuongHieu());
            }
        }
        for (ThuocTinh tt : context.attributes.values()) {
            if (!Boolean.TRUE.equals(tt.getTrangThai())) {
                throw new IllegalStateException("[VALIDATION_FAILED] ThuocTinh not active: " + tt.getTenThuocTinh());
            }
        }
        if (!verifyTargetCategoryAttributes()) {
            throw new IllegalStateException("[VALIDATION_FAILED] Target 26 Cat-Attr links verification failed.");
        }
        if (context.products.size() != 71) {
            throw new IllegalStateException("[VALIDATION_FAILED] Expected 71 products, found: " + context.products.size());
        }
        long uniqueProductIds = context.products.values().stream().map(SanPham::getId).distinct().count();
        if (uniqueProductIds != 71) {
            throw new IllegalStateException("[VALIDATION_FAILED] Duplicate Product IDs detected.");
        }
        for (Map.Entry<String, SanPham> entry : context.products.entrySet()) {
            SanPham sp = entry.getValue();
            ProductGroupSpec group = plan.productGroups.get(entry.getKey());
            if (group == null) continue;
            if (sp.getDanhMuc() == null
                    || !normalizeKey(sp.getDanhMuc().getTenDanhMuc()).equals(normalizeKey(group.categoryName))) {
                throw new IllegalStateException("[VALIDATION_FAILED] Product '" + sp.getTenSanPham() + "' category mismatch.");
            }
            if (sp.getThuongHieu() == null
                    || !normalizeKey(sp.getThuongHieu().getTenThuongHieu()).equals(normalizeKey(group.brandName))) {
                throw new IllegalStateException("[VALIDATION_FAILED] Product '" + sp.getTenSanPham() + "' brand mismatch.");
            }
        }
        if (context.productVariants.size() != 71) {
            throw new IllegalStateException("[VALIDATION_FAILED] Expected 71 BASE variants, found: " + context.productVariants.size());
        }
        long uniqueVariantIds = context.productVariants.values().stream().map(SanPhamChiTiet::getId).distinct().count();
        if (uniqueVariantIds != 71) {
            throw new IllegalStateException("[VALIDATION_FAILED] Duplicate BASE variant IDs.");
        }
        for (SanPham sp : context.products.values()) {
            List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
            List<SanPhamChiTiet> bvs = new ArrayList<>();
            for (SanPhamChiTiet v : variants) {
                List<SanPhamChiTietThuocTinh> atts = sanPhamChiTietThuocTinhRepository.findBySanPhamChiTiet_Id(v.getId());
                if (atts == null || atts.isEmpty()) bvs.add(v);
            }
            if (bvs.size() != 1) {
                throw new IllegalStateException("[VALIDATION_FAILED] Product ID " + sp.getId() + " has " + bvs.size() + " BASE variants.");
            }
        }
        List<HinhAnhSanPham> allDbImages = hinhAnhSanPhamDAO.findAll();
        Map<String, List<HinhAnhSanPham>> urlToDbImages = new LinkedHashMap<>();
        for (HinhAnhSanPham img : allDbImages) {
            if (img.getUrlHinhAnh() != null) {
                String key = normalizeImageUrlKey(img.getUrlHinhAnh());
                if (key != null) urlToDbImages.computeIfAbsent(key, k -> new ArrayList<>()).add(img);
            }
        }
        List<String> allExpectedUrlKeys = new ArrayList<>();
        for (ProductGroupSpec group : plan.productGroups.values()) {
            for (ImageFileSpec imgSpec : group.images) {
                String key = normalizeImageUrlKey(imgSpec.relPath);
                if (key == null) throw new IllegalStateException("[VALIDATION_FAILED] Null URL key.");
                if (allExpectedUrlKeys.contains(key)) throw new IllegalStateException("[VALIDATION_FAILED] Duplicate expected URL key: " + key);
                allExpectedUrlKeys.add(key);
            }
        }
        if (allExpectedUrlKeys.size() != 118) {
            throw new IllegalStateException("[VALIDATION_FAILED] Expected 118 distinct URL keys, found: " + allExpectedUrlKeys.size());
        }
        for (ProductGroupSpec group : plan.productGroups.values()) {
            SanPhamChiTiet spct = context.productVariants.get(group.stableGroupKey);
            if (spct == null) throw new IllegalStateException("[VALIDATION_FAILED] Missing variant for key: " + group.stableGroupKey);
            List<HinhAnhSanPham> varImages = allDbImages.stream()
                .filter(img -> img.getSanPhamChiTiet() != null && img.getSanPhamChiTiet().getId().equals(spct.getId()))
                .collect(Collectors.toList());
            long mainCount = varImages.stream().filter(img -> Boolean.TRUE.equals(img.getLaAnhChinh())).count();
            if (!varImages.isEmpty() && mainCount != 1) {
                throw new IllegalStateException("[VALIDATION_FAILED] BASE variant ID " + spct.getId() + " has " + mainCount + " main images.");
            }
            List<Integer> orders = new ArrayList<>();
            for (HinhAnhSanPham img : varImages) {
                if (img.getThuTu() == null || img.getThuTu() <= 0 || orders.contains(img.getThuTu())) {
                    throw new IllegalStateException("[VALIDATION_FAILED] Image ID " + img.getId() + " invalid/duplicate thuTu: " + img.getThuTu());
                }
                orders.add(img.getThuTu());
            }
            for (HinhAnhSanPham img : varImages) {
                String url = img.getUrlHinhAnh();
                if (url == null || url.contains(":") || url.startsWith("/")
                        || url.startsWith("uploads/") || url.startsWith("uploads\\") || url.contains("..")) {
                    throw new IllegalStateException("[VALIDATION_FAILED] Invalid URL format: " + url);
                }
                Path resolvedPath = Paths.get(productImageRoot, url).toAbsolutePath().normalize();
                Path normalizedRoot = Paths.get(productImageRoot).toAbsolutePath().normalize();
                if (!resolvedPath.startsWith(normalizedRoot)) {
                    throw new IllegalStateException("[VALIDATION_FAILED] Path traversal: " + url);
                }
                if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
                    throw new IllegalStateException("[VALIDATION_FAILED] Missing file: " + resolvedPath);
                }
            }
            for (ImageFileSpec expectedImg : group.images) {
                String urlKey = normalizeImageUrlKey(expectedImg.relPath);
                List<HinhAnhSanPham> matches = urlToDbImages.getOrDefault(urlKey, List.of());
                if (matches.size() != 1) {
                    throw new IllegalStateException("[VALIDATION_FAILED] URL '" + expectedImg.relPath + "' count: " + matches.size());
                }
                if (matches.get(0).getSanPhamChiTiet() == null || !matches.get(0).getSanPhamChiTiet().getId().equals(spct.getId())) {
                    throw new IllegalStateException("[VALIDATION_FAILED] URL '" + expectedImg.relPath + "' wrong variant.");
                }
            }
        }
        log.info("[COMMIT_VALIDATION] All post-flush validation constraints passed.");
    }

    private boolean verifyTargetCategoryAttributes() {
        List<DanhMucThuocTinh> allLinks = danhMucThuocTinhRepository.findAll();
        Map<String, List<String>> targets = getTargetCategoryAttributeMappings();
        for (Map.Entry<String, List<String>> entry : targets.entrySet()) {
            String catName = entry.getKey();
            for (String attrName : entry.getValue()) {
                long activeMatches = allLinks.stream().filter(l ->
                    l.getDanhMuc() != null
                    && normalizeKey(l.getDanhMuc().getTenDanhMuc()).equals(normalizeKey(catName))
                    && l.getThuocTinh() != null
                    && normalizeKey(l.getThuocTinh().getTenThuocTinh()).equals(normalizeKey(attrName))
                    && Boolean.TRUE.equals(l.getTrangThai())).count();

                long inactiveMatches = allLinks.stream().filter(l ->
                    l.getDanhMuc() != null
                    && normalizeKey(l.getDanhMuc().getTenDanhMuc()).equals(normalizeKey(catName))
                    && l.getThuocTinh() != null
                    && normalizeKey(l.getThuocTinh().getTenThuocTinh()).equals(normalizeKey(attrName))
                    && !Boolean.TRUE.equals(l.getTrangThai())).count();

                if (activeMatches != 1 || inactiveMatches != 0) {
                    log.error("[VALIDATION_FAILED] Cat-attr pair [{} x {}] count mismatch: active={}, inactive={}",
                        catName, attrName, activeMatches, inactiveMatches);
                    return false;
                }
            }
        }
        return true;
    }

    // =========================================================================
    //  REPORT
    // =========================================================================

    private void logCommitReport(SeedContext context, SeedPlan plan) {
        log.info("=================================================");
        log.info("            COMMIT EXECUTION REPORT              ");
        log.info("=================================================");
        log.info("ACCOUNTS               : INSERTED={} REUSED={}", context.accountsInserted, context.accountsReused);
        log.info("PROFILES               : INSERTED={} REUSED={}", context.profilesInserted, context.profilesReused);
        log.info("CATEGORIES             : INSERTED={} REUSED={}", context.categoriesInserted, context.categoriesReused);
        log.info("BRANDS                 : INSERTED={} REUSED={}", context.brandsInserted, context.brandsReused);
        log.info("ATTRIBUTES             : INSERTED={} REUSED={}", context.attributesInserted, context.attributesReused);
        log.info("CAT-ATTR LINKS         : INSERTED={} REUSED={}", context.linksInserted, context.linksReused);
        log.info("PRODUCTS               : INSERTED={} REUSED={}", context.productsInserted, context.productsReused);
        log.info("BASE VARIANTS          : INSERTED={} REUSED={}", context.variantsInserted, context.variantsReused);
        log.info("IMAGES                 : INSERTED={} REUSED={}", context.imagesInserted, context.imagesReused);
        log.info("=================================================");
    }

    private void logDryRunReport(SeedPlan plan) {
        boolean hasConflicts =
            plan.accountConflicts > 0 || plan.profileConflicts > 0
            || plan.masterDataConflicts > 0 || plan.catAttrConflicts > 0
            || plan.productConflicts > 0 || plan.variantConflicts > 0
            || plan.imageConflicts > 0 || plan.groupKeyConflicts > 0
            || plan.scanErrors > 0
            || plan.totalFilesFound != EXPECTED_TOTAL_FILES
            || plan.placeholdersSkipped != EXPECTED_PLACEHOLDERS
            || plan.validFileImages != EXPECTED_VALID_IMAGES
            || plan.rootUnmappedSkipped != EXPECTED_ROOT_UNMAPPED
            || plan.seedableImages.size() != EXPECTED_SEEDABLE_IMAGES
            || plan.productGroups.size() != EXPECTED_PRODUCT_GROUPS
            || plan.unsupportedExtensionSkipped != EXPECTED_UNSUPPORTED
            || plan.invalidHeaderSkipped != EXPECTED_INVALID_HEADER;

        String status = hasConflicts ? "DRY_RUN_BLOCKED" : "DRY_RUN_READY";

        log.info("");
        log.info("=================================================");
        log.info("            DRY-RUN EXECUTION REPORT             ");
        log.info("=================================================");
        log.info("SEED MODE              : DRY_RUN");
        log.info("DRY-RUN STATUS         : {}", status);
        log.info("DATABASE WRITES        : 0");
        log.info("IMAGE ROOT             : {}", productImageRoot);
        log.info("");
        log.info("--- FILESYSTEM ---");
        log.info("FILES DISCOVERED       : {} (expected {})", plan.totalFilesFound, EXPECTED_TOTAL_FILES);
        log.info("PLACEHOLDERS           : {} (expected {})", plan.placeholdersSkipped, EXPECTED_PLACEHOLDERS);
        log.info("VALID NON-PLACEHOLDER  : {} (expected {})", plan.validFileImages, EXPECTED_VALID_IMAGES);
        log.info("ROOT UNMAPPED          : {} (expected {})", plan.rootUnmappedSkipped, EXPECTED_ROOT_UNMAPPED);
        log.info("UNSUPPORTED EXT        : {} (expected {})", plan.unsupportedExtensionSkipped, EXPECTED_UNSUPPORTED);
        log.info("INVALID HEADER         : {} (expected {})", plan.invalidHeaderSkipped, EXPECTED_INVALID_HEADER);
        log.info("SEEDABLE IMAGES        : {} (expected {})", plan.seedableImages.size(), EXPECTED_SEEDABLE_IMAGES);
        log.info("PRODUCT GROUPS         : {} (expected {})", plan.productGroups.size(), EXPECTED_PRODUCT_GROUPS);
        log.info("SCAN ERRORS            : {}", plan.scanErrors);
        log.info("");
        log.info("--- ACCOUNTS ---");
        log.info("ACCOUNTS WOULD INSERT  : {}", plan.accountsWouldInsert);
        log.info("ACCOUNTS EXISTING      : {}", plan.accountsExisting);
        log.info("ACCOUNT CONFLICTS      : {}", plan.accountConflicts);
        log.info("PROFILES WOULD INSERT  : {}", plan.profilesWouldInsert);
        log.info("PROFILES EXISTING      : {}", plan.profilesExisting);
        log.info("PROFILE CONFLICTS      : {}", plan.profileConflicts);
        log.info("");
        log.info("--- MASTER DATA (Cat=7, Brand=9, Attr=9) ---");
        log.info("MASTER WOULD INSERT    : {}", plan.masterWouldInsert);
        log.info("MASTER EXISTING        : {}", plan.masterExisting);
        log.info("MASTER CONFLICTS       : {}", plan.masterDataConflicts);
        log.info("CAT-ATTR WOULD INSERT  : {}", plan.catAttrWouldInsert);
        log.info("CAT-ATTR EXISTING      : {}", plan.catAttrExisting);
        log.info("CAT-ATTR CONFLICTS     : {}", plan.catAttrConflicts);
        log.info("");
        log.info("--- PRODUCTS ---");
        log.info("PRODUCTS WOULD INSERT  : {}", plan.productsWouldInsert);
        log.info("PRODUCTS EXISTING      : {}", plan.productsExisting);
        log.info("PRODUCT CONFLICTS      : {}", plan.productConflicts);
        log.info("");
        log.info("--- BASE VARIANTS ---");
        log.info("VARIANTS WOULD INSERT  : {}", plan.variantsWouldInsert);
        log.info("VARIANTS EXISTING      : {}", plan.variantsExisting);
        log.info("VARIANT CONFLICTS      : {}", plan.variantConflicts);
        log.info("");
        log.info("--- IMAGES ---");
        log.info("IMAGES WOULD INSERT    : {}", plan.imagesWouldInsert);
        log.info("IMAGES EXISTING        : {}", plan.imagesExisting);
        log.info("IMAGE CONFLICTS        : {}", plan.imageConflicts);
        log.info("GROUP KEY CONFLICTS    : {}", plan.groupKeyConflicts);
        log.info("=================================================");
        if (!hasConflicts) {
            log.info("DRY-RUN completed cleanly. No database modifications made.");
        } else {
            log.warn("DRY-RUN BLOCKED due to conflicts/errors detected.");
        }
    }

    // =========================================================================
    //  HELPER: PRICE
    // =========================================================================

    private BigDecimal calculateDemoPrice(String categoryName, int hash) {
        long minPrice, maxPrice;
        switch (categoryName.toLowerCase(Locale.ROOT)) {
            case "v\u1ee3t c\u1ea7u l\u00f4ng": minPrice = 700000; maxPrice = 5500000; break;
            case "gi\u00e0y c\u1ea7u l\u00f4ng": minPrice = 600000; maxPrice = 3000000; break;
            case "trang ph\u1ee5c":   minPrice = 180000; maxPrice = 900000; break;
            case "balo":
            case "t\u00fai c\u1ea7u l\u00f4ng": minPrice = 300000; maxPrice = 1800000; break;
            case "c\u01b0\u1edbc c\u1ea7u l\u00f4ng": minPrice = 90000; maxPrice = 350000; break;
            default: minPrice = 80000; maxPrice = 250000; break;
        }
        long span = maxPrice - minPrice;
        long price = minPrice + Math.floorMod(hash, span);
        long rounded = Math.round(price / 10000.0) * 10000;
        return new BigDecimal(rounded);
    }

    // =========================================================================
    //  HELPER: IMAGE GROUPING
    // =========================================================================

    private Map<String, ProductGroupSpec> groupImageFiles(List<ImageFileSpec> images, SeedPlan plan) {
        Map<String, ProductGroupSpec> groups = new LinkedHashMap<>();
        for (ImageFileSpec img : images) {
            String[] parts = img.relPath.split("/");
            String topDir = parts[0];
            String categoryName = mapCategoryName(topDir);
            String brandName = resolveBrand(topDir, img.relPath);
            String productName;
            if (topDir.equalsIgnoreCase("Yonex") || topDir.equalsIgnoreCase("Li-ning") || topDir.equalsIgnoreCase("Li-Ning")) {
                String baseName = stripExtension(img.fileName);
                if (baseName.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_(.*)$")) {
                    baseName = baseName.replaceFirst("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_", "");
                }
                productName = baseName.replaceAll("[\\s_][-_\\(]?[1-9][\\)]?$", "").trim();
            } else {
                if (parts.length > 2) {
                    productName = parts[1].trim();
                } else {
                    String baseName = stripExtension(img.fileName);
                    if (baseName.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_(.*)$")) {
                        baseName = baseName.replaceFirst("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_", "");
                    }
                    productName = baseName.trim();
                }
            }
            String stableKey = buildStableGroupKey(topDir, productName, brandName, categoryName);
            ProductGroupSpec group = groups.computeIfAbsent(stableKey,
                k -> new ProductGroupSpec(stableKey, topDir, productName, categoryName, brandName));
            group.images.add(img);
        }
        for (ProductGroupSpec group : groups.values()) sortImageGroupFiles(group.images);
        return groups;
    }

    private void sortImageGroupFiles(List<ImageFileSpec> images) {
        images.sort((img1, img2) -> {
            int p1 = getImagePriority(img1.fileName);
            int p2 = getImagePriority(img2.fileName);
            if (p1 != p2) return Integer.compare(p1, p2);
            return naturalCompare(img1.relPath, img2.relPath);
        });
    }

    private int getImagePriority(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.contains("main")) return 1;
        if (lower.contains("cover")) return 2;
        if (lower.contains("front")) return 3;
        String baseName = stripExtension(fileName).toLowerCase(Locale.ROOT);
        if (baseName.equals("1") || (baseName.endsWith("1")
                && (baseName.length() == 1 || !Character.isDigit(baseName.charAt(baseName.length() - 2))))) {
            return 4;
        }
        return 5;
    }

    public static int naturalCompare(String s1, String s2) {
        int i1 = 0, i2 = 0;
        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1), c2 = s2.charAt(i2);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                StringBuilder num1 = new StringBuilder(), num2 = new StringBuilder();
                while (i1 < s1.length() && Character.isDigit(s1.charAt(i1))) num1.append(s1.charAt(i1++));
                while (i2 < s2.length() && Character.isDigit(s2.charAt(i2))) num2.append(s2.charAt(i2++));
                int cmp = new BigDecimal(num1.toString()).compareTo(new BigDecimal(num2.toString()));
                if (cmp != 0) return cmp;
            } else {
                int cmp = Character.compare(Character.toLowerCase(c1), Character.toLowerCase(c2));
                if (cmp != 0) return cmp;
                i1++; i2++;
            }
        }
        return Integer.compare(s1.length(), s2.length());
    }

    private String buildStableGroupKey(String topDir, String productName, String brandName, String categoryName) {
        return topDir.trim().toLowerCase(Locale.ROOT) + "::"
            + normalizeKey(brandName) + "::"
            + categoryName.trim().toLowerCase(Locale.ROOT) + "::"
            + productName.trim().toLowerCase(Locale.ROOT);
    }

    // =========================================================================
    //  HELPER: NORMALIZATION
    // =========================================================================

    private static String normalizeKey(String name) {
        if (name == null) return "";
        String nfc = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFC);
        return nfc.trim().toLowerCase(Locale.ROOT).replace("-", "").replace(" ", "");
    }

    /**
     * Normalize an image URL key for consistent matching across all URL lookups.
     * backslash to slash, trim, lowercase ROOT.
     */
    private static String normalizeImageUrlKey(String url) {
        if (url == null) return null;
        String nfc = java.text.Normalizer.normalize(url, java.text.Normalizer.Form.NFC);
        return nfc.replace('\\', '/').trim().toLowerCase(Locale.ROOT);
    }

    // =========================================================================
    //  HELPER: CATEGORY / BRAND MAPPING
    // =========================================================================

    private String mapCategoryName(String topDir) {
        String norm = normalizeKey(topDir);
        if (norm.equals("yonex") || norm.equals("lining")) return "V\u1ee3t C\u1ea7u L\u00f4ng";
        if (norm.equals("\u00e1o") || norm.equals("qu\u1ea7n") || norm.equals("ao") || norm.equals("quan")) return "Trang Ph\u1ee5c";
        if (norm.equals("gi\u00e0y") || norm.equals("giay")) return "Gi\u00e0y C\u1ea7u L\u00f4ng";
        if (norm.equals("balo")) return "Balo";
        if (norm.equals("t\u00faix\u00e1ch") || norm.equals("tuixach")) return "T\u00fai C\u1ea7u L\u00f4ng";
        if (norm.equals("c\u01b0\u1edbc") || norm.equals("cuoc")) return "C\u01b0\u1edbc C\u1ea7u L\u00f4ng";
        if (norm.equals("qu\u1ea5nc\u00e1n") || norm.equals("quancan")) return "Ph\u1ee5 Ki\u1ec7n C\u1ea7u L\u00f4ng";
        return null;
    }

    private String resolveBrand(String topDir, String relPath) {
        if (topDir.equalsIgnoreCase("Yonex")) return "Yonex";
        if (topDir.equalsIgnoreCase("Li-ning") || topDir.equalsIgnoreCase("Li-Ning")) return "Li-Ning";
        String lowerPath = relPath.toLowerCase(Locale.ROOT);
        if (lowerPath.contains("lining") || lowerPath.contains("li-ning") || lowerPath.contains("li ning")) return "Li-Ning";
        boolean hasVsToken = java.util.regex.Pattern
            .compile("(?iu)(?<![\\p{L}\\p{N}])vs(?![\\p{L}\\p{N}])")
            .matcher(relPath)
            .find();
        if (hasVsToken) {
            return "VS";
        }
        for (String brand : KNOWN_BRANDS) {
            String normBrand = normalizeKey(brand);
            if (normBrand.equals("vs") || normBrand.equals("lining")) continue;
            if (lowerPath.replace("-", "").replace(" ", "").contains(normBrand)) return brand;
        }
        return "Kh\u00e1c";
    }

    private Map<String, List<String>> getTargetCategoryAttributeMappings() {
        Map<String, List<String>> mappings = new LinkedHashMap<>();
        mappings.put("V\u1ee3t C\u1ea7u L\u00f4ng",      Arrays.asList("M\u00e0u s\u1eafc", "Tr\u1ecdng l\u01b0\u1ee3ng", "S\u1ee9c c\u0103ng"));
        mappings.put("Trang Ph\u1ee5c",          Arrays.asList("M\u00e0u s\u1eafc", "Size", "Ch\u1ea5t li\u1ec7u", "Ki\u1ec3u d\u00e1ng"));
        mappings.put("Gi\u00e0y C\u1ea7u L\u00f4ng",     Arrays.asList("M\u00e0u s\u1eafc", "Size", "Ch\u1ea5t li\u1ec7u"));
        mappings.put("Balo",               Arrays.asList("M\u00e0u s\u1eafc", "Ch\u1ea5t li\u1ec7u", "Ki\u1ec3u d\u00e1ng", "K\u00edch th\u01b0\u1edbc"));
        mappings.put("T\u00fai C\u1ea7u L\u00f4ng",      Arrays.asList("M\u00e0u s\u1eafc", "Ch\u1ea5t li\u1ec7u", "Ki\u1ec3u d\u00e1ng", "K\u00edch th\u01b0\u1edbc"));
        mappings.put("C\u01b0\u1edbc C\u1ea7u L\u00f4ng", Arrays.asList("M\u00e0u s\u1eafc", "Ch\u1ea5t li\u1ec7u", "\u0110\u01b0\u1eddng k\u00ednh", "Chi\u1ec1u d\u00e0i", "S\u1ee9c c\u0103ng"));
        mappings.put("Ph\u1ee5 Ki\u1ec7n C\u1ea7u L\u00f4ng", Arrays.asList("M\u00e0u s\u1eafc", "Ch\u1ea5t li\u1ec7u", "Ki\u1ec3u d\u00e1ng"));
        return mappings;
    }

    // =========================================================================
    //  HELPER: IMAGE HEADER / PLACEHOLDER
    // =========================================================================

    private boolean isValidImageHeader(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] bytes = is.readNBytes(12);
            if (bytes.length < 4) return false;
            if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50
                    && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) return true;
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return true;
            if (bytes.length >= 12
                    && bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49
                    && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46
                    && bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45
                    && bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) return true;
        } catch (Exception e) {
            log.debug("Error reading image header: {}", path, e);
        }
        return false;
    }

    /**
     * Detects the 18 known empty placeholder PNGs.
     *
     * Binary evidence (audit 2026-08-03):
     *   Size=84 bytes, PNG 110x10 RGBA, chunks: IHDR[13] IDAT[27] IEND[0]
     *   All 18 share SHA-256: 01f477834dbdd8d5b38739af3b68c12c75f6e723f824349adde2c5d24a95ceb9
     *
     * Detection: size==84 AND valid PNG sig AND SHA-256 matches.
     */
    private boolean isKnownEmptyPngPlaceholder(Path path, long fileSize) {
        if (fileSize != KNOWN_PLACEHOLDER_SIZE) return false;
        try {
            byte[] allBytes = Files.readAllBytes(path);
            if (allBytes.length != KNOWN_PLACEHOLDER_SIZE) return false;
            if (allBytes[0] != (byte) 0x89 || allBytes[1] != (byte) 0x50
                    || allBytes[2] != (byte) 0x4E || allBytes[3] != (byte) 0x47) return false;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(allBytes);
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hashBytes) hexBuilder.append(String.format("%02x", b));
            return KNOWN_PLACEHOLDER_SHA256.equals(hexBuilder.toString());
        } catch (Exception e) {
            log.debug("Error inspecting placeholder: {}", path, e);
        }
        return false;
    }

    // =========================================================================
    //  HELPER: STRING
    // =========================================================================

    private String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx) : "";
    }

    // =========================================================================
    //  INNER CLASSES
    // =========================================================================

    private static class SeedPlan {
        int totalFilesFound           = 0;
        int placeholdersSkipped       = 0;
        int validFileImages           = 0;
        int rootUnmappedSkipped       = 0;
        int unsupportedExtensionSkipped = 0;
        int invalidHeaderSkipped      = 0;
        int scanErrors                = 0;
        int accountsWouldInsert       = 0;
        int accountsExisting          = 0;
        int accountConflicts          = 0;
        int profilesWouldInsert       = 0;
        int profilesExisting          = 0;
        int profileConflicts          = 0;
        int masterWouldInsert         = 0;
        int masterExisting            = 0;
        int masterDataConflicts       = 0;
        int catAttrWouldInsert        = 0;
        int catAttrExisting           = 0;
        int catAttrConflicts          = 0;
        int productsWouldInsert       = 0;
        int productsExisting          = 0;
        int productConflicts          = 0;
        int variantsWouldInsert       = 0;
        int variantsExisting          = 0;
        int variantConflicts          = 0;
        int imagesWouldInsert         = 0;
        int imagesExisting            = 0;
        int imageConflicts            = 0;
        int groupKeyConflicts         = 0;

        List<TestAccountSpec> accountSpecs = new ArrayList<>();
        List<ImageFileSpec> seedableImages = new ArrayList<>();
        Map<String, ProductGroupSpec> productGroups = new LinkedHashMap<>();
    }

    private static class SeedContext {
        int accountsInserted  = 0;
        int accountsReused    = 0;
        int profilesInserted  = 0;
        int profilesReused    = 0;
        int categoriesInserted = 0;
        int categoriesReused  = 0;
        int brandsInserted    = 0;
        int brandsReused      = 0;
        int attributesInserted = 0;
        int attributesReused  = 0;
        int linksInserted     = 0;
        int linksReused       = 0;
        int productsInserted  = 0;
        int productsReused    = 0;
        int variantsInserted  = 0;
        int variantsReused    = 0;
        int imagesInserted    = 0;
        int imagesReused      = 0;

        Map<String, TaiKhoan> accounts = new LinkedHashMap<>();
        NhanVien adminNhanVien;
        Map<String, DanhMuc> categories = new LinkedHashMap<>();
        Map<String, ThuongHieu> brands = new LinkedHashMap<>();
        Map<String, ThuocTinh> attributes = new LinkedHashMap<>();
        Map<String, SanPham> products = new LinkedHashMap<>();
        Map<String, SanPhamChiTiet> productVariants = new LinkedHashMap<>();
    }

    private static class TestAccountSpec {
        String username;
        String role;
        TestAccountSpec(String username, String role) {
            this.username = username;
            this.role = role;
        }
    }

    private List<TestAccountSpec> getTestAccountSpecs() {
        List<TestAccountSpec> list = new ArrayList<>();
        list.add(new TestAccountSpec("admin", "QL"));
        list.add(new TestAccountSpec("nhanvien01", "NV"));
        for (int i = 1; i <= 12; i++) {
            list.add(new TestAccountSpec(String.format("khachhang%02d", i), "KH"));
        }
        return list;
    }

    private static class ImageFileSpec {
        String relPath;
        Path fullPath;
        long size;
        String fileName;
        ImageFileSpec(String relPath, Path fullPath, long size, String fileName) {
            this.relPath = relPath;
            this.fullPath = fullPath;
            this.size = size;
            this.fileName = fileName;
        }
    }

    private static class ProductGroupSpec {
        String stableGroupKey;
        String topCategoryDir;
        String productName;
        String categoryName;
        String brandName;
        List<ImageFileSpec> images = new ArrayList<>();
        ProductGroupSpec(String stableGroupKey, String topCategoryDir, String productName, String categoryName, String brandName) {
            this.stableGroupKey = stableGroupKey;
            this.topCategoryDir = topCategoryDir;
            this.productName = productName;
            this.categoryName = categoryName;
            this.brandName = brandName;
        }
    }
}
