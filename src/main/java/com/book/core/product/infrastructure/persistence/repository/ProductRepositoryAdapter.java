package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.inventory.application.port.ProductInventoryRepositoryPort;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.CartChangeProductRepositoryPort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.result.CartChangeProductResult;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.domain.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort, CartChangeProductRepositoryPort {
    private final ProductJpaRepository jpaRepository;
    private final ProductQueryRepository queryRepository;
    private final EntityManager entityManager;
    private final ProductInventoryRepositoryPort inventoryRepository;

    @Override
    public Map<Long, CartChangeProductResult> findByIdsForChange(final List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        final List<Product> found = jpaRepository.findByIdInWithBook(productIds);
        final Map<Long, CartChangeProductResult> products = new LinkedHashMap<>();
        final Map<Long, Integer> quantities = new java.util.HashMap<>();
        inventoryRepository.findByProductIds(found.stream().map(Product::id).toList())
            .forEach(inventory -> quantities.put(inventory.productId(), inventory.availableQuantity()));
        for (final Product product : found.stream().sorted(Comparator.comparing(Product::id)).toList()) {
            entityManager.lock(product, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            final Integer availableQuantity = quantities.get(product.id());
            if (availableQuantity == null) {
                throw new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT);
            }
            products.put(product.id(), new CartChangeProductResult(product.id(), product.discountedPrice(), availableQuantity,
                product.isActive() && product.book().isActive()));
        }
        for (final Product product : found.stream().sorted(Comparator.comparing((final Product value) -> value.book().id())).toList()) {
            entityManager.lock(product.book(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }
        return Map.copyOf(products);
    }

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
    public List<Product> findActiveByBookIds(final List<Long> bookIds) {
        return jpaRepository.findActiveByBookIdIn(bookIds);
    }

    @Override
    public List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
        final ProductListSort sort, final ProductListCursor cursor, final int limit) {
        return queryRepository.findActiveProducts(categoryId, publishedFrom, publishedTo, sort, cursor, limit);
    }
}
