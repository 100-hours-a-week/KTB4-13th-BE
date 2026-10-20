package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.application.port.ProductRestockRepositoryPort;
import com.book.core.product.domain.ProductInventory;
import com.book.core.product.domain.ProductRestock;
import com.book.core.product.domain.ProductRestockStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductRestockRepositoryAdapter implements ProductRestockRepositoryPort {
    private final ProductRestockJpaRepository restockJpaRepository;
    private final ProductInventoryJpaRepository inventoryJpaRepository;

    @Override
    public List<Long> findRestockCandidateProductIds() {
        return inventoryJpaRepository.findRestockCandidateProductIds(ProductRestockStatus.PENDING);
    }

    @Override
    public Optional<ProductInventory> findInventoryForUpdate(final Long productId) {
        return inventoryJpaRepository.findForUpdate(productId);
    }

    @Override
    public boolean existsPending(final Long productId) {
        return restockJpaRepository.existsByProductIdAndStatus(productId, ProductRestockStatus.PENDING);
    }

    @Override
    public ProductRestock save(final ProductRestock restock) {
        return restockJpaRepository.save(restock);
    }

    @Override
    public List<Long> findPendingIdsCreatedBefore(final LocalDateTime createdBefore) {
        return restockJpaRepository.findIdsByStatusCreatedBefore(ProductRestockStatus.PENDING, createdBefore);
    }

    @Override
    public Optional<ProductRestock> findPendingForUpdate(final Long restockId) {
        return restockJpaRepository.findByIdAndStatus(restockId, ProductRestockStatus.PENDING);
    }
}
