package com.book.core.review.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetReviewsUseCaseTest {
    private final ReviewRepositoryPort repository = mock(ReviewRepositoryPort.class);
    private final GetReviewsUseCase useCase = new GetReviewsUseCase(repository);

    @Test
    void 대상과_offset을_조회하고_목록과_다음_페이지_여부를_반환한다() {
        final var paging = new OffsetLimit(3, 2);
        final var item = new ReviewItemResult(8L, new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new BigDecimal("4.5"), "본문");
        when(repository.findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging))
            .thenReturn(new Page<>(List.of(item), true));

        final var result = useCase.execute(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging);

        assertThat(result.content()).containsExactly(item);
        assertThat(result.hasNext()).isTrue();
        verify(repository).findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging);
    }

    @Test
    void 리뷰가_없으면_빈_목록을_반환한다() {
        final var paging = new OffsetLimit(0, 20);
        when(repository.findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging))
            .thenReturn(new Page<>(List.of(), false));

        final var result = useCase.execute(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging);

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }
}
