package com.smashvn.shop.service;

public interface NewsletterService {
    void subscribe(String email, String gioiTinh);
    void unsubscribe(String token);
    void sendPromotionEmailAsync(Integer dotGiamGiaId);
    void sendVoucherEmailAsync(Integer phieuGiamGiaId);
}
