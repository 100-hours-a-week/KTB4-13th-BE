package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepositoryPort {

    private final CartJpaRepository jpaRepository;

    @Override
    public Optional<Cart> findByUserId(final Long userId) {
        return jpaRepository.findByUserId(userId);
    }
}
