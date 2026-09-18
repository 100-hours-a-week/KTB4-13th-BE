package com.book.core.cart.application;

import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InMemoryCartRepository implements CartRepository {
    private final Map<Long, Cart> carts = new HashMap<>();
    private long itemSequence;

    @Override
    public Cart findOrCreate(final long userId) {
        return carts.computeIfAbsent(userId, (final var id) -> new Cart(id, userId, List.of()));
    }

    @Override
    public void saveItem(final long cartId, final CartItem item) {
        final var cart = carts.get(cartId);
        final var items = new ArrayList<>(cart.items());
        items.removeIf((final var old) -> old.productId() == item.productId());
        final long itemId = item.id() == null ? ++itemSequence : item.id();
        items.add(new CartItem(Long.valueOf(itemId), item.productId(), item.quantity()));
        cart.loadItems(items);
    }

    Cart cart(final long userId) {
        return carts.get(userId);
    }
}
