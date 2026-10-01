package com.book.core.product.application.port;

import com.book.core.product.domain.Product;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.result.ProductListItem;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface ProductRepositoryPort {
    List<Product> findByIds(final List<Long> productIds);

    Optional<Product> findActiveById(final Long productId);

    Optional<Product> findActiveByBookId(final Long bookId);

    List<Product> findActiveByBookIds(final List<Long> bookIds);

    List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
        final ProductListSort sort, final ProductListCursor cursor, final int limit);
}
