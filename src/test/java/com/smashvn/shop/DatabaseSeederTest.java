package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Commit;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.service.product.DanhGiaService;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
public class DatabaseSeederTest {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private GhnConfig ghnConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DanhGiaService danhGiaService;

    static class RacketSeed {
        String fileName;
        String brandName;
        String productName;
        String color;
        BigDecimal price;
        String customWeight;

        RacketSeed(String fileName, String brandName, String productName, String color, double price) {
            this(fileName, brandName, productName, color, price, null);
        }

        RacketSeed(String fileName, String brandName, String productName, String color, double price, String customWeight) {
            this.fileName = fileName;
            this.brandName = brandName;
            this.productName = productName;
            this.color = color;
            this.price = BigDecimal.valueOf(price);
            this.customWeight = customWeight;
        }
    }

    private ThuongHieu findOrCreateBrand(String brandName) {
        return thuongHieuRepository.findAll().stream()
                .filter(b -> b.getTenThuongHieu().equalsIgnoreCase(brandName))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu(brandName);
                    th.setMoTa("Thương hiệu " + brandName + " chính hãng");
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });
    }

    private DanhMuc findOrCreateCategory(String categoryName, String description) {
        return danhMucRepository.findAll().stream()
                .filter(c -> c.getTenDanhMuc().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc dm = new DanhMuc();
                    dm.setTenDanhMuc(categoryName);
                    dm.setMoTa(description);
                    dm.setTrangThai(true);
                    return danhMucRepository.save(dm);
                });
    }

    private ThuongHieu detectBrand(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("yonex")) return findOrCreateBrand("Yonex");
        if (lower.contains("lining") || lower.contains("li-ning")) return findOrCreateBrand("Li-Ning");
        if (lower.contains("victor")) return findOrCreateBrand("Victor");
        if (lower.contains("gosen")) return findOrCreateBrand("GOSEN");
        if (lower.contains("kizuna")) return findOrCreateBrand("Kizuna");
        return findOrCreateBrand("Khác");
    }

    @Test
    @Transactional
    @Commit
    public void seedDatabase() {
        System.out.println("=== STARTING FULL DATABASE SEEDING FOR ALL CATEGORIES ===");

        // 1. Ensure Admin Account exists
        NhanVien employee = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("system_admin");
            tk.setMatKhau(passwordEncoder.encode("123456"));
            tk.setVaiTro("QL");
            tk.setTrangThai("hoat_dong");
            tk.setNgayTao(LocalDateTime.now());
            tk = taiKhoanRepository.save(tk);

            NhanVien nv = new NhanVien();
            nv.setTaiKhoan(tk);
            nv.setHoTenNv("Hệ thống Smash-VN");
            nv.setChucVu("Quản trị viên");
            nv.setSoDienThoaiNv("0987654321");
            nv.setNgayTao(LocalDateTime.now());
            return nhanVienRepository.save(nv);
        });

        // Ensure admin account with password 123456
        TaiKhoan adminAcc = taiKhoanRepository.findByUsername("admin");
        if (adminAcc == null) {
            adminAcc = new TaiKhoan();
            adminAcc.setUsername("admin");
            adminAcc.setMatKhau(passwordEncoder.encode("123456"));
            adminAcc.setVaiTro("QL");
            adminAcc.setTrangThai("hoat_dong");
            adminAcc.setNgayTao(LocalDateTime.now());
            adminAcc = taiKhoanRepository.save(adminAcc);

            NhanVien nvAdmin = new NhanVien();
            nvAdmin.setTaiKhoan(adminAcc);
            nvAdmin.setHoTenNv("Quản trị viên Admin");
            nvAdmin.setChucVu("Quản lý");
            nvAdmin.setSoDienThoaiNv("0912345678");
            nvAdmin.setNgayTao(LocalDateTime.now());
            nhanVienRepository.save(nvAdmin);
        } else {
            adminAcc.setMatKhau(passwordEncoder.encode("123456"));
            adminAcc.setVaiTro("QL");
            adminAcc.setTrangThai("hoat_dong");
            taiKhoanRepository.save(adminAcc);
        }

        // 2. Map of category folder names to formal category names
        Map<String, String> categoryFolderMap = new LinkedHashMap<>();
        categoryFolderMap.put("Áo", "Áo cầu lông");
        categoryFolderMap.put("Quần", "Quần cầu lông");
        categoryFolderMap.put("Balo", "Balo cầu lông");
        categoryFolderMap.put("Túi Xách", "Túi xách cầu lông");
        categoryFolderMap.put("Giày", "Giày cầu lông");
        categoryFolderMap.put("Cước", "Cước cầu lông");

        Map<String, BigDecimal> categoryPriceMap = new HashMap<>();
        categoryPriceMap.put("Áo", BigDecimal.valueOf(350000));
        categoryPriceMap.put("Quần", BigDecimal.valueOf(280000));
        categoryPriceMap.put("Balo", BigDecimal.valueOf(850000));
        categoryPriceMap.put("Túi Xách", BigDecimal.valueOf(950000));
        categoryPriceMap.put("Giày", BigDecimal.valueOf(1650000));
        categoryPriceMap.put("Cước", BigDecimal.valueOf(160000));

        File baseDir = new File("uploads/product");

        // 3. Process Subdirectory-based categories (Áo, Quần, Balo, Túi Xách, Giày, Cước)
        for (Map.Entry<String, String> entry : categoryFolderMap.entrySet()) {
            String folderName = entry.getKey();
            String formalCatName = entry.getValue();

            File catDir = new File(baseDir, folderName);
            if (!catDir.exists() || !catDir.isDirectory()) {
                System.out.println("Category directory not found: " + catDir.getAbsolutePath());
                continue;
            }

            DanhMuc category = findOrCreateCategory(formalCatName, "Sản phẩm " + formalCatName.toLowerCase() + " chính hãng");
            BigDecimal basePrice = categoryPriceMap.getOrDefault(folderName, BigDecimal.valueOf(500000));

            File[] productFolders = catDir.listFiles(File::isDirectory);
            if (productFolders == null) continue;

            for (File pFolder : productFolders) {
                String productName = pFolder.getName();
                ThuongHieu brand = detectBrand(productName);

                SanPham product = sanPhamRepository.findAll().stream()
                        .filter(sp -> sp.getTenSanPham().equalsIgnoreCase(productName))
                        .findFirst()
                        .orElseGet(() -> {
                            SanPham sp = new SanPham();
                            sp.setTenSanPham(productName);
                            sp.setMoTa(productName + " chính hãng phân phối tại Smash-VN. Thiết kế thời trang, chất liệu cao cấp thoáng khí, mang lại sự thoải mái tối đa.");
                            sp.setDanhMuc(category);
                            sp.setThuongHieu(brand);
                            sp.setNhanVien(employee);
                            sp.setTrangThai("dang_ban");
                            sp.setDiemTrungBinh(0.0);
                            sp.setSoDanhGia(0);
                            sp.setNgayTao(LocalDateTime.now());
                            sp.setNgayCapNhat(LocalDateTime.now());
                            return sanPhamRepository.save(sp);
                        });

                File[] imgFiles = pFolder.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
                });

                if (imgFiles != null && imgFiles.length > 0) {
                    List<String> sizeList = new ArrayList<>();
                    if (folderName.equals("Áo") || folderName.equals("Quần")) {
                        sizeList = List.of("M", "L");
                    } else if (folderName.equals("Giày")) {
                        sizeList = List.of("41", "42");
                    } else {
                        sizeList = List.of("Tiêu chuẩn");
                    }

                    for (String size : sizeList) {
                        String color = "Mặc định";
                        final String finalSize = size;

                        List<SanPhamChiTiet> existingList = sanPhamChiTietRepository.findAll().stream()
                                .filter(ct -> ct.getSanPham().getId().equals(product.getId())
                                        && color.equalsIgnoreCase(ct.getMauSac())
                                        && finalSize.equalsIgnoreCase(ct.getKichThuoc()))
                                .toList();

                        if (existingList.isEmpty()) {
                            SanPhamChiTiet spct = new SanPhamChiTiet();
                            spct.setSanPham(product);
                            spct.setMauSac(color);
                            spct.setKichThuoc(size);
                            spct.setTrongLuong("Tiêu chuẩn");
                            spct.setSucCang("N/A");
                            spct.setGiaBan(basePrice);
                            spct.setGiaNhap(basePrice.multiply(BigDecimal.valueOf(0.7)));
                            spct.setSoLuongTon(50);
                            spct.setTrangThai("dang_ban");
                            spct.setNgayTao(LocalDateTime.now());
                            spct.setNgayCapNhat(LocalDateTime.now());

                            List<HinhAnhSanPham> imageEntities = new ArrayList<>();
                            int imgIdx = 0;
                            for (File imgFile : imgFiles) {
                                String relPath = folderName + "/" + productName + "/" + imgFile.getName();
                                HinhAnhSanPham hasp = new HinhAnhSanPham();
                                hasp.setSanPhamChiTiet(spct);
                                hasp.setUrlHinhAnh(relPath);
                                hasp.setMauSac(color);
                                hasp.setLaAnhChinh(imgIdx == 0);
                                imageEntities.add(hasp);
                                imgIdx++;
                            }

                            spct.setHinhAnhSanPhams(imageEntities);
                            sanPhamChiTietRepository.save(spct);
                            System.out.println("Created variant for " + formalCatName + ": " + productName + " (" + size + ")");
                        }
                    }
                }
            }
        }

        // 4. Process Rackets in Li-ning and Yonex folders
        DanhMuc racketCat = findOrCreateCategory("Vợt cầu lông", "Các loại vợt cầu lông chính hãng");
        ThuongHieu yonexBrand = findOrCreateBrand("Yonex");
        ThuongHieu liningBrand = findOrCreateBrand("Li-Ning");

        List<RacketSeed> racketList = new ArrayList<>();
        // Li-Ning (16 images)
        racketList.add(new RacketSeed("Li-ning/AERONAUT 6000_Đỏ_xanh.png", "Li-Ning", "Li-Ning Aeronaut 6000", "Đỏ xanh", 2300000));
        racketList.add(new RacketSeed("Li-ning/AXFORCE 9 AYPT317-2_Đen.png", "Li-Ning", "Li-Ning Axforce 9 AYPT317-2", "Đen", 1850000));
        racketList.add(new RacketSeed("Li-ning/AXFORCE BIGBANG_Đen.png", "Li-Ning", "Li-Ning Axforce Bigbang Đen", "Đen", 1950000));
        racketList.add(new RacketSeed("Li-ning/Axforce Bigbang_Trắng.png", "Li-Ning", "Li-Ning Axforce Bigbang Trắng", "Trắng", 1950000));
        racketList.add(new RacketSeed("Li-ning/AXFORCE JR (5U) Trắng P-AYPT301-5.png", "Li-Ning", "Li-Ning Axforce JR Trắng", "Trắng", 1100000, "5U"));
        racketList.add(new RacketSeed("Li-ning/AXFORCE JR (5U) Đen P-AYPT299-5.png", "Li-Ning", "Li-Ning Axforce JR Đen", "Đen", 1100000, "5U"));
        racketList.add(new RacketSeed("Li-ning/Axforce 10_Xanh dương.png", "Li-Ning", "Li-Ning Axforce 10", "Xanh dương", 1250000));
        racketList.add(new RacketSeed("Li-ning/Axforce 30_Màu đen.png", "Li-Ning", "Li-Ning Axforce 30", "Đen", 1600000));
        racketList.add(new RacketSeed("Li-ning/Axforce 90 Tiger Max_Đỏ.png", "Li-Ning", "Li-Ning Axforce 90 Tiger Max", "Đỏ", 4200000));
        racketList.add(new RacketSeed("Li-ning/Axforce Thunder Cannon_Xanh Đậm.png", "Li-Ning", "Li-Ning Axforce Thunder Cannon", "Xanh Đậm", 2100000));
        racketList.add(new RacketSeed("Li-ning/CALIBAR 300C_Xam_Xanh.png", "Li-Ning", "Li-Ning Calibar 300C", "Xám Xanh", 1750000));
        racketList.add(new RacketSeed("Li-ning/CALIBAR 600I_Xám_Đen.png", "Li-Ning", "Li-Ning Calibar 600I", "Xám Đen", 2400000));
        racketList.add(new RacketSeed("Li-ning/CALIBAR 900_Vàng_Đen.png", "Li-Ning", "Li-Ning Calibar 900", "Vàng Đen", 3800000));
        racketList.add(new RacketSeed("Li-ning/Hỏa P-AYPT063-4_Đỏ.png", "Li-Ning", "Li-Ning Hỏa P-AYPT063-4", "Đỏ", 2600000));
        racketList.add(new RacketSeed("Li-ning/Phong P-AYPT059-4_Xanh.png", "Li-Ning", "Li-Ning Phong P-AYPT059-4", "Xanh", 2600000));
        racketList.add(new RacketSeed("Li-ning/WindStorm 72S Neon_Xanh nước biển nhạt.png", "Li-Ning", "Li-Ning Windstorm 72S Neon", "Xanh nước biển nhạt", 2150000));

        // Yonex (24 images)
        racketList.add(new RacketSeed("Yonex/ ARCSABER11_ Pro_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Pro", "Xám ngọc trai Đỏ", 4100000));
        racketList.add(new RacketSeed("Yonex/ARCSABER11_Play_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Play", "Xám ngọc trai Đỏ", 1350000));
        racketList.add(new RacketSeed("Yonex/ARCSABER11_Tour_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Tour", "Xám ngọc trai Đỏ", 2650000));
        racketList.add(new RacketSeed("Yonex/ARCSABER7_Pro_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Pro", "Xám Chuối", 3900000));
        racketList.add(new RacketSeed("Yonex/ARCSABER7_Tour_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Tour", "Xám Chuối", 2550000));
        racketList.add(new RacketSeed("Yonex/ARCSABER_7_PLAY_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Play", "Xám Chuối", 1300000));
        racketList.add(new RacketSeed("Yonex/ASTROX NEXTAGE_Đen_Xanh lá cây1.png", "Yonex", "Yonex Astrox Nextage", "Đen Xanh lá cây", 2700000));
        racketList.add(new RacketSeed("Yonex/ASTROX01f_lime_xanh chuối.png", "Yonex", "Yonex Astrox 01F", "Xanh chuối", 1150000));
        racketList.add(new RacketSeed("Yonex/ASTROX77-Pro_Cam Đậm.png", "Yonex", "Yonex Astrox 77 Pro", "Cam Đậm", 4050000));
        racketList.add(new RacketSeed("Yonex/ASTROX88D_Pro_Bạc đen.png", "Yonex", "Yonex Astrox 88D Pro", "Bạc đen", 4350000));
        racketList.add(new RacketSeed("Yonex/ASTROX88D_Tour_Bạc đen.png", "Yonex", "Yonex Astrox 88D Tour", "Bạc đen", 2750000));
        racketList.add(new RacketSeed("Yonex/ASTROX88S-Tour_Bạc đen.png", "Yonex", "Yonex Astrox 88S Tour", "Bạc đen", 2750000));
        racketList.add(new RacketSeed("Yonex/ASTROX99-Gen3-Pro_Đen_xanh lá cây(1).png", "Yonex", "Yonex Astrox 99 Gen 3 Pro", "Đen Xanh lá cây", 4500000));
        racketList.add(new RacketSeed("Yonex/ASTROX99-Gen3-Pro_Đen_xanh lá cây.png", "Yonex", "Yonex Astrox 99 Gen 3 Pro Special", "Đen Xanh lá cây", 4600000));
        racketList.add(new RacketSeed("Yonex/ASTROX_100_Play_Đỏ thẫm.png", "Yonex", "Yonex Astrox 100 Play", "Đỏ thẫm", 1400000));
        racketList.add(new RacketSeed("Yonex/Astrox_100_Tour_Đỏ thẫm.png", "Yonex", "Yonex Astrox 100 Tour", "Đỏ thẫm", 2800000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE NEXTAGE_Xám đậm.png", "Yonex", "Yonex Nanoflare Nextage", "Xám đậm", 2750000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE-800_Play_Xanh lá cây đậm.png", "Yonex", "Yonex Nanoflare 800 Play", "Xanh lá cây đậm", 1350000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE-800_Pro_Xanh lá cây đậm.png", "Yonex", "Yonex Nanoflare 800 Pro", "Xanh lá cây đậm", 4200000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_1000_z_Phay_Vàng_đen.png", "Yonex", "Yonex Nanoflare 1000 Z Play", "Vàng đen", 1450000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700G_Games_tím đêm.png", "Yonex", "Yonex Nanoflare 700G Games", "Tím đêm", 2200000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700_Play_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Play", "Tím đêm", 1350000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700_Pro_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Pro", "Tím đêm", 4250000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700_Tour_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Tour", "Tím đêm", 2700000));

        for (RacketSeed seed : racketList) {
            ThuongHieu brand = "Yonex".equalsIgnoreCase(seed.brandName) ? yonexBrand : liningBrand;

            SanPham product = sanPhamRepository.findAll().stream()
                    .filter(sp -> sp.getTenSanPham().equalsIgnoreCase(seed.productName))
                    .findFirst()
                    .orElseGet(() -> {
                        SanPham sp = new SanPham();
                        sp.setTenSanPham(seed.productName);
                        sp.setMoTa("Vợt cầu lông " + seed.productName + " chính hãng phân phối tại Smash-VN. Thiết kế hiện đại, công nghệ tiên tiến.");
                        sp.setDanhMuc(racketCat);
                        sp.setThuongHieu(brand);
                        sp.setNhanVien(employee);
                        sp.setTrangThai("dang_ban");
                        sp.setDiemTrungBinh(0.0);
                        sp.setSoDanhGia(0);
                        sp.setNgayTao(LocalDateTime.now());
                        sp.setNgayCapNhat(LocalDateTime.now());
                        return sanPhamRepository.save(sp);
                    });

            List<String> weights = (seed.customWeight != null) ? List.of(seed.customWeight) : List.of("3U", "4U");
            for (String weight : weights) {
                final String finalWeight = weight;
                boolean exists = (product.getSanPhamChiTiets() != null) && product.getSanPhamChiTiets().stream()
                        .anyMatch(ct -> seed.color.equalsIgnoreCase(ct.getMauSac()) && finalWeight.equalsIgnoreCase(ct.getTrongLuong()));

                if (!exists) {
                    SanPhamChiTiet spct = new SanPhamChiTiet();
                    spct.setSanPham(product);
                    spct.setMauSac(seed.color);
                    spct.setTrongLuong(weight);
                    spct.setMucCang("24-28 lbs");
                    spct.setGiaBan(seed.price);
                    spct.setSoLuongTon(50);
                    spct.setKichThuoc("G5");
                    spct.setGiaNhap(seed.price.multiply(BigDecimal.valueOf(0.7)));
                    spct.setTrangThai("dang_ban");
                    spct.setNgayTao(LocalDateTime.now());
                    spct.setNgayCapNhat(LocalDateTime.now());

                    HinhAnhSanPham hasp = new HinhAnhSanPham();
                    hasp.setSanPhamChiTiet(spct);
                    hasp.setUrlHinhAnh(seed.fileName);
                    hasp.setMauSac(seed.color);
                    hasp.setLaAnhChinh(true);

                    spct.setHinhAnhSanPhams(List.of(hasp));
                    sanPhamChiTietRepository.save(spct);
                    System.out.println("Created variant for Racket: " + product.getTenSanPham() + " | " + weight);
                }
            }
        }

        // 5. Recalculate ratings for all products based on actual customer reviews (no hardcoded ratings)
        List<SanPham> allProducts = sanPhamRepository.findAll();
        for (SanPham sp : allProducts) {
            danhGiaService.updateProductRatingStats(sp.getId());
        }
        System.out.println("Recalculated rating stats for all " + allProducts.size() + " products.");

        // 6. Ensure GHN shipping carrier exists
        donViVanChuyenDAO.findAll().stream()
                .filter(dv -> dv.getTenDonVi() != null && 
                        (dv.getTenDonVi().toUpperCase().contains("GIAO HÀNG NHANH") || 
                         dv.getTenDonVi().toUpperCase().contains("GHN")))
                .findFirst()
                .orElseGet(() -> {
                    DonViVanChuyen dv = new DonViVanChuyen();
                    dv.setMaDonVi("GHN");
                    dv.setTenDonVi("Giao Hàng Nhanh (GHN)");
                    dv.setHotline("1900 636677");
                    dv.setWebsite("https://ghn.vn");
                    dv.setPhiLocal(BigDecimal.valueOf(30000));
                    dv.setPhiNationwide(BigDecimal.valueOf(40000));
                    return donViVanChuyenDAO.save(dv);
                });

        System.out.println("=== FULL DATABASE SEEDING COMPLETED SUCCESSFULLY ===");
    }
}
