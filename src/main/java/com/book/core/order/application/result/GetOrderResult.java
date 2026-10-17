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
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<OrderItemResult> items,
        OrderAddressResult address,
        OrderPaymentResult payment,
        List<OrderCancellationResult> cancellations) {
    public static GetOrderResult from(final Order order) {
        final List<OrderItemResult> items = order.items().stream().filter(OrderItem::isActive).map(OrderItemResult::from).toList();
        return new GetOrderResult(order.key(), order.status(), order.totalPrice(), order.createdAt(), order.paidAt(), items,
                OrderAddressResult.from(order.address()), null, List.of());
    }

    public GetOrderResult withPayment(final OrderPaymentResult payment, final List<OrderCancellationResult> cancellations) {
        return new GetOrderResult(key, status, totalPrice, createdAt, paidAt, items, address, payment, cancellations);
    }
}
// @formatter:on
