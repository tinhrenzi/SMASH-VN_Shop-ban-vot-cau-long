package com.smashvn.shop.controller.home;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.service.blog.BlogService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SanPhamRepository sanPhamRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final BlogService blogService;

    @GetMapping("/")
    public String hienThiTrangChu(Model model) {
        // Lấy danh sách sản phẩm gốc (SanPham) thay vì biến thể
        List<SanPham> danhSachSanPham = sanPhamRepository.findAll();
        danhSachSanPham.sort((sp1, sp2) -> {
            boolean activeAndInStock1 = ("dang_ban".equals(sp1.getTrangThai()) || sp1.getTrangThai() == null) && sp1.getTongSoLuongTon() > 0;
            boolean activeAndInStock2 = ("dang_ban".equals(sp2.getTrangThai()) || sp2.getTrangThai() == null) && sp2.getTongSoLuongTon() > 0;
            if (activeAndInStock1 && !activeAndInStock2) return -1;
            if (!activeAndInStock1 && activeAndInStock2) return 1;
            return 0;
        });

        List<ThuongHieu> danhSachThuongHieu = thuongHieuRepository.findAll();

        // Lấy danh sách theo các tiêu chí (mỗi loại lấy tối đa 14 sản phẩm)
        Pageable pageLimit14 = PageRequest.of(0, 14);
        List<SanPham> newProductsList = sanPhamRepository.findNewProducts(pageLimit14);
        List<SanPham> bestSellersList = sanPhamRepository.findBestSellers(pageLimit14);
        List<SanPham> featuredProductsList = sanPhamRepository.findFeaturedProducts(pageLimit14);

        model.addAttribute("products", danhSachSanPham); // để đảm bảo tương thích ngược
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
    public String hienThiCuaHang(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "brandId", required = false) Integer brandId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
            Model model) {

        // Do ORDER BY đã được định nghĩa trực tiếp và đầy đủ trong @Query của SanPhamRepository.findByFilters,
        // chúng ta sử dụng PageRequest.of không truyền Sort (tương đương Sort.unsorted()) để tránh Spring Data JPA
        // tự động append thêm "order by sp.id desc/asc" gây ra lỗi trùng cột trong ORDER BY ở SQL Server.
        Pageable pageable = PageRequest.of(page, size);

        // Sử dụng query kết hợp nhiều điều kiện
        Page<SanPham> productPage = sanPhamRepository.findByFilters(
            categoryId, brandId, minPrice, maxPrice, rating, sort, pageable
        );

        // Lấy giá min/max toàn bộ sản phẩm để khởi tạo slider
        BigDecimal globalMinPrice = BigDecimal.ZERO;
        BigDecimal globalMaxPrice = new BigDecimal("100000000");

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

        long totalProductsCount = sanPhamRepository.count();

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

