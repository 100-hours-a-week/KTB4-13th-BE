package com.book.core.order.application.port;

import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(final Order order);

    Optional<Order> findActiveOrderByKeyForUpdate(final String orderKey);

    Optional<Order> findActiveOrderByKey(final String orderKey);

    List<Order> findActiveOrders(final Long userId, final OrderStatus status, final LocalDateTime from, final LocalDateTime to,
        final OrderListCursor cursor, final int limit);

    Optional<OrderItem> findActiveOrderItem(final Long orderItemId);
}
