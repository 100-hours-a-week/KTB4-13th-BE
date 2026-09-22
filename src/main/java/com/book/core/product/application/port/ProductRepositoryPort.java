package com.book.core.product.application.port;

import com.book.core.product.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Optional<Product> findActiveById(final Long productId);

    List<Product> findActiveProducts(final Long categoryId, final Long cursor, final int limit);
}
