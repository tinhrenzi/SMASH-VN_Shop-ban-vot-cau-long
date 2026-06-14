package com.smashvn.shop.controller.blog;

import com.smashvn.shop.dto.blog.BlogDTO;
import com.smashvn.shop.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping("/blog")
    public String showBlogList(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "3") int size,
            Model model) {

        // Giới hạn chỉ số trang tối thiểu là 0
        if (page < 0) page = 0;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogDTO> blogsPage = blogService.getBlogs(query, pageable);
        List<BlogDTO> recentBlogs = blogService.getRecentBlogs(3);

        model.addAttribute("blogsPage", blogsPage);
        model.addAttribute("query", query);
        model.addAttribute("recentBlogs", recentBlogs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogsPage.getTotalPages());

        return "blog";
    }

    @GetMapping("/blog/{slug}")
    public String showBlogDetail(
            @PathVariable("slug") String slug,
            Model model) {

        try {
            BlogDTO blog = blogService.getBlogBySlug(slug);
            List<BlogDTO> recentBlogs = blogService.getRecentBlogs(3);

            model.addAttribute("blog", blog);
            model.addAttribute("recentBlogs", recentBlogs);
            return "blog-detail";
        } catch (Exception e) {
            // Nếu không tìm thấy slug bài viết, trả về trang lỗi 404
            return "404";
        }
    }
}
