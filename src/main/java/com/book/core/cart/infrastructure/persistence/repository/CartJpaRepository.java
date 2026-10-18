package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.domain.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartJpaRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndDeletedAtIsNull(final Long userId);
}
