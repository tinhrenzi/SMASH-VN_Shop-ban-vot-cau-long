package com.smashvn.shop.service.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnShipmentPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Persist return shipment mapping into TichHopVanChuyen table using an independent REQUIRES_NEW transaction.
     * This guarantees that even if the outer order business transaction rolls back, the GHN return shipment tracking
     * record remains committed to the DB for reconciliation on retry.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveShipment(Integer idHoaDon, String orderCode, String provider, String status) {
        try {
            jdbcTemplate.update(
                "MERGE INTO TichHopVanChuyen WITH (HOLDLOCK) AS target " +
                "USING (SELECT ? AS id_hoa_don, ? AS ma_van_don, ? AS nha_cung_cap, ? AS trang_thai) AS source " +
                "ON target.id_hoa_don = source.id_hoa_don AND target.nha_cung_cap = source.nha_cung_cap " +
                "WHEN MATCHED THEN UPDATE SET ma_van_don = source.ma_van_don, ma_don_hang_ngoai = source.ma_van_don, trang_thai = source.trang_thai " +
                "WHEN NOT MATCHED THEN INSERT (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) " +
                "VALUES (source.id_hoa_don, source.nha_cung_cap, source.ma_van_don, source.ma_van_don, source.trang_thai, GETDATE());",
                idHoaDon, orderCode, provider, status
            );
            log.info("GHN: Successfully saved shipment mapping ({}) in REQUIRES_NEW transaction for HoaDon #{}", provider, idHoaDon);
        } catch (Exception dbEx) {
            log.error("GHN: Failed to save shipment mapping in TichHopVanChuyen for HoaDon #{}: {}", idHoaDon, dbEx.getMessage(), dbEx);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveReturnShipment(Integer idHoaDon, String orderCode, String status) {
        saveShipment(idHoaDon, orderCode, "GHN_RETURN", status);
    }
}
