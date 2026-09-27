package com.book.core.product.application.port;

import com.book.core.product.domain.Product;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.result.ProductListItem;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Optional<Product> findActiveById(final Long productId);

    List<ProductListItem> findActiveProducts(final Long categoryId, final ProductListSort sort, final ProductListCursor cursor,
        final int limit);
}
