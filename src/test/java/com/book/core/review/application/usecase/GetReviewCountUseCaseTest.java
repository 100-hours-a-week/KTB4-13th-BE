package com.book.core.review.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewTargetType;
import org.junit.jupiter.api.Test;

class GetReviewCountUseCaseTest {
    private final ReviewRepositoryPort repository = mock(ReviewRepositoryPort.class);
    private final GetReviewCountUseCase useCase = new GetReviewCountUseCase(repository);

    @Test
    void 대상의_활성_리뷰_수를_Repository에_요청해_반환한다() {
        final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
        when(repository.countActiveReviews(target)).thenReturn(2L);

        assertThat(useCase.execute(target)).isEqualTo(2L);

        verify(repository).countActiveReviews(target);
    }
}
