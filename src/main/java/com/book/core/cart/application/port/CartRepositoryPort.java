package com.book.core.cart.application.port;

import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface CartRepositoryPort {
    Optional<Cart> findByUserId(final Long userId);
}
