package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;
import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class CartRepositoryImpl implements CartRepository {
    private final CartJpaRepository cartRepository;
    private final CartItemJpaRepository itemRepository;

    @Override
    @Transactional
    public Cart findOrCreate(final long userId) {
        try {
            cartRepository.insertIfAbsent(userId);
            return cartRepository.findByUserId(userId).map(this::loadItems).orElseThrow(this::storageFailure);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new CoreException(ErrorType.STORAGE_FAILURE, exception);
        }
    }

    @Override
    @Transactional
    public void saveItem(final long cartId, final CartItem item) {
        try {
            final CartItem itemToSave;
            if (item.id() == null) {
                itemToSave = new CartItem(cartId, item.productId(), item.quantity());
            } else {
                final var existingItem = itemRepository
                        .findByCartIdAndId(cartId, item.id())
                        .orElseThrow(() -> new CoreException(ErrorType.INVALID_REQUEST));
                if (existingItem.productId() != item.productId()) {
                    throw new CoreException(ErrorType.INVALID_REQUEST);
                }
                itemToSave = existingItem.replaceQuantity(item.quantity());
            }
            itemRepository.saveAndFlush(itemToSave);
            touchCart(cartId);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new CoreException(ErrorType.STORAGE_FAILURE, exception);
        }
    }

    private Cart loadItems(final Cart cart) {
        final var items = itemRepository.findAllByCartIdOrderByCreatedAtAscIdAsc(cart.id());
        cart.loadItems(items);
        return cart;
    }

    private void touchCart(final long cartId) {
        cartRepository.findById(cartId).ifPresent(Cart::touch);
    }

    private CoreException storageFailure() {
        return new CoreException(ErrorType.STORAGE_FAILURE);
    }
}
