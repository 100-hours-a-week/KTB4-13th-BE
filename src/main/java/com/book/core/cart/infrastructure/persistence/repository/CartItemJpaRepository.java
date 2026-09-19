package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.cart.domain.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId);

    int countByCartIdAndStatus(final Long cartId, final EntityStatus status);
}
