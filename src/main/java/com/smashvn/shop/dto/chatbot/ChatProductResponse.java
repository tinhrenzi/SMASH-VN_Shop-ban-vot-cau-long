package com.smashvn.shop.dto.chatbot;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatProductResponse {
    private Integer id;
    private String name;
    private BigDecimal price;
    private String brand;
    private String shortDescription;
    private String imageUrl;
    private String productUrl;
}
