package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.repository.DanhMucRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DanhMucService {

    private static final Logger log = LoggerFactory.getLogger(DanhMucService.class);

    private final DanhMucRepository danhMucRepository;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Thêm mới danh mục.
     * Service là nguồn xác thực duy nhất: sanitize → validate → kiểm tra trùng → lưu.
     */
    public DanhMuc themDanhMuc(String tenDanhMuc) {
        String normalized = normalize(tenDanhMuc);
        validateLength(normalized);

        if (danhMucRepository.existsByTenDanhMucIgnoreCase(normalized)) {
            log.warn("[CATEGORY] Duplicate category add attempt detected.");
            throw new IllegalArgumentException(
                    "Danh mục \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        DanhMuc dm = new DanhMuc();
        dm.setTenDanhMuc(normalized);
        return danhMucRepository.save(dm);
    }

    /**
     * Cập nhật danh mục theo ID.
     * Cho phép giữ nguyên tên hiện tại (self-exclude khi kiểm tra trùng).
     */
    public DanhMuc suaDanhMuc(Integer id, String tenDanhMuc) {
        String normalized = normalize(tenDanhMuc);
        validateLength(normalized);

        if (danhMucRepository.existsByTenDanhMucIgnoreCaseAndIdNot(normalized, id)) {
            log.warn("[CATEGORY] Duplicate category edit attempt detected.");
            throw new IllegalArgumentException(
                    "Danh mục \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        DanhMuc dm = danhMucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));
        dm.setTenDanhMuc(normalized);
        return danhMucRepository.save(dm);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Normalization pipeline (applied before ALL validation and duplicate checks):
     * 1. Jsoup.clean(Safelist.none()) — strip all HTML / XSS tags
     * 2. .trim()                       — remove leading/trailing whitespace
     * 3. .replaceAll("\\s+", " ")      — collapse multiple inner spaces to one
     *
     * Examples:
     *   "  NIKE  "  →  "NIKE"
     *   "<b>Nike</b>" →  "Nike"
     *   "Nike  Pro"  →  "Nike Pro"
     */
    String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Tên danh mục không được để trống!");
        }
        String cleaned = Jsoup.clean(input, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống!");
        }
        return cleaned;
    }

    /** Validate 2–100 characters after normalization */
    private void validateLength(String normalized) {
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("Tên danh mục phải có ít nhất 2 ký tự!");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 100 ký tự!");
        }
    }
}
