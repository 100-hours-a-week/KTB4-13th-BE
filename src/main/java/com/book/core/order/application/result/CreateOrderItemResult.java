package com.book.core.order.application.result;

import java.math.BigDecimal;

public record CreateOrderItemResult(Long productId, String itemName, String thumbnailUrl, String author, BigDecimal salePrice,
    BigDecimal discountedPrice, Integer quantity, BigDecimal totalPrice) {
}
