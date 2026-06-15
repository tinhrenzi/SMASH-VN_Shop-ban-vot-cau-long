package com.smashvn.shop.service;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDangKyServiceTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private KhachHangRepository khachHangRepository;

    private UserDangKyService userDangKyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDangKyService = new UserDangKyService(taiKhoanRepository, khachHangRepository);
    }

    @Test
    void testDangKy_Success() {
        String email = "newcustomer@gmail.com";
        String matKhau = "SecurePass123";

        when(taiKhoanRepository.existsByEmail(email)).thenReturn(false);
        
        // Mock save returning the same entity
        when(taiKhoanRepository.save(any(TaiKhoan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaiKhoan result = userDangKyService.dangKy(email, matKhau);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertTrue(result.getLaKhachHang(), "laKhachHang should be true for manually registered customers");
        assertFalse(result.getLaNhanVien(), "laNhanVien should be false");
        assertFalse(result.getLaQuanLy(), "laQuanLy should be false");
        assertEquals("KH", result.getVaiTro());
        assertEquals("hoat_dong", result.getTrangThai());

        // Verify profile is created
        ArgumentCaptor<KhachHang> profileCaptor = ArgumentCaptor.forClass(KhachHang.class);
        verify(khachHangRepository).save(profileCaptor.capture());
        
        KhachHang createdProfile = profileCaptor.getValue();
        assertNotNull(createdProfile);
        assertEquals("Người dùng mới", createdProfile.getTenKh());
        assertEquals(result, createdProfile.getTaiKhoan());
    }

    @Test
    void testDangKy_DuplicateEmail() {
        String email = "duplicate@gmail.com";
        String matKhau = "SecurePass123";

        when(taiKhoanRepository.existsByEmail(email)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Email này đã được sử dụng!", exception.getMessage());
        verify(taiKhoanRepository, never()).save(any(TaiKhoan.class));
    }

    @Test
    void testDangKy_InvalidEmailFormat() {
        String email = "invalid-email";
        String matKhau = "SecurePass123";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Định dạng email không hợp lệ!", exception.getMessage());
    }

    @Test
    void testDangKy_WeakPassword() {
        String email = "valid@gmail.com";
        String matKhau = "weak"; // less than 8 chars

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Mật khẩu phải dài ít nhất 8 ký tự và chứa cả chữ và số!", exception.getMessage());
    }
}
