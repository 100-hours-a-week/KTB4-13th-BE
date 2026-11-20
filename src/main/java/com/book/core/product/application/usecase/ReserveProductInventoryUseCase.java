package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.result.ProductInventoryState;
import com.book.core.product.application.result.LowStockInventoryEntry;
import com.book.core.product.domain.ProductInventory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReserveProductInventoryUseCase {
    private final ProductInventoryRepositoryPort inventoryRepository;
    private final Clock clock;

    @Transactional
    public Map<Long, Integer> execute(final ProductQuantitiesCommand command) {
        return execute(command, ProductInventory.LOW_STOCK_QUANTITY);
    }

    @Transactional
    public Map<Long, Integer> execute(final ProductQuantitiesCommand command, final int lowStockThreshold) {
        final Map<Long, Integer> lowStockQuantitiesByProductId = new LinkedHashMap<>();
        executeWithLowStockNotificationEntries(command, lowStockThreshold)
            .forEach((productId, entry) -> lowStockQuantitiesByProductId.put(productId, entry.state().availableQuantity()));
        return lowStockQuantitiesByProductId;
    }

    @Transactional
    public Map<Long, LowStockInventoryEntry> executeWithLowStockNotificationEntries(final ProductQuantitiesCommand command,
        final int lowStockThreshold) {
        final Map<Long, LowStockInventoryEntry> lowStockEntriesByProductId = new LinkedHashMap<>();
        command.quantities().forEach((final Long productId, final Integer quantity) -> {
            if (!inventoryRepository.reserve(productId, quantity)) {
                throw new CoreException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
            }
            final ProductInventoryState state = inventoryRepository.state(productId);
            if (ProductInventory.entersLowStock(state.availableQuantity() + quantity, state.availableQuantity(), lowStockThreshold)) {
                lowStockEntriesByProductId.put(productId, new LowStockInventoryEntry(state, LocalDateTime.now(clock)));
            }
        });
        return lowStockEntriesByProductId;
    }
}
