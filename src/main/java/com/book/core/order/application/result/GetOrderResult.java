package com.book.core.order.application.result;

import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @formatter:off
public record GetOrderResult(
        String key,
        String name,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemResult> items,
        OrderAddressResult address) {
    public static GetOrderResult from(final Order order) {
        final List<OrderItemResult> items = order.items().stream()
                .filter(OrderItem::isActive)
                .map(OrderItemResult::from)
                .toList();
        return new GetOrderResult(order.key(), order.name(), order.status(), order.totalPrice(), order.createdAt(), items,
                OrderAddressResult.from(order.address()));
    }
}
// @formatter:on
