package com.smashvn.shop.entity;

public enum OrderStatus {
    CHO_THANH_TOAN("cho_thanh_toan"),
    CHO_XAC_NHAN("cho_xac_nhan"),
    DA_XAC_NHAN("da_xac_nhan"),
    DANG_GIAO("dang_giao"),
    DANG_LAY_HANG("dang_lay_hang"),
    DA_GIAO("da_giao"),
    DA_HUY("da_huy"),
    STOCK_CONFLICT("stock_conflict");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
