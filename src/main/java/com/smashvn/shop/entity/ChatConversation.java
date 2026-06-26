package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatConversation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @Column(name = "tieu_de", length = 255)
    private String tieuDe;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat = LocalDateTime.now();

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "trang_thai", length = 20)
    private String trangThai = "ACTIVE";
}
