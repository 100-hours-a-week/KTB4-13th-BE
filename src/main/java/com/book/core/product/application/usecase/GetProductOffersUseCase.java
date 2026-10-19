package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.ProductOfferResult;
import com.book.core.product.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProductOffersUseCase {
    private final ProductRepositoryPort products;

    @Transactional(readOnly = true)
    public List<ProductOfferResult> execute(final List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        final Map<Long, Product> found =
            products.findByIds(productIds).stream().collect(Collectors.toMap(Product::id, Function.identity()));
        return productIds.stream().distinct().filter(found::containsKey).map((final Long id) -> {
            final Product product = found.get(id);
            final boolean active = product.isActive() && product.book().isActive();
            BigDecimal price = null;
            if (active) {
                price = product.discountedPrice();
            }
            return new ProductOfferResult(id, product.book().isbn13(), product.book().title(), price, product.stockQuantity(), active);
        }).toList();
    }
}
