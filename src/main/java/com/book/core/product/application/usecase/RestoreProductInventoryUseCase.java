package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RestoreProductInventoryUseCase {
    private final ProductInventoryRepositoryPort inventoryRepository;

    @Transactional
    public void execute(final ProductQuantitiesCommand command) {
        command.quantities().forEach((final Long productId, final Integer quantity) -> {
            if (!inventoryRepository.restore(productId, quantity)) {
                throw new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT);
            }
        });
    }
}
