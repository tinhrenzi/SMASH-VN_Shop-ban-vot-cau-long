package com.smashvn.shop.dto.chatbot;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSuggestionDto {
    private Integer id;
    private String tenSanPham;
    private String imageUrl;
    private BigDecimal giaBan;
}
