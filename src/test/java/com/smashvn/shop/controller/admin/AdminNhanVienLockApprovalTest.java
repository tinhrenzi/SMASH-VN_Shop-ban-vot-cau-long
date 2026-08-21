package com.smashvn.shop.controller.admin;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminNhanVienService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminNhanVienLockApprovalTest {

    @Autowired
    private AdminNhanVienService adminNhanVienService;

    @Autowired
    private AdminNhanVienController adminNhanVienController;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    private NhanVien createSampleEmployee(String email, String phone) {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(email);
        tk.setMatKhau("12345678");
        tk.setVaiTro("NV");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.save(tk);

        NhanVien nv = new NhanVien();
        nv.setHoTenNv("Nhân Viên Thử Nghiệm");
        nv.setChucVu("Nhân viên bán hàng");
        nv.setSoDienThoaiNv(phone);
        nv.setTaiKhoan(tk);
        return nhanVienRepository.save(nv);
    }

    private TaiKhoan createSampleManager(String email) {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(email);
        tk.setMatKhau("12345678");
        tk.setVaiTro("QL");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        return taiKhoanRepository.save(tk);
    }

    @Test
    public void testToggleStatusToPendingLock() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_toggle_" + ts + "@smash.vn", "0912" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_toggle_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        TaiKhoan updatedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("cho_khoa", updatedTk.getTrangThai());
        assertEquals(AccountStatus.PENDING_LOCK, updatedTk.getTrangThaiTaiKhoan());
        assertNotNull(updatedTk.getTokenXacThucKhoa());
    }

    @Test
    public void testApproveLockViaAjax() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_ajax_" + ts + "@smash.vn", "0913" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_ajax_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", manager.getId());
        session.setAttribute("vaiTro", "QL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object response = adminNhanVienController.xuLyPheDuyetKhoa(
                nv.getId(), null, true, null, session, request, model, redirectAttributes);

        assertNotNull(response);
        assertTrue(response instanceof org.springframework.http.ResponseEntity);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(200, entity.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));

        TaiKhoan lockedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("bi_khoa", lockedTk.getTrangThai());
        assertEquals(AccountStatus.LOCKED, lockedTk.getTrangThaiTaiKhoan());
        assertNull(lockedTk.getTokenXacThucKhoa());
    }

    @Test
    public void testApproveLockViaTokenLink() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_token_" + ts + "@smash.vn", "0914" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_token_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        TaiKhoan pendingTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        String token = pendingTk.getTokenXacThucKhoa();
        assertNotNull(token);

        MockHttpSession unauthSession = new MockHttpSession(); // No logged in user
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object resultView = adminNhanVienController.xuLyPheDuyetKhoa(
                nv.getId(), token, false, null, unauthSession, request, model, redirectAttributes);

        assertEquals("admin/confirm-result", resultView);
        assertEquals(true, model.getAttribute("success"));

        TaiKhoan lockedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("bi_khoa", lockedTk.getTrangThai());
        assertNull(lockedTk.getTokenXacThucKhoa());
    }

    @Test
    public void testRejectLockViaAjax() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_rej_" + ts + "@smash.vn", "0915" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_rej_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", manager.getId());
        session.setAttribute("vaiTro", "QL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object response = adminNhanVienController.xuLyTuChoiKhoa(
                nv.getId(), null, true, null, session, request, model, redirectAttributes);

        assertNotNull(response);
        assertTrue(response instanceof org.springframework.http.ResponseEntity);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(200, entity.getStatusCode().value());

        TaiKhoan activeTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("hoat_dong", activeTk.getTrangThai());
        assertEquals(AccountStatus.ACTIVE, activeTk.getTrangThaiTaiKhoan());
        assertNull(activeTk.getTokenXacThucKhoa());
    }

    @Test
    public void testApproveLockWithRedirectUrl() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_redir_" + ts + "@smash.vn", "0916" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_redir_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", manager.getId());
        session.setAttribute("vaiTro", "QL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = adminNhanVienController.xuLyPheDuyetKhoa(
                nv.getId(), null, false, "/admin/nhan-vien", session, request, model, redirectAttributes);

        assertEquals("redirect:/admin/nhan-vien", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));

        TaiKhoan lockedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("bi_khoa", lockedTk.getTrangThai());
    }
}
