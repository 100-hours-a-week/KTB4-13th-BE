package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.domain.Product;
import com.book.core.product.application.result.ProductListItem;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {
    private final ProductJpaRepository jpaRepository;
    private final ProductQueryRepository queryRepository;

    @Override
    public List<Product> findByIds(final List<Long> productIds) {
        return jpaRepository.findByIdInWithBook(productIds);
    }

    @Override
    public Optional<Product> findActiveById(final Long productId) {
        return jpaRepository.findActiveById(productId);
    }

    @Override
    public Optional<Product> findActiveByBookId(final Long bookId) {
        return jpaRepository.findActiveByBookId(bookId);
    }

    @Override
    public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
        final ProductListSort sort, final ProductListCursor cursor, final int limit) {
        return queryRepository.findActiveProducts(categoryId, publishedFrom, publishedTo, sort, cursor, limit);
    }
}
