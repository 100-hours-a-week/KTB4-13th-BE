package com.book.core.order.application.result;

import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import java.math.BigDecimal;

// @formatter:off
public record OrderItemResult(
        Long orderItemId,
        Long productId,
        String itemName,
        String thumbnailUrl,
        String author,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Integer quantity,
        long canceledQuantity,
        OrderItemStatus status) {
    public static OrderItemResult from(final OrderItem item) {
        return new OrderItemResult(item.id(), item.productId(), item.itemName(), item.thumbnailUrl(), item.author(), item.unitPrice(),
                item.totalPrice(), item.quantity(), item.canceledQuantity(), item.status());
    }
}
// @formatter:on
