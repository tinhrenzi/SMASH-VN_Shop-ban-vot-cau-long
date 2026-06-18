package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho một PHIẾU GIẢM GIÁ (Voucher) trong hệ thống.
 *
 * <p>Khác với {@link DotGiamGia} (áp dụng tự động lên sản phẩm), phiếu giảm giá
 * yêu cầu khách hàng nhập MÃ PHIẾU tại trang thanh toán. Hệ thống sẽ kiểm tra
 * tính hợp lệ và trừ tiền vào tổng đơn hàng.</p>
 *
 * <p>Có hai loại phiếu:</p>
 * <ul>
 *   <li><b>Giảm trực tiếp (VND)</b>: trừ thẳng một số tiền cố định, ví dụ: -50.000đ.</li>
 *   <li><b>Giảm phần trăm (%)</b>: tính theo % tổng đơn, có thể bị giới hạn bởi
 *       {@code giaTriGiamToiDa}.</li>
 * </ul>
 *
 * <p>Logic tính giảm giá được tập trung tại {@code VoucherCalculator.calculateVoucherDiscount()}.</p>
 */
@Entity
@Table(name = "PhieuGiamGia")
@Data
public class PhieuGiamGia {

    /** Khóa chính, tự tăng. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Mã phiếu mà khách hàng nhập để áp dụng giảm giá.
     * Luôn lưu dạng CHỮ IN HOA, độ dài 2–50 ký tự, chỉ gồm A-Z, 0-9, dấu gạch dưới (_).
     * Ví dụ: {@code "SUMMER20"}, {@code "SALE_100K"}.
     */
    @Column(name = "ma_phieu", nullable = false, unique = true, length = 50)
    private String maPhieu;

    /**
     * Giá trị giảm. Ý nghĩa phụ thuộc vào {@code donVi}:
     * <ul>
     *   <li>Nếu {@code donVi = "%"}: giá trị là phần trăm giảm (1–100).</li>
     *   <li>Nếu {@code donVi = "VND"}: giá trị là số tiền giảm cố định (đơn vị đồng).</li>
     * </ul>
     */
    @Column(name = "gia_tri", nullable = false)
    private BigDecimal giaTri;

    /**
     * Đơn vị tính của giá trị giảm. Chỉ nhận một trong hai giá trị:
     * <ul>
     *   <li>{@code "%"}   – phiếu giảm theo phần trăm.</li>
     *   <li>{@code "VND"} – phiếu giảm số tiền cố định.</li>
     * </ul>
     */
    @Column(name = "don_vi", nullable = false, length = 10)
    private String donVi;

    /** Ngày bắt đầu hiệu lực của phiếu giảm giá. Bắt buộc nhập. */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    /** Ngày hết hạn của phiếu giảm giá. Bắt buộc nhập, phải sau ngày bắt đầu. */
    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    /**
     * Số lượng phiếu còn có thể sử dụng. Mỗi lần khách đặt hàng thành công,
     * số này bị trừ đi 1. Khi về 0, phiếu không còn dùng được nữa.
     * Tại trang chỉnh sửa, admin được phép đặt lại về 0 (hàng hết).
     */
    @Column(name = "so_luong_con_lai", nullable = false)
    private Integer soLuongConLai;

    /**
     * Phân loại phiếu giảm giá (song song với {@code donVi}):
     * <ul>
     *   <li>{@code "Giảm trực tiếp"} tương ứng với {@code donVi = "VND"}.</li>
     *   <li>{@code "Giảm phần trăm"} tương ứng với {@code donVi = "%"}.</li>
     * </ul>
     * Hai trường này phải nhất quán; service sẽ throw exception nếu không khớp.
     */
    @Column(name = "loai_giam_gia", nullable = false, length = 100)
    private String loaiGiamGia;

    /**
     * Nhân viên đã tạo / cập nhật phiếu này. Dùng để tra cứu lịch sử.
     */
    @ManyToOne
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    /**
     * Cờ bật/tắt thủ công (soft-delete).
     * {@code false} → phiếu bị vô hiệu hóa, khách không thể dùng dù còn hạn và còn số lượng.
     */
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Giá trị tổng đơn hàng tối thiểu để phiếu có hiệu lực.
     * Mặc định là 0 (không yêu cầu giá trị tối thiểu).
     * Ví dụ: 200.000đ → chỉ áp dụng khi đơn hàng ≥ 200.000đ.
     */
    @Column(name = "gia_tri_don_hang_toi_thieu")
    private BigDecimal giaTriDonHangToiThieu = BigDecimal.ZERO;

    /**
     * Mức giảm tối đa tính bằng VNĐ, chỉ có ý nghĩa khi {@code donVi = "%"}.
     * Nếu {@code null}: không giới hạn mức giảm.
     * Ví dụ: giaTri = 30%, giaTriGiamToiDa = 100.000đ → dù đơn lớn đến đâu,
     * cũng chỉ giảm tối đa 100.000đ.
     * Với phiếu giảm trực tiếp (VND), trường này luôn là {@code null}.
     */
    @Column(name = "gia_tri_giam_toi_da")
    private BigDecimal giaTriGiamToiDa;

    /**
     * Trả về {@code active}, mặc định {@code true} nếu cột chưa có dữ liệu (NULL trong DB).
     */
    public Boolean getActive() {
        return active == null ? true : active;
    }

    /**
     * Trả về {@code giaTriDonHangToiThieu}, mặc định {@code 0} nếu chưa được set.
     */
    public BigDecimal getGiaTriDonHangToiThieu() {
        return giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu;
    }

    /**
     * Tính trạng thái thực tế của phiếu giảm giá tại thời điểm hiện tại.
     * Sử dụng trên giao diện admin để hiển thị badge màu tương ứng.
     *
     * <ul>
     *   <li>{@code "INACTIVE"} – bị vô hiệu hóa thủ công ({@code active = false}).</li>
     *   <li>{@code "UPCOMING"} – chưa đến ngày bắt đầu.</li>
     *   <li>{@code "EXPIRED"}  – hết hạn hoặc hết số lượng.</li>
     *   <li>{@code "ACTIVE"}   – đang hoạt động, khách có thể dùng.</li>
     * </ul>
     *
     * @return chuỗi trạng thái, không lưu vào cơ sở dữ liệu.
     */
    public String getDynamicStatus() {
        if (active != null && !active) {
            return "INACTIVE";
        }
        LocalDateTime now = LocalDateTime.now();
        if (ngayBatDau != null && now.isBefore(ngayBatDau)) {
            return "UPCOMING";
        } else if (ngayKetThuc != null && now.isAfter(ngayKetThuc)) {
            return "EXPIRED";
        } else if (soLuongConLai != null && soLuongConLai <= 0) {
            // Hết số lượng → coi như hết hạn để hiển thị badge xám
            return "EXPIRED";
        } else {
            return "ACTIVE";
        }
    }
}
