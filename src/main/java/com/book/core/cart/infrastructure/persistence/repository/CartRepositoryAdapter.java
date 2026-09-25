package com.book.core.cart.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepositoryPort {

    private final CartJpaRepository jpaRepository;

    @Override
    public Optional<Cart> findByUserIdWithLock(final Long userId) {
        return jpaRepository.findByUserIdWithLock(userId);
    }

    @Override
    public Optional<Cart> findActiveByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndStatus(userId, EntityStatus.ACTIVE);
    }
}
