package com.smashvn.shop.dto.payment;

import lombok.Data;

@Data
public class ZaloPayCallbackDTO {
    private String data;
    private String mac;
}
