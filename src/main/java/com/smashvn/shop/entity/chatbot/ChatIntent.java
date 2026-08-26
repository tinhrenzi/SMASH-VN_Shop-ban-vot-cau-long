package com.smashvn.shop.entity.chatbot;

public enum ChatIntent {
    PRODUCT_SEARCH,          // Hỏi tìm kiếm danh sách sản phẩm
    PRODUCT_INFORMATION,     // Hỏi chi tiết một sản phẩm cụ thể
    STORE_INFORMATION,       // Hỏi địa chỉ, hotline, giờ hoạt động
    BASIC_CONSULTATION,      // Tư vấn cơ bản (người mới, thiên công/thủ...)
    ADVANCED_CONSULTATION,   // Tư vấn chuyên sâu/kỹ thuật/y tế (bao gồm "vận động viên")
    OUT_OF_SCOPE,            // Ngoài phạm vi dự án (tin tức, lập trình, giải trí...)
    SECURITY_SENSITIVE       // Yêu cầu nhạy cảm bảo mật (API key, system prompt, SQL...)
}
