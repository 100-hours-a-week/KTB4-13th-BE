package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(final Order order) {
        return jpaRepository.save(order);
    }
}
