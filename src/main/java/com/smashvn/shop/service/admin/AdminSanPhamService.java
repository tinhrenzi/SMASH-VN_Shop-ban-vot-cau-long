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
import com.smashvn.shop.dto.AttributeValueRequest;
import com.smashvn.shop.dto.BienTheCreateRequest;
import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
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
    private final ThuocTinhRepository thuocTinhRepository;
    private final AuditService auditService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    private String saveImageSecurely(MultipartFile file, String fieldName, List<Path> uploadedFiles) throws Exception {
        if (file == null || file.isEmpty()) return null;

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file " + fieldName + " vượt quá giới hạn 5MB!");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File " + fieldName + " không hợp lệ!");
        }

        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Định dạng file " + fieldName + " không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP)!");
        }

        String safeFileName = UUID.randomUUID().toString() + ext;
        Path targetUploadDir = Paths.get(uploadPathConfig, "product").toAbsolutePath().normalize();

        if (!Files.exists(targetUploadDir)) {
            Files.createDirectories(targetUploadDir);
        }

        Path targetFilePath = targetUploadDir.resolve(safeFileName).normalize();
        if (!targetFilePath.startsWith(targetUploadDir)) {
            throw new IllegalArgumentException("Đường dẫn lưu file không hợp lệ!");
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

    // --- HÀM THÊM MỚI CẢ SẢN PHẨM & TỰ ĐỘNG SINH BIẾN THỂ ---
    @Transactional(rollbackFor = Exception.class)
    public void themSanPhamVaBienThe(SanPhamCreateRequest request, Integer idNguoiDung, String remoteAddr) throws Exception {
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
                                    log.error("Xóa file thất bại sau rollback: {}", path, e);
                                }
                            }
                        }
                    }
                }
            );
        }

        try {
            if (request == null) {
                throw new IllegalArgumentException("Dữ liệu không hợp lệ");
            }

            Integer idDanhMuc = request.getIdDanhMuc();
            if (idDanhMuc == null || idDanhMuc < 1) {
                throw new IllegalArgumentException("Vui lòng chọn danh mục hợp lệ!");
            }
            DanhMuc dm = danhMucRepository.findById(idDanhMuc)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
            if (Boolean.FALSE.equals(dm.getTrangThai())) {
                throw new IllegalArgumentException("Danh mục này đã bị ẩn, không thể thêm sản phẩm mới vào danh mục!");
            }

            com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromIdOrName(dm, idDanhMuc);
            if (catType == com.smashvn.shop.constant.CategoryType.OTHER && !DanhMucIds.isSupported(idDanhMuc)) {
                throw new IllegalArgumentException("Danh mục chưa được cấu hình thuộc tính");
            }

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
            ThuongHieu th = thuongHieuRepository.findById(request.getIdThuongHieu())
                    .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại"));
            if (Boolean.FALSE.equals(th.getTrangThai())) {
                throw new IllegalArgumentException("Hãng/thương hiệu này đã bị ẩn, không thể thêm sản phẩm mới vào thương hiệu này!");
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

            String secureMainFileName = saveImageSecurely(request.getFileAnh(), "chính của sản phẩm", uploadedFiles);

            List<MultipartFile> colorImages = request.getColorImages();
            Map<String, Integer> colorIndexByColor = new LinkedHashMap<>();
            List<BienTheCreateRequest> rawVariants = request.getVariants() != null ? request.getVariants() : new ArrayList<>();

            if (catType != com.smashvn.shop.constant.CategoryType.HOP_CAU) {
                if (rawVariants.isEmpty()) {
                    throw new IllegalArgumentException("Danh sách biến thể không được để trống!");
                }
                if (rawVariants.size() > 100) {
                    throw new IllegalArgumentException("Số lượng biến thể được tạo ra vượt quá giới hạn cho phép (Tối đa 100 biến thể)!");
                }

                for (BienTheCreateRequest v : rawVariants) {
                    String normMau = normalizeText(v.getMauSac());
                    if (normMau == null) {
                        normMau = "Mặc định";
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
                        creator.setSoDienThoaiNv("0000000000");
                        creator = nhanVienRepository.save(creator);
                    }
                }
            }

            if (creator == null) {
                List<NhanVien> listNV = nhanVienRepository.findAll();
                if (!listNV.isEmpty()) {
                    creator = listNV.get(0);
                } else {
                    throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên thao tác!");
                }
            }
            sp.setNhanVien(creator);
            sp = sanPhamRepository.save(sp);

            Set<String> checkDuplicates = new HashSet<>();
            int savedCount = 0;

            if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
                BigDecimal gNhap = request.getGiaNhapDefault() != null ? request.getGiaNhapDefault() : BigDecimal.ZERO;
                BigDecimal gBan = request.getGiaBanDefault();
                if (gBan == null || gBan.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Giá bán phải lớn hơn 0 VNĐ!");
                }
                Integer sTon = request.getSoLuongTonDefault() != null ? request.getSoLuongTonDefault() : 0;

                SanPhamChiTiet spct = new SanPhamChiTiet();
                spct.setSanPham(sp);
                spct.setGiaNhap(gNhap);
                spct.setGiaBan(gBan);
                spct.setSoLuongTon(sTon);
                spct.setTrangThai("dang_ban");
                spct.setHinhAnhSanPham(secureMainFileName);

                saveVariantAttribute(spct, "Màu sắc", "Mặc định");
                sanPhamChiTietRepository.save(spct);
                savedCount = 1;
            } else {
                String cleanMucCangChung = RacketSpecUtils.sanitizeRecommendedTension(request.getMucCang());

                for (BienTheCreateRequest v : rawVariants) {
                    String normMau = normalizeText(v.getMauSac());
                    if (normMau == null) normMau = "Mặc định";
                    String normTrong = null;
                    String normKich = null;
                    String normCang = null;

                    if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        normTrong = normalizeCode(v.getTrongLuong());
                        normCang = cleanMucCangChung;
                    } else if (catType == com.smashvn.shop.constant.CategoryType.GIAY || catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC) {
                        normKich = normalizeCode(v.getKichThuoc());
                    }

                    BigDecimal gNhap = v.getGiaNhap() != null ? v.getGiaNhap() : (request.getGiaNhapDefault() != null ? request.getGiaNhapDefault() : BigDecimal.ZERO);
                    BigDecimal gBan = v.getGiaBan() != null ? v.getGiaBan() : request.getGiaBanDefault();
                    if (gBan == null || gBan.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Giá bán của biến thể phải lớn hơn 0 VNĐ!");
                    }
                    Integer sTon = v.getSoLuongTon() != null ? v.getSoLuongTon() : (request.getSoLuongTonDefault() != null ? request.getSoLuongTonDefault() : 0);

                    String combKey = normMau.toLowerCase() + "|" + (normTrong != null ? normTrong.toLowerCase() : "") + "|" + (normKich != null ? normKich.toLowerCase() : "");
                    if (checkDuplicates.contains(combKey)) {
                        throw new IllegalArgumentException("Có biến thể bị trùng lặp trong danh sách nhập!");
                    }
                    checkDuplicates.add(combKey);

                    Integer cIdx = v.getColorIndex();
                    String variantFileName = (cIdx != null && savedColorImages.containsKey(cIdx)) ? savedColorImages.get(cIdx) : secureMainFileName;

                    SanPhamChiTiet spct = new SanPhamChiTiet();
                    spct.setSanPham(sp);
                    spct.setGiaNhap(gNhap);
                    spct.setGiaBan(gBan);
                    spct.setSoLuongTon(sTon);
                    spct.setTrangThai("dang_ban");
                    spct.setHinhAnhSanPham(variantFileName);

                    if (v.getAttributes() != null && !v.getAttributes().isEmpty()) {
                        for (AttributeValueRequest attrReq : v.getAttributes()) {
                            if (attrReq.getAttributeName() != null && attrReq.getValue() != null && !attrReq.getValue().isBlank()) {
                                saveVariantAttribute(spct, attrReq.getAttributeName(), attrReq.getValue());
                            }
                        }
                    } else {
                        saveVariantAttribute(spct, "Màu sắc", normMau);
                        saveVariantAttribute(spct, "Trọng lượng", normTrong);
                        saveVariantAttribute(spct, "Kích thước", normKich);
                        saveVariantAttribute(spct, "Sức căng", normCang);
                    }

                    sanPhamChiTietRepository.save(spct);
                    savedCount++;
                }
            }

            String role = (creator != null && creator.getChucVu() != null) ? creator.getChucVu() : "Quản trị viên";
            String note = "Thêm sản phẩm '" + sp.getTenSanPham() + "' cùng " + savedCount + " biến thể.";
            auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "INSERT", "", sp.getTenSanPham(), remoteAddr, note, role);

        } catch (Exception e) {
            for (Path path : uploadedFiles) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ex) {
                    log.error("Xóa file thất bại sau exception: {}", path, ex);
                }
            }
            throw e;
        }
    }

    private void saveVariantAttribute(SanPhamChiTiet spct, String tenThuocTinh, String giaTri) {
        if (giaTri == null || giaTri.isBlank()) return;
        ThuocTinh tt = thuocTinhRepository.findByTenThuocTinhIgnoreCase(tenThuocTinh)
                .orElseGet(() -> thuocTinhRepository.save(ThuocTinh.builder()
                        .tenThuocTinh(tenThuocTinh.trim())
                        .trangThai(true)
                        .build()));
        SanPhamChiTietThuocTinh attVal = SanPhamChiTietThuocTinh.builder()
                .sanPhamChiTiet(spct)
                .thuocTinh(tt)
                .giaTri(giaTri.trim())
                .build();
        if (spct.getSanPhamChiTietThuocTinhs() == null) {
            spct.setSanPhamChiTietThuocTinhs(new java.util.LinkedHashSet<>());
        }
        spct.getSanPhamChiTietThuocTinhs().add(attVal);
    }

    private String normalizeText(String str) {
        if (str == null) return null;
        String s = str.trim();
        return s.isEmpty() ? null : s;
    }

    private String normalizeCode(String str) {
        if (str == null) return null;
        String s = str.trim().toUpperCase();
        return s.isEmpty() ? null : s;
    }

    public List<SanPham> layTatCaSanPham() {
        return sanPhamRepository.findAll();
    }

    public SanPham layTheoId(Integer id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm id: " + id));
    }

    @Transactional
    public void capNhatSanPham(Integer id, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        capNhatSanPham(id, tenSanPham, idDanhMuc, idThuongHieu, moTa, null, null);
    }

    @Transactional
    public void capNhatSanPham(Integer id, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = layTheoId(id);
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sanPhamRepository.save(sp);
    }

    @Transactional
    public void xoaSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        ngungKinhDoanhSanPham(idSanPham, idNguoiDung, remoteAddr);
    }

    @Transactional
    public void ngungKinhDoanhSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm id: " + idSanPham));
        sp.setTrangThai("ngung_kinh_doanh");
        sanPhamRepository.save(sp);
    }

    @Transactional
    public void moBanLaiSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm id: " + idSanPham));
        sp.setTrangThai("dang_ban");
        sanPhamRepository.save(sp);
    }
}
