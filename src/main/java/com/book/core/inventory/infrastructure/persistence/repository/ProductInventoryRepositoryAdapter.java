package com.book.core.inventory.infrastructure.persistence.repository;

import com.book.core.inventory.application.port.ProductInventoryRepositoryPort;
import com.book.core.inventory.domain.ProductInventory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductInventoryRepositoryAdapter implements ProductInventoryRepositoryPort {
    private final ProductInventoryJpaRepository repository;

    @Override
    public List<ProductInventory> findByProductIds(final List<Long> productIds) {
        return productIds.isEmpty() ? List.of() : repository.findByProductIdIn(productIds);
    }
}
