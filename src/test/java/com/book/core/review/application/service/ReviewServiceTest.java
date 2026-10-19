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
import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.enums.ReviewEventType;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    private final GetReviewsUseCase getReviews = mock(GetReviewsUseCase.class);
    private final AddReviewUseCase addReview = mock(AddReviewUseCase.class);
    private final UpdateReviewUseCase updateReview = mock(UpdateReviewUseCase.class);
    private final DeleteReviewUseCase deleteReview = mock(DeleteReviewUseCase.class);
    private final ValidateReviewPolicyUseCase validateReviewPolicy = mock(ValidateReviewPolicyUseCase.class);
    private final ReviewHistoryRecorder historyRecorder = mock(ReviewHistoryRecorder.class);
    private final EarnPointUseCase earnPoint = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deductPoint = mock(DeductPointUseCase.class);
    private final ReviewService service =
        new ReviewService(getReviews, addReview, updateReview, deleteReview, validateReviewPolicy, historyRecorder, earnPoint, deductPoint);
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
        verifyNoInteractions(addReview, updateReview, deleteReview, validateReviewPolicy, historyRecorder, earnPoint, deductPoint);
    }

    @Test
    void 리뷰와_이력을_기록한_뒤_포인트를_적립하고_ID를_반환한다() {
        final ReviewKey reviewKey = new ReviewKey(42L, "ORDER_ITEM_42");
        final Review review = mock(Review.class);
        when(review.id()).thenReturn(801L);
        when(validateReviewPolicy.validateNew(42L, target)).thenReturn(reviewKey);
        when(addReview.execute(target, reviewKey, content)).thenReturn(review);

        assertThat(service.addReview(addCommand)).isEqualTo(801L);

        final var order = inOrder(validateReviewPolicy, addReview, historyRecorder, earnPoint);
        order.verify(validateReviewPolicy).validateNew(42L, target);
        order.verify(addReview).execute(target, reviewKey, content);
        order.verify(historyRecorder).record(review, ReviewEventType.CREATE);
        order.verify(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(deductPoint);
    }

    @Test
    void 리뷰_수정과_이력을_기록하고_ID를_반환한다() {
        final Review review = mock(Review.class);
        when(review.id()).thenReturn(801L);
        when(updateReview.execute(42L, 801L, content)).thenReturn(review);

        assertThat(service.updateReview(updateCommand)).isEqualTo(801L);

        final var order = inOrder(validateReviewPolicy, updateReview, historyRecorder);
        order.verify(validateReviewPolicy).validateUpdate(42L, 801L);
        order.verify(updateReview).execute(42L, 801L, content);
        order.verify(historyRecorder).record(review, ReviewEventType.UPDATE);
        verifyNoInteractions(earnPoint, deductPoint);
    }

    @Test
    void 삭제_이력을_기록한_뒤_포인트를_차감하고_ID를_반환한다() {
        final Review review = mock(Review.class);
        when(review.id()).thenReturn(801L);
        when(deleteReview.execute(42L, 801L)).thenReturn(review);

        assertThat(service.removeReview(removeCommand)).isEqualTo(801L);

        final var order = inOrder(deleteReview, historyRecorder, deductPoint);
        order.verify(deleteReview).execute(42L, 801L);
        order.verify(historyRecorder).record(review, ReviewEventType.DELETE);
        order.verify(deductPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(earnPoint);
    }

    @Test
    void 작성_정책이_실패하면_리뷰_이력과_포인트를_호출하지_않는다() {
        when(validateReviewPolicy.validateNew(42L, target)).thenThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));

        assertThatThrownBy(() -> service.addReview(addCommand)).isInstanceOf(CoreException.class);

        verifyNoInteractions(addReview, historyRecorder);
        verifyNoInteractions(earnPoint, deductPoint);
    }

    @Test
    void 포인트_적립_실패를_호출자에게_전파한다() {
        final ReviewKey reviewKey = new ReviewKey(42L, "ORDER_ITEM_42");
        final Review review = mock(Review.class);
        when(review.id()).thenReturn(801L);
        when(validateReviewPolicy.validateNew(42L, target)).thenReturn(reviewKey);
        when(addReview.execute(target, reviewKey, content)).thenReturn(review);
        doThrow(new CoreException(ErrorCode.NOT_FOUND_DATA)).when(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> service.addReview(addCommand)).isInstanceOf(CoreException.class);

        final var order = inOrder(addReview, historyRecorder, earnPoint);
        order.verify(addReview).execute(target, reviewKey, content);
        order.verify(historyRecorder).record(review, ReviewEventType.CREATE);
        order.verify(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
    }
}
