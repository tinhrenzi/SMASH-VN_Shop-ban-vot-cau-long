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
import java.util.List;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;

@Service
@RequiredArgsConstructor
public class AdminBienTheService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    // 1. Lấy danh sách biến thể theo ID Sản phẩm gốc
    public List<SanPhamChiTiet> layDanhSachBienThe(Integer idSanPham) {
        return sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
    }

    // 2. Thêm biến thể mới (Có xử lý upload ảnh ra ngoài thư mục uploads)
    @Transactional
    public void themBienThe(Integer idSanPham, BigDecimal giaBan, Integer soLuongTon,
                            String mauSac, String trongLuong, String mucCang, MultipartFile fileAnh) throws Exception {
        
        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm gốc"));
        
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);
        spct.setMauSac(mauSac);
        spct.setTrongLuong(trongLuong);
        spct.setMucCang(mucCang);

        // Xử lý lưu ảnh
        if (fileAnh != null && !fileAnh.isEmpty()) {
            String tenFileAnh = System.currentTimeMillis() + "_" + fileAnh.getOriginalFilename();
            Path uploadPath = Paths.get("uploads/product/"); // Lưu ra thư mục ảo an toàn
            
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            
            try (InputStream inputStream = fileAnh.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(tenFileAnh), StandardCopyOption.REPLACE_EXISTING);
                spct.setHinhAnhSanPham(tenFileAnh);
            }
        } else {
            spct.setHinhAnhSanPham("default.jpg"); // Ảnh mặc định nếu admin quên up
        }
        
        sanPhamChiTietRepository.save(spct);
    }

    // 3. Xóa biến thể
    @Transactional
    public void xoaBienThe(Integer idBienThe) {
        sanPhamChiTietRepository.deleteById(idBienThe);
    }
    
 // Thêm hàm lấy 1 biến thể duy nhất để đổ lên Form sửa
    public SanPhamChiTiet layBienTheTheoId(Integer idBienThe) {
        return sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể này"));
    }

    // Thêm hàm Cập nhật Biến thể
    @Transactional
    public void capNhatBienThe(Integer idBienThe, BigDecimal giaBan, Integer soLuongTon,
                               String mauSac, String trongLuong, String mucCang, MultipartFile fileAnh) throws Exception {
        
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể để sửa"));

        spct.setGiaBan(giaBan);
        spct.setSoLuongTon(soLuongTon);
        spct.setMauSac(mauSac);
        spct.setTrongLuong(trongLuong);
        spct.setMucCang(mucCang);

        // Chỉ cập nhật ảnh nếu Admin có chọn file mới (nếu không thì giữ nguyên ảnh cũ)
        if (fileAnh != null && !fileAnh.isEmpty()) {
            String tenFileAnh = System.currentTimeMillis() + "_" + fileAnh.getOriginalFilename();
            Path uploadPath = Paths.get("uploads/product/"); 
            
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            
            try (InputStream inputStream = fileAnh.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(tenFileAnh), StandardCopyOption.REPLACE_EXISTING);
                spct.setHinhAnhSanPham(tenFileAnh);
            }
        }
        
        sanPhamChiTietRepository.save(spct);
    }
}