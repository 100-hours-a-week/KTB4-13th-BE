package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductPopularitySnapshotRefreshPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RefreshProductPopularitySnapshotUseCase {
    private final ProductPopularitySnapshotRefreshPort popularitySnapshotRefreshPort;

    @Transactional
    public void execute() {
        popularitySnapshotRefreshPort.refresh();
    }
}
