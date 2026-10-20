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

    @Query("""
            SELECT orders.key
            FROM Order orders
            WHERE orders.status = :status
              AND orders.expiresAt <= :now
              AND orders.deletedAt IS NULL
              AND NOT EXISTS (SELECT 1 FROM Payment payment WHERE payment.orderId = orders.id)
            ORDER BY orders.expiresAt ASC, orders.id ASC
            """)
    List<String> findExpiredOrderKeys(@Param("status") final OrderStatus status, @Param("now") final LocalDateTime now,
        final Pageable pageable);

    @Query("SELECT COUNT(payment) > 0 FROM Payment payment WHERE payment.orderId = :orderId")
    boolean hasPayment(@Param("orderId") final Long orderId);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT orders FROM Order orders WHERE orders.key = :key AND orders.deletedAt IS NULL")
    Optional<Order> findActiveWithItemsByKey(@Param("key") final String key);

    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.userId = :userId
              AND orders.deletedAt IS NULL
              AND orders.paidAt IS NOT NULL
              AND (:status IS NULL OR orders.status = :status)
              AND (:paidAtFrom IS NULL OR orders.paidAt >= :paidAtFrom)
              AND (:paidAtBefore IS NULL OR orders.paidAt < :paidAtBefore)
              AND (:cursorPaidAt IS NULL
                   OR orders.paidAt < :cursorPaidAt
                   OR (orders.paidAt = :cursorPaidAt AND orders.id < :cursorId))
            ORDER BY orders.paidAt DESC, orders.id DESC
            """)
    List<Order> findPaidOrders(@Param("userId") final Long userId, @Param("status") final OrderStatus status,
        @Param("paidAtFrom") final LocalDateTime paidAtFrom, @Param("paidAtBefore") final LocalDateTime paidAtBefore,
        @Param("cursorPaidAt") final LocalDateTime cursorPaidAt, @Param("cursorId") final Long cursorId, final Pageable pageable);
}
