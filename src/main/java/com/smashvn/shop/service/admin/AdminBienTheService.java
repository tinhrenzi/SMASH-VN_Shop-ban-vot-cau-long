package com.smashvn.shop.service.admin;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.util.RacketSpecUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBienTheService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

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
        String cleanMucCang = RacketSpecUtils.normalizeStringTensionToLbs(mucCang);

        validateBienThe(giaBan, soLuongTon, cleanMauSac, cleanTrongLuong, cleanMucCang);

        // Kiểm tra trùng lặp tổ hợp duy nhất (id_san_pham, mau_sac, trong_luong, muc_cang)
        boolean exists = sanPhamChiTietRepository.findBySanPham_Id(idSanPham).stream()
                .anyMatch(bt -> bt.getMauSac().equalsIgnoreCase(cleanMauSac)
                        && bt.getTrongLuong().equalsIgnoreCase(cleanTrongLuong)
                        && (bt.getMucCang() == null ? cleanMucCang == null : bt.getMucCang().equalsIgnoreCase(cleanMucCang)));
        if (exists) {
            throw new IllegalArgumentException("Biến thể với màu sắc, trọng lượng và mức căng này đã tồn tại!");
        }

        // Save image securely (required on creation)
        String secureFileName = saveImageSecurely(fileAnh, true);

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);
        spct.setMauSac(cleanMauSac);
        spct.setTrongLuong(cleanTrongLuong);
        spct.setMucCang(cleanMucCang);
        spct.setHinhAnhSanPham(secureFileName);

        sanPhamChiTietRepository.save(spct);
    }

    // 3. Xóa biến thể
    @Transactional
    public void xoaBienThe(Integer idBienThe) {
        if (hoaDonChiTietRepository.existsBySanPhamChiTiet_Id(idBienThe)) {
            throw new IllegalStateException("Không thể xóa biến thể này vì đã có khách hàng đặt mua biến thể này trong đơn hàng!");
        }
        sanPhamChiTietRepository.deleteById(idBienThe);
    }

    public java.util.Set<Integer> layDanhSachBienTheDaDatHang(Integer idSanPham) {
        return new java.util.HashSet<>(hoaDonChiTietRepository.findOrderedVariantIdsBySanPhamId(idSanPham));
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
        String cleanMucCang = RacketSpecUtils.normalizeStringTensionToLbs(mucCang);

        validateBienThe(giaBan, soLuongTon, cleanMauSac, cleanTrongLuong, cleanMucCang);

        // Kiểm tra trùng lặp tổ hợp duy nhất (trừ chính biến thể đang sửa)
        boolean exists = sanPhamChiTietRepository.findBySanPham_Id(spct.getSanPham().getId()).stream()
                .anyMatch(bt -> !bt.getId().equals(idBienThe)
                        && bt.getMauSac().equalsIgnoreCase(cleanMauSac)
                        && bt.getTrongLuong().equalsIgnoreCase(cleanTrongLuong)
                        && (bt.getMucCang() == null ? cleanMucCang == null : bt.getMucCang().equalsIgnoreCase(cleanMucCang)));
        if (exists) {
            throw new IllegalArgumentException("Biến thể với màu sắc, trọng lượng và mức căng này đã tồn tại!");
        }

        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);
        spct.setMauSac(cleanMauSac);
        spct.setTrongLuong(cleanTrongLuong);
        spct.setMucCang(cleanMucCang);

        // Save image securely (optional on update)
        String secureFileName = saveImageSecurely(fileAnh, false);
        if (secureFileName != null) {
            spct.setHinhAnhSanPham(secureFileName);
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
            throw new IllegalArgumentException("Mức căng không được vượt quá 50 ký tự.");
        }
    }

    // --- Helper Image Saving with Security Measures ---
    private String saveImageSecurely(MultipartFile file, boolean isRequired) throws Exception {
        if (file == null || file.isEmpty()) {
            if (isRequired) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc.");
            }
            return null;
        }

        // 1. Check file extension
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("webp")) {
            throw new IllegalArgumentException("Định dạng tệp không hợp lệ! Chỉ cho phép JPG, JPEG, PNG, WEBP.");
        }

        // 2. Check file size (max 5 MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước hình ảnh quá lớn! Kích thước tối đa cho phép là 5MB.");
        }

        // 3. Verify MIME type using Apache Tika
        org.apache.tika.Tika tika = new org.apache.tika.Tika();
        try (InputStream is = file.getInputStream()) {
            String mimeType = tika.detect(is);
            if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp"))) {
                throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ! MIME type không được chấp nhận.");
            }
        }

        // 4. Verify genuine image using ImageIO
        try (InputStream is = file.getInputStream()) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
        }

        // 5. Generate random filename
        String secureFileName = java.util.UUID.randomUUID().toString() + "." + ext;

        // 6. Prevent path traversal
        Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
        Path productUploadPath = rootUploadPath.resolve("product").normalize();
        if (!Files.exists(productUploadPath)) {
            Files.createDirectories(productUploadPath);
        }

        Path targetFilePath = productUploadPath.resolve(secureFileName).normalize().toAbsolutePath();
        Path normalizedRoot = productUploadPath.normalize().toAbsolutePath();

        if (!targetFilePath.startsWith(normalizedRoot)) {
            throw new SecurityException("Invalid upload path");
        }

        // 7. Save file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("[UPLOAD_FAILURE] Failed to save product variant image: {}", e.getMessage());
            throw e;
        }

        return secureFileName;
    }
}
