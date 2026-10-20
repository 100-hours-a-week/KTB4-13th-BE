package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRestockRepositoryPort;
import com.book.core.product.domain.ProductInventory;
import com.book.core.product.domain.ProductRestock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateProductRestockUseCase {
    private final ProductRestockRepositoryPort restockRepository;

    @Transactional
    public boolean execute(final Long productId) {
        final ProductInventory inventory = restockRepository.findInventoryForUpdate(productId).orElse(null);
        if (inventory == null || !inventory.needsRestock() || restockRepository.existsPending(productId)) {
            return false;
        }
        restockRepository.save(new ProductRestock(productId, inventory.restockQuantity()));
        return true;
    }
}
