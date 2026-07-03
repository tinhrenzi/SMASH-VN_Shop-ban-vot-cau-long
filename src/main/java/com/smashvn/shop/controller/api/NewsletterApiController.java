package com.smashvn.shop.controller.api;

import com.smashvn.shop.exception.NewsletterValidationException;
import com.smashvn.shop.service.NewsletterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class NewsletterApiController {

    private final NewsletterService newsletterService;

    public static class SubscribeRequest {
        private String email;
        private String gioiTinh;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getGioiTinh() { return gioiTinh; }
        public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    }

    @PostMapping("/api/newsletter/subscribe")
    @ResponseBody
    public ResponseEntity<?> subscribe(@RequestBody SubscribeRequest request) {
        newsletterService.subscribe(request.getEmail(), request.getGioiTinh());
        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng ký nhận ưu đãi thành công!"));
    }

    @GetMapping("/api/newsletter/unsubscribe")
    public String unsubscribe(@RequestParam("token") String token, Model model) {
        try {
            newsletterService.unsubscribe(token);
            return "unsubscribe-success";
        } catch (NewsletterValidationException e) {
            log.warn("[Newsletter] Validation failed during unsubscribe: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "unsubscribe-error";
        } catch (Exception e) {
            log.error("[Newsletter] Unexpected error during unsubscribe: ", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra trong hệ thống, vui lòng thử lại sau.");
            return "unsubscribe-error";
        }
    }

    @ExceptionHandler(NewsletterValidationException.class)
    @ResponseBody
    public ResponseEntity<?> handleValidationException(NewsletterValidationException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<?> handleAllOtherExceptions(Exception e) {
        log.error("[Newsletter API] Unhandled exception: ", e);
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Có lỗi xảy ra trong hệ thống, vui lòng thử lại sau."));
    }
}
