package com.smashvn.shop.service.admin;

import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNhanVienServiceTest {

    @Mock
    private NhanVienRepository nhanVienRepository;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private AdminNhanVienService adminNhanVienService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminNhanVienService = new AdminNhanVienService(
                nhanVienRepository,
                taiKhoanRepository,
                auditService,
                mailSender,
                khachHangRepository,
                passwordEncoder);
    }

    @Test
    void createNhanVien_duplicatePhone_showsFriendlyMessageAndDoesNotInsert() {
        when(nhanVienRepository.existsBySoDienThoai("0357059078")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminNhanVienService.createNhanVien(
                        "staff@example.com",
                        "SecurePass123",
                        "Nguyen Van A",
                        "Nhan vien ban hang",
                        "0357059078",
                        1,
                        "127.0.0.1"));

        assertEquals("Số điện thoại nhân viên đã tồn tại. Vui lòng nhập số khác.", ex.getMessage());
        verify(taiKhoanRepository, never()).saveAndFlush(any());
        verify(nhanVienRepository, never()).saveAndFlush(any());
    }

    @Test
    void createNhanVien_invalidVietnamPhone_showsValidationMessageAndDoesNotInsert() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminNhanVienService.createNhanVien(
                        "staff@example.com",
                        "SecurePass123",
                        "Nguyen Van A",
                        "Nhan vien ban hang",
                        "012345",
                        1,
                        "127.0.0.1"));

        assertEquals("Số điện thoại không đúng định dạng Việt Nam. Vui lòng nhập số bắt đầu bằng 03, 05, 07, 08, 09 và đủ 10 số.", ex.getMessage());
        verify(taiKhoanRepository, never()).saveAndFlush(any());
        verify(nhanVienRepository, never()).saveAndFlush(any());
    }
}
