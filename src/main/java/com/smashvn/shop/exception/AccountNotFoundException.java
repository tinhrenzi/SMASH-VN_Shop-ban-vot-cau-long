package com.smashvn.shop.exception;

/**
 * Ném khi người dùng nhập email chưa được đăng ký trong hệ thống.
 * Dùng để phân biệt với lỗi sai mật khẩu, cho phép hiển thị UI gợi ý đăng ký.
 */
public class AccountNotFoundException extends RuntimeException {
    private final String email;

    public AccountNotFoundException(String email) {
        super("Email " + email + " chưa được đăng ký trong hệ thống.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
