package com.smashvn.shop.service.admin;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.util.RacketSpecUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;

    @Value("${app.upload.path}")
    private String uploadPathConfig;

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

    private String saveImageSecurely(MultipartFile file, String label, List<Path> uploadedFiles) throws Exception {
        validateImageFile(file, label);

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
            throw new RuntimeException("Tên tệp không hợp lệ (Phòng chống Path Traversal)!");
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

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        uploadedFiles.add(targetFilePath);
        return safeFileName;
    }

    // --- HÀM THÊM MỚI (CHỈ LƯU SẢN PHẨM GỐC) ---
    @Transactional
    public void themSanPhamMoi(String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        SanPham sp = new SanPham();
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sp.setTrangThai("dang_ban");

        List<NhanVien> listNV = nhanVienRepository.findAll();
        if (!listNV.isEmpty()) {
            sp.setNhanVien(listNV.get(0));
        }

        sanPhamRepository.save(sp);
    }

    // --- HÀM THÊM MỚI CẢ SẢN PHẨM & TỰ ĐỘNG SINH BIẾN THỂ (NÂNG CẤP BẢO MẬT & KIỂM TOÁN) ---
    @Transactional(rollbackFor = Exception.class)
    public void themSanPhamVaBienThe(
            String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa,
            BigDecimal giaBan, Integer soLuongTon, MultipartFile fileAnh,
            List<String> mauSacs, List<String> trongLuongs, String mucCang,
            Map<String, MultipartFile> colorImageMap,
            Map<String, BigDecimal> variantPriceMap,
            Map<String, Integer> variantQuantityMap,
            Integer idNguoiDung, String remoteAddr) throws Exception {

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

        try {
            // 1. Validate dữ liệu đầu vào cơ bản
            String trimmedTen = (tenSanPham == null) ? "" : tenSanPham.trim();
            String sanitizedTen = org.jsoup.Jsoup.clean(trimmedTen, org.jsoup.safety.Safelist.none());
            if (sanitizedTen.isEmpty()) {
                throw new RuntimeException("Tên sản phẩm bắt buộc!");
            }
            if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
                throw new RuntimeException("Tên sản phẩm phải có độ dài từ 2 đến 100 ký tự!");
            }
            if (idDanhMuc == null || idDanhMuc < 1) {
                throw new RuntimeException("Vui lòng chọn danh mục hợp lệ!");
            }
            if (idThuongHieu == null || idThuongHieu < 1) {
                throw new RuntimeException("Vui lòng chọn thương hiệu hợp lệ!");
            }
            String sanitizedMoTa = "";
            if (moTa != null) {
                String trimmedMoTa = moTa.trim();
                sanitizedMoTa = org.jsoup.Jsoup.clean(trimmedMoTa, org.jsoup.safety.Safelist.none());
                if (sanitizedMoTa.length() > 2000) {
                    throw new RuntimeException("Mô tả sản phẩm không được vượt quá 2000 ký tự!");
                }
            }
            if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá bán phải lớn hơn 0 VNĐ!");
            }
            if (soLuongTon == null || soLuongTon < 0) {
                throw new RuntimeException("Số lượng kho không được âm!");
            }
            if (fileAnh == null || fileAnh.isEmpty()) {
                throw new RuntimeException("Hình ảnh sản phẩm là bắt buộc khi thêm mới!");
            }

            // 2. Validate và lọc các thuộc tính checkbox
            if (mauSacs == null || mauSacs.isEmpty()
                    || trongLuongs == null || trongLuongs.isEmpty()) {
                throw new RuntimeException("Vui lòng chọn ít nhất một màu sắc và một trọng lượng!");
            }
            String cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);

            Set<String> uniqueMauSacs = new LinkedHashSet<>(mauSacs);
            Set<String> uniqueTrongLuongs = new LinkedHashSet<>(trongLuongs);
            Set<String> uniqueMucCangs = Set.of(cleanMucCang);

            // 3. Giới hạn số lượng biến thể tối đa (Variant Generation Limit)
            int totalVariants = uniqueMauSacs.size() * uniqueTrongLuongs.size() * uniqueMucCangs.size();
            if (totalVariants > 100) {
                throw new RuntimeException("Số lượng biến thể được tạo ra vượt quá giới hạn cho phép (Tối đa 100 biến thể)! Hiện tại đang yêu cầu tạo: " + totalVariants);
            }

            // 4. File Upload Security & Validation (Ảnh chính)
            String secureFileName = saveImageSecurely(fileAnh, "chính của sản phẩm", uploadedFiles);

            // 5. Lưu ảnh màu sắc trước
            Map<String, String> imageByColor = new HashMap<>();
            if (colorImageMap != null) {
                for (Map.Entry<String, MultipartFile> entry : colorImageMap.entrySet()) {
                    String color = entry.getKey();
                    MultipartFile colorFile = entry.getValue();
                    if (colorFile != null && !colorFile.isEmpty()) {
                        String colorFileName = saveImageSecurely(colorFile, "màu " + color, uploadedFiles);
                        imageByColor.put(color, colorFileName);
                    }
                }
            }

            // 6. Lưu sản phẩm gốc
            SanPham sp = new SanPham();
            sp.setTenSanPham(sanitizedTen);
            sp.setMoTa(sanitizedMoTa);
            sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
            sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
            sp.setTrangThai("dang_ban");

            NhanVien creator = null;
            if (idNguoiDung != null) {
                creator = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
                if (creator == null) {
                    TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
                    if (tk != null && ("QL".equals(tk.getVaiTro()) || "NV".equals(tk.getVaiTro()))) {
                        creator = new NhanVien();
                        creator.setTaiKhoan(tk);
                        String namePrefix = tk.getUsername();
                        if (namePrefix.contains("@")) {
                            namePrefix = namePrefix.split("@")[0];
                        }
                        creator.setHoTenNv("Quản trị viên " + namePrefix);
                        creator.setChucVu("QL".equals(tk.getVaiTro()) ? "Quản lý" : "Nhân viên");
                        creator.setSoDienThoaiNv("0999999999");
                        creator.setNgayTao(java.time.LocalDateTime.now());
                        creator = nhanVienRepository.save(creator);
                    }
                }
            }
            if (creator == null) {
                List<NhanVien> listNV = nhanVienRepository.findAll();
                if (!listNV.isEmpty()) {
                    creator = listNV.get(0);
                } else {
                    List<TaiKhoan> staffAccounts = taiKhoanRepository.findByVaiTroIn(List.of("QL", "NV"));
                    if (!staffAccounts.isEmpty()) {
                        TaiKhoan tk = staffAccounts.get(0);
                        creator = new NhanVien();
                        creator.setTaiKhoan(tk);
                        String namePrefix = tk.getUsername();
                        if (namePrefix.contains("@")) {
                            namePrefix = namePrefix.split("@")[0];
                        }
                        creator.setHoTenNv("Quản trị viên " + namePrefix);
                        creator.setChucVu("QL".equals(tk.getVaiTro()) ? "Quản lý" : "Nhân viên");
                        creator.setSoDienThoaiNv("0999999999");
                        creator.setNgayTao(java.time.LocalDateTime.now());
                        creator = nhanVienRepository.save(creator);
                    } else {
                        throw new RuntimeException("Hệ thống chưa có nhân viên nào! Không thể tạo sản phẩm.");
                    }
                }
            }
            sp.setNhanVien(creator);
            sp = sanPhamRepository.save(sp);

            // 7. Tạo Cartesian Product và phòng chống trùng lặp
            Set<String> checkDuplicates = new HashSet<>();

            for (String mau : uniqueMauSacs) {
                for (String trong : uniqueTrongLuongs) {
                    for (String cang : uniqueMucCangs) {

                        String combKey = mau.toLowerCase().trim() + "_" + trong.toLowerCase().trim() + "_" + cang.toLowerCase().trim();
                        if (checkDuplicates.contains(combKey)) {
                            continue;
                        }
                        checkDuplicates.add(combKey);

                        // Lấy ảnh theo màu, nếu không có thì fallback sang ảnh chính
                        String variantFileName = imageByColor.get(mau);
                        if (variantFileName == null || variantFileName.isEmpty()) {
                            variantFileName = secureFileName;
                        }

                        String variantKey = mau.trim() + "_" + trong.trim() + "_" + cang.trim();
                        BigDecimal vPrice = (variantPriceMap != null && variantPriceMap.containsKey(variantKey)) ? variantPriceMap.get(variantKey) : giaBan;
                        Integer vQty = (variantQuantityMap != null && variantQuantityMap.containsKey(variantKey)) ? variantQuantityMap.get(variantKey) : soLuongTon;

                        if (vPrice == null || vPrice.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new RuntimeException("Giá bán của biến thể " + variantKey + " phải lớn hơn 0 VNĐ!");
                        }
                        if (vQty == null || vQty < 0) {
                            throw new RuntimeException("Số lượng kho của biến thể " + variantKey + " không được âm!");
                        }

                        SanPhamChiTiet spct = new SanPhamChiTiet();
                        spct.setSanPham(sp);
                        spct.setGiaBan(vPrice);
                        spct.setSoLuongTon(vQty);
                        spct.setMauSac(mau.trim());
                        spct.setTrongLuong(trong.trim());
                        spct.setMucCang(cang.trim());
                        spct.setHinhAnhSanPham(variantFileName);

                        sanPhamChiTietRepository.save(spct);
                    }
                }
            }

            // Ghi nhật ký kiểm toán
            String note = "Thêm sản phẩm '" + sp.getTenSanPham() + "' cùng " + totalVariants + " biến thể.";
            auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "INSERT", "", sp.getTenSanPham(), remoteAddr, note, creator.getChucVu());

        } catch (Exception e) {
            // fallback thủ công phòng hờ khi sync manager không chạy
            for (Path path : uploadedFiles) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ex) {
                    // ignore
                }
            }
            throw e;
        }
    }

    // --- HÀM CẬP NHẬT (CHỈ SỬA SẢN PHẨM GỐC) ---
    @Transactional
    public void capNhatSanPham(Integer idSanPham, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa, Integer idNguoiDung, String remoteAddr) {
        String trimmedTen = (tenSanPham == null) ? "" : tenSanPham.trim();
        String sanitizedTen = org.jsoup.Jsoup.clean(trimmedTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.isEmpty()) {
            throw new RuntimeException("Tên sản phẩm bắt buộc!");
        }
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new RuntimeException("Tên sản phẩm phải có độ dài từ 2 đến 100 ký tự!");
        }
        if (idDanhMuc == null || idDanhMuc < 1) {
            throw new RuntimeException("Vui lòng chọn danh mục hợp lệ!");
        }
        if (idThuongHieu == null || idThuongHieu < 1) {
            throw new RuntimeException("Vui lòng chọn thương hiệu hợp lệ!");
        }
        String sanitizedMoTa = "";
        if (moTa != null) {
            String trimmedMoTa = moTa.trim();
            sanitizedMoTa = org.jsoup.Jsoup.clean(trimmedMoTa, org.jsoup.safety.Safelist.none());
            if (sanitizedMoTa.length() > 2000) {
                throw new RuntimeException("Mô tả sản phẩm không được vượt quá 2000 ký tự!");
            }
        }

        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        String oldVal = "Ten: " + sp.getTenSanPham() + ", DanhMuc: " + sp.getDanhMuc().getId() + ", ThuongHieu: " + sp.getThuongHieu().getId();

        sp.setTenSanPham(sanitizedTen);
        sp.setMoTa(sanitizedMoTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sanPhamRepository.save(sp);

        String newVal = "Ten: " + sanitizedTen + ", DanhMuc: " + idDanhMuc + ", ThuongHieu: " + idThuongHieu;
        NhanVien creator = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
        String role = (creator != null) ? creator.getChucVu() : "UNKNOWN";
        auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "UPDATE", oldVal, newVal, remoteAddr, "Cập nhật thông tin sản phẩm gốc", role);
    }

    // --- HÀM XÓA MỀM ---
    @Transactional
    public void xoaSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        sp.setTrangThai("ngung_kinh_doanh");
        sanPhamRepository.save(sp);

        NhanVien creator = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
        String role = (creator != null) ? creator.getChucVu() : "UNKNOWN";
        auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "UPDATE", "trangThai: dang_ban", "trangThai: ngung_kinh_doanh", remoteAddr, "Ngừng kinh doanh sản phẩm (Xóa mềm)", role);
    }

    // --- HÀM MỞ BÁN LẠI ---
    @Transactional
    public void moBanLaiSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        sp.setTrangThai("dang_ban");
        sanPhamRepository.save(sp);

        NhanVien creator = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
        String role = (creator != null) ? creator.getChucVu() : "UNKNOWN";
        auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "UPDATE", "trangThai: ngung_kinh_doanh", "trangThai: dang_ban", remoteAddr, "Mở bán lại sản phẩm", role);
    }
}
