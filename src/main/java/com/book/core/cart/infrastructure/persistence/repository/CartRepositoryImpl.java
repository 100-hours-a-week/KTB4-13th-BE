package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.cart.domain.exception.CartErrorCode;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
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
    @Transactional(readOnly = true)
    public Optional<Cart> findByUserId(final long userId) {
        try {
            return cartRepository.findByUserId(userId).map(this::loadItems);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    @Transactional
    public Cart lockOrCreate(final long userId) {
        try {
            cartRepository.insertIfAbsent(userId);
            return cartRepository
                    .findByUserIdForUpdate(userId)
                    .map(this::loadItems)
                    .orElseThrow(this::storageFailure);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    @Transactional
    public Optional<Cart> lockByUserId(final long userId) {
        try {
            return cartRepository.findByUserIdForUpdate(userId).map(this::loadItems);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    @Transactional
    public void saveItem(final long cartId, final CartItem item) {
        try {
            final CartItem persistedItem;
            if (item.id() == null) {
                persistedItem = new CartItem(cartId, item.productId(), item.quantity());
            } else {
                persistedItem = itemRepository
                        .findByCartIdAndId(cartId, item.id())
                        .map((final var existing) -> {
                            if (existing.productId() != item.productId()) {
                                throw new BusinessException(CartErrorCode.OPERATION_NOT_ALLOWED);
                            }
                            existing.updateQuantity(item.quantity());
                            return existing;
                        })
                        .orElseThrow(() -> new BusinessException(CartErrorCode.OPERATION_NOT_ALLOWED));
            }
            itemRepository.saveAndFlush(persistedItem);
            touchCart(cartId);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    @Transactional
    public void deleteItems(final long cartId, final List<Long> cartItemIds) {
        try {
            if (itemRepository.deleteByCartIdAndIdIn(cartId, cartItemIds) > 0) {
                touchCart(cartId);
            }
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
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

    private BusinessException storageFailure() {
        return new BusinessException(CommonErrorCode.STORAGE_FAILURE);
    }
}
