package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.port.ProductRestockRepositoryPort;
import com.book.core.product.domain.ProductRestock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReceiveProductRestockUseCase {
    private final ProductRestockRepositoryPort restockRepository;
    private final ProductInventoryRepositoryPort inventoryRepository;

    @Transactional
    public boolean execute(final Long restockId, final LocalDateTime now) {
        final ProductRestock restock = restockRepository.findPendingForUpdate(restockId).orElse(null);
        if (restock == null) {
            return false;
        }
        restock.receive(now);
        if (!inventoryRepository.receive(restock.productId(), restock.quantity())) {
            throw new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT);
        }
        return true;
    }
}
