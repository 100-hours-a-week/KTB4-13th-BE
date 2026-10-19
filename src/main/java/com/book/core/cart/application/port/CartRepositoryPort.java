package com.book.core.cart.application.port;

import com.book.core.cart.domain.Cart;
import java.util.Optional;

public interface CartRepositoryPort {
    Cart save(final Cart cart);

    Optional<Cart> findByUserIdWithLock(final Long userId);

    Optional<Cart> findByUserId(final Long userId);
}
