package com.book.core.cart.infrastructure.persistence.repository;

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
              and item.deletedAt is null
              and item.cartId in (
                  select cart.id from Cart cart
                  where cart.userId = :userId and cart.deletedAt is null
              )
            """)
    Optional<CartItem> findActiveByUserIdAndId(@Param("userId") final Long userId, @Param("cartItemId") final Long cartItemId);

    @Query("""
            select item
            from CartItem item
            where item.id in :cartItemIds
              and item.deletedAt is null
              and item.cartId in (
                  select cart.id from Cart cart
                  where cart.userId = :userId and cart.deletedAt is null
              )
            """)
    List<CartItem> findActiveByUserIdAndIds(@Param("userId") final Long userId, @Param("cartItemIds") final List<Long> cartItemIds);

    Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId);

    int countByCartIdAndDeletedAtIsNull(final Long cartId);

    List<CartItem> findByCartIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(final Long cartId);
}
