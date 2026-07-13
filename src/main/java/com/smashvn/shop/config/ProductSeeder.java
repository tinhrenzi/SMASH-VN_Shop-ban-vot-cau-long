package com.smashvn.shop.config;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.HinhAnhSanPham;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Component
@RequiredArgsConstructor
@Slf4j
public class ProductSeeder implements CommandLineRunner {

    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final NhanVienRepository nhanVienRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("[SEEDER] Bắt đầu quét thư mục uploads để thêm sản phẩm...");

        // 1. Tìm DanhMuc "Vợt Cầu Lông"
        DanhMuc danhMuc = danhMucRepository.findAll().stream()
                .filter(dm -> dm.getTenDanhMuc().equalsIgnoreCase("Vợt Cầu Lông") || dm.getTenDanhMuc().equalsIgnoreCase("Vợt"))
                .findFirst()
                .orElse(null);

        // 2. Tìm ThuongHieu "Li-Ning" và "Yonex"
        ThuongHieu lining = thuongHieuRepository.findAll().stream()
                .filter(th -> th.getTenThuongHieu().equalsIgnoreCase("Li-Ning") || th.getTenThuongHieu().equalsIgnoreCase("Lining"))
                .findFirst()
                .orElse(null);

        ThuongHieu yonex = thuongHieuRepository.findAll().stream()
                .filter(th -> th.getTenThuongHieu().equalsIgnoreCase("Yonex"))
                .findFirst()
                .orElse(null);

        // 3. Tìm nhân viên hiện có để liên kết với sản phẩm
        NhanVien staff = nhanVienRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        if (danhMuc == null || lining == null || yonex == null || staff == null) {
            log.warn("[SEEDER] Thiếu Danh mục, Thương hiệu hoặc Nhân viên trong DB. Bỏ qua quá trình seed sản phẩm.");
            return;
        }

        // 4. Quét uploads/product/Li-ning
        File liningDir = new File("uploads/product/Li-ning");
        if (liningDir.exists() && liningDir.isDirectory()) {
            seedFromDirectory(liningDir, "Li-ning", lining, danhMuc, staff);
        } else {
            log.warn("[SEEDER] Thư mục Li-ning không tồn tại hoặc không hợp lệ: {}", liningDir.getAbsolutePath());
        }

        // 5. Quét uploads/product/Yonex
        File yonexDir = new File("uploads/product/Yonex");
        if (yonexDir.exists() && yonexDir.isDirectory()) {
            seedFromDirectory(yonexDir, "Yonex", yonex, danhMuc, staff);
        } else {
            log.warn("[SEEDER] Thư mục Yonex không tồn tại hoặc không hợp lệ: {}", yonexDir.getAbsolutePath());
        }

        log.info("[SEEDER] Hoàn thành quá trình seeding dữ liệu!");
    }

    private void seedFromDirectory(File dir, String folderName, ThuongHieu brand, DanhMuc category, NhanVien employee) {
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String filename = file.getName();
            // Parse name & color from file name (e.g. "AXFORCE 9 AYPT317-2_Đen.png")
            String cleanName = filename.substring(0, filename.lastIndexOf('.')).trim();

            // Remove trailing '1', '(1)' or similar
            if (cleanName.endsWith("(1)")) {
                cleanName = cleanName.substring(0, cleanName.length() - 3).trim();
            }
            if (cleanName.endsWith("1")) {
                cleanName = cleanName.substring(0, cleanName.length() - 1).trim();
            }

            String productName = cleanName;
            String color = "Tiêu chuẩn";

            int underscoreIdx = cleanName.indexOf('_');
            if (underscoreIdx != -1) {
                productName = cleanName.substring(0, underscoreIdx).trim();
                color = cleanName.substring(underscoreIdx + 1).trim();
            }

            // Find or create SanPham
            final String finalProdName = productName;
            SanPham sanPham = sanPhamRepository.findAll().stream()
                    .filter(sp -> sp.getTenSanPham().equalsIgnoreCase(finalProdName))
                    .findFirst()
                    .orElseGet(() -> {
                        SanPham sp = new SanPham();
                        sp.setTenSanPham(finalProdName);
                        sp.setDanhMuc(category);
                        sp.setThuongHieu(brand);
                        sp.setNhanVien(employee);
                        sp.setMoTa("Vợt cầu lông chính hãng " + finalProdName + " chất lượng cao, mang lại trải nghiệm chơi tối ưu.");
                        sp.setTrangThai("dang_ban");
                        sp.setNgayTao(LocalDateTime.now());
                        sp.setNgayCapNhat(LocalDateTime.now());
                        return sanPhamRepository.save(sp);
                    });

            // Calculate price based on name
            BigDecimal basePrice = new BigDecimal("1850000"); // Default
            String prodNameUpper = productName.toUpperCase();
            if (prodNameUpper.contains("GEN3") || prodNameUpper.contains("1000") || prodNameUpper.contains("99")) {
                basePrice = new BigDecimal("3950000");
            } else if (prodNameUpper.contains("PRO") || prodNameUpper.contains("90") || prodNameUpper.contains("9")) {
                basePrice = new BigDecimal("3500000");
            } else if (prodNameUpper.contains("TOUR") || prodNameUpper.contains("700") || prodNameUpper.contains("88")) {
                basePrice = new BigDecimal("2400000");
            } else if (prodNameUpper.contains("PLAY") || prodNameUpper.contains("30") || prodNameUpper.contains("JR")) {
                basePrice = new BigDecimal("1250000");
            }

            // Add 4U variant
            addVariant(sanPham, color, "4U", "26 lbs (11.7 kg)", basePrice, folderName + "/" + filename);
            // Add 3U variant
            addVariant(sanPham, color, "3U", "27 lbs (12.2 kg)", basePrice.add(new BigDecimal("100000")), folderName + "/" + filename);
        }
    }

    private void addVariant(SanPham sanPham, String color, String weight, String tension, BigDecimal price, String relativeImagePath) {
        // Check if variant exists
        boolean exists = sanPhamChiTietRepository.findAll().stream()
                .anyMatch(v -> v.getSanPham().getId().equals(sanPham.getId())
                && v.getMauSac().equalsIgnoreCase(color)
                && v.getTrongLuong().equalsIgnoreCase(weight));

        if (!exists) {
            SanPhamChiTiet spct = new SanPhamChiTiet();
            spct.setSanPham(sanPham);
            spct.setMauSac(color);
            spct.setTrongLuong(weight);
            spct.setSucCang(tension);
            spct.setGiaBan(price);
            spct.setGiaNhap(price.multiply(new BigDecimal("0.7")));
            spct.setSoLuongTon(50);
            spct.setChatLieu("High Modulus Graphite");
            spct.setKichThuoc("G5");
            spct.setTrangThai("dang_ban");
            spct.setNgayTao(LocalDateTime.now());
            spct.setNgayCapNhat(LocalDateTime.now());

            // Build image entity
            List<HinhAnhSanPham> images = new ArrayList<>();
            HinhAnhSanPham img = new HinhAnhSanPham();
            img.setSanPhamChiTiet(spct);
            img.setUrlHinhAnh(relativeImagePath);
            img.setMauSac(color);
            img.setLaAnhChinh(true);
            images.add(img);
            spct.setHinhAnhSanPhams(images);

            sanPhamChiTietRepository.save(spct);
            log.info("[SEEDER] Đã thêm biến thể: {} - Màu: {}, Trọng lượng: {}, Giá: {}",
                    sanPham.getTenSanPham(), color, weight, price);
        }
    }
}
