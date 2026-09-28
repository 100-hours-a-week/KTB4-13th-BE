package com.book.core.order.api.response;

import java.math.BigDecimal;

// @formatter:off
public record OrderItemResponse(
        Long itemId,
        String itemName,
        String thumbnailUrl,
        String author,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {}
// @formatter:on
