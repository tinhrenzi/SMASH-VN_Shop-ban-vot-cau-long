package com.smashvn.shop.service.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dto.inventory.*;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLotService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final AuditService auditService;

    @Value("${inventory.lot.enabled-from:2026-08-07T08:30:00}")
    private String enabledFromStr;

    /**
     * Phân bổ tồn kho FIFO hai giai đoạn (Two-Phase Allocation):
     * Giai đoạn 1: Sắp xếp idSanPham tăng dần, khóa PESSIMISTIC_WRITE, lập kế hoạch phân bổ trong bộ nhớ.
     * Giai đoạn 2: Nếu TẤT CẢ mặt hàng ĐỦ TỒN mới thực hiện trừ kho DB. Nếu thiếu kho, trả INSUFFICIENT_STOCK và giữ nguyên DB.
     */
    @Transactional
    public AllocationResult allocateFifo(List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return new AllocationResult(AllocationStatus.SUCCESS, Collections.emptyList(), "Danh sách yêu cầu rỗng");
        }

        // Step 1: Tra cứu SPCT đại diện để lấy danh sách idSanPham
        Map<Integer, SanPhamChiTiet> repSpctMap = new HashMap<>();
        for (OrderItemRequest req : itemRequests) {
            SanPhamChiTiet rep = sanPhamChiTietRepository.findById(req.getRepresentativeSpctId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể đại diện ID: " + req.getRepresentativeSpctId()));
            repSpctMap.put(req.getRepresentativeSpctId(), rep);
        }

        // Step 2: Sắp xếp danh sách idSanPham tăng dần để tránh Deadlock
        List<Integer> sortedProductIds = repSpctMap.values().stream()
                .map(spct -> spct.getSanPham().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Step 3: Khóa PESSIMISTIC_WRITE từng SanPham cha theo thứ tự ID tăng dần
        Map<Integer, SanPham> lockedProductMap = new HashMap<>();
        for (Integer spId : sortedProductIds) {
            SanPham sp = sanPhamRepository.findByIdWithLock(spId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + spId));
            lockedProductMap.put(spId, sp);
        }

        // Step 4: GIAI ĐOẠN 1 - Lập kế hoạch phân bổ trong bộ nhớ (Memory Allocation Plan)
        List<LotAllocation> plannedAllocations = new ArrayList<>();
        boolean hasInsufficientStock = false;
        StringBuilder errorMessageBuilder = new StringBuilder();

        for (OrderItemRequest req : itemRequests) {
            SanPhamChiTiet repSpct = repSpctMap.get(req.getRepresentativeSpctId());
            String targetAttrKey = buildAttributeKey(repSpct);
            Integer productId = repSpct.getSanPham().getId();

            // Tải danh sách kandidat SPCT còn bán thuộc sản phẩm đó
            List<SanPhamChiTiet> candidates = sanPhamChiTietRepository.findActiveCandidatesBySanPhamId(productId);
            List<SanPhamChiTiet> matchingCandidates = candidates.stream()
                    .filter(c -> buildAttributeKey(c).equals(targetAttrKey))
                    .sorted(Comparator.comparing(SanPhamChiTiet::getId)) // FIFO theo ID (Lô cũ tạo trước có ID nhỏ hơn)
                    .collect(Collectors.toList());

            int totalAvailableStock = matchingCandidates.stream()
                    .mapToInt(c -> c.getSoLuongTon() != null ? c.getSoLuongTon() : 0)
                    .sum();

            if (totalAvailableStock < req.getQuantity()) {
                hasInsufficientStock = true;
                errorMessageBuilder.append(String.format("Sản phẩm '%s' (%s) không đủ tồn kho (Cần: %d, Khả dụng: %d). ",
                        repSpct.getSanPham().getTenSanPham(), targetAttrKey, req.getQuantity(), totalAvailableStock));
                continue;
            }

            // Lập kế hoạch phân bổ từng lô cho mặt hàng này
            int remainingNeeded = req.getQuantity();
            for (SanPhamChiTiet candidate : matchingCandidates) {
                if (remainingNeeded <= 0) break;
                int currentStock = candidate.getSoLuongTon() != null ? candidate.getSoLuongTon() : 0;
                if (currentStock <= 0) continue;

                int allocateQty = Math.min(currentStock, remainingNeeded);
                plannedAllocations.add(new LotAllocation(
                        req.getSourceLineId(),
                        req.getRepresentativeSpctId(),
                        candidate,
                        allocateQty
                ));
                remainingNeeded -= allocateQty;
            }
        }

        // GIAI ĐOẠN 2: Kiểm tra kết quả lập kế hoạch
        if (hasInsufficientStock) {
            log.warn("[InventoryLotService] Phân bổ FIFO thất bại do thiếu kho: {}", errorMessageBuilder.toString());
            return new AllocationResult(AllocationStatus.INSUFFICIENT_STOCK, Collections.emptyList(), errorMessageBuilder.toString().trim());
        }

        // Áp dụng trừ tồn kho thực tế vào Database nếu TẤT CẢ mặt hàng đủ tồn
        for (LotAllocation alloc : plannedAllocations) {
            SanPhamChiTiet spct = alloc.allocatedSpct();
            int newStock = spct.getSoLuongTon() - alloc.quantityAllocated();
            spct.setSoLuongTon(newStock);
            sanPhamChiTietRepository.save(spct);
        }

        log.info("[InventoryLotService] Phân bổ FIFO thành công cho {} dòng hàng", itemRequests.size());
        return new AllocationResult(AllocationStatus.SUCCESS, plannedAllocations, "Phân bổ tồn kho FIFO thành công");
    }

    /**
     * Hoàn kho hàng loạt: Sắp xếp idSanPham tăng dần, khóa PESSIMISTIC_WRITE, cộng lại tồn kho.
     */
    @Transactional
    public void hoanKhoHangLoat(List<RestockItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        List<RestockItemRequest> validRequests = requests.stream()
                .filter(r -> r.isConBanDuoc() && r.getQuantityToRestock() > 0 && r.getIdSanPhamChiTiet() != null)
                .collect(Collectors.toList());

        if (validRequests.isEmpty()) return;

        // Step 1: Tra cứu danh sách SPCT
        Map<Integer, SanPhamChiTiet> spctMap = new HashMap<>();
        for (RestockItemRequest req : validRequests) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(req.getIdSanPhamChiTiet())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SPCT ID: " + req.getIdSanPhamChiTiet()));
            spctMap.put(req.getIdSanPhamChiTiet(), spct);
        }

        // Step 2: Sắp xếp danh sách idSanPham tăng dần
        List<Integer> sortedProductIds = spctMap.values().stream()
                .map(spct -> spct.getSanPham().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Step 3: Khóa PESSIMISTIC_WRITE từng sản phẩm cha
        for (Integer spId : sortedProductIds) {
            sanPhamRepository.findByIdWithLock(spId);
        }

        // Step 4: Thực hiện cộng hoàn tồn kho
        for (RestockItemRequest req : validRequests) {
            SanPhamChiTiet spct = spctMap.get(req.getIdSanPhamChiTiet());
            int oldStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            spct.setSoLuongTon(oldStock + req.getQuantityToRestock());
            sanPhamChiTietRepository.save(spct);
            log.info("[InventoryLotService] Hoàn kho SPCT #{}: {} -> {}", spct.getId(), oldStock, spct.getSoLuongTon());
        }
    }

    /**
     * Nhập lô mới cho biến thể đã tồn tại: Lấy LocalDateTime.now() 1 lần duy nhất cho toàn bộ SPCT đợt này.
     */
    @Transactional
    public SanPhamChiTiet nhapLoMoi(Integer representativeSpctId, int soLuongNhap, BigDecimal giaNhap, Integer idNguoiDung) {
        if (soLuongNhap <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }

        SanPhamChiTiet repSpct = sanPhamChiTietRepository.findById(representativeSpctId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể đại diện ID: " + representativeSpctId));

        SanPham sanPham = repSpct.getSanPham();
        // Khóa sản phẩm cha
        sanPhamRepository.findByIdWithLock(sanPham.getId());

        LocalDateTime thoiGianNhap = LocalDateTime.now();

        SanPhamChiTiet newLotSpct = new SanPhamChiTiet();
        newLotSpct.setSanPham(sanPham);
        newLotSpct.setGiaBan(repSpct.getGiaBan());
        newLotSpct.setGiaNhap(giaNhap);
        newLotSpct.setSoLuongTon(soLuongNhap);
        newLotSpct.setTrangThaiValue(repSpct.getTrangThaiValue() != null ? repSpct.getTrangThaiValue() : true);
        newLotSpct.setNgayTao(thoiGianNhap);
        newLotSpct.setNgayCapNhat(thoiGianNhap);

        // Tự động kế thừa hình ảnh từ biến thể đại diện
        String mainImg = repSpct.getHinhAnhSanPham();
        if (mainImg != null && !mainImg.isBlank()) {
            newLotSpct.setHinhAnhSanPham(mainImg);
        }

        if (repSpct.getHinhAnhSanPhams() != null && !repSpct.getHinhAnhSanPhams().isEmpty()) {
            for (com.smashvn.shop.entity.HinhAnhSanPham oldImg : repSpct.getHinhAnhSanPhams()) {
                com.smashvn.shop.entity.HinhAnhSanPham newImg = new com.smashvn.shop.entity.HinhAnhSanPham();
                newImg.setSanPhamChiTiet(newLotSpct);
                newImg.setUrlHinhAnh(oldImg.getUrlHinhAnh());
                newImg.setLaAnhChinh(oldImg.getLaAnhChinh());
                newImg.setThuTu(oldImg.getThuTu());
                newLotSpct.getHinhAnhSanPhams().add(newImg);
            }
        }

        // Copy thuộc tính từ representativeSpct
        Set<SanPhamChiTietThuocTinh> newAttrSet = new java.util.LinkedHashSet<>();
        if (repSpct.getSanPhamChiTietThuocTinhs() != null) {
            for (SanPhamChiTietThuocTinh oldAttr : repSpct.getSanPhamChiTietThuocTinhs()) {
                SanPhamChiTietThuocTinh newAttr = new SanPhamChiTietThuocTinh();
                newAttr.setSanPhamChiTiet(newLotSpct);
                newAttr.setThuocTinh(oldAttr.getThuocTinh());
                newAttr.setGiaTri(oldAttr.getGiaTri());
                newAttrSet.add(newAttr);
            }
        }
        newLotSpct.setSanPhamChiTietThuocTinhs(newAttrSet);

        SanPhamChiTiet savedSpct = sanPhamChiTietRepository.save(newLotSpct);


        if (idNguoiDung != null) {
            String note = String.format("Nhập lô mới cho SP '%s' (Rep ID: %d): SL=%d, Giá nhập=%s",
                    sanPham.getTenSanPham(), representativeSpctId, soLuongNhap, giaNhap);
            Long recId = savedSpct.getId() != null ? savedSpct.getId().longValue() : 0L;
            auditService.log(idNguoiDung, "SanPhamChiTiet", recId, "INSERT",
                    "", giaNhap != null ? giaNhap.toString() : "", "127.0.0.1", note, "ADMIN");

        }



        log.info("[InventoryLotService] Đã nhập lô mới SPCT #{} cho sản phẩm #{} với timestamp {}",
                savedSpct.getId(), sanPham.getId(), thoiGianNhap);

        return savedSpct;
    }

    /**
     * Tính toán gom nhóm biến thể theo AttributeKey cho 1 Sản Phẩm
     */
    @Transactional(readOnly = true)
    public List<VariantGroupDTO> calculateAggregatedVariants(Integer idSanPham) {
        List<SanPhamChiTiet> allSpcts = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        if (allSpcts.isEmpty()) return Collections.emptyList();

        Map<String, List<SanPhamChiTiet>> grouped = allSpcts.stream()
                .collect(Collectors.groupingBy(this::buildAttributeKey));

        List<VariantGroupDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<SanPhamChiTiet>> entry : grouped.entrySet()) {
            String attrKey = entry.getKey();
            List<SanPhamChiTiet> groupSpcts = entry.getValue();

            // Tìm minSpctId làm đại diện
            SanPhamChiTiet repSpct = groupSpcts.stream()
                    .min(Comparator.comparing(SanPhamChiTiet::getId))
                    .orElse(groupSpcts.get(0));

            int totalStock = groupSpcts.stream()
                    .mapToInt(s -> s.getSoLuongTon() != null ? s.getSoLuongTon() : 0)
                    .sum();

            // Tìm ảnh fallback
            String mainImageUrl = findFallbackMainImage(groupSpcts);

            VariantGroupDTO groupDto = VariantGroupDTO.builder()
                    .attributeKey(attrKey)
                    .displayTitle(getDisplayTitle(repSpct))
                    .representativeSpctId(repSpct.getId())
                    .representativeSpct(repSpct)
                    .giaBan(repSpct.getGiaBan())
                    .trangThai(repSpct.getTrangThai())
                    .isDangBan(repSpct.getTrangThaiValue() != null && repSpct.getTrangThaiValue())
                    .tongSoLuongTon(totalStock)
                    .soLuongLoActive(groupSpcts.size())
                    .hinhAnhUrl(mainImageUrl)
                    .danhSachSpctLo(groupSpcts)
                    .build();

            result.add(groupDto);
        }

        result.sort(Comparator.comparing(VariantGroupDTO::getRepresentativeSpctId));
        return result;
    }

    /**
     * Tính toán thông tin Tab Lô Hàng (Legacy Lot vs New Lots)
     */
    @Transactional(readOnly = true)
    public List<LoHangDTO> calculateLotSummaries(Integer idSanPham) {
        List<SanPhamChiTiet> allSpcts = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        if (allSpcts.isEmpty()) return Collections.emptyList();

        LocalDateTime enabledFromThreshold;
        try {
            enabledFromThreshold = LocalDateTime.parse(enabledFromStr);
        } catch (Exception e) {
            enabledFromThreshold = LocalDateTime.of(2026, 8, 7, 8, 30);
        }

        Map<String, List<SanPhamChiTiet>> lotGrouped = new LinkedHashMap<>();

        for (SanPhamChiTiet spct : allSpcts) {
            LocalDateTime ngayTao = spct.getNgayTao();
            String lotGroupKey;
            if (ngayTao == null || ngayTao.isBefore(enabledFromThreshold)) {
                lotGroupKey = "LEGACY_LOT";
            } else {
                lotGroupKey = ngayTao.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
            lotGrouped.computeIfAbsent(lotGroupKey, k -> new ArrayList<>()).add(spct);
        }

        List<LoHangDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<SanPhamChiTiet>> entry : lotGrouped.entrySet()) {
            String lotKey = entry.getKey();
            List<SanPhamChiTiet> lotSpcts = entry.getValue();

            boolean isLegacy = "LEGACY_LOT".equals(lotKey);
            int minSpctId = lotSpcts.stream().mapToInt(SanPhamChiTiet::getId).min().orElse(0);

            LocalDateTime ngayTaoLo = (lotSpcts != null && !lotSpcts.isEmpty()) ? lotSpcts.get(0).getNgayTao() : null;
            String maLoDisplay = isLegacy ? "LÔ BAN ĐẦU" :
                    String.format("LO-%d-%s-%d", idSanPham, lotKey, minSpctId);
            String thoiGianHienThi = ngayTaoLo != null ? ngayTaoLo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";


            int totalVariants = (int) lotSpcts.stream().map(this::buildAttributeKey).distinct().count();
            int inStockVariants = (int) lotSpcts.stream()
                    .filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() > 0)
                    .map(this::buildAttributeKey)
                    .distinct().count();

            int totalStock = lotSpcts.stream()
                    .mapToInt(s -> s.getSoLuongTon() != null ? s.getSoLuongTon() : 0)
                    .sum();

            BigDecimal totalCapitalValue = BigDecimal.ZERO;
            boolean isUncertain = false;
            for (SanPhamChiTiet s : lotSpcts) {
                if (s.getGiaNhap() != null && s.getSoLuongTon() != null) {
                    totalCapitalValue = totalCapitalValue.add(s.getGiaNhap().multiply(new BigDecimal(s.getSoLuongTon())));
                } else if (s.getSoLuongTon() != null && s.getSoLuongTon() > 0) {
                    isUncertain = true;
                }
            }

            LoHangDTO dto = LoHangDTO.builder()
                    .maLo(maLoDisplay)
                    .ngayTao(ngayTaoLo)
                    .thoiGianHienThi(thoiGianHienThi)
                    .isLegacyLot(isLegacy)
                    .tongSoBienThe(totalVariants)
                    .soBienTheConHang(inStockVariants)
                    .tongSoLuongTonHienTai(totalStock)
                    .tongGiaTriVonHienTai(totalCapitalValue)
                    .coGiaNhapUncertain(isUncertain)
                    .danhSachSpctInLot(lotSpcts)
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * Sinh AttributeKey chuẩn hóa từ danh sách thuộc tính của SPCT
     */
    public String buildAttributeKey(SanPhamChiTiet spct) {
        if (spct == null) return "DEFAULT";
        Collection<SanPhamChiTietThuocTinh> attrs = spct.getSanPhamChiTietThuocTinhs();
        if (attrs == null || attrs.isEmpty()) return "DEFAULT";

        com.smashvn.shop.constant.CategoryType catType = (spct.getSanPham() != null && spct.getSanPham().getDanhMuc() != null)
                ? com.smashvn.shop.constant.CategoryType.fromIdOrName(spct.getSanPham().getDanhMuc(), spct.getSanPham().getDanhMuc().getId())
                : com.smashvn.shop.constant.CategoryType.OTHER;

        List<SanPhamChiTietThuocTinh> validAttrs = attrs.stream()
                .filter(a -> a.getThuocTinh() != null && a.getGiaTri() != null && !a.getGiaTri().isBlank())
                .filter(a -> {
                    String tenTT = a.getThuocTinh().getTenThuocTinh().trim().toLowerCase();
                    if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC || catType == com.smashvn.shop.constant.CategoryType.GIAY) {
                        if (tenTT.contains("căng") || tenTT.contains("trọng lượng") || tenTT.contains("weight")) return false;
                    } else if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        if (tenTT.contains("kích thước") || tenTT.contains("size")) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(a -> a.getThuocTinh().getTenThuocTinh() != null ? a.getThuocTinh().getTenThuocTinh() : ""))
                .collect(Collectors.toList());

        if (validAttrs.isEmpty()) return "DEFAULT";

        return validAttrs.stream()
                .map(a -> a.getThuocTinh().getTenThuocTinh() + "=" + a.getGiaTri())
                .collect(Collectors.joining("|"));
    }

    /**
     * Lấy tiêu đề hiển thị cho SPCT dựa trên các thuộc tính
     */
    public String getDisplayTitle(SanPhamChiTiet spct) {
        if (spct == null) return "";
        Collection<SanPhamChiTietThuocTinh> attrs = spct.getSanPhamChiTietThuocTinhs();
        if (attrs == null || attrs.isEmpty()) return spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "";

        com.smashvn.shop.constant.CategoryType catType = (spct.getSanPham() != null && spct.getSanPham().getDanhMuc() != null)
                ? com.smashvn.shop.constant.CategoryType.fromIdOrName(spct.getSanPham().getDanhMuc(), spct.getSanPham().getDanhMuc().getId())
                : com.smashvn.shop.constant.CategoryType.OTHER;

        List<String> values = attrs.stream()
                .filter(a -> a.getThuocTinh() != null && a.getGiaTri() != null && !a.getGiaTri().isBlank())
                .filter(a -> {
                    String tenTT = a.getThuocTinh().getTenThuocTinh().trim().toLowerCase();
                    if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC || catType == com.smashvn.shop.constant.CategoryType.GIAY) {
                        if (tenTT.contains("căng") || tenTT.contains("trọng lượng") || tenTT.contains("weight")) return false;
                    } else if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        if (tenTT.contains("kích thước") || tenTT.contains("size")) return false;
                    }
                    return true;
                })
                .map(a -> a.getGiaTri() != null ? a.getGiaTri() : "")
                .collect(Collectors.toList());

        if (values.isEmpty()) return spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "";

        return String.join(" - ", values);
    }



    /**
     * Tim ảnh đại diện fallback cho nhóm SPCT
     */
    private String findFallbackMainImage(List<SanPhamChiTiet> spcts) {
        for (SanPhamChiTiet s : spcts) {
            if (s.getHinhAnhSanPhams() != null && !s.getHinhAnhSanPhams().isEmpty()) {
                for (HinhAnhSanPham ha : s.getHinhAnhSanPhams()) {
                    if (ha.getLaAnhChinh() != null && ha.getLaAnhChinh()) {
                        return ha.getUrlHinhAnh();
                    }
                }
                return s.getHinhAnhSanPhams().get(0).getUrlHinhAnh();
            }
        }
        return "/images/default-product.png";
    }
}

