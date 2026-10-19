package com.book.core.cart.infrastructure.persistence.repository;

import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepositoryPort {

    private final CartJpaRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public Cart save(final Cart cart) {
        return jpaRepository.save(cart);
    }

    @Override
    public Optional<Cart> findByUserIdForChange(final Long userId) {
        final Optional<Cart> found = jpaRepository.findByUserIdAndDeletedAtIsNull(userId);
        found.ifPresent((final Cart cart) -> {
            final int updated = entityManager
                .createQuery(
                    "update Cart c set c.version = c.version + 1 " + "where c.id = :id and c.version = :version and c.deletedAt is null")
                .setParameter("id", cart.id()).setParameter("version", cart.version()).executeUpdate();
            if (updated != 1) {
                throw new OptimisticLockException("Cart changed", null, cart);
            }
            // 같은 트랜잭션의 재참여와 JPA 변경 감지가 갱신된 버전을 사용하도록 동기화합니다.
            entityManager.refresh(cart);
        });
        return found;
    }

    @Override
    public Optional<Cart> findByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }
}
