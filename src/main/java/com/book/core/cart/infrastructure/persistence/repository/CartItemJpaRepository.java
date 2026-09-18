package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByCartIdOrderByCreatedAtAscIdAsc(long cartId);

    Optional<CartItem> findByCartIdAndId(long cartId, long id);

    long deleteByCartIdAndIdIn(long cartId, List<Long> ids);
}
