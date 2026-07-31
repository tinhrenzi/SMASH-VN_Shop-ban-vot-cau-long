package com.smashvn.shop.service.admin;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dao.HinhAnhSanPhamDAO;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import com.smashvn.shop.util.RacketSpecUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBienTheService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ThuocTinhRepository thuocTinhRepository;
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
            String mauSac, String trongLuong, String kichThuoc, String mucCang, MultipartFile fileAnh) throws Exception {

        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm gốc"));

        com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromIdOrName(sp.getDanhMuc(), sp.getDanhMuc().getId());
        if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            throw new IllegalArgumentException("Hộp cầu chỉ được phép có duy nhất một biến thể mặc định!");
        }

        String cleanMauSac = normalizeText(mauSac);
        if (cleanMauSac != null && cleanMauSac.length() > 50) {
            throw new IllegalArgumentException("Màu sắc không được vượt quá 50 ký tự.");
        }
        String cleanTrongLuong = null;
        String cleanKichThuoc = null;
        String cleanMucCang = null;

        if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
            cleanTrongLuong = normalizeCode(trongLuong);
            if (cleanTrongLuong == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_TRONG_LUONG_VOT.contains(cleanTrongLuong)) {
                throw new IllegalArgumentException("Trọng lượng vợt không hợp lệ (Chỉ chấp nhận: 3U, 4U, 5U)!");
            }
            cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);
        } else if (catType == com.smashvn.shop.constant.CategoryType.GIAY) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_GIAY.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước giày không hợp lệ (Chỉ chấp nhận: 36 đến 46)!");
            }
        } else if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_TRANG_PHUC.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước trang phục không hợp lệ (Chỉ chấp nhận: XS đến 3XL)!");
            }
        } else if (cleanMauSac == null) {
            throw new IllegalArgumentException("Màu sắc không được để trống!");
        }

        validateBasicFinancial(giaBan, soLuongTon);

        final String fMau = cleanMauSac != null ? cleanMauSac.toLowerCase() : "";
        final String fTrong = cleanTrongLuong != null ? cleanTrongLuong.toLowerCase() : "";
        final String fKich = cleanKichThuoc != null ? cleanKichThuoc.toLowerCase() : "";

        boolean exists = sanPhamChiTietRepository.findBySanPham_Id(idSanPham).stream()
                .anyMatch(bt -> (bt.getMauSac() == null ? "" : bt.getMauSac().toLowerCase()).equals(fMau)
                        && (bt.getTrongLuong() == null ? "" : bt.getTrongLuong().toLowerCase()).equals(fTrong)
                        && (bt.getKichThuoc() == null ? "" : bt.getKichThuoc().toLowerCase()).equals(fKich));
        if (exists) {
            throw new IllegalArgumentException("Biến thể với thuộc tính này đã tồn tại!");
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
            List<SanPhamChiTiet> existingVariants = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
            for (SanPhamChiTiet existing : existingVariants) {
                if (existing.getMauSac() != null && existing.getMauSac().equalsIgnoreCase(cleanMauSac)
                        && existing.getHinhAnhSanPham() != null && !existing.getHinhAnhSanPham().isEmpty()) {
                    secureFileName = existing.getHinhAnhSanPham();
                    break;
                }
            }
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
        spct.setTrangThai(TRANG_THAI_DANG_BAN);

        saveOrUpdateAttribute(spct, "Màu sắc", cleanMauSac);
        saveOrUpdateAttribute(spct, "Trọng lượng", cleanTrongLuong);
        saveOrUpdateAttribute(spct, "Kích thước", cleanKichThuoc);
        saveOrUpdateAttribute(spct, "Sức căng", cleanMucCang);

        spct.setHinhAnhSanPham(secureFileName);
        sanPhamChiTietRepository.save(spct);

        if (catType == com.smashvn.shop.constant.CategoryType.VOT && cleanMucCang != null) {
            updateMucCangAllVariants(idSanPham, cleanMucCang);
        }
    }

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

    public SanPhamChiTiet layBienTheTheoId(Integer idBienThe) {
        return sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
    }

    @Transactional
    public void capNhatBienThe(Integer idBienThe, BigDecimal giaBan, Integer soLuongTon,
            String mauSac, String trongLuong, String kichThuoc, String mucCang, MultipartFile fileAnh) throws Exception {

        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể để sửa"));

        com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromIdOrName(spct.getSanPham().getDanhMuc(), spct.getSanPham().getDanhMuc().getId());
        if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            mauSac = "Mặc định";
            trongLuong = null;
            kichThuoc = null;
            mucCang = null;
        } else if (mauSac != null) {
            mauSac = mauSac.replaceAll("(?i),\\s*Mặc\\s*định", "").trim();
        }

        String cleanMauSac = normalizeText(mauSac);
        if (cleanMauSac != null && cleanMauSac.length() > 50) {
            throw new IllegalArgumentException("Màu sắc không được vượt quá 50 ký tự.");
        }
        String cleanTrongLuong = null;
        String cleanKichThuoc = null;
        String cleanMucCang = null;

        if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
            cleanTrongLuong = normalizeCode(trongLuong);
            if (cleanTrongLuong == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_TRONG_LUONG_VOT.contains(cleanTrongLuong)) {
                throw new IllegalArgumentException("Trọng lượng vợt không hợp lệ (Chỉ chấp nhận: 3U, 4U, 5U)!");
            }
            cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);
        } else if (catType == com.smashvn.shop.constant.CategoryType.GIAY) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_GIAY.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước giày không hợp lệ (Chỉ chấp nhận: 36 đến 46)!");
            }
        } else if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_TRANG_PHUC.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước trang phục không hợp lệ (Chỉ chấp nhận: XS đến 3XL)!");
            }
        } else if (catType != com.smashvn.shop.constant.CategoryType.HOP_CAU && cleanMauSac == null) {
            throw new IllegalArgumentException("Màu sắc không được để trống!");
        }

        validateBasicFinancial(giaBan, soLuongTon);

        final String fMau = cleanMauSac != null ? cleanMauSac.toLowerCase() : "";
        final String fTrong = cleanTrongLuong != null ? cleanTrongLuong.toLowerCase() : "";
        final String fKich = cleanKichThuoc != null ? cleanKichThuoc.toLowerCase() : "";

        boolean exists = sanPhamChiTietRepository.findBySanPham_Id(spct.getSanPham().getId()).stream()
                .anyMatch(bt -> !bt.getId().equals(idBienThe)
                        && (bt.getMauSac() == null ? "" : bt.getMauSac().toLowerCase()).equals(fMau)
                        && (bt.getTrongLuong() == null ? "" : bt.getTrongLuong().toLowerCase()).equals(fTrong)
                        && (bt.getKichThuoc() == null ? "" : bt.getKichThuoc().toLowerCase()).equals(fKich));
        if (exists) {
            throw new IllegalArgumentException("Biến thể với thuộc tính này đã tồn tại!");
        }

        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);

        saveOrUpdateAttribute(spct, "Màu sắc", cleanMauSac);
        saveOrUpdateAttribute(spct, "Trọng lượng", cleanTrongLuong);
        saveOrUpdateAttribute(spct, "Kích thước", cleanKichThuoc);
        saveOrUpdateAttribute(spct, "Sức căng", cleanMucCang);

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
        if (secureFileName != null) {
            String oldFileName = spct.getHinhAnhSanPham();
            spct.setHinhAnhSanPham(secureFileName);
        } else {
            if (spct.getHinhAnhSanPhams() != null) {
                for (com.smashvn.shop.entity.HinhAnhSanPham hasp : spct.getHinhAnhSanPhams()) {
                    hasp.setMauSac(cleanMauSac);
                }
            }
        }

        sanPhamChiTietRepository.save(spct);

        if (catType == com.smashvn.shop.constant.CategoryType.VOT && cleanMucCang != null) {
            updateMucCangAllVariants(spct.getSanPham().getId(), cleanMucCang);
        }
    }

    private void saveOrUpdateAttribute(SanPhamChiTiet spct, String tenThuocTinh, String giaTri) {
        if (spct.getSanPhamChiTietThuocTinhs() == null) {
            spct.setSanPhamChiTietThuocTinhs(new java.util.LinkedHashSet<>());
        }
        SanPhamChiTietThuocTinh existing = spct.getSanPhamChiTietThuocTinhs().stream()
                .filter(tt -> tt.getThuocTinh() != null && tenThuocTinh.equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()))
                .findFirst()
                .orElse(null);

        if (giaTri == null || giaTri.isBlank()) {
            if (existing != null) {
                spct.getSanPhamChiTietThuocTinhs().remove(existing);
            }
            return;
        }

        if (existing != null) {
            existing.setGiaTri(giaTri.trim());
        } else {
            ThuocTinh tt = thuocTinhRepository.findByTenThuocTinhIgnoreCase(tenThuocTinh)
                    .orElseGet(() -> thuocTinhRepository.save(ThuocTinh.builder()
                            .tenThuocTinh(tenThuocTinh.trim())
                            .trangThai(true)
                            .build()));
            SanPhamChiTietThuocTinh val = SanPhamChiTietThuocTinh.builder()
                    .sanPhamChiTiet(spct)
                    .thuocTinh(tt)
                    .giaTri(giaTri.trim())
                    .build();
            spct.getSanPhamChiTietThuocTinhs().add(val);
        }
    }

    private void updateMucCangAllVariants(Integer idSanPham, String cleanMucCang) {
        List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        for (SanPhamChiTiet v : variants) {
            saveOrUpdateAttribute(v, "Sức căng", cleanMucCang);
            sanPhamChiTietRepository.save(v);
        }
    }

    private String normalizeText(String s) {
        if (s == null) return null;
        String trimmed = s.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCode(String s) {
        String text = normalizeText(s);
        return text == null ? null : text.toUpperCase();
    }

    private void validateBasicFinancial(BigDecimal giaBan, Integer soLuongTon) {
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0.");
        }
        if (soLuongTon == null || soLuongTon < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho phải lớn hơn hoặc bằng 0.");
        }
    }

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");

    private String saveImageSecurely(MultipartFile file, boolean requireImage, List<Path> uploadedFiles) throws Exception {
        if (file == null || file.isEmpty()) {
            if (requireImage) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc.");
            }
            return null;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn 5MB.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP).");
        }
        String safeFileName = UUID.randomUUID().toString() + ext;
        Path targetUploadDir = Paths.get(uploadPathConfig, "product").toAbsolutePath().normalize();
        if (!Files.exists(targetUploadDir)) {
            Files.createDirectories(targetUploadDir);
        }
        Path targetFilePath = targetUploadDir.resolve(safeFileName).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        uploadedFiles.add(targetFilePath);
        return safeFileName;
    }
}
