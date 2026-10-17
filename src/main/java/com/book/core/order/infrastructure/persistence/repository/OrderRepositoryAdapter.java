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
    public Optional<OrderItem> findOrderItemById(final Long orderItemId) {
        return orderItemJpaRepository.findWithOrderById(orderItemId);
    }

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
    public List<Order> findPaidOrders(final Long userId, final OrderStatus status, final LocalDateTime paidAtFrom,
        final LocalDateTime paidAtBefore, final OrderListCursor cursor, final int limit) {
        final PageRequest page = PageRequest.of(0, limit);
        if (cursor == null) {
            return jpaRepository.findPaidOrders(userId, status, paidAtFrom, paidAtBefore, null, null, page);
        }
        return jpaRepository.findPaidOrders(userId, status, paidAtFrom, paidAtBefore, cursor.paidAt(), cursor.orderId(), page);
    }

    @Override
    public Optional<OrderItem> findActiveOrderItem(final Long orderItemId) {
        return orderItemJpaRepository.findByIdAndDeletedAtIsNullAndOrder_DeletedAtIsNull(orderItemId);
    }
}
