package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long userId);
}
