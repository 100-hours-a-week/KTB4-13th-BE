package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {
    @Query("""
            select item
            from CartItem item
            where item.id = :cartItemId
              and item.cartId in (
                  select cart.id from Cart cart
                  where cart.userId = :userId and cart.status = :status
              )
              and item.status = :status
            """)
    Optional<CartItem> findByUserIdAndIdAndStatus(
            @Param("userId") final Long userId,
            @Param("cartItemId") final Long cartItemId,
            @Param("status") final EntityStatus status);

    Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId);

    int countByCartIdAndStatus(final Long cartId, final EntityStatus status);

    List<CartItem> findByCartIdAndStatusOrderByCreatedAtDescIdDesc(final Long cartId, final EntityStatus status);
}
