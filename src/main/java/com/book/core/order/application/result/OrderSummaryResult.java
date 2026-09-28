package com.book.core.order.application.result;

import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record OrderSummaryResult(String key, String name, OrderStatus status, BigDecimal totalPrice, LocalDateTime createdAt) {
    public static OrderSummaryResult from(final Order order) {
        return new OrderSummaryResult(order.key(), order.name(), order.status(), order.totalPrice(), order.createdAt());
    }
}
// @formatter:on
