package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.GetProductDetailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProductDetailUseCase {
    private final ProductRepositoryPort productRepository;

    @Transactional(readOnly = true)
    public GetProductDetailResult execute(final GetProductDetailCommand command) {
        return productRepository
                .findActiveById(command.productId())
                .map(GetProductDetailResult::from)
                .orElseThrow(() -> new CoreException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public void validateAvailableStock(final Long productId, final int requestedQuantity) {
        final var product = execute(new GetProductDetailCommand(productId));
        if (product.stockQuantity() < requestedQuantity) {
            throw new CoreException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        }
    }
}
