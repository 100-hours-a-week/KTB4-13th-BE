package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
    public Optional<OrderItem> findActiveOrderItem(final Long orderItemId) {
        return orderItemJpaRepository.findByIdAndDeletedAtIsNullAndOrder_DeletedAtIsNull(orderItemId);
    }
}
