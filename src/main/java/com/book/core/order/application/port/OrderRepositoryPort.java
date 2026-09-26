package com.book.core.order.application.port;

import com.book.core.order.domain.Order;

public interface OrderRepositoryPort {
    Order save(final Order order);
}
