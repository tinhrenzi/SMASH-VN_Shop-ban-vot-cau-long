package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.service.product.PriceSnapshot;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCartService {

    public static final int MAX_CART_QUANTITY = 999;
    private static final String SESSION_CART_KEY = "guestCart";

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final TrangThaiGioHangRepository trangThaiGioHangRepository;
    private final PricingService pricingService;

    private boolean isDangBan(String trangThai) {
        return trangThai == null || trangThai.isBlank() || "dang_ban".equals(trangThai);
    }

    private boolean isSanPhamChiTietDangBan(SanPhamChiTiet spct) {
        return spct != null
                && spct.getSanPham() != null
                && isDangBan(spct.getSanPham().getTrangThai())
                && isDangBan(spct.getTrangThai());
    }

    public static class GuestCartItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer idSanPhamChiTiet;
        private Integer soLuong;

        public GuestCartItem() {}

        public GuestCartItem(Integer idSanPhamChiTiet, Integer soLuong) {
            this.idSanPhamChiTiet = idSanPhamChiTiet;
            this.soLuong = soLuong;
        }

        public Integer getIdSanPhamChiTiet() {
            return idSanPhamChiTiet;
        }

        public void setIdSanPhamChiTiet(Integer idSanPhamChiTiet) {
            this.idSanPhamChiTiet = idSanPhamChiTiet;
        }

        public Integer getSoLuong() {
            return soLuong;
        }

        public void setSoLuong(Integer soLuong) {
            this.soLuong = soLuong;
        }
    }

    @SuppressWarnings("unchecked")
    public List<GuestCartItem> getGuestCartItems(HttpSession session) {
        List<GuestCartItem> cart = (List<GuestCartItem>) session.getAttribute(SESSION_CART_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
        }
        return cart;
    }

    public void addToGuestCart(HttpSession session, Integer idSanPhamChiTiet, Integer soLuong) {
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm thêm vào giỏ hàng phải lớn hơn 0.");
        }
        if (soLuong > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Số lượng sản phẩm thêm vào giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        if (idSanPhamChiTiet == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ.");
        }

        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        if (!isSanPhamChiTietDangBan(spct)) {
            throw new RuntimeException("Phân loại sản phẩm này đã ngừng bán!");
        }

        List<GuestCartItem> cart = getGuestCartItems(session);
        GuestCartItem existingItem = null;
        for (GuestCartItem item : cart) {
            if (item.getIdSanPhamChiTiet().equals(idSanPhamChiTiet)) {
                existingItem = item;
                break;
            }
        }

        int soLuongHienCo = (existingItem != null) ? existingItem.getSoLuong() : 0;
        long tongYeuCauLong = (long) soLuongHienCo + soLuong;
        if (tongYeuCauLong > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Tổng số lượng sản phẩm này trong giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        int tongYeuCau = (int) tongYeuCauLong;

        if (spct.getSoLuongTon() < tongYeuCau) {
            if (soLuongHienCo > 0) {
                throw new RuntimeException("Bạn đã có " + soLuongHienCo + " sản phẩm này trong giỏ. Không thể thêm vượt quá số lượng tồn kho (" + spct.getSoLuongTon() + ")!");
            } else {
                throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + spct.getSoLuongTon() + " sản phẩm.");
            }
        }

        if (existingItem != null) {
            existingItem.setSoLuong(tongYeuCau);
        } else {
            cart.add(new GuestCartItem(idSanPhamChiTiet, soLuong));
        }

        session.setAttribute(SESSION_CART_KEY, cart);
        log.info("[GUEST_CART] Added product details ID {} with qty {} to session cart.", idSanPhamChiTiet, soLuong);
    }

    public void updateGuestCartQuantity(HttpSession session, Integer idSanPhamChiTiet, Integer soLuongMoi) {
        if (soLuongMoi == null || soLuongMoi <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ hàng phải lớn hơn 0.");
        }
        if (soLuongMoi > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }

        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        if (!isSanPhamChiTietDangBan(spct)) {
            throw new RuntimeException("Phân loại sản phẩm này đã ngừng bán!");
        }

        if (spct.getSoLuongTon() < soLuongMoi) {
            throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + spct.getSoLuongTon() + " sản phẩm.");
        }

        List<GuestCartItem> cart = getGuestCartItems(session);
        boolean found = false;
        for (GuestCartItem item : cart) {
            if (item.getIdSanPhamChiTiet().equals(idSanPhamChiTiet)) {
                item.setSoLuong(soLuongMoi);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng");
        }

        session.setAttribute(SESSION_CART_KEY, cart);
        log.info("[GUEST_CART] Updated product details ID {} quantity to {} in session.", idSanPhamChiTiet, soLuongMoi);
    }

    public void removeFromGuestCart(HttpSession session, Integer idSanPhamChiTiet) {
        List<GuestCartItem> cart = getGuestCartItems(session);
        cart.removeIf(item -> item.getIdSanPhamChiTiet().equals(idSanPhamChiTiet));
        session.setAttribute(SESSION_CART_KEY, cart);
        log.info("[GUEST_CART] Removed product details ID {} from session cart.", idSanPhamChiTiet);
    }

    public void clearGuestCart(HttpSession session) {
        session.removeAttribute(SESSION_CART_KEY);
        log.info("[GUEST_CART] Cleared guest session cart.");
    }

    public Map<String, Object> layDuLieuMiniCart(HttpSession session) {
        List<GuestCartItem> items = getGuestCartItems(session);
        BigDecimal tongTien = BigDecimal.ZERO;
        int tongSoLuong = 0;
        List<Map<String, Object>> danhSachMini = new ArrayList<>();

        for (GuestCartItem item : items) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(item.getIdSanPhamChiTiet()).orElse(null);
            if (spct == null) continue;

            SanPham sp = spct.getSanPham();
            int tonKho = spct.getSoLuongTon();
            boolean hopLe = tonKho > 0 && isSanPhamChiTietDangBan(spct) && item.getSoLuong() != null && item.getSoLuong() > 0;
            PriceSnapshot priceSnapshot = pricingService.buildPriceSnapshot(spct);

            if (hopLe) {
                BigDecimal gia = priceSnapshot.giaBanSauGiam();
                BigDecimal soLuong = new BigDecimal(item.getSoLuong());
                tongTien = tongTien.add(gia.multiply(soLuong));
                tongSoLuong += item.getSoLuong();
            }

            Map<String, Object> map = new HashMap<>();
            // Sử dụng idSanPhamChiTiet làm idChiTiet cho guest để đồng bộ giao diện
            map.put("idChiTiet", item.getIdSanPhamChiTiet());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("hinhAnh", spct.getHinhAnhSanPham());
            map.put("giaBan", priceSnapshot.giaBanSauGiam());
            map.put("giaGoc", priceSnapshot.giaNiemYet());
            map.put("phanTramGiam", priceSnapshot.phanTramGiam());
            map.put("soLuong", item.getSoLuong());
            map.put("idSanPham", sp.getId());
            map.put("mauSac", spct.getMauSac());
            map.put("trongLuong", spct.getTrongLuong());
            map.put("kichThuoc", spct.getKichThuoc());
            danhSachMini.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("trangThai", "ok");
        response.put("tongSoLuong", tongSoLuong);
        response.put("tongTien", tongTien);
        response.put("danhSach", danhSachMini);

        return response;
    }

    @Transactional
    public void transferGuestCartToDb(HttpSession session, Integer idKhachHang) {
        List<GuestCartItem> guestCart = getGuestCartItems(session);
        if (guestCart.isEmpty()) {
            return;
        }

        KhachHang khachHang = khachHangRepository.findById(idKhachHang)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Khách hàng"));

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setKhachHang(khachHang);
            gioHang = gioHangRepository.save(gioHang);
        }

        TrangThaiGioHang trangThai = trangThaiGioHangRepository.findById(1)
                .orElseGet(() -> trangThaiGioHangRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Lỗi cấu hình CSDL: Không tìm thấy trạng thái giỏ hàng")));

        for (GuestCartItem item : guestCart) {
            // Khóa dòng sản phẩm bằng Pessimistic Write Lock để chống race condition
            SanPhamChiTiet lockedSpct = sanPhamChiTietRepository.findByIdWithLock(item.getIdSanPhamChiTiet())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: ID " + item.getIdSanPhamChiTiet()));
            if (!isSanPhamChiTietDangBan(lockedSpct)) {
                throw new RuntimeException("Phân loại sản phẩm '" + lockedSpct.getSanPham().getTenSanPham() + "' đã ngừng bán!");
            }

            GioHangChiTiet chiTiet = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), item.getIdSanPhamChiTiet());

            int soLuongHienCo = (chiTiet != null) ? chiTiet.getSoLuong() : 0;
            int newQuantity = soLuongHienCo + item.getSoLuong();

            if (newQuantity > MAX_CART_QUANTITY) {
                newQuantity = MAX_CART_QUANTITY;
            }

            if (lockedSpct.getSoLuongTon() < newQuantity) {
                throw new RuntimeException("Sản phẩm '" + lockedSpct.getSanPham().getTenSanPham() + "' không đủ tồn kho để gộp giỏ hàng! Chỉ còn " + lockedSpct.getSoLuongTon() + " sản phẩm.");
            }

            if (chiTiet != null) {
                chiTiet.setSoLuong(newQuantity);
            } else {
                chiTiet = new GioHangChiTiet();
                chiTiet.setGioHang(gioHang);
                chiTiet.setSanPhamChiTiet(lockedSpct);
                chiTiet.setSoLuong(newQuantity);
                chiTiet.setTrangThai(trangThai);
            }
            gioHangChiTietRepository.save(chiTiet);
        }

        clearGuestCart(session);
        log.info("[GUEST_CART] Successfully merged guest session cart into database cart for KhachHang ID {}.", idKhachHang);
    }
}
