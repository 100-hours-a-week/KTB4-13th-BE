package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.RecommendationProductResult;
import com.book.core.product.domain.Product;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetRecommendationProductsUseCase {
    private final ProductRepositoryPort products;

    @Transactional(readOnly = true)
    public Map<Long, RecommendationProductResult> execute(final List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return products.findActiveByBookIds(bookIds).stream().collect(Collectors.toMap((final Product product) -> product.book().id(),
            (final Product product) -> new RecommendationProductResult(product.id(), product.discountedPrice())));
    }
}
