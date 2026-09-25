package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.cart.domain.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CartJpaRepository extends JpaRepository<Cart, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.userId = :userId")
    Optional<Cart> findByUserIdWithLock(@Param("userId") final Long userId);

    Optional<Cart> findByUserIdAndStatus(final Long userId, final EntityStatus status);
}
