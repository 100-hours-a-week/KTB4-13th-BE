package com.book.core.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewFormat;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    private final ReviewWriteService reviewWrite = mock(ReviewWriteService.class);
    private final GetReviewsUseCase getReviews = mock(GetReviewsUseCase.class);
    private final EarnPointUseCase earnPoint = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deductPoint = mock(DeductPointUseCase.class);
    private final ReviewService service = new ReviewService(reviewWrite, getReviews, earnPoint, deductPoint);
    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
    private final ReviewContent content = new ReviewContent(new BigDecimal("4.5"), "본문", false);
    private final AddReviewCommand addCommand = new AddReviewCommand(42L, target, content);
    private final UpdateReviewCommand updateCommand = new UpdateReviewCommand(42L, 801L, content);
    private final RemoveReviewCommand removeCommand = new RemoveReviewCommand(42L, 801L);

    @Test
    void 조회_Command의_VO를_조회_UseCase에_전달한다() {
        final var paging = new OffsetLimit(0, 20);
        final Page<ReviewItemResult> result = new Page<>(List.of(), false);
        when(getReviews.execute(target, paging)).thenReturn(result);
        assertThat(service.findReviews(new GetReviewsCommand(target, paging))).isSameAs(result);
        verifyNoInteractions(reviewWrite, earnPoint, deductPoint);
    }

    @Test
    void 작성_트랜잭션이_끝난_뒤_포인트를_적립하고_ID를_반환한다() {
        final var result = new ReviewProcessResult(801L, ReviewFormat.TEXT);
        when(reviewWrite.addReview(addCommand)).thenReturn(result);

        assertThat(service.addReview(addCommand)).isEqualTo(801L);

        final var order = inOrder(reviewWrite, earnPoint);
        order.verify(reviewWrite).addReview(addCommand);
        order.verify(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(deductPoint);
    }

    @Test
    void 수정_트랜잭션에_ID를_반환하고_포인트를_호출하지_않는다() {
        when(reviewWrite.updateReview(updateCommand)).thenReturn(801L);

        assertThat(service.updateReview(updateCommand)).isEqualTo(801L);

        verifyNoInteractions(earnPoint, deductPoint);
    }

    @Test
    void 삭제_트랜잭션이_끝난_뒤_포인트를_차감하고_ID를_반환한다() {
        when(reviewWrite.removeReview(removeCommand)).thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));

        assertThat(service.removeReview(removeCommand)).isEqualTo(801L);

        final var order = inOrder(reviewWrite, deductPoint);
        order.verify(reviewWrite).removeReview(removeCommand);
        order.verify(deductPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(earnPoint);
    }

    @Test
    void 작성_트랜잭션이_실패하면_포인트를_호출하지_않는다() {
        when(reviewWrite.addReview(addCommand)).thenThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));

        assertThatThrownBy(() -> service.addReview(addCommand)).isInstanceOf(CoreException.class);

        verifyNoInteractions(earnPoint, deductPoint);
    }

    @Test
    void 포인트_적립_실패는_작성_트랜잭션_완료_후_전파된다() {
        final var result = new ReviewProcessResult(801L, ReviewFormat.TEXT);
        when(reviewWrite.addReview(addCommand)).thenReturn(result);
        doThrow(new CoreException(ErrorCode.NOT_FOUND_DATA)).when(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> service.addReview(addCommand)).isInstanceOf(CoreException.class);

        final var order = inOrder(reviewWrite, earnPoint);
        order.verify(reviewWrite).addReview(addCommand);
        order.verify(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
    }
}
