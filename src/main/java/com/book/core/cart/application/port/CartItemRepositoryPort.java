package com.book.core.cart.application.port;

import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;

public interface CartItemRepositoryPort {
    Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId);

    List<CartItem> findActiveByUserIdAndIds(final Long userId, final List<Long> cartItemIds);

    Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId);

    CartItem save(final CartItem cartItem);

    int countActiveByCartId(final Long cartId);

    List<CartItem> findActiveByCartId(final Long cartId);
}
