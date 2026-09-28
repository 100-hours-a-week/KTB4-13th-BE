package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.domain.Order;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByKeyAndDeletedAtIsNull(final String key);
}
