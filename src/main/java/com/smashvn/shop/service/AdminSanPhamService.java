package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

@Service
@RequiredArgsConstructor
public class AdminSanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final NhanVienRepository nhanVienRepository;

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
        if (!listNV.isEmpty()) sp.setNhanVien(listNV.get(0));
        
        sanPhamRepository.save(sp);
    }

    // --- HÀM THÊM MỚI CẢ SẢN PHẨM & TỰ ĐỘNG SINH BIẾN THỂ (NÂNG CẤP BẢO MẬT) ---
    @Transactional(rollbackFor = Exception.class)
    public void themSanPhamVaBienThe(
            String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa,
            BigDecimal giaBan, Integer soLuongTon, MultipartFile fileAnh,
            List<String> mauSacs, List<String> trongLuongs, List<String> mucCangs) throws Exception {
        
        // 1. Validate dữ liệu đầu vào cơ bản
        if (tenSanPham == null || tenSanPham.trim().length() < 3) {
            throw new RuntimeException("Tên sản phẩm bắt buộc và phải có ít nhất 3 ký tự!");
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
        if (mauSacs == null || mauSacs.isEmpty() || 
            trongLuongs == null || trongLuongs.isEmpty() || 
            mucCangs == null || mucCangs.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một màu sắc, một trọng lượng và một mức căng!");
        }

        // Loại bỏ trùng lặp nếu có từ danh sách checkbox
        Set<String> uniqueMauSacs = new LinkedHashSet<>(mauSacs);
        Set<String> uniqueTrongLuongs = new LinkedHashSet<>(trongLuongs);
        Set<String> uniqueMucCangs = new LinkedHashSet<>(mucCangs);

        // 3. Giới hạn số lượng biến thể tối đa (Variant Generation Limit)
        int totalVariants = uniqueMauSacs.size() * uniqueTrongLuongs.size() * uniqueMucCangs.size();
        if (totalVariants > 100) {
            throw new RuntimeException("Số lượng biến thể được tạo ra vượt quá giới hạn cho phép (Tối đa 100 biến thể)! Hiện tại đang yêu cầu tạo: " + totalVariants);
        }

        // 4. File Upload Security
        String origName = fileAnh.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf(".")).toLowerCase();
        }
        
        // Chỉ cho phép các định dạng an toàn
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
            throw new RuntimeException("Định dạng tệp không hợp lệ! Chỉ cho phép tải lên ảnh JPG, JPEG, PNG, WEBP.");
        }
        
        // Giới hạn dung lượng dưới 5MB
        if (fileAnh.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Kích thước hình ảnh quá lớn! Kích thước tối đa cho phép là 5MB.");
        }

        // Đổi tên ngẫu nhiên UUID để đảm bảo bảo mật & tránh trùng lặp đè tệp
        String secureFileName = UUID.randomUUID().toString() + ext;

        // Lưu ảnh ra thư mục vật lý an toàn
        Path uploadPath = Paths.get("uploads/product/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        try (InputStream inputStream = fileAnh.getInputStream()) {
            Files.copy(inputStream, uploadPath.resolve(secureFileName), StandardCopyOption.REPLACE_EXISTING);
        }

        // 5. Lưu sản phẩm gốc
        SanPham sp = new SanPham();
        sp.setTenSanPham(tenSanPham.trim());
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sp.setTrangThai("dang_ban");

        List<NhanVien> listNV = nhanVienRepository.findAll();
        if (!listNV.isEmpty()) {
            sp.setNhanVien(listNV.get(0));
        }
        sp = sanPhamRepository.save(sp);

        // 6. Tạo Cartesian Product và phòng chống trùng lặp
        Set<String> checkDuplicates = new HashSet<>();
        
        for (String mau : uniqueMauSacs) {
            for (String trong : uniqueTrongLuongs) {
                for (String cang : uniqueMucCangs) {
                    
                    String combKey = mau.toLowerCase().trim() + "_" + trong.toLowerCase().trim() + "_" + cang.toLowerCase().trim();
                    if (checkDuplicates.contains(combKey)) {
                        continue; // Bỏ qua tổ hợp trùng lặp trong đợt sinh này
                    }
                    checkDuplicates.add(combKey);

                    SanPhamChiTiet spct = new SanPhamChiTiet();
                    spct.setSanPham(sp);
                    spct.setGiaBan(giaBan);
                    spct.setSoLuongTon(soLuongTon);
                    spct.setMauSac(mau.trim());
                    spct.setTrongLuong(trong.trim());
                    spct.setMucCang(cang.trim());
                    spct.setHinhAnhSanPham(secureFileName);

                    sanPhamChiTietRepository.save(spct);
                }
            }
        }
    }

    // --- HÀM CẬP NHẬT (CHỈ SỬA SẢN PHẨM GỐC) ---
    @Transactional
    public void capNhatSanPham(Integer idSanPham, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sanPhamRepository.save(sp);
    }

    // --- HÀM XÓA MỀM ---
    @Transactional
    public void xoaSanPham(Integer idSanPham) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        sp.setTrangThai("ngung_kinh_doanh");
        sanPhamRepository.save(sp);
    }
}