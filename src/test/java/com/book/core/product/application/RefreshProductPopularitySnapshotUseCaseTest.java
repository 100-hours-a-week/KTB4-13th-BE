package com.book.core.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.product.application.port.ProductPopularitySnapshotRefreshPort;
import com.book.core.product.application.usecase.RefreshProductPopularitySnapshotUseCase;
import org.junit.jupiter.api.Test;

class RefreshProductPopularitySnapshotUseCaseTest {
    @Test
    void 인기순_스냅샷_갱신을_Port에_위임한다() {
        final var refreshPort = new RecordingRefreshPort();
        final var useCase = new RefreshProductPopularitySnapshotUseCase(refreshPort);

        useCase.execute();

        assertThat(refreshPort.refreshCount).isEqualTo(1);
    }

    private static final class RecordingRefreshPort implements ProductPopularitySnapshotRefreshPort {
        private int refreshCount;

        @Override
        public void refresh() {
            refreshCount++;
        }
    }
}
