package com.book.core.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.paging.OffsetLimit;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.result.GetReviewsResult;
import com.book.core.review.application.usecase.CreateReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    @Test
    void 조회_Command를_UseCase에_전달하고_결과를_반환한다() {
        final var getReviews = mock(GetReviewsUseCase.class);
        final var service = new ReviewService(mock(CreateReviewUseCase.class), mock(UpdateReviewUseCase.class),
            mock(DeleteReviewUseCase.class), getReviews, mock(ValidateReviewPolicyUseCase.class));
        final var command = new GetReviewsCommand(ReviewTargetType.PRODUCT, 10L, new OffsetLimit(0, 20));
        final var result = new GetReviewsResult(List.of(), false);
        when(getReviews.execute(command)).thenReturn(result);

        assertThat(service.findReviews(command)).isSameAs(result);
        verify(getReviews).execute(command);
    }
}
