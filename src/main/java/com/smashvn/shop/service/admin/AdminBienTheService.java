package com.smashvn.shop.service.admin;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dao.HinhAnhSanPhamDAO;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.util.RacketSpecUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBienTheService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HinhAnhSanPhamDAO hinhAnhSanPhamDAO;

    private static final String TRANG_THAI_DANG_BAN = "dang_ban";
    private static final String TRANG_THAI_NGUNG_KINH_DOANH = "ngung_kinh_doanh";

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    // 1. Lấy danh sách biến thể theo ID Sản phẩm gốc
    public List<SanPhamChiTiet> layDanhSachBienThe(Integer idSanPham) {
        return sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
    }

    // 2. Thêm biến thể mới
    @Transactional
    public void themBienThe(Integer idSanPham, BigDecimal giaBan, Integer soLuongTon,
            String mauSac, String trongLuong, String mucCang, MultipartFile fileAnh) throws Exception {

        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm gốc"));

        // Trim and validate input values
        String cleanMauSac = mauSac != null ? mauSac.trim() : null;
        String cleanTrongLuong = trongLuong != null ? trongLuong.trim() : null;
        String cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);

        validateBienThe(giaBan, soLuongTon, cleanMauSac, cleanTrongLuong, cleanMucCang);

        // Kiểm tra trùng lặp tổ hợp duy nhất (id_san_pham, mau_sac, trong_luong, muc_cang)
        boolean exists = sanPhamChiTietRepository.findBySanPham_Id(idSanPham).stream()
                .anyMatch(bt -> bt.getMauSac().equalsIgnoreCase(cleanMauSac)
                        && bt.getTrongLuong().equalsIgnoreCase(cleanTrongLuong)
                        && (bt.getMucCang() == null ? cleanMucCang == null : bt.getMucCang().equalsIgnoreCase(cleanMucCang)));
        if (exists) {
            throw new IllegalArgumentException("Biến thể với màu sắc, trọng lượng và sức căng khuyến nghị này đã tồn tại!");
        }

        List<Path> uploadedFiles = new ArrayList<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            for (Path path : uploadedFiles) {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
            );
        }

        String secureFileName = saveImageSecurely(fileAnh, false, uploadedFiles);
        if (secureFileName == null || secureFileName.isEmpty()) {
            // Fallback: Check if another variant of the same color exists
            List<SanPhamChiTiet> existingVariants = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
            for (SanPhamChiTiet existing : existingVariants) {
                if (existing.getMauSac().equalsIgnoreCase(cleanMauSac) && existing.getHinhAnhSanPham() != null && !existing.getHinhAnhSanPham().isEmpty()) {
                    secureFileName = existing.getHinhAnhSanPham();
                    break;
                }
            }
            // If still null, fallback to the first variant's image (main image)
            if (secureFileName == null && !existingVariants.isEmpty()) {
                secureFileName = existingVariants.get(0).getHinhAnhSanPham();
            }
            if (secureFileName == null) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc.");
            }
        }

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);
        spct.setMauSac(cleanMauSac);
        spct.setTrongLuong(cleanTrongLuong);
        spct.setMucCang(cleanMucCang);
        spct.setTrangThai(TRANG_THAI_DANG_BAN);
        spct.setHinhAnhSanPham(secureFileName);

        sanPhamChiTietRepository.save(spct);
    }

    // 3. Ẩn biến thể khỏi khách hàng (xóa mềm)
    @Transactional
    public void xoaBienThe(Integer idBienThe) {
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
        spct.setTrangThai(TRANG_THAI_NGUNG_KINH_DOANH);
        sanPhamChiTietRepository.save(spct);
    }

    @Transactional
    public void moBanLaiBienThe(Integer idBienThe) {
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
        spct.setTrangThai(TRANG_THAI_DANG_BAN);
        sanPhamChiTietRepository.save(spct);
    }

    // Thêm hàm lấy 1 biến thể duy nhất để đổ lên Form sửa
    public SanPhamChiTiet layBienTheTheoId(Integer idBienThe) {
        return sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
    }

    // Thêm hàm Cập nhật Biến thể
    @Transactional
    public void capNhatBienThe(Integer idBienThe, BigDecimal giaBan, Integer soLuongTon,
            String mauSac, String trongLuong, String mucCang, MultipartFile fileAnh) throws Exception {

        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể để sửa"));

        // Trim and validate input values
        String cleanMauSac = mauSac != null ? mauSac.trim() : null;
        String cleanTrongLuong = trongLuong != null ? trongLuong.trim() : null;
        String cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);

        validateBienThe(giaBan, soLuongTon, cleanMauSac, cleanTrongLuong, cleanMucCang);

        // Kiểm tra trùng lặp tổ hợp duy nhất (trừ chính biến thể đang sửa)
        boolean exists = sanPhamChiTietRepository.findBySanPham_Id(spct.getSanPham().getId()).stream()
                .anyMatch(bt -> !bt.getId().equals(idBienThe)
                        && bt.getMauSac().equalsIgnoreCase(cleanMauSac)
                        && bt.getTrongLuong().equalsIgnoreCase(cleanTrongLuong)
                        && (bt.getMucCang() == null ? cleanMucCang == null : bt.getMucCang().equalsIgnoreCase(cleanMucCang)));
        if (exists) {
            throw new IllegalArgumentException("Biến thể với màu sắc, trọng lượng và sức căng khuyến nghị này đã tồn tại!");
        }

        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);
        spct.setMauSac(cleanMauSac);
        spct.setTrongLuong(cleanTrongLuong);
        spct.setMucCang(cleanMucCang);

        List<Path> uploadedFiles = new ArrayList<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            for (Path path : uploadedFiles) {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
            );
        }

        // Save image securely (optional on update)
        String secureFileName = saveImageSecurely(fileAnh, false, uploadedFiles);
        if (secureFileName != null) {
            String oldFileName = spct.getHinhAnhSanPham();
            spct.setHinhAnhSanPham(secureFileName);

            if (oldFileName != null && !oldFileName.equals(secureFileName)) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_COMMITTED) {
                                boolean stillReferenced = hinhAnhSanPhamDAO.existsByUrlHinhAnh(oldFileName);
                                if (!stillReferenced) {
                                    try {
                                        Path oldFilePath = Paths.get(uploadPathConfig).resolve("product").resolve(oldFileName).normalize().toAbsolutePath();
                                        Files.deleteIfExists(oldFilePath);
                                    } catch (Exception e) {
                                        log.error("Failed to delete unused image file: {}", oldFileName, e);
                                    }
                                }
                            }
                        }
                    }
                );
            }
        } else {
            // Update mauSac in existing image records if color changed
            if (spct.getHinhAnhSanPhams() != null) {
                for (com.smashvn.shop.entity.HinhAnhSanPham hasp : spct.getHinhAnhSanPhams()) {
                    hasp.setMauSac(cleanMauSac);
                }
            }
        }

        sanPhamChiTietRepository.save(spct);
    }

    // --- Helper Validation ---
    private void validateBienThe(BigDecimal giaBan, Integer soLuongTon, String mauSac, String trongLuong, String mucCang) {
        if (giaBan == null) {
            throw new IllegalArgumentException("Giá bán không được để trống.");
        }
        if (giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0.");
        }
        if (soLuongTon == null) {
            throw new IllegalArgumentException("Số lượng tồn kho không được để trống.");
        }
        if (soLuongTon < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho phải lớn hơn hoặc bằng 0.");
        }
        if (mauSac == null || mauSac.isEmpty()) {
            throw new IllegalArgumentException("Màu sắc không được để trống.");
        }
        if (mauSac.length() > 50) {
            throw new IllegalArgumentException("Màu sắc không được vượt quá 50 ký tự.");
        }
        if (trongLuong == null || trongLuong.isEmpty()) {
            throw new IllegalArgumentException("Trọng lượng / Size không được để trống.");
        }
        if (trongLuong.length() > 50) {
            throw new IllegalArgumentException("Trọng lượng / Size không được vượt quá 50 ký tự.");
        }
        if (mucCang != null && mucCang.length() > 50) {
            throw new IllegalArgumentException("Sức căng khuyến nghị không được vượt quá 50 ký tự.");
        }
    }

    private String computeFileHash(InputStream is) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "image.jpg";
        String trimmed = filename.trim();
        return trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void validateImageFile(MultipartFile file, String fileLabel) throws Exception {
        if (file == null || file.isEmpty()) {
            return;
        }
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf(".")).toLowerCase();
        }

        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
            throw new IllegalArgumentException("Định dạng tệp không hợp lệ! Chỉ cho phép JPG, JPEG, PNG, WEBP.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước hình ảnh quá lớn! Kích thước tối đa cho phép là 5MB.");
        }

        org.apache.tika.Tika tika = new org.apache.tika.Tika();
        try (InputStream is = file.getInputStream()) {
            String mimeType = tika.detect(is);
            if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp"))) {
                throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ! MIME type không được chấp nhận.");
            }
        }

        try (InputStream is = file.getInputStream()) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
            if (img == null) {
                throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
            }
            if (img.getWidth() > 5000 || img.getHeight() > 5000) {
                throw new IllegalArgumentException("Độ phân giải hình ảnh vượt quá giới hạn cho phép (Tối đa 5000x5000px)!");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
        }
    }

    // --- Helper Image Saving with Security Measures ---
    private String saveImageSecurely(MultipartFile file, boolean isRequired, List<Path> uploadedFiles) throws Exception {
        if (file == null || file.isEmpty()) {
            if (isRequired) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc.");
            }
            return null;
        }

        validateImageFile(file, "biến thể");

        String origName = file.getOriginalFilename();
        if (origName != null) {
            origName = Paths.get(origName).getFileName().toString();
        }
        String safeFileName = sanitizeFilename(origName);

        Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
        Path productUploadPath = rootUploadPath.resolve("product").normalize();
        if (!Files.exists(productUploadPath)) {
            Files.createDirectories(productUploadPath);
        }

        Path targetFilePath = productUploadPath.resolve(safeFileName).normalize().toAbsolutePath();
        Path normalizedRoot = productUploadPath.normalize().toAbsolutePath();

        if (!targetFilePath.startsWith(normalizedRoot)) {
            throw new SecurityException("Invalid upload path");
        }

        if (Files.exists(targetFilePath)) {
            String uploadedHash;
            try (InputStream is = file.getInputStream()) {
                uploadedHash = computeFileHash(is);
            }
            String existingHash;
            try (InputStream is = Files.newInputStream(targetFilePath)) {
                existingHash = computeFileHash(is);
            }

            if (uploadedHash.equals(existingHash)) {
                return safeFileName;
            } else {
                throw new IllegalArgumentException("Đã tồn tại ảnh có tên '" + safeFileName + "' nhưng nội dung khác. Vui lòng đổi tên file trước khi tải lên.");
            }
        }

        // Save file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("[UPLOAD_FAILURE] Failed to save product variant image: {}", e.getMessage());
            throw e;
        }

        uploadedFiles.add(targetFilePath);
        return safeFileName;
    }
}
