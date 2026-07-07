package com.smashvn.shop;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest
public class SeedDatabaseTest {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private TrangThaiGioHangRepository trangThaiGioHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private DanhGiaAnhRepository danhGiaAnhRepository;

    @Test
    public void doSeed() {
        System.out.println("=== BAT DAU SEED CSDL MOI ===");
        
        // 1. Seed TrangThaiGioHang
        if (trangThaiGioHangRepository.count() == 0) {
            TrangThaiGioHang tt = new TrangThaiGioHang();
            tt.setTenTrangThai("Hoạt động");
            trangThaiGioHangRepository.save(tt);
            System.out.println("-> Da seed TrangThaiGioHang");
        }

        // 2. Seed PhuongThucThanhToan
        seedPaymentMethod("cod", "Thanh toán khi nhận hàng (COD)");
        seedPaymentMethod("zalopay", "Ví điện tử ZaloPay");
        seedPaymentMethod("sepay", "Chuyển khoản Ngân hàng (Qua SePay)");

        // 3. Seed DanhMuc
        DanhMuc dmVot = seedDanhMuc("Vợt cầu lông");
        DanhMuc dmGiay = seedDanhMuc("Giày cầu lông");
        DanhMuc dmPhuKien = seedDanhMuc("Phụ kiện cầu lông");
        DanhMuc dmBao = seedDanhMuc("Bao vợt / Balo");
        DanhMuc dmAo = seedDanhMuc("Quần áo cầu lông");

        // 4. Seed ThuongHieu
        ThuongHieu thYonex = seedThuongHieu("Yonex");
        ThuongHieu thVictor = seedThuongHieu("Victor");
        ThuongHieu thLining = seedThuongHieu("Li-Ning");
        ThuongHieu thMizuno = seedThuongHieu("Mizuno");

        // 5. Seed Accounts & Profiles
        // 5.1. Admin/Manager Account (QL)
        TaiKhoan tkAdmin = seedAccount("admin@smashvn.com", "Admin@123", "QL");
        NhanVien nvAdmin = seedNhanVien(tkAdmin, "Hệ thống quản trị", "Quản lý", "0901234567");

        // 5.2. Staff Account (NV)
        TaiKhoan tkStaff = seedAccount("staff@smashvn.com", "Staff@123", "NV");
        NhanVien nvStaff = seedNhanVien(tkStaff, "Nguyễn Văn Nhân Viên", "Nhân viên bán hàng", "0907654321");

        // 5.3. Customer Account (KH)
        TaiKhoan tkCustomer = seedAccount("customer@smashvn.com", "Customer@123", "KH");
        KhachHang khCustomer = seedKhachHang(tkCustomer, "Nguyễn", "Văn Khách Hàng", "0911223344");

        // 6. Seed Products
        // 6.1. Product 1: Yonex Astrox 100ZZ (Vợt cầu lông)
        SanPham spAstrox = seedSanPham(dmVot, thYonex, nvAdmin, "Yonex Astrox 100ZZ", 
            "Vợt cầu lông Yonex Astrox 100ZZ chính hãng là dòng vợt cao cấp nhất của Yonex dòng Astrox, phù hợp lối đánh công thủ toàn diện thiên công.");
        
        seedVariant(spAstrox, "Xanh Cam", "4U", "28 LBS", new BigDecimal("4100000"), 15, "ASTROX100_Pro_Do_tham.png");
        seedVariant(spAstrox, "Đỏ Kurenai", "3U", "29 LBS", new BigDecimal("4200000"), 10, "Astrox_100_Tour_Do_tham.png");

        // 6.2. Product 2: Yonex Arcsaber 11 Pro (Vợt cầu lông)
        SanPham spArc = seedSanPham(dmVot, thYonex, nvAdmin, "Yonex Arcsaber 11 Pro", 
            "Vợt cầu lông Yonex Arcsaber 11 Pro chính hãng mang đến khả năng kiểm soát cầu cực kỳ chuẩn xác, chuyên cho lối đánh điều cầu và thủ phản tạt.");
        seedVariant(spArc, "Xám Đỏ", "4U", "27 LBS", new BigDecimal("3950000"), 20, "arcsaber11_pro_xam_ngoc_trai_do.png");

        // 6.3. Product 3: Li-Ning Axforce 100 (Vợt cầu lông)
        SanPham spAxforce = seedSanPham(dmVot, thLining, nvAdmin, "Li-Ning Axforce 100", 
            "Vợt cầu lông Li-Ning Axforce 100 chính hãng là siêu phẩm thiên công mạnh mẽ, trợ lực tốt cho các cú đập cầu uy lực từ phía sau sân.");
        seedVariant(spAxforce, "Đen Vàng", "4U", "30 LBS", new BigDecimal("4500000"), 8, "Axforce 100.png");

        // 6.4. Product 4: Victor DriveX 12 (Vợt cầu lông)
        SanPham spDriveX = seedSanPham(dmVot, thVictor, nvAdmin, "Victor DriveX 12", 
            "Vợt cầu lông Victor DriveX 12 chính hãng mang lại sự linh hoạt tối đa, công thủ toàn diện xuất sắc cho cả đánh đơn và đôi.");
        seedVariant(spDriveX, "Cam Đen", "3U", "26 LBS", new BigDecimal("2600000"), 12, "DriveX 12.png");

        // 6.5. Product 5: Giày Yonex Power Cushion 65Z3 C1 (Giày cầu lông)
        SanPham spGiayYonex = seedSanPham(dmGiay, thYonex, nvAdmin, "Giày Yonex Power Cushion 65Z3 C1", 
            "Giày cầu lông Yonex Power Cushion 65Z3 C1 chính hãng là mẫu giày cao cấp thế hệ mới, tích hợp công nghệ giảm chấn đỉnh cao của Yonex, hỗ trợ tối đa cho các pha di chuyển thanh thoát và êm ái trên sân.");
        seedVariant(spGiayYonex, "Trắng Xanh", "40", "N/A", new BigDecimal("2900000"), 10, "yonex_65z3_white_blue.png");
        seedVariant(spGiayYonex, "Đen Cam", "41", "N/A", new BigDecimal("2900000"), 8, "yonex_65z3_black_orange.png");

        // 7. Seed Blogs
        seedBlog(tkAdmin, "Chọn vợt cầu lông phù hợp cho người mới bắt đầu", 
            "chon-vot-cau-long-phu-hop-cho-nguoi-moi-bat-dau", 
            "Hướng dẫn chi tiết cách chọn vợt cầu lông phù hợp với lối chơi, lực cổ tay dành cho người mới chơi cầu lông.", 
            "<p>Chọn vợt cầu lông là một trong những quyết định quan trọng nhất đối với người mới bắt đầu. Một cây vợt phù hợp không chỉ giúp bạn nhanh chóng cải thiện kỹ năng mà còn giảm thiểu nguy cơ chấn thương.</p><h3>1. Trọng lượng vợt (U)</h3><p>Trọng lượng vợt thường được ký hiệu bằng chữ U trên tem vợt. Số U càng lớn thì vợt càng nhẹ. Người mới chơi nên chọn vợt có trọng lượng 4U (80-84g) hoặc 5U (75-79g) để dễ dàng kiểm soát và không gây mỏi tay.</p>", 
            "/uploads/blog/post-1.jpg", "Kinh nghiệm");

        seedBlog(tkAdmin, "Top 5 cây vợt cầu lông Yonex đáng mua nhất năm 2026", 
            "top-5-cay-vot-cau-long-yonex-dang-mua-nhat-nam-2026", 
            "Đánh giá chi tiết top 5 cây vợt cầu lông Yonex được ưa chuộng và đánh giá cao nhất trong năm nay.", 
            "<p>Yonex luôn là thương hiệu đi đầu trong công nghệ sản xuất vợt cầu lông. Dưới đây là danh sách 5 cây vợt Yonex tốt nhất hiện nay phù hợp cho nhiều đối tượng người chơi khác nhau.</p>", 
            "/uploads/blog/post-2.jpg", "Đánh giá");

        seedBlog(tkAdmin, "Cách tập luyện cổ tay khỏe để đập cầu uy lực hơn", 
            "cach-tap-luyen-co-tay-khoe-de-dap-cau-uy-luc-hon", 
            "Chia sẻ các bài tập đơn giản tại nhà giúp bạn cải thiện sức mạnh cổ tay, tăng lực đập cầu rõ rệt.", 
            "<p>Đập cầu (smash) là kỹ thuật tấn công ghi điểm hấp dẫn nhất trong cầu lông. Để có cú đập mạnh, cổ tay dẻo dai và khỏe mạnh đóng vai trò cực kỳ quan trọng.</p>", 
            "/uploads/blog/post-3.jpg", "Kỹ thuật");

        System.out.println("=== SEED CSDL MOI HOAN THANH ===");
    }

