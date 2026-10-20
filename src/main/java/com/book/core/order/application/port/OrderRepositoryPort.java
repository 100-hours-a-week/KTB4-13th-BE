package com.book.core.order.application.port;

import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {

    Optional<OrderItem> findOrderItemById(final Long orderItemId);

    Order save(final Order order);

    Optional<Order> findActiveOrderByKeyForUpdate(final String orderKey);

    Optional<Order> findActiveOrderByKey(final String orderKey);

    List<Order> findPaidOrders(final Long userId, final OrderStatus status, final LocalDateTime paidAtFrom,
        final LocalDateTime paidAtBefore, final OrderListCursor cursor, final int limit);

    default List<Order> findOrdersForAutoConfirmation(final LocalDateTime paidAtBefore, final int limit) {
        return List.of();
    }

    default List<Order> findOrdersForConfirmationReminder(final LocalDateTime paidAtAfter, final LocalDateTime paidAtBefore,
        final int limit) {
        return List.of();
    }

    Optional<OrderItem> findActiveOrderItem(final Long orderItemId);

    List<OrderItem> findOrderItemsByOrderIdIn(final Collection<Long> orderIds);

    List<OrderItem> findOrderItemsByIdIn(final Collection<Long> orderItemIds);
}
