package com.book.core.order.application.result;

// @formatter:off
public record OrderCancellationItemResult(
        Long orderItemId,
        Long productId,
        String itemName,
        long quantity) {
    static OrderCancellationItemResult of(final OrderItemResult item, final long quantity) {
        return new OrderCancellationItemResult(item.orderItemId(), item.productId(), item.itemName(), quantity);
    }
}
// @formatter:on
