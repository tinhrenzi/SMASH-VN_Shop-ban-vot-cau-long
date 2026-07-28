package com.smashvn.shop.controller.home;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.blog.BlogService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final BlogService blogService;
    private final DotGiamGiaDAO dotGiamGiaDAO;

    @GetMapping("/")
    @Transactional(readOnly = true)
    public String hienThiTrangChu(Model model) {
        // Lấy danh sách sản phẩm gốc (SanPham) thay vì biến thể
        List<SanPham> danhSachSanPham = sanPhamRepository.findAll();
        danhSachSanPham.sort((sp1, sp2) -> {
            boolean activeAndInStock1 = ("dang_ban".equals(sp1.getTrangThai()) || sp1.getTrangThai() == null) && sp1.getTongSoLuongTon() > 0;
            boolean activeAndInStock2 = ("dang_ban".equals(sp2.getTrangThai()) || sp2.getTrangThai() == null) && sp2.getTongSoLuongTon() > 0;
            if (activeAndInStock1 && !activeAndInStock2) {
                return -1;
            }
            if (!activeAndInStock1 && activeAndInStock2) {
                return 1;
            }
            return 0;
        });

        // Lọc ra danh sách các sản phẩm đang được giảm giá để hiển thị trên banner countdown đặc biệt
        List<SanPham> discountedProducts = danhSachSanPham.stream()
                .filter(p -> p.getActiveGiamGiaPhanTram() > 0 && ("dang_ban".equals(p.getTrangThai()) || p.getTrangThai() == null))
                .limit(2)
                .collect(Collectors.toList());

        // Nếu không đủ 2 sản phẩm giảm giá, bổ sung sản phẩm thông thường
        if (discountedProducts.size() < 2) {
            List<SanPham> fallback = danhSachSanPham.stream()
                    .filter(p -> !discountedProducts.contains(p) && ("dang_ban".equals(p.getTrangThai()) || p.getTrangThai() == null))
                    .limit(2 - discountedProducts.size())
                    .collect(Collectors.toList());
            discountedProducts.addAll(fallback);
        }

        List<ThuongHieu> danhSachThuongHieu = thuongHieuRepository.findAll();

        // Lấy danh sách theo các tiêu chí (mỗi loại lấy tối đa 14 sản phẩm, riêng nổi bật lấy 4)
        Pageable pageLimit14 = PageRequest.of(0, 14);
        Pageable pageLimit4 = PageRequest.of(0, 4);
        List<SanPham> newProductsList = sanPhamRepository.findNewProducts(pageLimit14);
        
        // Lấy pool lớn sản phẩm bán chạy, sau đó phân bổ đều giữa các thương hiệu
        Pageable pageLimitLarge = PageRequest.of(0, 100);
        List<SanPham> allBestSellers = sanPhamRepository.findBestSellers(pageLimitLarge);
        
        // Nhóm theo thương hiệu rồi lấy lần lượt (round-robin) để đảm bảo mỗi hãng đều có sản phẩm
        java.util.Map<String, java.util.List<SanPham>> byBrand = allBestSellers.stream()
                .collect(Collectors.groupingBy(sp -> sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : "Khác", 
                         java.util.LinkedHashMap::new, Collectors.toList()));
        
        List<SanPham> bestSellersList = new java.util.ArrayList<>();
        int maxPerBrand = Math.max(1, 14 / Math.max(1, byBrand.size()));
        for (java.util.List<SanPham> brandProducts : byBrand.values()) {
            bestSellersList.addAll(brandProducts.stream().limit(maxPerBrand).collect(Collectors.toList()));
        }
        // Nếu chưa đủ 14, bổ sung thêm từ các sản phẩm còn lại
        if (bestSellersList.size() < 14) {
            for (SanPham sp : allBestSellers) {
                if (!bestSellersList.contains(sp)) {
                    bestSellersList.add(sp);
                    if (bestSellersList.size() >= 14) break;
                }
            }
        }
        
        List<SanPham> featuredProductsList = sanPhamRepository.findFeaturedProducts(pageLimit4);

        // Tìm chiến dịch giảm giá có phần trăm giảm cao nhất đang hoạt động
        List<DotGiamGia> activeCampaigns = dotGiamGiaDAO.findAll().stream()
                .filter(dgg -> dgg.getActive() && "ACTIVE".equals(dgg.getDynamicStatus()))
                .sorted((d1, d2) -> d2.getPhanTramGiam().compareTo(d1.getPhanTramGiam()))
                .collect(Collectors.toList());

        DotGiamGia highestDiscountCampaign = activeCampaigns.isEmpty() ? null : activeCampaigns.get(0);
        if (highestDiscountCampaign != null) {
            String formattedEndDate = highestDiscountCampaign.getNgayKetThuc()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            model.addAttribute("highestDiscountCampaign", highestDiscountCampaign);
            model.addAttribute("campaignEndDate", formattedEndDate);
        } else {
            model.addAttribute("highestDiscountCampaign", null);
        }

        // Tìm sản phẩm có phần trăm giảm giá cao nhất đang hoạt động
        SanPham highestDiscountProduct = danhSachSanPham.stream()
                .filter(p -> p.getActiveGiamGiaPhanTram() > 0 && ("dang_ban".equals(p.getTrangThai()) || p.getTrangThai() == null))
                .max((p1, p2) -> Integer.compare(p1.getActiveGiamGiaPhanTram(), p2.getActiveGiamGiaPhanTram()))
                .orElse(null);
        model.addAttribute("highestDiscountProduct", highestDiscountProduct);

        model.addAttribute("products", discountedProducts); // để đảm bảo tương thích ngược
        model.addAttribute("newProducts", newProductsList);
        model.addAttribute("bestSellers", bestSellersList);
        model.addAttribute("featuredProducts", featuredProductsList);
        model.addAttribute("brands", danhSachThuongHieu);
        model.addAttribute("blogs", blogService.getRecentBlogs(3));

        return "index";
    }

    @GetMapping("/about")
    public String hienThiTrangGioiThieu() {
        return "about";
    }

    @GetMapping("/contact")
    public String hienThiTrangLienHe() {
        return "about";
    }

    @GetMapping("/shop")
    @Transactional(readOnly = true)
    public String hienThiCuaHang(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "brandId", required = false) Integer brandId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "trongLuong", required = false) List<String> trongLuong,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Model model) {

        List<String> normalizedTrongLuong = trongLuong;
        if (normalizedTrongLuong != null) {
            normalizedTrongLuong = normalizedTrongLuong.stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.toList());
            if (normalizedTrongLuong.isEmpty()) {
                normalizedTrongLuong = null;
            }
        }

        // Do ORDER BY đã được định nghĩa trực tiếp và đầy đủ trong @Query của SanPhamRepository.findByFilters,
        // chúng ta sử dụng PageRequest.of không truyền Sort (tương đương Sort.unsorted()) để tránh Spring Data JPA
        // tự động append thêm "order by sp.id desc/asc" gây ra lỗi trùng cột trong ORDER BY ở SQL Server.
        Pageable pageable = PageRequest.of(page, size);

        // Sanitize keyword (XSS prevention)
        String sanitizedKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            sanitizedKeyword = Jsoup.clean(keyword.trim(), Safelist.none());
            if (sanitizedKeyword.length() > 100) {
                sanitizedKeyword = sanitizedKeyword.substring(0, 100);
            }
        }

        // Sử dụng query kết hợp nhiều điều kiện
        Page<SanPham> productPage = sanPhamRepository.findByFilters(
                sanitizedKeyword, categoryId, brandId, minPrice, maxPrice, rating, normalizedTrongLuong, sort, pageable
        );

        // Lấy giá min/max toàn bộ sản phẩm để khởi tạo slider
        BigDecimal globalMinPrice = BigDecimal.ZERO;
        BigDecimal globalMaxPrice = new BigDecimal("30000000");

        List<DanhMuc> danhSachDanhMuc = danhMucRepository.findAll();
        List<ThuongHieu> danhSachThuongHieu = thuongHieuRepository.findAll();

        java.util.Map<Integer, Long> categoryCounts = new java.util.HashMap<>();
        for (DanhMuc dm : danhSachDanhMuc) {
            categoryCounts.put(dm.getId(), sanPhamRepository.countByDanhMucId(dm.getId()));
        }

        java.util.Map<Integer, Long> brandCounts = new java.util.HashMap<>();
        for (ThuongHieu th : danhSachThuongHieu) {
            brandCounts.put(th.getId(), sanPhamRepository.countByThuongHieuId(th.getId()));
        }

        long totalProductsCount = sanPhamRepository.countActiveProducts();

        // Lấy danh sách 8 sản phẩm mới nhất để gắn tag "MỚI"
        List<SanPham> newProductsList = sanPhamRepository.findNewProducts(PageRequest.of(0, 8));
        Set<Integer> newProductIds = newProductsList.stream().map(SanPham::getId).collect(Collectors.toSet());
        model.addAttribute("newProductIds", newProductIds);

        // Initialize common racket weights (sizes)
        List<String> listTrongLuong = new java.util.ArrayList<>(java.util.Arrays.asList("2U", "3U", "4U", "5U", "6U"));
        List<String> dbTrongLuongs = sanPhamChiTietRepository.findDistinctTrongLuong();
        for (String dbTl : dbTrongLuongs) {
            if (dbTl != null && !dbTl.trim().isEmpty() && !listTrongLuong.contains(dbTl)) {
                listTrongLuong.add(dbTl);
            }
        }
        java.util.Map<String, Long> sizeCounts = new java.util.HashMap<>();
        for (String tl : listTrongLuong) {
            sizeCounts.put(tl, sanPhamRepository.countByTrongLuong(tl));
        }

        model.addAttribute("danhSachTrongLuong", listTrongLuong);
        model.addAttribute("sizeCounts", sizeCounts);
        model.addAttribute("selectedTrongLuong", normalizedTrongLuong);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", danhSachDanhMuc);
        model.addAttribute("brands", danhSachThuongHieu);
        model.addAttribute("totalProductsCount", totalProductsCount);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("brandCounts", brandCounts);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("selectedMinPrice", minPrice);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("globalMinPrice", globalMinPrice);
        model.addAttribute("globalMaxPrice", globalMaxPrice);
        model.addAttribute("selectedRating", rating != null ? rating : 0.0);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("keyword", sanitizedKeyword);

        if ("XMLHttpRequest".equals(requestedWith)) {
            return "shop :: #shop-content-area";
        }
        return "shop";
    }

    @GetMapping("/shop-side-version-2.html")
    public String redirectShopLegacy() {
        return "redirect:/shop";
    }

    @GetMapping({
        "/blog-left-sidebar.html",
        "/blog-right-sidebar.html",
        "/blog-sidebar-none.html",
        "/blog-masonry.html",
        "/blog-detail.html"
    })
    public String redirectBlogLegacy() {
        return "redirect:/blog";
    }

    @GetMapping({
        "/index.html",
        "/product-detail.html"
    })
    public String redirectLegacyTemplates() {
        return "redirect:/";
    }
}
