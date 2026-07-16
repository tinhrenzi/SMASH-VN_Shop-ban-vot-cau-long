package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Commit;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.config.GhnConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

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
    private com.smashvn.shop.service.api.GhnService ghnService;

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

    @Test
    @Transactional
    @Commit
    public void seedDatabase() {
        System.out.println("=== STARTING DATABASE SEEDING ===");

        // 1. Ensure Category "Vợt cầu lông" exists
        DanhMuc category = danhMucRepository.findAll().stream()
                .filter(c -> c.getTenDanhMuc().equalsIgnoreCase("Vợt cầu lông"))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc dm = new DanhMuc();
                    dm.setTenDanhMuc("Vợt cầu lông");
                    dm.setMoTa("Các loại vợt cầu lông chính hãng");
                    dm.setTrangThai(true);
                    return danhMucRepository.save(dm);
                });
        System.out.println("Category loaded: ID " + category.getId() + " - " + category.getTenDanhMuc());

        // 2. Ensure Brands "Yonex" and "Li-Ning" exist
        ThuongHieu yonex = thuongHieuRepository.findAll().stream()
                .filter(b -> b.getTenThuongHieu().equalsIgnoreCase("Yonex"))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu("Yonex");
                    th.setMoTa("Thương hiệu cầu lông Yonex Nhật Bản");
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });
        System.out.println("Brand loaded: ID " + yonex.getId() + " - " + yonex.getTenThuongHieu());

        ThuongHieu lining = thuongHieuRepository.findAll().stream()
                .filter(b -> b.getTenThuongHieu().equalsIgnoreCase("Li-Ning"))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu("Li-Ning");
                    th.setMoTa("Thương hiệu thể thao Li-Ning Trung Quốc");
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });
        System.out.println("Brand loaded: ID " + lining.getId() + " - " + lining.getTenThuongHieu());

        // 3. Ensure Staff (NhanVien) exists
        NhanVien employee = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            // Need a TaiKhoan first
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("system_admin");
            tk.setMatKhau("$2a$12$R9h/cIPz0gi.UR1gKdgpJeCVS7.p8s59W/H5.f3bC9mE3KzP9vD4O"); // BCrypt for "123456"
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
        System.out.println("Employee loaded: ID " + employee.getId() + " - " + employee.getHoTenNv());

        // 4. Set up the list of 40 racket images
        List<RacketSeed> racketList = new ArrayList<>();

        // Li-Ning (16 images)
        racketList.add(new RacketSeed("AERONAUT 6000_Đỏ_xanh.png", "Li-Ning", "Li-Ning Aeronaut 6000", "Đỏ xanh", 2300000));
        racketList.add(new RacketSeed("AXFORCE 9 AYPT317-2_Đen.png", "Li-Ning", "Li-Ning Axforce 9 AYPT317-2", "Đen", 1050000));
        racketList.add(new RacketSeed("AXFORCE BIGBANG_Đen.png", "Li-Ning", "Li-Ning Axforce Bigbang", "Đen", 1150000));
        racketList.add(new RacketSeed("AXFORCE JR (5U) Trắng P-AYPT301-5.png", "Li-Ning", "Li-Ning Axforce JR", "Trắng", 950000, "5U"));
        racketList.add(new RacketSeed("AXFORCE JR (5U) Đen P-AYPT299-5.png", "Li-Ning", "Li-Ning Axforce JR", "Đen", 950000, "5U"));
        racketList.add(new RacketSeed("Axforce 10_Xanh dương.png", "Li-Ning", "Li-Ning Axforce 10", "Xanh dương", 1000000));
        racketList.add(new RacketSeed("Axforce 30_Màu đen.png", "Li-Ning", "Li-Ning Axforce 30", "Đen", 1400000));
        racketList.add(new RacketSeed("Axforce 90 Tiger Max_Đỏ.png", "Li-Ning", "Li-Ning Axforce 90 Tiger Max", "Đỏ", 4100000));
        racketList.add(new RacketSeed("Axforce Bigbang_Trắng.png", "Li-Ning", "Li-Ning Axforce Bigbang", "Trắng", 1150000));
        racketList.add(new RacketSeed("Axforce Thunder Cannon_Xanh Đậm.png", "Li-Ning", "Li-Ning Axforce Thunder Cannon", "Xanh đậm", 1200000));
        racketList.add(new RacketSeed("CALIBAR 300C_Xam_Xanh.png", "Li-Ning", "Li-Ning Calibar 300C", "Xám Xanh", 1600000));
        racketList.add(new RacketSeed("CALIBAR 600I_Xám_Đen.png", "Li-Ning", "Li-Ning Calibar 600I", "Xám Đen", 2400000));
        racketList.add(new RacketSeed("CALIBAR 900_Vàng_Đen.png", "Li-Ning", "Li-Ning Calibar 900", "Vàng Đen", 3600000));
        racketList.add(new RacketSeed("Hỏa P-AYPT063-4_Đỏ.png", "Li-Ning", "Li-Ning Hỏa P-AYPT063-4", "Đỏ", 1500000));
        racketList.add(new RacketSeed("Phong P-AYPT059-4_Xanh.png", "Li-Ning", "Li-Ning Phong P-AYPT059-4", "Xanh", 1500000));
        racketList.add(new RacketSeed("WindStorm 72S Neon_Xanh nước biển nhạt.png", "Li-Ning", "Li-Ning Windstorm 72S Neon", "Xanh nước biển nhạt", 1800000));

        // Yonex (24 images)
        racketList.add(new RacketSeed(" ARCSABER11_ Pro_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Pro", "Xám ngọc trai Đỏ", 4000000));
        racketList.add(new RacketSeed("ARCSABER11_Play_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Play", "Xám ngọc trai Đỏ", 1300000));
        racketList.add(new RacketSeed("ARCSABER11_Tour_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Tour", "Xám ngọc trai Đỏ", 2700000));
        racketList.add(new RacketSeed("ARCSABER7_Pro_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Pro", "Xám Chuối", 3850000));
        racketList.add(new RacketSeed("ARCSABER7_Tour_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Tour", "Xám Chuối", 2600000));
        racketList.add(new RacketSeed("ARCSABER_7_PLAY_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Play", "Xám Chuối", 1250000));
        racketList.add(new RacketSeed("ASTROX NEXTAGE_Đen_Xanh lá cây1.png", "Yonex", "Yonex Astrox Nextage", "Đen Xanh lá cây", 2900000));
        racketList.add(new RacketSeed("ASTROX01f_lime_xanh chuối.png", "Yonex", "Yonex Astrox 01f", "Lime xanh chuối", 1100000));
        racketList.add(new RacketSeed("ASTROX77-Pro_Cam Đậm.png", "Yonex", "Yonex Astrox 77 Pro", "Cam Đậm", 3900000));
        racketList.add(new RacketSeed("ASTROX88D_Pro_Bạc đen.png", "Yonex", "Yonex Astrox 88D Pro", "Bạc đen", 4100000));
        racketList.add(new RacketSeed("ASTROX88D_Tour_Bạc đen.png", "Yonex", "Yonex Astrox 88D Tour", "Bạc đen", 2800000));
        racketList.add(new RacketSeed("ASTROX88S-Tour_Bạc đen.png", "Yonex", "Yonex Astrox 88S Tour", "Bạc đen", 2800000));
        racketList.add(new RacketSeed("ASTROX99-Gen3-Pro_Đen_xanh lá cây(1).png", "Yonex", "Yonex Astrox 99 Gen 3 Pro", "Đen xanh lá cây", 4200000));
        racketList.add(new RacketSeed("ASTROX99-Gen3-Pro_Đen_xanh lá cây.png", "Yonex", "Yonex Astrox 99 Gen 3 Pro", "Đen xanh lá cây", 4200000));
        racketList.add(new RacketSeed("ASTROX_100_Play_Đỏ thẫm.png", "Yonex", "Yonex Astrox 100 Play", "Đỏ thẫm", 1300000));
        racketList.add(new RacketSeed("Astrox_100_Tour_Đỏ thẫm.png", "Yonex", "Yonex Astrox 100 Tour", "Đỏ thẫm", 2700000));
        racketList.add(new RacketSeed("NANOFLARE NEXTAGE_Xám đậm.png", "Yonex", "Yonex Nanoflare Nextage", "Xám đậm", 2950000));
        racketList.add(new RacketSeed("NANOFLARE-800_Play_Xanh lá cây đậm.png", "Yonex", "Yonex Nanoflare 800 Play", "Xanh lá cây đậm", 1300000));
        racketList.add(new RacketSeed("NANOFLARE-800_Pro_Xanh lá cây đậm.png", "Yonex", "Yonex Nanoflare 800 Pro", "Xanh lá cây đậm", 4000000));
        racketList.add(new RacketSeed("NANOFLARE_1000_z_Phay_Vàng_đen.png", "Yonex", "Yonex Nanoflare 1000 Z Play", "Vàng đen", 1350000));
        racketList.add(new RacketSeed("NANOFLARE_700G_Games_tím đêm.png", "Yonex", "Yonex Nanoflare 700G Games", "Tím đêm", 1250000));
        racketList.add(new RacketSeed("NANOFLARE_700_Play_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Play", "Tím đêm", 1300000));
        racketList.add(new RacketSeed("NANOFLARE_700_Pro_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Pro", "Tím đêm", 4000000));
        racketList.add(new RacketSeed("NANOFLARE_700_Tour_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Tour", "Tím đêm", 2750000));

        // 5. Seed products
        for (RacketSeed seed : racketList) {
            ThuongHieu brand = seed.brandName.equalsIgnoreCase("Yonex") ? yonex : lining;
            String folder = seed.brandName.equalsIgnoreCase("Yonex") ? "Yonex/" : "Li-ning/";
            String imagePath = folder + seed.fileName;

            // Find or create product
            SanPham product = sanPhamRepository.findAll().stream()
                    .filter(sp -> sp.getTenSanPham().equalsIgnoreCase(seed.productName))
                    .findFirst()
                    .orElseGet(() -> {
                        SanPham sp = new SanPham();
                        sp.setTenSanPham(seed.productName);
                        sp.setMoTa("Vợt cầu lông " + seed.productName + " chính hãng, mang lại hiệu suất vượt trội và độ bền cao, phù hợp cho người chơi ở mọi cấp độ.");
                        sp.setDanhMuc(category);
                        sp.setThuongHieu(brand);
                        sp.setNhanVien(employee);
                        sp.setTrangThai("dang_ban");
                        sp.setNgayTao(LocalDateTime.now());
                        sp.setNgayCapNhat(LocalDateTime.now());
                        return sanPhamRepository.save(sp);
                    });

            // Set of weights to create
            List<String> weights = new ArrayList<>();
            if (seed.customWeight != null) {
                weights.add(seed.customWeight);
            } else {
                weights.add("3U");
                weights.add("4U");
            }

            String tension = "20-28 lbs";

            // Find existing variants for this product
            List<SanPhamChiTiet> existingVariants = sanPhamChiTietRepository.findBySanPham_Id(product.getId());

            for (String weight : weights) {
                // Check if this variant combination already exists
                boolean variantExists = existingVariants.stream().anyMatch(ev ->
                        ev.getMauSac().equalsIgnoreCase(seed.color) &&
                        ev.getTrongLuong().equalsIgnoreCase(weight) &&
                        (ev.getSucCang() == null ? "" : ev.getSucCang()).equalsIgnoreCase(tension)
                );

                if (!variantExists) {
                    SanPhamChiTiet spct = new SanPhamChiTiet();
                    spct.setSanPham(product);
                    spct.setMauSac(seed.color);
                    spct.setTrongLuong(weight);
                    spct.setMucCang(tension);
                    spct.setGiaBan(seed.price);
                    spct.setSoLuongTon(50); // Default stock quantity
                    spct.setChatLieu("Carbon Fiber");
                    spct.setKichThuoc("G5");
                    spct.setGiaNhap(seed.price.multiply(BigDecimal.valueOf(0.7))); // Default 70% cost price
                    spct.setNgayTao(LocalDateTime.now());
                    spct.setNgayCapNhat(LocalDateTime.now());
                    spct.setTrangThai("dang_ban");

                    // Set Main image
                    HinhAnhSanPham hasp = new HinhAnhSanPham();
                    hasp.setSanPhamChiTiet(spct);
                    hasp.setUrlHinhAnh(imagePath);
                    hasp.setMauSac(seed.color);
                    hasp.setLaAnhChinh(true);
                    
                    spct.setHinhAnhSanPhams(List.of(hasp));

                    sanPhamChiTietRepository.save(spct);
                    System.out.println("Created variant for product " + product.getTenSanPham() + " | Color: " + seed.color + " | Weight: " + weight);
                } else {
                    System.out.println("Variant already exists for product " + product.getTenSanPham() + " | Color: " + seed.color + " | Weight: " + weight);
                }
            }
        }
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

        System.out.println("=== DATABASE SEEDING COMPLETED ===");
    }

    @Test
    public void testGhnWarehouse() {
        System.out.println("=== TESTING GHN CONFIG AND WAREHOUSE ===");
        System.out.println("GHN Base URL: " + ghnConfig.getBaseUrl());
        System.out.println("GHN Token: " + ghnConfig.getToken());
        System.out.println("GHN Shop ID: " + ghnConfig.getShopId());
        System.out.println("GHN From District ID: " + ghnConfig.getFromDistrictId());
        System.out.println("GHN From Ward Code: " + ghnConfig.getFromWardCode());
        System.out.println("GHN From Address: " + ghnConfig.getFromAddress());

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());

        try {
            String provinceUrl = ghnConfig.getBaseUrl() + "/shiip/public-api/master-data/province";
            org.springframework.http.HttpEntity<Void> req = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Map> resp = restTemplate.exchange(provinceUrl, org.springframework.http.HttpMethod.GET, req, Map.class);
            System.out.println("Province list status code: " + resp.getStatusCode());
            if (resp.getBody() != null) {
                System.out.println("GHN API Response Code: " + resp.getBody().get("code"));
            }
        } catch (Exception e) {
            System.err.println("Error querying provinces: " + e.getMessage());
        }

        try {
            String districtUrl = ghnConfig.getBaseUrl() + "/shiip/public-api/master-data/district";
            Map<String, Object> body = Map.of();
            org.springframework.http.HttpEntity<Map<String, Object>> req = new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<Map> resp = restTemplate.postForEntity(districtUrl, req, Map.class);
            System.out.println("District list response code: " + resp.getStatusCode());
            if (resp.getBody() != null && resp.getBody().get("data") != null) {
                List<Map<String, Object>> districts = (List<Map<String, Object>>) resp.getBody().get("data");
                System.out.println("Total districts loaded: " + districts.size());
                
                Integer targetId = ghnConfig.getFromDistrictId();
                Optional<Map<String, Object>> found = districts.stream()
                        .filter(d -> d.get("DistrictID") != null && String.valueOf(d.get("DistrictID")).equals(String.valueOf(targetId)))
                        .findFirst();
                
                if (found.isPresent()) {
                    System.out.println("FOUND CONFIGURED DISTRICT IN GHN SANDBOX:");
                    System.out.println("DistrictID: " + found.get().get("DistrictID"));
                    System.out.println("DistrictName: " + found.get().get("DistrictName"));
                    System.out.println("ProvinceID: " + found.get().get("ProvinceID"));
                } else {
                    System.err.println("CONFIGURED DISTRICT ID " + targetId + " NOT FOUND IN GHN SANDBOX!");
                }
                
                Optional<Map<String, Object>> baDinh = districts.stream()
                        .filter(d -> d.get("DistrictID") != null && String.valueOf(d.get("DistrictID")).equals("1454"))
                        .findFirst();
                if (baDinh.isPresent()) {
                    System.out.println("Found District 1454 (Ba Dinh?): " + baDinh.get().get("DistrictName") + ", ProvinceID: " + baDinh.get().get("ProvinceID"));
                }

                Optional<Map<String, Object>> d1484 = districts.stream()
                        .filter(d -> d.get("DistrictID") != null && String.valueOf(d.get("DistrictID")).equals("1484"))
                        .findFirst();
                if (d1484.isPresent()) {
                    System.out.println("Found District 1484: " + d1484.get().get("DistrictName") + ", ProvinceID: " + d1484.get().get("ProvinceID"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying districts: " + e.getMessage());
        }
        System.out.println("=== GHN TEST COMPLETED ===");
    }

    @Test
    public void testGetProvinces() {
        System.out.println("=== TESTING GHN SERVICE GET PROVINCES ===");
        List<Map<String, Object>> provinces = ghnService.getProvinces();
        System.out.println("Provinces returned: " + provinces.size());
        for (int i = 0; i < Math.min(5, provinces.size()); i++) {
            System.out.println("Province " + i + ": " + provinces.get(i).get("ProvinceName"));
        }
        System.out.println("=== GHN SERVICE TEST COMPLETED ===");
    }

    @Test
    public void checkCarriers() {
        System.out.println("=== CHECKING DON VI VAN CHUYEN DB RECORDS ===");
        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        System.out.println("Total carriers: " + carriers.size());
        for (DonViVanChuyen carrier : carriers) {
            System.out.println("ID: " + carrier.getId());
            System.out.println("  Ma: " + carrier.getMaDonVi());
            System.out.println("  Ten: " + carrier.getTenDonVi());
            System.out.println("  Token: '" + carrier.getToken() + "'");
            System.out.println("  ClientId: '" + carrier.getClientId() + "'");
        }
        System.out.println("=============================================");
    }
}
