package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.domain.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CartJpaRepository extends JpaRepository<Cart, Long> {
    @Query("select cart from Cart cart where cart.userId = :userId")
    Optional<Cart> findByUserId(@Param("userId") long userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO carts (user_id, created_at, updated_at)
        VALUES (:userId, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE id = id
        """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") long userId);
}
