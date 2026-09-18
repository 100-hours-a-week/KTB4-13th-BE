package com.book.core.cart.application.port;

import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;

public interface CartRepository {
    Cart findOrCreate(final long userId);

    void saveItem(final long cartId, final CartItem item);
}
