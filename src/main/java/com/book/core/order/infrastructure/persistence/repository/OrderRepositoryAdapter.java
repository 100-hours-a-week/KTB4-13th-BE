package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    private final OrderJpaRepository jpaRepository;
    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public Order save(final Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findActiveOrderByKeyForUpdate(final String orderKey) {
        return jpaRepository.findByKeyAndDeletedAtIsNull(orderKey);
    }

    @Override
    public Optional<Order> findActiveOrderByKey(final String orderKey) {
        return jpaRepository.findActiveWithItemsByKey(orderKey);
    }

    @Override
    public List<Order> findActiveOrders(final Long userId, final OrderStatus status, final LocalDateTime from, final LocalDateTime to,
        final OrderListCursor cursor, final int limit) {
        final LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        final Long cursorId = cursor == null ? null : cursor.orderId();
        return jpaRepository.findActiveOrders(userId, status, from, to, cursorCreatedAt, cursorId, PageRequest.of(0, limit));
    }

    @Override
    public Optional<OrderItem> findActiveOrderItem(final Long orderItemId) {
        return orderItemJpaRepository.findByIdAndDeletedAtIsNullAndOrder_DeletedAtIsNull(orderItemId);
    }
}
