package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CartJpaRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(final Long userId);
}
