package com.book.core.product.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.domain.Product;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {
    private final ProductJpaRepository jpaRepository;

    @Override
    public Optional<Product> findActiveById(final Long productId) {
        return jpaRepository.findActiveById(productId, EntityStatus.ACTIVE);
    }
}
