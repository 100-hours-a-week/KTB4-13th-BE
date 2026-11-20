package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.application.port.ProductViewRepositoryPort;
import com.book.core.product.domain.ProductView;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductViewRepositoryAdapter implements ProductViewRepositoryPort {
    private final ProductViewJpaRepository jpaRepository;

    @Override
    public ProductView save(final ProductView productView) {
        return jpaRepository.save(productView);
    }

    @Override
    public List<Long> findLowStockNotificationCandidateUserIds(final Long productId, final LocalDateTime viewedFrom,
        final LocalDateTime referenceTime) {
        return jpaRepository.findLowStockNotificationCandidateUserIds(productId, viewedFrom, referenceTime);
    }
}
