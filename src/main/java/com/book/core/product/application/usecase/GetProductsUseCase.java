package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.GetProductItemResult;
import com.book.core.product.application.result.GetProductsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProductsUseCase {
    private final ProductRepositoryPort productRepository;

    @Transactional(readOnly = true)
    public GetProductsResult execute(final GetProductsCommand command) {
        final int pageSize = command.pageSize();
        final int fetchSize;
        try {
            fetchSize = Math.addExact(pageSize, 1);
        } catch (ArithmeticException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        final var products = productRepository.findActiveProducts(command.categoryId(), command.cursor(), fetchSize);
        final boolean hasNext = products.size() > pageSize;
        final var items = products.stream()
                .limit(pageSize)
                .map(GetProductItemResult::from)
                .toList();
        final String nextCursor = hasNext ? String.valueOf(items.getLast().itemId()) : null;
        return GetProductsResult.of(items, nextCursor);
    }
}
