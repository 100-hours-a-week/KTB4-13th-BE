package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class InitializeProductInventoriesUseCase {
    private final ProductInventoryRepositoryPort inventoryRepository;

    @Transactional
    public int execute() {
        return inventoryRepository.initializeMissing();
    }
}
