package com.book.core.order.application.result;

import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record OrderSummaryResult(
        String key,
        String representativeBookName,
        int itemCount,
        String thumbnailUrl,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime paidAt) {
    public static OrderSummaryResult from(final Order order) {
        final OrderItem representativeItem = order.representativeItem();
        return new OrderSummaryResult(order.key(), representativeItem.itemName(), order.itemCount(), representativeItem.thumbnailUrl(),
                order.totalPrice(), order.status(), order.paidAt());
    }
}
// @formatter:on
