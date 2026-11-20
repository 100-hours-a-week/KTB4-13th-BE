package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.core.product.application.port.ProductViewRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindLowStockNotificationCandidatesUseCaseTest {
    private final ProductViewRepositoryPort repository = mock(ProductViewRepositoryPort.class);
    private final FindLowStockNotificationCandidatesUseCase useCase = new FindLowStockNotificationCandidatesUseCase(repository);

    @Test
    void 재고_임계치_진입_시각을_기준으로_최근_7일_조회_대상을_요청한다() {
        final LocalDateTime referenceTime = LocalDateTime.of(2026, 10, 26, 12, 0);
        final LocalDateTime viewedFrom = referenceTime.minusDays(7);
        when(repository.findLowStockNotificationCandidateUserIds(20L, viewedFrom, referenceTime)).thenReturn(List.of(10L, 11L));

        assertThat(useCase.execute(20L, referenceTime)).containsExactly(10L, 11L);
        verify(repository).findLowStockNotificationCandidateUserIds(20L, viewedFrom, referenceTime);
    }

    @Test
    void 상품이나_기준_시각이_유효하지_않으면_조회하지_않는다() {
        final LocalDateTime referenceTime = LocalDateTime.of(2026, 10, 26, 12, 0);
        assertThatThrownBy(() -> useCase.execute(null, referenceTime)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> useCase.execute(0L, referenceTime)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> useCase.execute(20L, null)).isInstanceOf(CoreException.class);
        verifyNoInteractions(repository);
    }
}
