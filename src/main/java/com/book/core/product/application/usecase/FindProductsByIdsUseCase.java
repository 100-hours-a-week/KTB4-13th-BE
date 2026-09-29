package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.domain.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindProductsByIdsUseCase {
    private final ProductRepositoryPort productRepository;

    @Transactional(readOnly = true)
    public List<Product> execute(final List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return productRepository.findByIds(productIds);
    }
}
