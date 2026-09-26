package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.domain.OrderItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = "order")
    Optional<OrderItem> findByIdAndDeletedAtIsNullAndOrder_DeletedAtIsNull(final Long id);
}
