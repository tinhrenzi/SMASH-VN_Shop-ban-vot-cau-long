package com.smashvn.shop.service.chatbot;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.chatbot.ChatService.PriceRange;
import com.smashvn.shop.service.chatbot.ChatService.BotResponseWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Test
    public void testParsePriceQuery_Range() {
        PriceRange r1 = chatService.parsePriceQuery("900k-1m5");
        assertTrue(r1.isHasRange());
        assertEquals(0, r1.getMinPrice().compareTo(BigDecimal.valueOf(900000)));
        assertEquals(0, r1.getMaxPrice().compareTo(BigDecimal.valueOf(1500000)));

        PriceRange r2 = chatService.parsePriceQuery("từ 1 triệu đến 2 triệu");
        assertTrue(r2.isHasRange());
        assertEquals(0, r2.getMinPrice().compareTo(BigDecimal.valueOf(1000000)));
        assertEquals(0, r2.getMaxPrice().compareTo(BigDecimal.valueOf(2000000)));
    }

    @Test
    public void testParsePriceQuery_Under() {
        PriceRange r1 = chatService.parsePriceQuery("dưới 2 triệu");
        assertTrue(r1.isHasRange());
        assertEquals(0, r1.getMinPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, r1.getMaxPrice().compareTo(BigDecimal.valueOf(2000000)));

        PriceRange r2 = chatService.parsePriceQuery("thấp hơn 900 nghìn");
        assertTrue(r2.isHasRange());
        assertEquals(0, r2.getMinPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, r2.getMaxPrice().compareTo(BigDecimal.valueOf(900000)));
    }

    @Test
    public void testParsePriceQuery_Over() {
        PriceRange r1 = chatService.parsePriceQuery("trên 1.5 triệu");
        assertTrue(r1.isHasRange());
        assertEquals(0, r1.getMinPrice().compareTo(BigDecimal.valueOf(1500000)));
        assertEquals(0, r1.getMaxPrice().compareTo(BigDecimal.valueOf(99000000)));
    }

    @Test
    public void testParsePriceQuery_About() {
        PriceRange r1 = chatService.parsePriceQuery("khoảng 1 triệu");
        assertTrue(r1.isHasRange());
        assertEquals(0, r1.getMinPrice().compareTo(BigDecimal.valueOf(800000)));
        assertEquals(0, r1.getMaxPrice().compareTo(BigDecimal.valueOf(1200000)));
    }

    @Test
    public void testOffTopicDetection() {
        // Questions that are completely off-topic must trigger the default rejection message
        BotResponseWrapper r1 = chatService.getBotResponseWrapper(null, "viết code java in ra hello world");
        assertEquals("Tôi là trợ lý của SMASH VN và hiện chỉ hỗ trợ các nội dung liên quan đến sản phẩm cầu lông, mua sắm và dịch vụ của cửa hàng.", r1.getMessageText());

        BotResponseWrapper r2 = chatService.getBotResponseWrapper(null, "ai là tổng thống Mỹ?");
        assertEquals("Tôi là trợ lý của SMASH VN và hiện chỉ hỗ trợ các nội dung liên quan đến sản phẩm cầu lông, mua sắm và dịch vụ của cửa hàng.", r2.getMessageText());
    }

    @Test
    public void testDiscontinuedProductRequest() {
        // Seed Catalog
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Vợt Cầu Lông");
            return danhMucRepository.save(newDm);
        });

        // Seed Brand
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Yonex");
            return thuongHieuRepository.save(newTh);
        });

        // Seed Staff
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan nvUser = new TaiKhoan();
            nvUser.setEmail("staff_test_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
            nvUser.setMatKhau("pass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");
            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff");
            newNv.setChucVu("Nhân viên");
            newNv.setSoDienThoaiNv("0981112224");
            return nhanVienRepository.save(newNv);
        });

        // Create a discontinued product in repository
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Cầu Lông Yonex Nano 9999");
        sp.setTrangThai("ngung_ban");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Mô tả sản phẩm ngừng bán test");
        sp = sanPhamRepository.save(sp);

        // Directly query for it
        BotResponseWrapper response = chatService.getBotResponseWrapper(null, "Cho tôi xem Vợt Cầu Lông Yonex Nano 9999");
        assertEquals("Sản phẩm này hiện đã ngừng kinh doanh tại cửa hàng. Bạn có muốn tôi gợi ý các sản phẩm tương tự đang còn bán không?", response.getMessageText());
    }
}
