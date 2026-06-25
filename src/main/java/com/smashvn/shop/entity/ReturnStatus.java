package com.smashvn.shop.entity;

public enum ReturnStatus {
    PENDING_RETURN("PENDING_RETURN", "Chờ nhập kho"),
    RETURNED("RETURNED", "Đã nhập kho (Đã hoàn kho)"),
    LOST("LOST", "Mất hàng (Không hoàn kho)"),
    DAMAGED("DAMAGED", "Hỏng hàng (Không hoàn kho)");

    private final String value;
    private final String label;

    ReturnStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
