package com.smashvn.shop.dto;

import java.math.BigDecimal;

public record TopProductDTO(
    Integer productId,
    String productName,
    String image,
    String categoryName,
    Long soldQuantity,
    BigDecimal revenue
) {}
