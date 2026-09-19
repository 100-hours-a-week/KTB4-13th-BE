package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryAdapter implements CartItemRepositoryPort {

    private final CartItemJpaRepository jpaRepository;

    @Override
    public Optional<CartItem> findActiveByUserIdAndId(final Long userId, final Long cartItemId) {
        return jpaRepository.findByUserIdAndIdAndStatus(userId, cartItemId, EntityStatus.ACTIVE);
    }

    @Override
    public Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long productId) {
        return jpaRepository.findByCartIdAndProductId(cartId, productId);
    }

    @Override
    public CartItem save(final CartItem cartItem) {
        return jpaRepository.save(cartItem);
    }

    @Override
    public int countActiveByCartId(final Long cartId) {
        return jpaRepository.countByCartIdAndStatus(cartId, EntityStatus.ACTIVE);
    }

    @Override
    public List<CartItem> findActiveByCartId(final Long cartId) {
        return jpaRepository.findByCartIdAndStatusOrderByCreatedAtDescIdDesc(cartId, EntityStatus.ACTIVE);
    }
}
