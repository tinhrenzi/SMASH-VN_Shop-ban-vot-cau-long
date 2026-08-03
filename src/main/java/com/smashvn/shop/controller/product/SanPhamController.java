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
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
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
            Map<String, String> attributes = ct.getSanPhamChiTietThuocTinhs().stream()
                    .filter(tt -> tt.getThuocTinh() != null && tt.getGiaTri() != null)
                    .collect(Collectors.toMap(tt -> tt.getThuocTinh().getTenThuocTinh(), tt -> tt.getGiaTri(), (a, b) -> a));
            if (ct.getMauSac() != null && !ct.getMauSac().isBlank()) attributes.putIfAbsent("Màu sắc", ct.getMauSac().trim());
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank()) attributes.putIfAbsent("Kích thước", ct.getKichThuoc().trim());
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank()) attributes.putIfAbsent("Trọng lượng", ct.getTrongLuong().trim());
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank()) attributes.putIfAbsent("Sức căng", ct.getMucCang().trim());
            map.put("attributes", attributes);
            map.put("phanLoai", ct.getPhanLoaiHienThi());
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

        // 4. Xác định loại danh mục để lọc thuộc tính chọn biến thể phù hợp
        com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromIdOrName(
                sanPham.getDanhMuc(), sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getId() : null);

        Map<String, java.util.Set<String>> allAttributes = new java.util.LinkedHashMap<>();
        Map<String, java.util.Set<String>> dynamicAttributes = new java.util.LinkedHashMap<>();

        for (SanPhamChiTiet ct : danhSachChiTiet) {
            // 1. Thuộc tính từ bảng EAV (SanPhamChiTietThuocTinh)
            if (ct.getSanPhamChiTietThuocTinhs() != null) {
                for (SanPhamChiTietThuocTinh scttt : ct.getSanPhamChiTietThuocTinhs()) {
                    if (scttt.getThuocTinh() != null && scttt.getGiaTri() != null && !scttt.getGiaTri().isBlank()) {
                        String attrName = scttt.getThuocTinh().getTenThuocTinh().trim();
                        String attrVal = scttt.getGiaTri().trim();

                        allAttributes.computeIfAbsent(attrName, k -> new java.util.LinkedHashSet<>()).add(attrVal);

                        if (isSelectableAttribute(catType, attrName)) {
                            dynamicAttributes.computeIfAbsent(attrName, k -> new java.util.LinkedHashSet<>()).add(attrVal);
                        }
                    }
                }
            }

            // 2. Cột thuộc tính trực tiếp (mauSac, kichThuoc, trongLuong, mucCang)
            if (ct.getMauSac() != null && !ct.getMauSac().isBlank()) {
                String val = ct.getMauSac().trim();
                allAttributes.computeIfAbsent("Màu sắc", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(catType, "Màu sắc")) {
                    dynamicAttributes.computeIfAbsent("Màu sắc", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank()) {
                String val = ct.getKichThuoc().trim();
                allAttributes.computeIfAbsent("Kích thước", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(catType, "Kích thước")) {
                    dynamicAttributes.computeIfAbsent("Kích thước", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank()) {
                String val = ct.getTrongLuong().trim();
                allAttributes.computeIfAbsent("Trọng lượng", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(catType, "Trọng lượng")) {
                    dynamicAttributes.computeIfAbsent("Trọng lượng", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank()) {
                String val = ct.getMucCang().trim();
                allAttributes.computeIfAbsent("Sức căng", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(catType, "Sức căng")) {
                    dynamicAttributes.computeIfAbsent("Sức căng", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
        }

        // Fallback: tự tạo thuộc tính mặc định nếu dynamicAttributes rỗng
        applyDefaultCategoryAttributesIfEmpty(catType, sanPham, dynamicAttributes, allAttributes, listBienTheJS);

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
        model.addAttribute("allAttributes", allAttributes);
        model.addAttribute("dynamicAttributes", dynamicAttributes);
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

    @GetMapping("/api/san-pham/{id}/quick-look")
    public String quickLook(@PathVariable("id") Integer id, Model model, HttpSession session) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm này!"));
        
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findActiveBySanPham_Id(id);
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhSanPham();

        java.util.Set<String> listMauSac = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMauSac)
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        
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
            Map<String, String> attributes = ct.getSanPhamChiTietThuocTinhs().stream()
                    .filter(tt -> tt.getThuocTinh() != null && tt.getGiaTri() != null)
                    .collect(Collectors.toMap(tt -> tt.getThuocTinh().getTenThuocTinh(), tt -> tt.getGiaTri(), (a, b) -> a));
            if (ct.getMauSac() != null && !ct.getMauSac().isBlank()) attributes.putIfAbsent("Màu sắc", ct.getMauSac().trim());
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank()) attributes.putIfAbsent("Kích thước", ct.getKichThuoc().trim());
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank()) attributes.putIfAbsent("Trọng lượng", ct.getTrongLuong().trim());
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank()) attributes.putIfAbsent("Sức căng", ct.getMucCang().trim());
            map.put("attributes", attributes);
            map.put("phanLoai", ct.getPhanLoaiHienThi());
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

        com.smashvn.shop.constant.CategoryType catTypeQuick = com.smashvn.shop.constant.CategoryType.fromIdOrName(
                sanPham.getDanhMuc(), sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getId() : null);

        Map<String, java.util.Set<String>> dynamicAttributes = new java.util.LinkedHashMap<>();
        for (SanPhamChiTiet ct : danhSachChiTiet) {
            if (ct.getSanPhamChiTietThuocTinhs() != null) {
                for (SanPhamChiTietThuocTinh scttt : ct.getSanPhamChiTietThuocTinhs()) {
                    if (scttt.getThuocTinh() != null && scttt.getGiaTri() != null && !scttt.getGiaTri().isBlank()) {
                        String attrName = scttt.getThuocTinh().getTenThuocTinh().trim();
                        if (isSelectableAttribute(catTypeQuick, attrName)) {
                            String attrVal = scttt.getGiaTri().trim();
                            dynamicAttributes.computeIfAbsent(attrName, k -> new java.util.LinkedHashSet<>()).add(attrVal);
                        }
                    }
                }
            }

            if (ct.getMauSac() != null && !ct.getMauSac().isBlank() && isSelectableAttribute(catTypeQuick, "Màu sắc")) {
                dynamicAttributes.computeIfAbsent("Màu sắc", k -> new java.util.LinkedHashSet<>()).add(ct.getMauSac().trim());
            }
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank() && isSelectableAttribute(catTypeQuick, "Kích thước")) {
                dynamicAttributes.computeIfAbsent("Kích thước", k -> new java.util.LinkedHashSet<>()).add(ct.getKichThuoc().trim());
            }
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank() && isSelectableAttribute(catTypeQuick, "Trọng lượng")) {
                dynamicAttributes.computeIfAbsent("Trọng lượng", k -> new java.util.LinkedHashSet<>()).add(ct.getTrongLuong().trim());
            }
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank() && isSelectableAttribute(catTypeQuick, "Sức căng")) {
                dynamicAttributes.computeIfAbsent("Sức căng", k -> new java.util.LinkedHashSet<>()).add(ct.getMucCang().trim());
            }
        }

        applyDefaultCategoryAttributesIfEmpty(catTypeQuick, sanPham, dynamicAttributes, new java.util.LinkedHashMap<>(), listBienTheJS);

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
        model.addAttribute("dynamicAttributes", dynamicAttributes);
        model.addAttribute("listBienTheJS", listBienTheJS);
        model.addAttribute("inWishlist", inWishlist);
        model.addAttribute("soLuongYeuThich", wishlistRepository.countById_SanPhamId(id));
        
        return "layout/modals :: quick-look-fragment"; 
    }

    private boolean isSelectableAttribute(com.smashvn.shop.constant.CategoryType catType, String attrName) {
        if (attrName == null || attrName.isBlank()) return false;
        String norm = attrName.toLowerCase().trim();

        // 1. Hộp cầu (HOP_CAU): Không cần nút chọn biến thể
        if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            return false;
        }

        // 2. Vợt cầu lông (VOT): Chọn Màu sắc, Trọng lượng, Sức căng. (Ẩn Kích thước / Size khỏi nút chọn biến thể)
        if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
            if (norm.contains("kích") || norm.contains("size")) {
                return false;
            }
        }

        // 3. Tất cả các sản phẩm / danh mục còn lại (Trang phục, Giày, Cước, Balo, Quấn cán, Phụ kiện...):
        // Hiển thị TẤT CẢ các thuộc tính hiện có của biến thể
        return true;
    }

    private void applyDefaultCategoryAttributesIfEmpty(
            com.smashvn.shop.constant.CategoryType catType,
            SanPham sanPham,
            Map<String, java.util.Set<String>> dynamicAttributes,
            Map<String, java.util.Set<String>> allAttributes,
            List<Map<String, Object>> listBienTheJS) {

        if (!dynamicAttributes.isEmpty()) {
            return;
        }

        if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            return;
        }

        String title = sanPham != null && sanPham.getTenSanPham() != null ? sanPham.getTenSanPham() : "";
        String titleLower = title.toLowerCase();

        String detectedColor = null;
        if (titleLower.contains("đỏ")) detectedColor = "Đỏ";
        else if (titleLower.contains("xanh")) detectedColor = "Xanh";
        else if (titleLower.contains("đen")) detectedColor = "Đen";
        else if (titleLower.contains("trắng")) detectedColor = "Trắng";
        else if (titleLower.contains("vàng")) detectedColor = "Vàng";
        else if (titleLower.contains("cam")) detectedColor = "Cam";
        else if (titleLower.contains("hồng")) detectedColor = "Hồng";
        else if (titleLower.contains("tím")) detectedColor = "Tím";
        else if (titleLower.contains("xám")) detectedColor = "Xám";

        switch (catType) {
            case TRANG_PHUC:
                dynamicAttributes.put("Kích thước", new java.util.LinkedHashSet<>(java.util.List.of("S", "M", "L", "XL")));
                if (detectedColor != null) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor)));
                }
                break;

            case GIAY:
                dynamicAttributes.put("Kích thước", new java.util.LinkedHashSet<>(java.util.List.of("39", "40", "41", "42", "43")));
                if (detectedColor != null) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor)));
                }
                break;

            case VOT:
                dynamicAttributes.put("Trọng lượng", new java.util.LinkedHashSet<>(java.util.List.of("4U", "3U")));
                dynamicAttributes.put("Sức căng", new java.util.LinkedHashSet<>(java.util.List.of("24-28 lbs")));
                if (detectedColor != null) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor)));
                }
                break;

            case CUOC:
                dynamicAttributes.put("Đường kính", new java.util.LinkedHashSet<>(java.util.List.of("0.65mm", "0.68mm", "0.70mm")));
                if (detectedColor != null) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor)));
                }
                break;

            case BALO:
            case QUAN_CAN:
            case BANG_QUAN:
            default:
                if (detectedColor != null) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor)));
                } else {
                    dynamicAttributes.put("Phân loại", new java.util.LinkedHashSet<>(java.util.List.of("Tiêu chuẩn")));
                }
                break;
        }

        allAttributes.putAll(dynamicAttributes);

        if (listBienTheJS != null) {
            for (Map<String, Object> map : listBienTheJS) {
                @SuppressWarnings("unchecked")
                Map<String, String> attrs = (Map<String, String>) map.get("attributes");
                if (attrs == null) {
                    attrs = new java.util.HashMap<>();
                    map.put("attributes", attrs);
                }
                for (Map.Entry<String, java.util.Set<String>> entry : dynamicAttributes.entrySet()) {
                    String firstVal = entry.getValue().iterator().next();
                    attrs.putIfAbsent(entry.getKey(), firstVal);
                }
            }
        }
    }
}
