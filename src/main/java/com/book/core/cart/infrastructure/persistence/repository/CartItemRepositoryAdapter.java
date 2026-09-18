package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.domain.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryAdapter implements CartItemRepositoryPort {

    private final CartItemJpaRepository jpaRepository;

    @Override
    public Optional<CartItem> findByCartIdAndProductId(final Long cartId, final Long userId) {
        return jpaRepository.findByCartIdAndProductId(cartId, userId);
    }
}
