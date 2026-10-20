package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.application.command.OrderListCursor;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
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
    public List<Order> findOrdersForAutoConfirmation(final LocalDateTime paidAtBefore, final int limit) {
        return jpaRepository.findOrdersForAutoConfirmation(List.of(OrderStatus.PAID, OrderStatus.PARTIAL_CANCELED), paidAtBefore,
            PageRequest.of(0, limit));
    }

    @Override
    public List<Order> findOrdersForConfirmationReminder(final LocalDateTime paidAtAfter, final LocalDateTime paidAtBefore,
        final int limit) {
        return jpaRepository.findOrdersForConfirmationReminder(List.of(OrderStatus.PAID, OrderStatus.PARTIAL_CANCELED), paidAtAfter,
            paidAtBefore, PageRequest.of(0, limit));
    }

    @Override
    public Optional<OrderItem> findActiveOrderItem(final Long orderItemId) {
        return orderItemJpaRepository.findByIdAndDeletedAtIsNullAndOrder_DeletedAtIsNull(orderItemId);
    }

    @Override
    public List<OrderItem> findOrderItemsByOrderIdIn(final Collection<Long> orderIds) {
        return orderItemJpaRepository.findByOrder_IdIn(orderIds);
    }

    @Override
    public List<OrderItem> findOrderItemsByIdIn(final Collection<Long> orderItemIds) {
        return orderItemJpaRepository.findByIdIn(orderItemIds);
    }
}
