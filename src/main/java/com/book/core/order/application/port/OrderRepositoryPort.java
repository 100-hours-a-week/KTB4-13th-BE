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

    List<String> findExpiredOrderKeys(final LocalDateTime now, final int limit);

    boolean hasPendingPgOperation(final String orderKey);

    Optional<Order> findActiveOrderByKeyForUpdate(final String orderKey);

    Optional<Order> findActiveOrderByKey(final String orderKey);

    List<Order> findPaidOrders(final Long userId, final OrderStatus status, final LocalDateTime paidAtFrom,
        final LocalDateTime paidAtBefore, final OrderListCursor cursor, final int limit);

    Optional<OrderItem> findActiveOrderItem(final Long orderItemId);

    List<OrderItem> findOrderItemsByOrderIdIn(final Collection<Long> orderIds);

    List<OrderItem> findOrderItemsByIdIn(final Collection<Long> orderItemIds);
}
