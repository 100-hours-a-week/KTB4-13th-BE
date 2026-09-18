package com.book.core.cart.application;

import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class MemoryCartRepository implements CartRepository {
    private final Map<Long, List<CartItem>> carts = new HashMap<>();
    private long sequence;

    @Override
    public Optional<Cart> findByUserId(final long userId) {
        return Optional.ofNullable(carts.get(userId)).map((final var items) -> new Cart(userId, userId, items));
    }

    @Override
    public Cart lockOrCreate(final long userId) {
        carts.computeIfAbsent(userId, (final var ignored) -> new ArrayList<>());
        return findByUserId(userId).orElseThrow();
    }

    @Override
    public Optional<Cart> lockByUserId(final long userId) {
        return findByUserId(userId);
    }

    @Override
    public void saveItem(final long cartId, final CartItem item) {
        final var items = carts.get(cartId);
        items.removeIf((final var old) -> old.productId() == item.productId());
        final long itemId;
        if (item.id() == null) {
            itemId = ++sequence;
        } else {
            itemId = item.id();
        }
        items.add(new CartItem(itemId, item.productId(), item.quantity()));
    }

    @Override
    public void deleteItems(final long cartId, final List<Long> ids) {
        carts.get(cartId).removeIf((final var item) -> ids.contains(item.id()));
    }
}
