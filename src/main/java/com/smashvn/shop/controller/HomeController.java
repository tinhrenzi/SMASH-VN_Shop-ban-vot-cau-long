package com.smashvn.shop.controller;

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
import com.smashvn.shop.service.BlogService;

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

    @GetMapping("/shop")
    public String hienThiCuaHang(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "brandId", required = false) Integer brandId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
            Model model) {

        // Xây dựng Sort dựa trên tham số sort
        Sort sortOrder;
        switch (sort) {
            case "price_asc":
                sortOrder = Sort.by(Sort.Direction.ASC, "id"); // sẽ sort bởi giá ở phía client hoặc dùng subquery
                break;
            case "price_desc":
                sortOrder = Sort.by(Sort.Direction.DESC, "id");
                break;
            default: // newest
                sortOrder = Sort.by(Sort.Direction.DESC, "id");
                break;
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Sử dụng query kết hợp nhiều điều kiện
        Page<SanPham> productPage = sanPhamRepository.findByFilters(
            categoryId, brandId, minPrice, maxPrice, pageable
        );

        // Lấy giá min/max toàn bộ sản phẩm để khởi tạo slider
        BigDecimal globalMinPrice = sanPhamRepository.findMinPrice();
        BigDecimal globalMaxPrice = sanPhamRepository.findMaxPrice();
        if (globalMinPrice == null) globalMinPrice = BigDecimal.ZERO;
        if (globalMaxPrice == null) globalMaxPrice = new BigDecimal("10000000");

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
        model.addAttribute("selectedMinPrice", minPrice != null ? minPrice : globalMinPrice);
        model.addAttribute("selectedMaxPrice", maxPrice != null ? maxPrice : globalMaxPrice);
        model.addAttribute("globalMinPrice", globalMinPrice);
        model.addAttribute("globalMaxPrice", globalMaxPrice);
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

