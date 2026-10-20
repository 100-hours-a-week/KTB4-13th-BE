package com.book.core.order.api.response;

// @formatter:off
public record OrderCancellationItemResponse(
        Long orderItemId,
        Long productId,
        String itemName,
        long quantity) {}
// @formatter:on
