package com.book.core.order.application.port;

import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(final Order order);

    Optional<Order> findActiveOrderByKeyForUpdate(final String orderKey);

    Optional<OrderItem> findActiveOrderItem(final Long orderItemId);
}
