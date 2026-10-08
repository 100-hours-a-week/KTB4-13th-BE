package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.domain.Product;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindProductIdsByBookIdsUseCase {
    private final ProductRepositoryPort productRepository;

    // A book has at most one product (uk_products_book_id), so each bookId maps to a single productId.
    @Transactional(readOnly = true)
    public Map<Long, Long> execute(final List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findActiveByBookIds(bookIds).stream()
            .collect(Collectors.toMap((final Product product) -> product.book().id(), Product::id));
    }
}
