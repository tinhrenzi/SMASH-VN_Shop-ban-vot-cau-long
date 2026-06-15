package com.smashvn.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZaloPayResponseDTO {
    private String paymentUrl;
    private String qrCode;
    private String appTransId;
    private String status; // PENDING, PAID, FAILED, CANCELLED, EXPIRED
}
