package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrderJpaRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByKeyAndDeletedAtIsNull(final String key);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT orders FROM Order orders WHERE orders.key = :key AND orders.deletedAt IS NULL")
    Optional<Order> findActiveWithItemsByKey(@Param("key") final String key);

    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.userId = :userId
              AND orders.deletedAt IS NULL
              AND (:status IS NULL OR orders.status = :status)
              AND orders.createdAt >= :from
              AND orders.createdAt <= :to
              AND (:cursorCreatedAt IS NULL
                   OR orders.createdAt < :cursorCreatedAt
                   OR (orders.createdAt = :cursorCreatedAt AND orders.id < :cursorId))
            ORDER BY orders.createdAt DESC, orders.id DESC
            """)
    List<Order> findActiveOrders(@Param("userId") final Long userId, @Param("status") final OrderStatus status,
        @Param("from") final LocalDateTime from, @Param("to") final LocalDateTime to,
        @Param("cursorCreatedAt") final LocalDateTime cursorCreatedAt, @Param("cursorId") final Long cursorId, final Pageable pageable);
}
