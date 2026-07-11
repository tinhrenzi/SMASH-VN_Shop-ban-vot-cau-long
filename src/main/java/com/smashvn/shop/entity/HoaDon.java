package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HoaDon")
@EntityListeners(HoaDonEntityListener.class)
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
    @JoinColumn(name = "id_don_vi_van_chuyen")
    private DonViVanChuyen donViVanChuyen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dia_chi")
    private SoDiaChi diaChi;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "tien_hang", nullable = false)
    private BigDecimal tongTienHang = BigDecimal.ZERO;

    @Column(name = "phi_van_chuyen", nullable = false)
    private BigDecimal phiVanChuyen = BigDecimal.ZERO;

    @Column(name = "so_tien_giam_gia", nullable = false)
    private BigDecimal soTienGiamVoucher = BigDecimal.ZERO;

    @Column(name = "tong_tien", nullable = false)
    private BigDecimal tongTien;

    @Column(name = "trang_thai_don_hang", nullable = false, length = 50)
    private String trangThaiDonHang = "CHO_XAC_NHAN";

    @Column(name = "trang_thai_thanh_toan", nullable = false, length = 50)
    private String trangThaiThanhToan = "CHO_THANH_TOAN";

    @Column(name = "ten_nguoi_nhan", length = 100)
    private String tenNguoiNhan;

    @Column(name = "sdt_nhan", nullable = false, length = 15)
    private String sdtNhan;

    @Column(name = "dia_chi_nhan", nullable = false, length = 500)
    private String diaChiNhan;

    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    @Column(name = "ly_do_hoan_tien", length = 500)
    private String lyDoHoanTien;

    @Transient
    private String maDonHang;

    public String getMaDonHang() {
        if (id == null) {
            return maDonHang;
        }
        String prefix = (nhanVien != null) ? "HDSVN" : "DHSVN";
        String dateStr = ngayTao != null ? ngayTao.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) : "20260708";
        return prefix + dateStr + "-" + id;
    }

    public void setMaDonHang(String maDonHang) {
        this.maDonHang = maDonHang;
    }

    @Transient
    private String paymentMethod;

    @Transient
    private String paymentStatus;

    @Transient
    private String maGiaoDich;

    @Transient
    private String transactionId;

    @Transient
    private String gatewayResponse;

    @org.hibernate.annotations.Formula("(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id)")
    private String ghnOrderCode;

    @org.hibernate.annotations.Formula("(SELECT TOP 1 t.trang_thai FROM TichHopVanChuyen t WHERE t.id_hoa_don = id)")
    private String ghnStatus;

    @Transient
    private Integer ghnToDistrictId;

    @Transient
    private String ghnToWardCode;

    @Transient
    private String maVoucherApDung;

    @Transient
    private String tenVoucherApDung;

    @Transient
    private String moTaVoucherSnapshot;

    @Transient
    private String ghiChu;

    @Transient
    private String nguoiXacNhanThanhToan;

    @Transient
    private LocalDateTime thoiGianXacNhan;

    @Transient
    private ReturnStatus trangThaiHoanHang;

    @Transient
    private LocalDateTime ngayXacNhanHoanHang;

    @Transient
    private NhanVien nhanVienXacNhan;

    @org.hibernate.annotations.Formula("CASE WHEN trang_thai_thanh_toan = 'REFUNDED' THEN 'COMPLETED' WHEN trang_thai_thanh_toan = 'CHO_HOAN_TIEN' THEN 'PENDING' ELSE NULL END")
    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

    @Transient
    private LocalDateTime refundTime;

    @Transient
    private NhanVien refundConfirmedBy;

    // Getters overriding Lombok for NULL safety
    public BigDecimal getSoTienGiamVoucher() {
        return soTienGiamVoucher == null ? BigDecimal.ZERO : soTienGiamVoucher;
    }

    public BigDecimal getTongTienHang() {
        return tongTienHang == null ? BigDecimal.ZERO : tongTienHang;
    }

    public BigDecimal getTongTien() {
        return tongTien == null ? BigDecimal.ZERO : tongTien;
    }

    public BigDecimal getPhiVanChuyen() {
        return phiVanChuyen == null ? BigDecimal.ZERO : phiVanChuyen;
    }

    public String getPaymentMethod() {
        if (paymentMethod != null) {
            return paymentMethod.toLowerCase();
        }
        if (phuongThucThanhToan != null && phuongThucThanhToan.getTenPhuongThuc() != null) {
            return phuongThucThanhToan.getTenPhuongThuc().toLowerCase();
        }
        return null;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : trangThaiThanhToan;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        this.trangThaiThanhToan = paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return ngayThanhToan;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.ngayThanhToan = paidAt;
    }

    @PrePersist
    @PreUpdate
    public void normalizeAmounts() {
        if (this.soTienGiamVoucher == null) {
            this.soTienGiamVoucher = BigDecimal.ZERO;
        }
        if (this.tongTienHang == null) {
            this.tongTienHang = BigDecimal.ZERO;
        }
        if (this.tongTien == null) {
            this.tongTien = BigDecimal.ZERO;
        }
        if (this.phiVanChuyen == null) {
            this.phiVanChuyen = BigDecimal.ZERO;
        }
    }
}
