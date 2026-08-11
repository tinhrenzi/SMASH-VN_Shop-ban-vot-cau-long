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
    private final com.smashvn.shop.repository.EditLogRepository editLogRepository;
    private final com.smashvn.shop.repository.PhieuNhapRepository phieuNhapRepository;
    private final com.smashvn.shop.repository.PhieuNhapChiTietRepository phieuNhapChiTietRepository;
    private final com.smashvn.shop.repository.NhanVienRepository nhanVienRepository;
    private final AuditService auditService;

    @Value("${inventory.lot.enabled-from:2026-08-07T08:30:00}")
    private String enabledFromStr;

    /**
     * Sinh mã phiếu nhập tự động theo định dạng PN000001, PN000002...
     */
    public synchronized String generateMaPhieuNhap() {
        Integer maxId = phieuNhapRepository.findMaxId();
        int nextId = (maxId != null) ? maxId + 1 : 1;
        String code = String.format("PN%06d", nextId);
        while (phieuNhapRepository.findByMaPhieuNhap(code).isPresent()) {
            nextId++;
            code = String.format("PN%06d", nextId);
        }
        return code;
    }

    /**
     * Phân bổ tồn kho FIFO hai giai đoạn (Two-Phase Allocation)
     */
    @Transactional
    public AllocationResult allocateFifo(List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return new AllocationResult(AllocationStatus.SUCCESS, Collections.emptyList(), "Danh sách yêu cầu rỗng");
        }

        Map<Integer, SanPhamChiTiet> repSpctMap = new HashMap<>();
        for (OrderItemRequest req : itemRequests) {
            SanPhamChiTiet rep = sanPhamChiTietRepository.findById(req.getRepresentativeSpctId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể đại diện ID: " + req.getRepresentativeSpctId()));
            repSpctMap.put(req.getRepresentativeSpctId(), rep);
        }

        List<Integer> sortedProductIds = repSpctMap.values().stream()
                .map(spct -> spct.getSanPham().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<Integer, SanPham> lockedProductMap = new HashMap<>();
        for (Integer spId : sortedProductIds) {
            SanPham sp = sanPhamRepository.findByIdWithLock(spId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + spId));
            lockedProductMap.put(spId, sp);
        }

        List<LotAllocation> plannedAllocations = new ArrayList<>();
        boolean hasInsufficientStock = false;
        StringBuilder errorMessageBuilder = new StringBuilder();

        for (OrderItemRequest req : itemRequests) {
            SanPhamChiTiet repSpct = repSpctMap.get(req.getRepresentativeSpctId());
            String targetAttrKey = buildAttributeKey(repSpct);
            Integer productId = repSpct.getSanPham().getId();

            List<SanPhamChiTiet> candidates = sanPhamChiTietRepository.findActiveCandidatesBySanPhamId(productId);
            List<SanPhamChiTiet> matchingCandidates = candidates.stream()
                    .filter(c -> buildAttributeKey(c).equals(targetAttrKey))
                    .sorted(Comparator.comparing(SanPhamChiTiet::getId))
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

        if (hasInsufficientStock) {
            log.warn("[InventoryLotService] Phân bổ FIFO thất bại do thiếu kho: {}", errorMessageBuilder.toString());
            return new AllocationResult(AllocationStatus.INSUFFICIENT_STOCK, Collections.emptyList(), errorMessageBuilder.toString().trim());
        }

        for (LotAllocation alloc : plannedAllocations) {
            SanPhamChiTiet spct = alloc.allocatedSpct();
            int newStock = spct.getSoLuongTon() - alloc.quantityAllocated();
            spct.setSoLuongTon(newStock);
            sanPhamChiTietRepository.save(spct);
        }

        log.info("[InventoryLotService] Phân bổ FIFO thành công cho {} dòng hàng", itemRequests.size());
        return new AllocationResult(AllocationStatus.SUCCESS, plannedAllocations, "Phân bổ tồn kho FIFO thành công");
    }

    @Transactional
    public void hoanKho(List<RestockItemRequest> requests) {
        hoanKhoHangLoat(requests);
    }

    @Transactional
    public void hoanKhoHangLoat(List<RestockItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        List<RestockItemRequest> validRequests = requests.stream()
                .filter(r -> r.isConBanDuoc() && r.getQuantityToRestock() > 0 && r.getIdSanPhamChiTiet() != null)
                .collect(Collectors.toList());

        if (validRequests.isEmpty()) return;

        Map<Integer, SanPhamChiTiet> spctMap = new HashMap<>();
        for (RestockItemRequest req : validRequests) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(req.getIdSanPhamChiTiet())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SPCT ID: " + req.getIdSanPhamChiTiet()));
            spctMap.put(req.getIdSanPhamChiTiet(), spct);
        }

        List<Integer> sortedProductIds = spctMap.values().stream()
                .map(spct -> spct.getSanPham().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (Integer spId : sortedProductIds) {
            sanPhamRepository.findByIdWithLock(spId);
        }

        for (RestockItemRequest req : validRequests) {
            SanPhamChiTiet spct = spctMap.get(req.getIdSanPhamChiTiet());
            int oldStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            spct.setSoLuongTon(oldStock + req.getQuantityToRestock());
            sanPhamChiTietRepository.save(spct);
            log.info("[InventoryLotService] Hoàn kho SPCT #{}: {} -> {}", spct.getId(), oldStock, spct.getSoLuongTon());
        }
    }

    /**
     * Nhập hàng cho biến thể: Tạo PhieuNhap + PhieuNhapChiTiet và tính lại Giá vốn bình quân (Weighted Average Cost)
     */
    @Transactional
    public SanPhamChiTiet nhapLoMoi(Integer representativeSpctId, int soLuongNhap, BigDecimal giaNhap, Integer idNguoiDung) {
        return nhapLoMoi(representativeSpctId, soLuongNhap, giaNhap, idNguoiDung, null);
    }

    @Transactional
    public SanPhamChiTiet nhapLoMoi(Integer representativeSpctId, int soLuongNhap, BigDecimal giaNhap, Integer idNguoiDung, String ghiChu) {
        if (soLuongNhap <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        if (giaNhap == null || giaNhap.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá nhập phải lớn hơn 0");
        }

        SanPhamChiTiet targetSpct = sanPhamChiTietRepository.findById(representativeSpctId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể ID: " + representativeSpctId));

        SanPham sanPham = targetSpct.getSanPham();
        sanPhamRepository.findByIdWithLock(sanPham.getId());

        // Lấy NhanVien
        NhanVien nhanVien = null;
        if (idNguoiDung != null) {
            nhanVien = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
        }
        if (nhanVien == null) {
            List<NhanVien> allNv = nhanVienRepository.findAll();
            if (!allNv.isEmpty()) {
                nhanVien = allNv.get(0);
            }
        }
        if (nhanVien == null) {
            throw new IllegalStateException("Không tìm thấy nhân viên thực hiện nhập hàng");
        }

        LocalDateTime now = LocalDateTime.now();
        String maPN = generateMaPhieuNhap();

        BigDecimal thanhTien = giaNhap.multiply(BigDecimal.valueOf(soLuongNhap));

        PhieuNhap phieuNhap = PhieuNhap.builder()
                .maPhieuNhap(maPN)
                .nhanVien(nhanVien)
                .ngayNhap(now)
                .tongTien(thanhTien)
                .ghiChu((ghiChu != null && !ghiChu.isBlank()) ? ghiChu : ("Nhập hàng cho SP " + sanPham.getTenSanPham()))
                .ngayTao(now)
                .ngayCapNhat(now)
                .build();

        PhieuNhapChiTiet pnct = PhieuNhapChiTiet.builder()
                .phieuNhap(phieuNhap)
                .sanPhamChiTiet(targetSpct)
                .soLuong(soLuongNhap)
                .giaNhap(giaNhap)
                .thanhTien(thanhTien)
                .build();

        phieuNhap.getChiTietList().add(pnct);
        PhieuNhap savedPN = phieuNhapRepository.save(phieuNhap);

        // Tính Giá Vốn Bình Quân Gia Quyền (Weighted Average Purchase Cost)
        int tonCu = targetSpct.getSoLuongTon() != null ? targetSpct.getSoLuongTon() : 0;
        int tonMoi = tonCu + soLuongNhap;
        BigDecimal giaVonCu = targetSpct.getGiaNhap() != null ? targetSpct.getGiaNhap() : BigDecimal.ZERO;

        BigDecimal giaVonMoi;
        if (tonCu > 0 && giaVonCu.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalOldCost = giaVonCu.multiply(BigDecimal.valueOf(tonCu));
            BigDecimal totalNewCost = giaNhap.multiply(BigDecimal.valueOf(soLuongNhap));
            giaVonMoi = totalOldCost.add(totalNewCost).divide(BigDecimal.valueOf(tonMoi), 2, java.math.RoundingMode.HALF_UP);
        } else {
            giaVonMoi = giaNhap;
        }

        targetSpct.setSoLuongTon(tonMoi);
        targetSpct.setGiaNhap(giaVonMoi);
        targetSpct.setNgayCapNhat(now);

        SanPhamChiTiet savedSpct = sanPhamChiTietRepository.save(targetSpct);

        if (idNguoiDung != null) {
            String note = String.format("Nhập hàng [%s] cho biến thể #%d SP '%s': Thêm SL=%d (Tồn cũ=%d -> Tồn mới=%d), Giá nhập=%s, Giá vốn bình quân mới=%s",
                    maPN, representativeSpctId, sanPham.getTenSanPham(), soLuongNhap, tonCu, tonMoi, giaNhap, giaVonMoi);
            auditService.log(idNguoiDung, "SanPhamChiTiet", representativeSpctId.longValue(), "UPDATE",
                    String.valueOf(tonCu), String.valueOf(tonMoi), "127.0.0.1", note, "ADMIN");
        }

        log.info("[InventoryLotService] Đã nhập hàng [{}] SPCT #{}: SL nhập={}, Giá nhập={}, Tồn cũ {} -> {}, Giá vốn cũ {} -> mới {}",
                maPN, savedSpct.getId(), soLuongNhap, giaNhap, tonCu, tonMoi, giaVonCu, giaVonMoi);

        return savedSpct;
    }

    /**
     * Lấy lịch sử các đợt nhập hàng theo PhieuNhapChiTiet cho 1 biến thể cụ thể
     */
    @Transactional(readOnly = true)
    public List<PhieuNhapChiTietDTO> getLichSuPhieuNhapBySpct(Integer idSpct) {
        if (idSpct == null) return Collections.emptyList();

        List<PhieuNhapChiTiet> list = phieuNhapChiTietRepository.findBySpctIdWithReceiptDetails(idSpct);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<PhieuNhapChiTietDTO> dtos = new ArrayList<>();

        for (PhieuNhapChiTiet pnct : list) {
            PhieuNhap pn = pnct.getPhieuNhap();
            SanPhamChiTiet spct = pnct.getSanPhamChiTiet();
            String phanLoai = spct != null ? spct.getPhanLoaiHienThi() : "";
            String nhanVienName = (pn != null && pn.getNhanVien() != null) ? pn.getNhanVien().getHoTen() : "N/A";

            PhieuNhapChiTietDTO dto = PhieuNhapChiTietDTO.builder()
                    .id(pnct.getId())
                    .idPhieuNhap(pn != null ? pn.getId() : null)
                    .maPhieuNhap(pn != null ? pn.getMaPhieuNhap() : "N/A")
                    .ngayNhap(pn != null ? pn.getNgayNhap() : null)
                    .ngayNhapHienThi(pn != null && pn.getNgayNhap() != null ? pn.getNgayNhap().format(dtf) : "N/A")
                    .idSpct(idSpct)
                    .phanLoaiHienThi(phanLoai)
                    .soLuongNhap(pnct.getSoLuong())
                    .giaNhap(pnct.getGiaNhap())
                    .thanhTien(pnct.getThanhTien())
                    .tenNhanVien(nhanVienName)
                    .ghiChu(pn != null ? pn.getGhiChu() : "")
                    .build();
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Lấy tổng quan (Summary) về tồn kho và lịch sử nhập hàng cho 1 biến thể cụ thể
     */
    @Transactional(readOnly = true)
    public BienTheImportSummaryDTO getSummaryBySpct(Integer idSpct) {
        if (idSpct == null) return new BienTheImportSummaryDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);

        Optional<SanPhamChiTiet> spctOpt = sanPhamChiTietRepository.findById(idSpct);
        if (spctOpt.isEmpty()) return new BienTheImportSummaryDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);

        SanPhamChiTiet spct = spctOpt.get();
        Long totalImported = phieuNhapChiTietRepository.sumSoLuongNhapBySpctId(idSpct);
        Long countImportTimes = phieuNhapChiTietRepository.countSoLanNhapBySpctId(idSpct);

        int currentStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
        long sumImported = (totalImported != null && totalImported > 0) ? totalImported : (long) currentStock;
        long importTimes = (countImportTimes != null && countImportTimes > 0) ? countImportTimes : (currentStock > 0 ? 1L : 0L);

        return BienTheImportSummaryDTO.builder()
                .tonKhoHienTai(currentStock)
                .giaVonBinhQuan(spct.getGiaNhap() != null ? spct.getGiaNhap() : BigDecimal.ZERO)
                .giaBanHienTai(spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO)
                .tongSoLuongTungNhap(sumImported)
                .soLanNhapHang(importTimes)
                .build();
    }

    /**
     * Lấy thông tin chi tiết một phiếu nhập (dùng cho Modal xem chi tiết phiếu nhập)
     */
    @Transactional(readOnly = true)
    public PhieuNhapDetailDTO getPhieuNhapDetail(Integer idPhieuNhap) {
        PhieuNhap pn = phieuNhapRepository.findById(idPhieuNhap)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhập ID: " + idPhieuNhap));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String nvName = (pn.getNhanVien() != null) ? pn.getNhanVien().getHoTen() : "N/A";

        List<PhieuNhapDetailDTO.ItemDetail> items = new ArrayList<>();
        if (pn.getChiTietList() != null) {
            for (PhieuNhapChiTiet pnct : pn.getChiTietList()) {
                SanPhamChiTiet spct = pnct.getSanPhamChiTiet();
                String tenSP = (spct != null && spct.getSanPham() != null) ? spct.getSanPham().getTenSanPham() : "";
                String phanLoai = spct != null ? spct.getPhanLoaiHienThi() : "";

                items.add(PhieuNhapDetailDTO.ItemDetail.builder()
                        .idSpct(spct != null ? spct.getId() : null)
                        .tenSanPham(tenSP)
                        .phanLoaiHienThi(phanLoai)
                        .soLuong(pnct.getSoLuong())
                        .giaNhap(pnct.getGiaNhap())
                        .thanhTien(pnct.getThanhTien())
                        .build());
            }
        }

        return PhieuNhapDetailDTO.builder()
                .id(pn.getId())
                .maPhieuNhap(pn.getMaPhieuNhap())
                .ngayNhap(pn.getNgayNhap())
                .ngayNhapHienThi(pn.getNgayNhap() != null ? pn.getNgayNhap().format(dtf) : "N/A")
                .tenNhanVien(nvName)
                .ghiChu(pn.getGhiChu())
                .tongTien(pn.getTongTien())
                .chiTietList(items)
                .build();
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
            String maLoDisplay = isLegacy ? "LÔ KHỞI TẠO (KHO BAN ĐẦU)" :

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
     * Lấy lịch sử nhập hàng thực tế từ PhieuNhapChiTiet hoặc EditLog audit trail cho sản phẩm
     */
    @Transactional(readOnly = true)
    public List<LichSuNhapHangDTO> getLichSuNhapHang(Integer idSanPham) {
        List<SanPhamChiTiet> spcts = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        if (spcts.isEmpty()) return Collections.emptyList();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // 1. Ưu tiên lấy dữ liệu từ PhieuNhapChiTiet
        List<PhieuNhapChiTiet> pncts = phieuNhapChiTietRepository.findBySanPhamIdWithReceiptDetails(idSanPham);
        if (!pncts.isEmpty()) {
            List<LichSuNhapHangDTO> result = new ArrayList<>();
            for (PhieuNhapChiTiet pnct : pncts) {
                PhieuNhap pn = pnct.getPhieuNhap();
                SanPhamChiTiet spct = pnct.getSanPhamChiTiet();
                String phanLoai = spct != null ? spct.getPhanLoaiHienThi() : "";
                String nvName = (pn != null && pn.getNhanVien() != null) ? pn.getNhanVien().getHoTen() : "Admin";

                LichSuNhapHangDTO dto = LichSuNhapHangDTO.builder()
                        .id(pnct.getId())
                        .idPhieuNhap(pn != null ? pn.getId() : null)
                        .maPhieuNhap(pn != null ? pn.getMaPhieuNhap() : "PN-LEGACY")
                        .idSpct(spct != null ? spct.getId() : null)
                        .phanLoaiHienThi(phanLoai)
                        .thoiGianNhap(pn != null ? pn.getNgayNhap() : null)
                        .thoiGianHienThi((pn != null && pn.getNgayNhap() != null) ? pn.getNgayNhap().format(dtf) : "N/A")
                        .soLuongNhap(pnct.getSoLuong())
                        .giaNhap(pnct.getGiaNhap())
                        .nguoiThucHien(nvName)
                        .ghiChu(pn != null ? pn.getGhiChu() : "")
                        .build();
                result.add(dto);
            }
            return result;
        }

        // 2. Fallback: Parse dữ liệu từ EditLog
        Map<Integer, SanPhamChiTiet> spctMap = spcts.stream()
                .collect(Collectors.toMap(SanPhamChiTiet::getId, s -> s, (s1, s2) -> s1));
        List<Integer> spctIds = new ArrayList<>(spctMap.keySet());

        List<EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiInOrderByThoiGianDesc("SanPhamChiTiet", spctIds);
        List<LichSuNhapHangDTO> result = new ArrayList<>();

        for (EditLog logItem : logs) {
            SanPhamChiTiet spct = spctMap.get(logItem.getIdBanGhi());
            if (spct == null) continue;

            String phanLoai = spct.getPhanLoaiHienThi();
            String ghiChu = logItem.getGhiChu() != null ? logItem.getGhiChu() : "";

            int soLuongNhap = 0;
            Integer tonCu = null;
            Integer tonMoi = null;
            BigDecimal giaNhap = spct.getGiaNhap();
            String maPN = "PN-LEGACY";

            if (ghiChu.contains("[") && ghiChu.contains("]")) {
                try {
                    String extracted = ghiChu.substring(ghiChu.indexOf("[") + 1, ghiChu.indexOf("]")).trim();
                    if (extracted.startsWith("PN")) {
                        maPN = extracted;
                    }
                } catch (Exception ignored) {}
            }

            try {
                if (logItem.getGiaTriCu() != null && !logItem.getGiaTriCu().isBlank()
                        && logItem.getGiaTriMoi() != null && !logItem.getGiaTriMoi().isBlank()) {
                    tonCu = Integer.parseInt(logItem.getGiaTriCu().trim());
                    tonMoi = Integer.parseInt(logItem.getGiaTriMoi().trim());
                    soLuongNhap = Math.max(0, tonMoi - tonCu);
                }
            } catch (Exception ignored) {}

            if (ghiChu.contains("Giá nhập=")) {
                try {
                    String sub = ghiChu.substring(ghiChu.indexOf("Giá nhập=") + 9).trim();
                    if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(",")).trim();
                    if (sub.contains(" ")) sub = sub.substring(0, sub.indexOf(" ")).trim();
                    if (!sub.isEmpty() && !sub.equalsIgnoreCase("null")) {
                        giaNhap = new BigDecimal(sub);
                    }
                } catch (Exception ignored) {}
            }

            if (soLuongNhap <= 0 && ghiChu.contains("SL=")) {
                try {
                    String sub = ghiChu.substring(ghiChu.indexOf("SL=") + 3);
                    if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
                    if (sub.contains(" ")) sub = sub.substring(0, sub.indexOf(" "));
                    soLuongNhap = Integer.parseInt(sub.trim());
                } catch (Exception ignored) {}
            }

            String nguoiThucHien = logItem.getTaiKhoan() != null ? logItem.getTaiKhoan().getUsername() : "Admin";

            LichSuNhapHangDTO dto = LichSuNhapHangDTO.builder()
                    .id(logItem.getId())
                    .idPhieuNhap(null)
                    .maPhieuNhap(maPN)
                    .idSpct(spct.getId())
                    .phanLoaiHienThi(phanLoai)
                    .thoiGianNhap(logItem.getThoiGian())
                    .thoiGianHienThi(logItem.getThoiGian() != null ? logItem.getThoiGian().format(dtf) : "N/A")
                    .soLuongNhap(soLuongNhap > 0 ? soLuongNhap : (tonMoi != null ? tonMoi : (spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)))
                    .tonCu(tonCu)
                    .tonMoi(tonMoi)
                    .giaNhap(giaNhap)
                    .nguoiThucHien(nguoiThucHien)
                    .ghiChu(ghiChu)
                    .build();

            result.add(dto);
        }

        if (result.isEmpty()) {
            for (SanPhamChiTiet spct : spcts) {
                LocalDateTime time = spct.getNgayTao() != null ? spct.getNgayTao() : LocalDateTime.now();
                LichSuNhapHangDTO dto = LichSuNhapHangDTO.builder()
                        .id(spct.getId())
                        .idPhieuNhap(null)
                        .maPhieuNhap("PN-LEGACY")
                        .idSpct(spct.getId())
                        .phanLoaiHienThi(spct.getPhanLoaiHienThi())
                        .thoiGianNhap(time)
                        .thoiGianHienThi(time.format(dtf))
                        .soLuongNhap(spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                        .tonCu(0)
                        .tonMoi(spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                        .giaNhap(spct.getGiaNhap())
                        .nguoiThucHien("Hệ thống")
                        .ghiChu("Khởi tạo ban đầu")
                        .build();
                result.add(dto);
            }
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

