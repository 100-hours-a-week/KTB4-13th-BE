package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.domain.ProductInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductInventoryRepositoryAdapter implements ProductInventoryRepositoryPort {
    private final ProductInventoryJpaRepository jpaRepository;

    @Override
    public boolean reserve(final Long productId, final int quantity) {
        if (jpaRepository.reserve(productId, quantity) == 1) {
            return true;
        }
        if (jpaRepository.existsById(productId)) {
            return false;
        }
        jpaRepository.insertIfMissing(productId, ProductInventory.INITIAL_ON_HAND_QUANTITY, ProductInventory.DEFAULT_REORDER_POINT,
            ProductInventory.DEFAULT_TARGET_STOCK);
        return jpaRepository.reserve(productId, quantity) == 1;
    }

    @Override
    public int availableQuantity(final Long productId) {
        return jpaRepository.findAvailableQuantity(productId);
    }

    @Override
    public boolean release(final Long productId, final int quantity) {
        return jpaRepository.release(productId, quantity) == 1;
    }

    @Override
    public boolean complete(final Long productId, final int quantity) {
        return jpaRepository.complete(productId, quantity) == 1;
    }

    @Override
    public boolean restore(final Long productId, final int quantity) {
        return jpaRepository.restore(productId, quantity) == 1;
    }

    @Override
    public boolean receive(final Long productId, final int quantity) {
        return jpaRepository.restore(productId, quantity) == 1;
    }
}
