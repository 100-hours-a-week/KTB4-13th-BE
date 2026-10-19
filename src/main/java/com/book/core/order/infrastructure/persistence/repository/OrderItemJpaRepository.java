package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.domain.OrderItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = "order")
    @Query("SELECT item FROM OrderItem item WHERE item.id = :id")
    Optional<OrderItem> findWithOrderById(@Param("id") final Long id);

    @EntityGraph(attributePaths = "order")
    Optional<OrderItem> findByIdAndDeletedAtIsNullAndOrder_DeletedAtIsNull(final Long id);
}
