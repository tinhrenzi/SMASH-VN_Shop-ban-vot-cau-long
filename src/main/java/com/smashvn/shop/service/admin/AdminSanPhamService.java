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

import com.smashvn.shop.constant.DanhMucIds;
import com.smashvn.shop.constant.SanPhamAttributeConfig;
import com.smashvn.shop.dto.BienTheCreateRequest;
import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.DanhMuc;
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

    // --- HÀM THÊM MỚI CẢ SẢN PHẨM & TỰ ĐỘNG SINH BIẾN THỂ (NÂNG CẤP BẢO MẬT & KIỂM TOÁN DÙNG DTO) ---
    @Transactional(rollbackFor = Exception.class)
    public void themSanPhamVaBienThe(SanPhamCreateRequest request, Integer idNguoiDung, String remoteAddr) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ!");
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

        try {
            // 1. Validate idDanhMuc null trước khi gọi findById
            if (request.getIdDanhMuc() == null) {
                throw new IllegalArgumentException("Vui lòng chọn danh mục hợp lệ!");
            }

            DanhMuc dm = danhMucRepository.findById(request.getIdDanhMuc())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));

            if (Boolean.FALSE.equals(dm.getTrangThai())) {
                throw new IllegalArgumentException("Danh mục đã ngừng hoạt động");
            }

            int idDanhMuc = dm.getId();
            if (!DanhMucIds.isSupported(idDanhMuc)) {
                throw new IllegalArgumentException("Danh mục chưa được cấu hình thuộc tính");
            }

            // Validate Tên sản phẩm & Thương hiệu
            String trimmedTen = (request.getTenSanPham() == null) ? "" : request.getTenSanPham().trim();
            String sanitizedTen = org.jsoup.Jsoup.clean(trimmedTen, org.jsoup.safety.Safelist.none());
            if (sanitizedTen.isEmpty()) {
                throw new IllegalArgumentException("Tên sản phẩm bắt buộc!");
            }
            if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
                throw new IllegalArgumentException("Tên sản phẩm phải có độ dài từ 2 đến 100 ký tự!");
            }
            if (request.getIdThuongHieu() == null || request.getIdThuongHieu() < 1) {
                throw new IllegalArgumentException("Vui lòng chọn thương hiệu hợp lệ!");
            }

            String sanitizedMoTa = "";
            if (request.getMoTa() != null) {
                String trimmedMoTa = request.getMoTa().trim();
                sanitizedMoTa = org.jsoup.Jsoup.clean(trimmedMoTa, org.jsoup.safety.Safelist.none());
                if (sanitizedMoTa.length() > 2000) {
                    throw new IllegalArgumentException("Mô tả sản phẩm không được vượt quá 2000 ký tự!");
                }
            }

            if (request.getFileAnh() == null || request.getFileAnh().isEmpty()) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc khi thêm mới!");
            }

            // 2. Validate và lưu ảnh chính + ảnh theo màu (mỗi colorIndex chỉ upload/copy 1 lần)
            String secureMainFileName = saveImageSecurely(request.getFileAnh(), "chính của sản phẩm", uploadedFiles);

            List<MultipartFile> colorImages = request.getColorImages();
            Map<String, Integer> colorIndexByColor = new LinkedHashMap<>();
            List<BienTheCreateRequest> rawVariants = request.getVariants() != null ? request.getVariants() : new ArrayList<>();

            if (idDanhMuc != DanhMucIds.HOP_CAU) {
                if (rawVariants.isEmpty()) {
                    throw new IllegalArgumentException("Danh sách biến thể không được để trống!");
                }
                if (rawVariants.size() > 100) {
                    throw new IllegalArgumentException("Số lượng biến thể được tạo ra vượt quá giới hạn cho phép (Tối đa 100 biến thể)!");
                }

                // Kiểm tra sự thống nhất colorIndex theo màu
                for (BienTheCreateRequest v : rawVariants) {
                    String normMau = normalizeText(v.getMauSac());
                    if (normMau == null) {
                        throw new IllegalArgumentException("Màu sắc của biến thể không được để trống!");
                    }
                    Integer idx = v.getColorIndex();
                    if (colorIndexByColor.containsKey(normMau)) {
                        if (!Objects.equals(colorIndexByColor.get(normMau), idx)) {
                            throw new IllegalArgumentException("Tất cả biến thể cùng màu '" + normMau + "' phải dùng thống nhất một chỉ số ảnh màu (colorIndex)!");
                        }
                    } else {
                        colorIndexByColor.put(normMau, idx);
                    }
                }
            }

            // Upload các file ảnh theo colorIndex duy nhất (1 file chỉ upload 1 lần)
            Map<Integer, String> savedColorImages = new HashMap<>();
            for (Integer cIdx : new HashSet<>(colorIndexByColor.values())) {
                if (cIdx != null) {
                    if (cIdx < 0 || colorImages == null || cIdx >= colorImages.size()) {
                        throw new IllegalArgumentException("Chỉ số ảnh màu (colorIndex) vượt quá phạm vi danh sách ảnh!");
                    }
                    MultipartFile colorFile = colorImages.get(cIdx);
                    if (colorFile != null && !colorFile.isEmpty()) {
                        String colorFileName = saveImageSecurely(colorFile, "màu index " + cIdx, uploadedFiles);
                        savedColorImages.put(cIdx, colorFileName);
                    }
                }
            }

            // 3. Tạo và lưu SanPham gốc
            SanPham sp = new SanPham();
            sp.setTenSanPham(sanitizedTen);
            sp.setMoTa(sanitizedMoTa);
            sp.setDanhMuc(dm);
            sp.setThuongHieu(thuongHieuRepository.findById(request.getIdThuongHieu())
                    .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại")));
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
                        throw new IllegalArgumentException("Hệ thống chưa có nhân viên nào! Không thể tạo sản phẩm.");
                    }
                }
            }
            sp.setNhanVien(creator);
            sp = sanPhamRepository.save(sp);

            // 4. Xử lý lưu các biến thể SanPhamChiTiet
            Set<String> checkDuplicates = new HashSet<>();
            int savedCount = 0;

            if (idDanhMuc == DanhMucIds.HOP_CAU) {
                // Hộp cầu: backend tự sinh đúng 1 biến thể mặc định
                BigDecimal gNhap = request.getGiaNhapDefault() != null ? request.getGiaNhapDefault() : BigDecimal.ZERO;
                if (gNhap.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Giá nhập không được nhỏ hơn 0!");
                }
                BigDecimal gBan = request.getGiaBanDefault();
                if (gBan == null || gBan.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Giá bán phải lớn hơn 0 VNĐ!");
                }
                Integer sTon = request.getSoLuongTonDefault() != null ? request.getSoLuongTonDefault() : 0;
                if (sTon < 0) {
                    throw new IllegalArgumentException("Số lượng tồn kho không được âm!");
                }

                SanPhamChiTiet spct = new SanPhamChiTiet();
                spct.setSanPham(sp);
                spct.setMauSac("Mặc định");
                spct.setTrongLuong(null);
                spct.setKichThuoc(null);
                spct.setMucCang(null);
                spct.setGiaNhap(gNhap);
                spct.setGiaBan(gBan);
                spct.setSoLuongTon(sTon);
                spct.setTrangThai("dang_ban");
                spct.setHinhAnhSanPham(secureMainFileName);
                sanPhamChiTietRepository.save(spct);
                savedCount = 1;
            } else {
                String cleanMucCangChung = RacketSpecUtils.sanitizeRecommendedTension(request.getMucCang());

                for (BienTheCreateRequest v : rawVariants) {
                    String normMau = normalizeText(v.getMauSac());
                    String normTrong = null;
                    String normKich = null;
                    String normCang = null;

                    if (idDanhMuc == DanhMucIds.VOT) {
                        normTrong = normalizeCode(v.getTrongLuong());
                        if (normTrong == null || !SanPhamAttributeConfig.ALLOWED_TRONG_LUONG_VOT.contains(normTrong)) {
                            throw new IllegalArgumentException("Trọng lượng vợt không hợp lệ (Chỉ chấp nhận: 3U, 4U, 5U)!");
                        }
                        normKich = null; // Force null
                        normCang = cleanMucCangChung;
                    } else if (idDanhMuc == DanhMucIds.GIAY) {
                        normTrong = null; // Force null
                        normKich = normalizeCode(v.getKichThuoc());
                        if (normKich == null || !SanPhamAttributeConfig.ALLOWED_KICH_THUOC_GIAY.contains(normKich)) {
                            throw new IllegalArgumentException("Kích thước giày không hợp lệ (Chỉ chấp nhận: 36 đến 46)!");
                        }
                        normCang = null; // Force null
                    } else if (idDanhMuc == DanhMucIds.TRANG_PHUC) {
                        normTrong = null; // Force null
                        normKich = normalizeCode(v.getKichThuoc());
                        if (normKich == null || !SanPhamAttributeConfig.ALLOWED_KICH_THUOC_TRANG_PHUC.contains(normKich)) {
                            throw new IllegalArgumentException("Kích thước trang phục không hợp lệ (Chỉ chấp nhận: XS đến 3XL)!");
                        }
                        normCang = null; // Force null
                    } else if (idDanhMuc == DanhMucIds.CUOC || idDanhMuc == DanhMucIds.BALO || idDanhMuc == DanhMucIds.QUAN_CAN || idDanhMuc == DanhMucIds.BANG_QUAN) {
                        normTrong = null; // Force null
                        normKich = null; // Force null
                        normCang = null; // Force null
                    }

                    // Kiểm tra tài chính & kho
                    BigDecimal gNhap = v.getGiaNhap() != null ? v.getGiaNhap() : (request.getGiaNhapDefault() != null ? request.getGiaNhapDefault() : BigDecimal.ZERO);
                    if (gNhap.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Giá nhập của biến thể không được nhỏ hơn 0!");
                    }
                    BigDecimal gBan = v.getGiaBan() != null ? v.getGiaBan() : request.getGiaBanDefault();
                    if (gBan == null || gBan.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Giá bán của biến thể phải lớn hơn 0 VNĐ!");
                    }
                    Integer sTon = v.getSoLuongTon() != null ? v.getSoLuongTon() : (request.getSoLuongTonDefault() != null ? request.getSoLuongTonDefault() : 0);
                    if (sTon < 0) {
                        throw new IllegalArgumentException("Số lượng kho của biến thể không được âm!");
                    }

                    // Kiểm tra trùng lặp tổ hợp duy nhất
                    String combKey = normMau.toLowerCase() + "|" + (normTrong != null ? normTrong.toLowerCase() : "") + "|" + (normKich != null ? normKich.toLowerCase() : "");
                    if (checkDuplicates.contains(combKey)) {
                        throw new IllegalArgumentException("Có biến thể bị trùng lặp trong danh sách nhập!");
                    }
                    checkDuplicates.add(combKey);

                    // Chọn ảnh cho biến thể
                    Integer cIdx = v.getColorIndex();
                    String variantFileName = (cIdx != null && savedColorImages.containsKey(cIdx)) ? savedColorImages.get(cIdx) : secureMainFileName;

                    SanPhamChiTiet spct = new SanPhamChiTiet();
                    spct.setSanPham(sp);
                    spct.setGiaNhap(gNhap);
                    spct.setGiaBan(gBan);
                    spct.setSoLuongTon(sTon);
                    spct.setMauSac(normMau);
                    spct.setTrongLuong(normTrong);
                    spct.setKichThuoc(normKich);
                    spct.setMucCang(normCang);
                    spct.setTrangThai("dang_ban");
                    spct.setHinhAnhSanPham(variantFileName);

                    sanPhamChiTietRepository.save(spct);
                    savedCount++;
                }
            }

            // Ghi nhật ký kiểm toán
            String note = "Thêm sản phẩm '" + sp.getTenSanPham() + "' cùng " + savedCount + " biến thể.";
            auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "INSERT", "", sp.getTenSanPham(), remoteAddr, note, creator.getChucVu());

        } catch (Exception e) {
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

    private String normalizeText(String s) {
        if (s == null) return null;
        String trimmed = s.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCode(String s) {
        String text = normalizeText(s);
        return text == null ? null : text.toUpperCase();
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
