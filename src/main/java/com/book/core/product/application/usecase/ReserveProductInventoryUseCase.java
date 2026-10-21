package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.domain.ProductInventory;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReserveProductInventoryUseCase {
    private final ProductInventoryRepositoryPort inventoryRepository;

    @Transactional
    public Map<Long, Integer> execute(final ProductQuantitiesCommand command) {
        final Map<Long, Integer> lowStockQuantitiesByProductId = new LinkedHashMap<>();
        command.quantities().forEach((final Long productId, final Integer quantity) -> {
            if (!inventoryRepository.reserve(productId, quantity)) {
                throw new CoreException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
            }
            final int availableQuantity = inventoryRepository.availableQuantity(productId);
            if (ProductInventory.entersLowStock(availableQuantity + quantity, availableQuantity)) {
                lowStockQuantitiesByProductId.put(productId, availableQuantity);
            }
        });
        return lowStockQuantitiesByProductId;
    }
}
