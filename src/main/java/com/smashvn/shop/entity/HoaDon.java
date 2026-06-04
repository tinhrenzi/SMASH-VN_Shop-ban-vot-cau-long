package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HoaDon")
@Data
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien; // Có thể null

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_thanh_toan", nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia; // Có thể null

    @ManyToOne
    @JoinColumn(name = "id_don_vi_van_chuyen", nullable = false)
    private DonViVanChuyen donViVanChuyen;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "tong_tien", nullable = false)
    private BigDecimal tongTien;

    @Column(name = "trang_thai_don_hang", nullable = false, length = 50)
    private String trangThaiDonHang = "cho_xac_nhan";

    @Column(name = "trang_thai_thanh_toan", nullable = false, length = 50)
    private String trangThaiThanhToan = "CHO_THANH_TOAN";

    @Column(name = "dia_chi_nhan", nullable = false, length = 500)
    private String diaChiNhan;

    @Column(name = "sdt_nhan", nullable = false, length = 15)
    private String sdtNhan;

    // POS-specific fields
    /** Ghi chú hóa đơn (nhân viên hoặc khách hàng) */
    @Column(name = "ghi_chu", length = 500, columnDefinition = "NVARCHAR(500)")
    private String ghiChu;

    /** Mã giao dịch chuyển khoản (nếu có) */
    @Column(name = "ma_giao_dich", length = 100, columnDefinition = "NVARCHAR(100)")
    private String maGiaoDich;

    /** Người xác nhận thanh toán (tên nhân viên) */
    @Column(name = "nguoi_xac_nhan_thanh_toan", length = 100, columnDefinition = "NVARCHAR(100)")
    private String nguoiXacNhanThanhToan;

    /** Thời gian xác nhận thanh toán */
    @Column(name = "thoi_gian_xac_nhan")
    private LocalDateTime thoiGianXacNhan;
}
