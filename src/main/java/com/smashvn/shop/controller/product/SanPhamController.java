package com.smashvn.shop.controller.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.product.DanhGiaService;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamYeuThichRepository wishlistRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final DanhGiaService danhGiaService;

    @GetMapping("/san-pham/{id}")
    public String hienThiChiTietSanPham(@PathVariable("id") Integer id, Model model, HttpSession session) {
        
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm này!"));
        
        if ("ngung_ban".equals(sanPham.getTrangThai()) || "ngung_kinh_doanh".equals(sanPham.getTrangThai())) {
            return "redirect:/shop?loi=" + java.net.URLEncoder.encode("Sản phẩm này đã ngừng kinh doanh tại cửa hàng!", java.nio.charset.StandardCharsets.UTF_8);
        }
        
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findActiveBySanPham_Id(id);
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhSanPham();

        // 1. Lấy danh sách Màu Sắc không trùng lặp
        java.util.Set<String> listMauSac = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMauSac)
                .collect(java.util.stream.Collectors.toSet());
        
        // 2. Lấy danh sách Size (Trọng lượng / Kích thước) không trùng lặp
        java.util.Set<String> listKichThuoc = danhSachChiTiet.stream()
                .map(ct -> ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank() ? ct.getTrongLuong() : ct.getKichThuoc())
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> listMucCang = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMucCang)
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        // 3. Tạo một list "gọn nhẹ" (Map) chỉ chứa đúng các thông tin JS cần thiết để tránh lỗi đệ quy
        int phanTram = sanPham.getActiveGiamGiaPhanTram();
        List<Map<String, Object>> listBienTheJS = danhSachChiTiet.stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            map.put("mauSac", ct.getMauSac());
            map.put("trongLuong", ct.getTrongLuong());
            map.put("kichThuoc", ct.getKichThuoc());
            map.put("mucCang", ct.getMucCang());
            map.put("giaBan", ct.getGiaBan());
            map.put("giaSauGiam", sanPham.getGiaSauGiam(ct.getGiaBan()));
            map.put("phanTramGiam", phanTram);
            map.put("soLuongTon", ct.getSoLuongTon());
            map.put("hinhAnhSanPham", ct.getHinhAnhSanPham());
            return map;
        }).collect(Collectors.toList());

        boolean inWishlist = false;
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan != null) {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
            if (kh != null) {
                inWishlist = wishlistRepository.existsById_KhachHangIdAndId_SanPhamId(kh.getId(), id);
            }
        }

        // Tích hợp dữ liệu Đánh giá
        boolean sanPhamKhaDung = sanPham.getTrangThai() == null || "dang_ban".equals(sanPham.getTrangThai());
        boolean daMuaHang = false;
        boolean daDanhGia = false;
        DanhGia oldDanhGia = null;
        boolean biKhoaBinhLuan = false;
        java.time.LocalDateTime ngayKhoaDen = null;

        if (idTaiKhoan != null) {
            daMuaHang = danhGiaService.daMuaSanPham(idTaiKhoan, id);
            Optional<DanhGia> dgOpt = danhGiaService.layDanhGiaDaCo(idTaiKhoan, id);
            if (dgOpt.isPresent()) {
                daDanhGia = true;
                oldDanhGia = dgOpt.get();
            }

            TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan).orElse(null);
            if (tk != null && tk.getNgayKhoaBinhLuanDen() != null && tk.getNgayKhoaBinhLuanDen().isAfter(java.time.LocalDateTime.now())) {
                biKhoaBinhLuan = true;
                ngayKhoaDen = tk.getNgayKhoaBinhLuanDen();
            }
        }

        List<DanhGia> listDanhGia = danhGiaService.layDanhSachDanhGiaTheoSanPham(id);

        model.addAttribute("listMauSac", listMauSac);
        model.addAttribute("listKichThuoc", listKichThuoc);
        model.addAttribute("listMucCang", listMucCang);
        model.addAttribute("sp", sanPham);
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        model.addAttribute("inWishlist", inWishlist);
        model.addAttribute("listBienTheJS", listBienTheJS); 
        model.addAttribute("soLuongYeuThich", wishlistRepository.countById_SanPhamId(id));

        // Lấy danh sách sản phẩm liên quan (cùng danh mục, bỏ qua sản phẩm hiện tại)
        List<SanPham> relatedProducts = sanPhamRepository.findByDanhMucId(sanPham.getDanhMuc().getId()).stream()
                .filter(p -> !p.getId().equals(id) && (p.getTrangThai() == null || "dang_ban".equals(p.getTrangThai())))
                .limit(8)
                .collect(Collectors.toList());
        if (relatedProducts.isEmpty()) {
            relatedProducts = sanPhamRepository.findAll().stream()
                    .filter(p -> !p.getId().equals(id) && (p.getTrangThai() == null || "dang_ban".equals(p.getTrangThai())))
                    .limit(8)
                    .collect(Collectors.toList());
        }
        model.addAttribute("relatedProducts", relatedProducts);

        // Đổ dữ liệu đánh giá ra frontend
        model.addAttribute("sanPhamKhaDung", sanPhamKhaDung);
        model.addAttribute("daMuaHang", daMuaHang);
        model.addAttribute("daDanhGia", daDanhGia);
        model.addAttribute("oldDanhGia", oldDanhGia);
        model.addAttribute("listDanhGia", listDanhGia);
        model.addAttribute("totalDanhGia", sanPham.getSoDanhGia());
        model.addAttribute("avgRating", sanPham.getDiemTrungBinh());
        model.addAttribute("biKhoaBinhLuan", biKhoaBinhLuan);
        model.addAttribute("ngayKhoaDen", ngayKhoaDen);
        
        return "product-detail"; 
    }

    // Gửi đánh giá / Chỉnh sửa đánh giá sản phẩm
    @PostMapping("/san-pham/{id}/danh-gia")
    public String guiDanhGia(@PathVariable("id") Integer idSanPham,
                             @RequestParam("rating") Double soSao,
                             @RequestParam("comment") String binhLuan,
                             @RequestParam(value = "fileAnh", required = false) List<MultipartFile> files,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Vui lòng đăng nhập để thực hiện đánh giá sản phẩm.");
            return "redirect:/user/dang-nhap";
        }

        try {
            boolean moderated = danhGiaService.themHoacCapNhatDanhGia(idTaiKhoan, idSanPham, soSao, binhLuan, files);
            redirectAttributes.addFlashAttribute("successMsg", moderated
                    ? "Bình luận của bạn đã được đăng. Một số nội dung không phù hợp đã được hệ thống tự động ẩn."
                    : "Gửi đánh giá sản phẩm thành công!");
        } catch (Exception e) {
            log.error("[REVIEW_SUBMIT_ERROR] Failed to save review: {}", e.getMessage());
            String message = e instanceof IllegalArgumentException || e instanceof IllegalStateException
                    ? e.getMessage()
                    : "Không thể gửi đánh giá lúc này. Vui lòng thử lại sau.";
            redirectAttributes.addFlashAttribute("errorMsg", message);
        }
        return "redirect:/san-pham/" + idSanPham + "#pd-rev";
    }

    // Trả về một đoạn HTML (fragment) thay vì trả về toàn bộ trang web
    @GetMapping("/modal/quick-look/{id}")
    public String hienThiQuickLookModal(@PathVariable("id") Integer id, Model model, HttpSession session) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm này!"));
                
        if ("ngung_ban".equals(sanPham.getTrangThai()) || "ngung_kinh_doanh".equals(sanPham.getTrangThai())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Sản phẩm này đã ngừng kinh doanh!");
        }
                
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findActiveBySanPham_Id(id);
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhSanPham();

        java.util.Set<String> listMauSac = danhSachChiTiet.stream().map(SanPhamChiTiet::getMauSac).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> listKichThuoc = danhSachChiTiet.stream()
                .map(ct -> ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank() ? ct.getTrongLuong() : ct.getKichThuoc())
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> listMucCang = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMucCang)
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        int phanTram = sanPham.getActiveGiamGiaPhanTram();
        List<Map<String, Object>> listBienTheJS = danhSachChiTiet.stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            map.put("mauSac", ct.getMauSac());
            map.put("trongLuong", ct.getTrongLuong());
            map.put("kichThuoc", ct.getKichThuoc());
            map.put("mucCang", ct.getMucCang());
            map.put("giaBan", ct.getGiaBan());
            map.put("giaSauGiam", sanPham.getGiaSauGiam(ct.getGiaBan()));
            map.put("phanTramGiam", phanTram);
            map.put("soLuongTon", ct.getSoLuongTon());
            map.put("hinhAnhSanPham", ct.getHinhAnhSanPham());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        boolean inWishlist = false;
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan != null) {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
            if (kh != null) {
                inWishlist = wishlistRepository.existsById_KhachHangIdAndId_SanPhamId(kh.getId(), id);
            }
        }

        model.addAttribute("spQuickLook", sanPham); 
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        model.addAttribute("listMauSac", listMauSac);
        model.addAttribute("listKichThuoc", listKichThuoc);
        model.addAttribute("listMucCang", listMucCang);
        model.addAttribute("listBienTheJS", listBienTheJS);
        model.addAttribute("inWishlist", inWishlist);
        model.addAttribute("soLuongYeuThich", wishlistRepository.countById_SanPhamId(id));
        
        return "layout/modals :: quick-look-fragment"; 
    }
}
