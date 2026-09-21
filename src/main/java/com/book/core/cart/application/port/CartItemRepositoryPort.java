package com.book.core.cart.application.port;

import com.book.core.cart.domain.CartItem;
import java.util.Optional;

public interface CartItemRepositoryPort {
    Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId);

    CartItem save(final CartItem cartItem);

    int countActiveByCartId(final Long cartId);
}
