package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.result.ProductInventoryState;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProductInventoryStateForUpdateUseCase {
    private final ProductInventoryRepositoryPort inventoryRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ProductInventoryState> execute(final Long productId) {
        return inventoryRepository.stateForUpdate(productId);
    }
}
