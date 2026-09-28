package com.book.core.product.infrastructure.persistence.repository;

import com.book.core.product.application.port.ProductPopularitySnapshotRefreshPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductPopularitySnapshotRefreshAdapter implements ProductPopularitySnapshotRefreshPort {
    private final ProductQueryRepository queryRepository;

    @Override
    public void refresh() {
        queryRepository.refreshPopularitySnapshots();
    }
}
