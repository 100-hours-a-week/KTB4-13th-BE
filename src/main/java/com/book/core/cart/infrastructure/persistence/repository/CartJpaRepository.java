package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.domain.Cart;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CartJpaRepository extends JpaRepository<Cart, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.userId = :userId and cart.deletedAt is null")
    Optional<Cart> findByUserIdWithLock(@Param("userId") final Long userId);

    Optional<Cart> findByUserIdAndDeletedAtIsNull(final Long userId);

    boolean existsByUserId(final Long userId);

    @Query(value = """
        SELECT u.id
        FROM users u
        WHERE u.deleted_at IS NULL
          AND u.created_at <= :registeredBefore
          AND u.id > :afterUserId
          AND NOT EXISTS (SELECT 1 FROM carts c WHERE c.user_id = u.id)
        ORDER BY u.id
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findActiveUserIdsWithoutCart(@Param("registeredBefore") final LocalDateTime registeredBefore,
        @Param("afterUserId") final Long afterUserId, @Param("limit") final int limit);
}
