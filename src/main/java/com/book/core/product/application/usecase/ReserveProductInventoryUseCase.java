package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.usecase.GetCartUserIdsByProductIdUseCase;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.port.ProductInventoryRepositoryPort;
import com.book.core.product.application.result.LowStockReservationResult;
import com.book.core.product.application.result.ProductInventoryReservationResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReserveProductInventoryUseCase {
    private final ProductInventoryRepositoryPort inventoryRepository;
    private final GetCartUserIdsByProductIdUseCase getCartUserIdsByProductIdUseCase;

    @Transactional
    public List<LowStockReservationResult> execute(final ProductQuantitiesCommand command) {
        final List<LowStockReservationResult> lowStockReservations = new ArrayList<>();
        command.quantities().forEach((final Long productId, final Integer quantity) -> {
            final ProductInventoryReservationResult result = inventoryRepository.reserve(productId, quantity);
            if (!result.reserved()) {
                throw new CoreException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
            }
            if (result.lowStockEntered()) {
                lowStockReservations.add(new LowStockReservationResult(productId, result.availableQuantity(), result.lowStockCycle(),
                    getCartUserIdsByProductIdUseCase.execute(productId)));
            }
        });
        return List.copyOf(lowStockReservations);
    }
}
