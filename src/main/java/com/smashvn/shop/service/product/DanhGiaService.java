package com.smashvn.shop.service.product;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.entity.DanhGiaAnh;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.dao.DanhGiaDAO;
import com.smashvn.shop.repository.DanhGiaAnhRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.service.common.FileStorageService;
import com.smashvn.shop.util.ProfanityFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DanhGiaService {

    private final DanhGiaDAO danhGiaDAO;
    private final DanhGiaAnhRepository danhGiaAnhRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final FileStorageService fileStorageService;
    private final ProfanityFilter profanityFilter;

    /**
     * Lấy danh sách đánh giá chưa xóa của một sản phẩm
     */
    public List<DanhGia> layDanhSachDanhGiaTheoSanPham(Integer sanPhamId) {
        return danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(sanPhamId);
    }

    /**
     * Lấy toàn bộ danh sách đánh giá của hệ thống (cho Admin)
     */
    public List<DanhGia> layTatCaDanhGia() {
        return danhGiaDAO.findAllByOrderByNgayDanhGiaDesc();
    }

    /**
     * Kiểm tra xem tài khoản đã mua sản phẩm này chưa
     */
    public boolean daMuaSanPham(Integer idTaiKhoan, Integer idSanPham) {
        if (idTaiKhoan == null || idSanPham == null) {
            return false;
        }
        return hoaDonChiTietRepository.hasPurchasedProduct(idTaiKhoan, idSanPham);
    }

    /**
     * Kiểm tra xem tài khoản đã đánh giá sản phẩm này chưa
     */
    public Optional<DanhGia> layDanhGiaDaCo(Integer idTaiKhoan, Integer idSanPham) {
        if (idTaiKhoan == null || idSanPham == null) {
            return Optional.empty();
        }
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            return Optional.empty();
        }
        return danhGiaDAO.findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(kh.getId(), idSanPham);
    }

    /**
     * Thêm mới hoặc Cập nhật đánh giá của khách hàng
     */
    @Transactional(rollbackFor = Exception.class)
    public void themHoacCapNhatDanhGia(Integer idTaiKhoan, Integer idSanPham, Integer soSao, String binhLuan, List<MultipartFile> files) throws Exception {
        // 1. Kiểm tra sự tồn tại và trạng thái sản phẩm (Chặn sản phẩm không hoạt động)
        SanPham sanPham = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm này!"));
        if (sanPham.getTrangThai() == null || !"dang_ban".equals(sanPham.getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm này hiện không còn hỗ trợ đánh giá.");
        }

        // 2. Lấy thông tin Khách Hàng
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập tài khoản khách hàng để thực hiện đánh giá.");
        }

        // 3. Kiểm tra lịch sử mua hàng thành công
        boolean daMua = hoaDonChiTietRepository.hasPurchasedProduct(idTaiKhoan, idSanPham);
        if (!daMua) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công.");
        }

        // 4. Kiểm tra Spam Rate Limit (Cấm spam liên tục)
        // Check 30s giữa 2 lần đánh giá liên tiếp
        Optional<DanhGia> latestOpt = danhGiaDAO.findTopByKhachHang_TaiKhoan_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(idTaiKhoan);
        if (latestOpt.isPresent()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime latestTime = latestOpt.get().getNgayCapNhat() != null ? latestOpt.get().getNgayCapNhat() : latestOpt.get().getNgayDanhGia();
            long seconds = Duration.between(latestTime, now).getSeconds();
            if (seconds < 30) {
                throw new IllegalArgumentException("Bạn gửi yêu cầu quá nhanh! Vui lòng đợi ít nhất 30 giây giữa các lần đánh giá.");
            }
        }
        // Check 5 đánh giá trong 1 giờ
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourCount = danhGiaDAO.countByKhachHang_TaiKhoan_IdAndDaXoaFalseAndNgayDanhGiaAfter(idTaiKhoan, oneHourAgo);
        if (hourCount >= 5) {
            throw new IllegalArgumentException("Bạn đã vượt quá số lượng đánh giá cho phép (tối đa 5 đánh giá mỗi giờ).");
        }

        // 5. Lọc từ tục tĩu
        String filteredComment = profanityFilter.filter(binhLuan);

        // Tìm đánh giá cũ của khách hàng cho sản phẩm này
        Optional<DanhGia> existingOpt = danhGiaDAO.findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(kh.getId(), idSanPham);
        List<String> uploadedFileNames = new ArrayList<>();

        try {
            if (existingOpt.isPresent()) {
                // CASE CHỈNH SỬA / GHI ĐÈ ĐÁNH GIÁ CŨ
                DanhGia dg = existingOpt.get();
                boolean starChanged = !dg.getSoSao().equals(soSao);
                
                dg.setSoSao(soSao);
                dg.setBinhLuan(filteredComment);
                dg.setNgayCapNhat(LocalDateTime.now());

                // Xử lý ảnh đính kèm cũ
                boolean hasNewUpload = false;
                if (files != null) {
                    for (MultipartFile file : files) {
                        if (file != null && !file.isEmpty()) {
                            hasNewUpload = true;
                            break;
                        }
                    }
                }

                if (hasNewUpload) {
                    // Xóa ảnh cũ trên đĩa vật lý
                    for (DanhGiaAnh oldAnh : dg.getDanhSachAnh()) {
                        fileStorageService.deleteImage(oldAnh.getDuongDan(), "reviews");
                    }
                    // Dọn danh sách trong DB
                    dg.getDanhSachAnh().clear();

                    // Tải ảnh mới lên
                    uploadedFileNames = fileStorageService.saveReviewImages(files);
                    for (String name : uploadedFileNames) {
                        DanhGiaAnh anh = DanhGiaAnh.builder()
                                .danhGia(dg)
                                .duongDan(name)
                                .ngayTao(LocalDateTime.now())
                                .build();
                        dg.getDanhSachAnh().add(anh);
                    }
                }

                danhGiaDAO.save(dg);

                // Cập nhật Cache Rating của sản phẩm nếu số sao thay đổi
                if (starChanged) {
                    updateProductRatingStats(idSanPham);
                }
            } else {
                // CASE GỬI ĐÁNH GIÁ MỚI LẦN ĐẦU
                DanhGia dg = DanhGia.builder()
                        .khachHang(kh)
                        .sanPham(sanPham)
                        .soSao(soSao)
                        .binhLuan(filteredComment)
                        .ngayDanhGia(LocalDateTime.now())
                        .daXoa(false)
                        .anBinhLuan(false)
                        .anHinhAnh(false)
                        .build();

                // Lưu ảnh đính kèm nếu có
                boolean hasUpload = false;
                if (files != null) {
                    for (MultipartFile file : files) {
                        if (file != null && !file.isEmpty()) {
                            hasUpload = true;
                            break;
                        }
                    }
                }

                if (hasUpload) {
                    uploadedFileNames = fileStorageService.saveReviewImages(files);
                    for (String name : uploadedFileNames) {
                        DanhGiaAnh anh = DanhGiaAnh.builder()
                                .danhGia(dg)
                                .duongDan(name)
                                .ngayTao(LocalDateTime.now())
                                .build();
                        dg.getDanhSachAnh().add(anh);
                    }
                }

                danhGiaDAO.save(dg);

                // Cập nhật Cache Rating của sản phẩm khi có review mới
                updateProductRatingStats(idSanPham);
            }
        } catch (Exception e) {
            // Rollback an toàn: Dọn dẹp tệp tin mồ côi vừa lưu trên đĩa nếu ghi DB thất bại
            log.error("[ROLLBACK_CLEANUP] Database transaction failed. Cleaning up uploaded review files.");
            for (String name : uploadedFileNames) {
                fileStorageService.deleteImage(name, "reviews");
            }
            throw e; // ném lại ngoại lệ để Spring Security / Controller xử lý và rollback
        }
    }

    /**
     * Tính toán điểm rating trung bình và tổng số lượng đánh giá để cache vào bảng SanPham
     */
    public void updateProductRatingStats(Integer idSanPham) {
        List<DanhGia> activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(idSanPham);
        int soDanhGia = activeReviews.size();
        double diemTrungBinh = activeReviews.stream()
                .mapToInt(DanhGia::getSoSao)
                .average()
                .orElse(0.0);

        // Làm tròn 1 chữ số thập phân
        diemTrungBinh = Math.round(diemTrungBinh * 10.0) / 10.0;

        SanPham sp = sanPhamRepository.findById(idSanPham).orElse(null);
        if (sp != null) {
            sp.setSoDanhGia(soDanhGia);
            sp.setDiemTrungBinh(diemTrungBinh);
            sanPhamRepository.save(sp);
            log.info("[CACHE_RATING] Updated stats for product {}: {} reviews, avg {} stars", idSanPham, soDanhGia, diemTrungBinh);
        }
    }

    /**
     * Ẩn Bình Luận (Soft Moderation)
     */
    @Transactional
    public void anBinhLuan(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnBinhLuan(true);
        dg.setNguoiAnBinhLuan(admin);
        dg.setNgayAnBinhLuan(LocalDateTime.now());
        danhGiaDAO.save(dg);

        // Lưu ý nghiệp vụ: KHÔNG cập nhật cache rating khi ẩn bình luận (sao vẫn hiển thị bình thường)
    }

    /**
     * Phục hồi hiển thị bình luận
     */
    @Transactional
    public void hienBinhLuan(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnBinhLuan(false);
        dg.setNguoiHienBinhLuan(admin);
        dg.setNgayHienBinhLuan(LocalDateTime.now());
        danhGiaDAO.save(dg);
    }

    /**
     * Ẩn Hình Ảnh (Soft Moderation)
     */
    @Transactional
    public void anHinhAnh(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnHinhAnh(true);
        dg.setNguoiAnHinhAnh(admin);
        dg.setNgayAnHinhAnh(LocalDateTime.now());
        danhGiaDAO.save(dg);

        // KHÔNG cập nhật cache rating khi ẩn hình ảnh
    }

    /**
     * Phục hồi hiển thị hình ảnh
     */
    @Transactional
    public void hienHinhAnh(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnHinhAnh(false);
        dg.setNguoiHienHinhAnh(admin);
        dg.setNgayHienHinhAnh(LocalDateTime.now());
        danhGiaDAO.save(dg);
    }

    /**
     * Xóa Mềm Đánh Giá (Soft Delete)
     */
    @Transactional
    public void xoaMemDanhGia(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setDaXoa(true);
        dg.setNguoiXoa(admin);
        dg.setNgayXoa(LocalDateTime.now());
        danhGiaDAO.save(dg);

        // Cập nhật lại cache rating trên thực thể SanPham khi đánh giá bị loại bỏ
        updateProductRatingStats(dg.getSanPham().getId());
    }
}
