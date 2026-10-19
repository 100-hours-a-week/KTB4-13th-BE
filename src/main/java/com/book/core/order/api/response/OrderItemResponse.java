package com.book.core.order.api.response;

import com.book.core.order.domain.OrderItemStatus;
import java.math.BigDecimal;

// @formatter:off
public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String itemName,
        String thumbnailUrl,
        String author,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Integer quantity,
        long canceledQuantity,
        OrderItemStatus status) {}
// @formatter:on