    private void seedPaymentMethod(String ma, String ten) {
        boolean exists = phuongThucThanhToanDAO.findAll().stream()
                .anyMatch(p -> ma.equalsIgnoreCase(p.getMaPhuongThuc()));
        if (!exists) {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setMaPhuongThuc(ma);
            p.setTenPhuongThuc(ten);
            phuongThucThanhToanDAO.save(p);
            System.out.println("-> Da seed PhuongThucThanhToan: " + ten);
        }
    }

    private DanhMuc seedDanhMuc(String name) {
        return danhMucRepository.findAll().stream()
                .filter(dm -> dm.getTenDanhMuc().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc dm = new DanhMuc();
                    dm.setTenDanhMuc(name);
                    DanhMuc saved = danhMucRepository.save(dm);
                    System.out.println("-> Da seed DanhMuc: " + name);
                    return saved;
                });
    }

    private ThuongHieu seedThuongHieu(String name) {
        return thuongHieuRepository.findAll().stream()
                .filter(th -> th.getTenThuongHieu().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu(name);
                    ThuongHieu saved = thuongHieuRepository.save(th);
                    System.out.println("-> Da seed ThuongHieu: " + name);
                    return saved;
                });
    }

    private TaiKhoan seedAccount(String email, String password, String vaiTro) {
        TaiKhoan tk = taiKhoanRepository.findByEmail(email);
        if (tk == null) {
            tk = new TaiKhoan();
            tk.setEmail(email);
            tk.setMatKhau(BCrypt.hashpw(password, BCrypt.gensalt()));
            tk.setVaiTro(vaiTro);
            tk.setTrangThai("hoat_dong");
            tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
            tk = taiKhoanRepository.save(tk);
            System.out.println("-> Da seed TaiKhoan: " + email);
        }
        return tk;
    }

    private NhanVien seedNhanVien(TaiKhoan tk, String hoTen, String chucVu, String sdt) {
        NhanVien nv = nhanVienRepository.findByTaiKhoanId(tk.getId());
        if (nv == null) {
            nv = new NhanVien();
            nv.setTaiKhoan(tk);
            nv.setHoTenNv(hoTen);
            nv.setChucVu(chucVu);
            nv.setSoDienThoaiNv(sdt);
            nv = nhanVienRepository.save(nv);
            System.out.println("-> Da seed NhanVien: " + hoTen);
        }
        return nv;
    }

    private KhachHang seedKhachHang(TaiKhoan tk, String hoKh, String tenKh, String sdt) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        if (kh == null) {
            kh = new KhachHang();
            kh.setTaiKhoan(tk);
            kh.setHoKh(hoKh);
            kh.setTenKh(tenKh);
            kh.setSoDienThoaiKh(sdt);
            kh.setNhanBanTin(false);
            kh.setLaTaiKhoanNoiBo(false);
            kh.setNgayTao(LocalDateTime.now());
            kh = khachHangRepository.save(kh);
            System.out.println("-> Da seed KhachHang: " + hoKh + " " + tenKh);
        }
        return kh;
    }

    private SanPham seedSanPham(DanhMuc dm, ThuongHieu th, NhanVien nv, String name, String moTa) {
        return sanPhamRepository.findAll().stream()
            .filter(sp -> sp.getTenSanPham().equals(name))
            .findFirst()
            .orElseGet(() -> {
                SanPham sp = new SanPham();
                sp.setDanhMuc(dm);
                sp.setThuongHieu(th);
                sp.setNhanVien(nv);
                sp.setTenSanPham(name);
                sp.setMoTa(moTa);
                sp.setTrangThai("dang_ban");
                sp.setSoDanhGia(0);
                sp.setDiemTrungBinh(0.0);
                SanPham saved = sanPhamRepository.save(sp);
                System.out.println("-> Da seed SanPham: " + name);
                return saved;
            });
    }

    private void seedVariant(SanPham sp, String mauSac, String trongLuong, String mucCang, BigDecimal giaBan, int qty, String image) {
        Optional<SanPhamChiTiet> opt = sanPhamChiTietRepository.findAll().stream()
            .filter(v -> v.getSanPham().getId().equals(sp.getId()) && v.getMauSac().equals(mauSac) && v.getTrongLuong().equals(trongLuong) && v.getMucCang().equals(mucCang))
            .findFirst();
        
        if (opt.isEmpty()) {
            SanPhamChiTiet v = new SanPhamChiTiet();
            v.setSanPham(sp);
            v.setMauSac(mauSac);
            v.setTrongLuong(trongLuong);
            v.setMucCang(mucCang);
            v.setGiaBan(giaBan);
            v.setSoLuongTon(qty);
            v.setTrangThai("dang_ban");
            v.setNgayTao(LocalDateTime.now());
            v.setNgayCapNhat(LocalDateTime.now());
            v.setHinhAnhSanPham(image);
            sanPhamChiTietRepository.save(v);
            System.out.println("   -> Da seed variant: " + mauSac + " - " + trongLuong);
        } else {
            SanPhamChiTiet v = opt.get();
            v.setHinhAnhSanPham(image);
            sanPhamChiTietRepository.save(v);
            System.out.println("   -> Da cap nhat anh variant: " + mauSac + " - " + trongLuong + " -> " + image);
        }
    }

    private void seedBlog(TaiKhoan tk, String title, String slug, String summary, String content, String image, String category) {
        if (!blogRepository.existsBySlug(slug)) {
            Blog blog = new Blog();
            blog.setTitle(title);
            blog.setSlug(slug);
            blog.setSummary(summary);
            blog.setContent(content);
            blog.setImage(image);
            blog.setCategory(category);
            blog.setPublishDate(java.time.LocalDate.now());
            blog.setStatus(BlogStatus.PUBLISHED);
            blog.setNguoiDang(tk);
            blog.setCreatedAt(LocalDateTime.now());
            blog.setDeleted(false);
            blogRepository.save(blog);
            System.out.println("-> Da seed Blog: " + title);
        }
    }

    @Test
    public void cleanupImages() {
        System.out.println("=== BAT DAU DON DEP ANH LOI / KHONG SU DUNG ===");
        
        // 1. Thu thập tất cả ảnh được tham chiếu trong DB
        java.util.Set<String> referencedProduct = new java.util.HashSet<>();
        java.util.Set<String> referencedBlog = new java.util.HashSet<>();
        java.util.Set<String> referencedReview = new java.util.HashSet<>();

        // 1.1 HinhAnhSanPham
        for (HinhAnhSanPham ha : sanPhamChiTietRepository.findAll().stream()
                .flatMap(v -> v.getHinhAnhSanPhams().stream())
                .collect(java.util.stream.Collectors.toList())) {
            if (ha.getUrlHinhAnh() != null) {
                referencedProduct.add(getFilenameOnly(ha.getUrlHinhAnh()));
            }
        }
        // Thêm trường hợp nếu hinhAnhSanPham được gán trực tiếp
        for (SanPhamChiTiet spct : sanPhamChiTietRepository.findAll()) {
            if (spct.getHinhAnhSanPham() != null) {
                referencedProduct.add(getFilenameOnly(spct.getHinhAnhSanPham()));
            }
        }

        // 1.2 Blog
        for (Blog b : blogRepository.findAll()) {
            if (b.getImage() != null) {
                referencedBlog.add(getFilenameOnly(b.getImage()));
            }
        }

        // 1.3 DanhGiaAnh
        for (DanhGiaAnh dga : danhGiaAnhRepository.findAll()) {
            if (dga.getDuongDan() != null) {
                referencedReview.add(getFilenameOnly(dga.getDuongDan()));
            }
        }

        System.out.println("Số lượng ảnh sản phẩm đang dùng trong DB: " + referencedProduct.size());
        System.out.println("Số lượng ảnh bài viết đang dùng trong DB: " + referencedBlog.size());
        System.out.println("Số lượng ảnh đánh giá đang dùng trong DB: " + referencedReview.size());

        // 2. Quét các thư mục vật lý
        java.io.File uploadsDir = new java.io.File("uploads");
        if (uploadsDir.exists() && uploadsDir.isDirectory()) {
            scanAndDelete(new java.io.File(uploadsDir, "product"), referencedProduct);
            scanAndDelete(new java.io.File(uploadsDir, "blog"), referencedBlog);
            scanAndDelete(new java.io.File(uploadsDir, "reviews"), referencedReview);
        }

        System.out.println("=== HOAN THANH DON DEP ANH ===");
    }

    private String getFilenameOnly(String path) {
        if (path == null) return "";
        path = path.replace("\\", "/");
        int idx = path.lastIndexOf("/");
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private void scanAndDelete(java.io.File dir, java.util.Set<String> referencedNames) {
        if (!dir.exists() || !dir.isDirectory()) return;

        java.io.File[] files = dir.listFiles();
        if (files == null) return;

        int deletedCorrupted = 0;
        int deletedUnused = 0;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                // Đệ quy quét thư mục con (ví dụ: Yonex, Li-ning)
                scanAndDelete(file, referencedNames);
            } else {
                String filename = file.getName();
                long size = file.length();

                // Kiểm tra nếu dung lượng <= 100 bytes (ảnh lỗi/ảnh trống)
                if (size <= 100) {
                    String path = file.getPath();
                    if (file.delete()) {
                        System.out.println("-> [Xóa ảnh lỗi] " + path + " (Dung lượng: " + size + " bytes)");
                        deletedCorrupted++;
                    }
                } 
                // Kiểm tra nếu không được tham chiếu trong DB
                else if (!referencedNames.contains(filename)) {
                    String path = file.getPath();
                    if (file.delete()) {
                        System.out.println("-> [Xóa ảnh không dùng] " + path + " (Dung lượng: " + size + " bytes)");
                        deletedUnused++;
                    }
                }
            }
        }
        if (deletedCorrupted > 0 || deletedUnused > 0) {
            System.out.println("Thư mục " + dir.getName() + ": Đã xóa " + deletedCorrupted + " ảnh lỗi, " + deletedUnused + " ảnh không sử dụng.");
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    public void printProducts() {
        System.out.println("=== PRODUCT LIST ===");
        for (SanPham sp : sanPhamRepository.findAll()) {
            System.out.println("SP_ID: " + sp.getId() + " | Name: " + sp.getTenSanPham() + " | Category: " + (sp.getDanhMuc() != null ? sp.getDanhMuc().getTenDanhMuc() : "NULL"));
            for (SanPhamChiTiet spct : sp.getSanPhamChiTiets()) {
                System.out.println("  -> CT_ID: " + spct.getId() + " | Color: " + spct.getMauSac() + " | Weight/Size: " + spct.getTrongLuong() + " | Image: " + spct.getHinhAnhSanPham());
            }
        }
        System.out.println("=== END PRODUCT LIST ===");
    }

    @Test
    public void checkAdminAccount() {
        System.out.println("=== CHECK ADMIN ACCOUNT ===");
        TaiKhoan tk = taiKhoanRepository.findByEmail("admin@smashvn.com");
        if (tk == null) {
            System.out.println("admin@smashvn.com is NULL");
        } else {
            System.out.println("Email: " + tk.getEmail());
            System.out.println("Password hash: " + tk.getMatKhau());
            System.out.println("Role: " + tk.getVaiTro());
            System.out.println("Status: " + tk.getTrangThai());
            System.out.println("AccountStatus: " + tk.getTrangThaiTaiKhoan());
            
            // Check if plain text "Admin@123" matches
            try {
                boolean matches = BCrypt.checkpw("Admin@123", tk.getMatKhau());
                System.out.println("Check 'Admin@123' match: " + matches);
            } catch (Exception e) {
                System.out.println("BCrypt check failed: " + e.getMessage());
            }
        }
        System.out.println("=== END CHECK ADMIN ACCOUNT ===");
    }
}
