package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DanhMucThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.DanhMucThuocTinhRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DanhMucService {

    private static final Logger log = LoggerFactory.getLogger(DanhMucService.class);

    private final DanhMucRepository danhMucRepository;
    private final DanhMucThuocTinhRepository danhMucThuocTinhRepository;
    private final ThuocTinhRepository thuocTinhRepository;

    public DanhMuc themDanhMuc(String tenDanhMuc) {
        return themDanhMuc(tenDanhMuc, List.of());
    }

    @Transactional
    public DanhMuc themDanhMuc(String tenDanhMuc, List<Integer> thuocTinhIds) {
        String normalized = normalize(tenDanhMuc);
        validateLength(normalized);

        if (danhMucRepository.existsByTenDanhMucIgnoreCase(normalized)) {
            log.warn("[CATEGORY] Duplicate category add attempt detected.");
            throw new IllegalArgumentException(
                    "Danh mục \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        DanhMuc dm = new DanhMuc();
        dm.setTenDanhMuc(normalized);
        dm = danhMucRepository.save(dm);

        if (thuocTinhIds != null && !thuocTinhIds.isEmpty()) {
            List<DanhMucThuocTinh> listMapping = new ArrayList<>();
            for (Integer ttId : thuocTinhIds) {
                if (ttId != null) {
                    ThuocTinh tt = thuocTinhRepository.findById(ttId).orElse(null);
                    if (tt != null) {
                        DanhMucThuocTinh mapping = DanhMucThuocTinh.builder()
                                .danhMuc(dm)
                                .thuocTinh(tt)
                                .trangThai(true)
                                .build();
                        listMapping.add(mapping);
                    }
                }
            }
            if (!listMapping.isEmpty()) {
                danhMucThuocTinhRepository.saveAll(listMapping);
                dm.setDanhMucThuocTinhs(listMapping);
            }
        }

        return dm;
    }

    public DanhMuc suaDanhMuc(Integer id, String tenDanhMuc) {
        return suaDanhMuc(id, tenDanhMuc, List.of());
    }

    @Transactional
    public DanhMuc suaDanhMuc(Integer id, String tenDanhMuc, List<Integer> thuocTinhIds) {
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

        // Xóa toàn bộ thuộc tính cũ và FLUSH NGAY để DB thực thi DELETE
        // trước khi INSERT bản ghi mới (tránh vi phạm UNIQUE constraint)
        dm.getDanhMucThuocTinhs().clear();
        DanhMuc flushedDm = danhMucRepository.saveAndFlush(dm);

        // Thêm lại các thuộc tính mới
        if (thuocTinhIds != null && !thuocTinhIds.isEmpty()) {
            for (Integer ttId : thuocTinhIds) {
                if (ttId != null) {
                    thuocTinhRepository.findById(ttId).ifPresent(tt -> {
                        DanhMucThuocTinh mapping = DanhMucThuocTinh.builder()
                                .danhMuc(flushedDm)
                                .thuocTinh(tt)
                                .trangThai(true)
                                .build();
                        flushedDm.getDanhMucThuocTinhs().add(mapping);
                    });
                }
            }
        }

        return danhMucRepository.save(flushedDm);
    }

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

    private void validateLength(String normalized) {
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("Tên danh mục phải có ít nhất 2 ký tự!");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 100 ký tự!");
        }
    }
}
