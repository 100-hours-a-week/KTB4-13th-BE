package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.domain.ProductView;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductViewJpaRepository extends JpaRepository<ProductView, Long> {
    @Query(value = """
        SELECT pv.user_id
        FROM product_views pv
        CROSS JOIN (
            SELECT MAX(pr.received_at) AS last_received_at
            FROM product_restocks pr
            WHERE pr.product_id = :productId
              AND pr.status = 'COMPLETED'
              AND pr.received_at <= :referenceTime
              AND pr.deleted_at IS NULL
        ) restock
        WHERE pv.product_id = :productId
          AND pv.viewed_at >= :viewedFrom
          AND pv.viewed_at <= :referenceTime
          AND (restock.last_received_at IS NULL OR pv.viewed_at > restock.last_received_at)
        GROUP BY pv.user_id
        HAVING COUNT(*) >= 2
        ORDER BY pv.user_id
        """, nativeQuery = true)
    List<Long> findLowStockNotificationCandidateUserIds(@Param("productId") Long productId, @Param("viewedFrom") LocalDateTime viewedFrom,
        @Param("referenceTime") LocalDateTime referenceTime);
}
