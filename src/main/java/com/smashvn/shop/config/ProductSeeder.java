package com.smashvn.shop.config;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSeeder implements CommandLineRunner {

    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final com.smashvn.shop.dao.DonViVanChuyenDAO donViVanChuyenDAO;

    @Override
    public void run(String... args) throws Exception {
        log.info("[SEEDER] Bắt đầu quét thư mục uploads để thêm sản phẩm...");

        // Seed DonViVanChuyen if missing
        List<DonViVanChuyen> carriers = donViVanChuyenDAO.findAll();
        log.info("[SEEDER] Danh sách đơn vị vận chuyển hiện tại trong DB: {}", carriers);
        
        boolean hasGhn = carriers.stream().anyMatch(c -> c.getTenDonVi() != null && 
                (c.getTenDonVi().toUpperCase().contains("GHN") || c.getTenDonVi().toUpperCase().contains("GIAO HÀNG NHANH")));
        if (!hasGhn) {
            DonViVanChuyen ghn = new DonViVanChuyen();
            ghn.setTenDonVi("Giao Hàng Nhanh (GHN)");
            ghn.setMaDonVi("GHN");
            ghn.setHotline("1900 636677");
            ghn.setWebsite("https://ghn.vn");
            ghn.setPhiLocal(new BigDecimal("25000"));
            ghn.setPhiNationwide(new BigDecimal("38000"));
            donViVanChuyenDAO.save(ghn);
            log.info("[SEEDER] Đã tự động tạo đơn vị vận chuyển GHN trong DB");
        }

        boolean hasGhtk = carriers.stream().anyMatch(c -> c.getTenDonVi() != null && 
                (c.getTenDonVi().toUpperCase().contains("GHTK") || c.getTenDonVi().toUpperCase().contains("GIAO HÀNG TIẾT KIỆM")));
        if (!hasGhtk) {
            DonViVanChuyen ghtk = new DonViVanChuyen();
            ghtk.setTenDonVi("Giao Hàng Tiết Kiệm (GHTK)");
            ghtk.setMaDonVi("GHTK");
            ghtk.setHotline("1900 6092");
            ghtk.setWebsite("https://ghtk.vn");
            ghtk.setPhiLocal(new BigDecimal("22000"));
            ghtk.setPhiNationwide(new BigDecimal("30000"));
            donViVanChuyenDAO.save(ghtk);
            log.info("[SEEDER] Đã tự động tạo đơn vị vận chuyển GHTK trong DB");
        }


        // 1. Tìm hoặc tạo DanhMuc "Vợt Cầu Lông"
        DanhMuc danhMuc = danhMucRepository.findAll().stream()
                .filter(dm -> dm.getTenDanhMuc().equalsIgnoreCase("Vợt Cầu Lông") || dm.getTenDanhMuc().equalsIgnoreCase("Vợt"))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc newDm = new DanhMuc();
                    newDm.setTenDanhMuc("Vợt Cầu Lông");
                    newDm.setMoTa("Các loại vợt cầu lông chính hãng");
                    newDm.setTrangThai(true);
                    return danhMucRepository.save(newDm);
                });

        // 2. Tìm hoặc tạo ThuongHieu "Li-Ning" và "Yonex"
        ThuongHieu lining = thuongHieuRepository.findAll().stream()
                .filter(th -> th.getTenThuongHieu().equalsIgnoreCase("Li-Ning") || th.getTenThuongHieu().equalsIgnoreCase("Lining"))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu("Li-Ning");
                    th.setMoTa("Thương hiệu thể thao hàng đầu Trung Quốc");
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });

        ThuongHieu yonex = thuongHieuRepository.findAll().stream()
                .filter(th -> th.getTenThuongHieu().equalsIgnoreCase("Yonex"))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu("Yonex");
                    th.setMoTa("Thương hiệu cầu lông số 1 thế giới đến từ Nhật Bản");
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });

        // 3. Tạo tài khoản quản trị
        TaiKhoan adminTk = taiKhoanRepository.findByEmail("admin@smashvn.com");
        if (adminTk == null) {
            adminTk = new TaiKhoan();
            adminTk.setEmail("admin@smashvn.com");
            adminTk.setMatKhau(BCrypt.hashpw("admin123", BCrypt.gensalt()));
            adminTk.setVaiTro("QL");
            adminTk.setTrangThai("hoat_dong");
            adminTk = taiKhoanRepository.save(adminTk);
            log.info("[SEEDER] Đã tạo tài khoản quản trị: email = admin@smashvn.com, password = admin123");
        }

        NhanVien adminNv = nhanVienRepository.findByTaiKhoanId(adminTk.getId());
        if (adminNv == null) {
            adminNv = new NhanVien();
            adminNv.setTaiKhoan(adminTk);
            adminNv.setHoTenNv("Administrator");
            adminNv.setChucVu("Quản trị viên");
            adminNv.setSoDienThoaiNv("0123456789");
            adminNv = nhanVienRepository.save(adminNv);
            log.info("[SEEDER] Đã tạo nhân viên quản trị gắn với tài khoản admin");
        }

        // 4. Quét uploads/product/Li-ning
        File liningDir = new File("uploads/product/Li-ning");
        if (liningDir.exists() && liningDir.isDirectory()) {
            seedFromDirectory(liningDir, "Li-ning", lining, danhMuc, adminNv);
        } else {
            log.warn("[SEEDER] Thư mục Li-ning không tồn tại hoặc không hợp lệ: {}", liningDir.getAbsolutePath());
        }

        // 5. Quét uploads/product/Yonex
        File yonexDir = new File("uploads/product/Yonex");
        if (yonexDir.exists() && yonexDir.isDirectory()) {
            seedFromDirectory(yonexDir, "Yonex", yonex, danhMuc, adminNv);
        } else {
            log.warn("[SEEDER] Thư mục Yonex không tồn tại hoặc không hợp lệ: {}", yonexDir.getAbsolutePath());
        }

        log.info("[SEEDER] Hoàn thành quá trình seeding dữ liệu!");
    }

    private void seedFromDirectory(File dir, String folderName, ThuongHieu brand, DanhMuc category, NhanVien employee) {
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
        if (files == null) return;

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
