package com.book.core.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
import com.book.core.review.application.command.GetReviewCountCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.GetReviewCountUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
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
    private final GetReviewCountUseCase getReviewCount = mock(GetReviewCountUseCase.class);
    private final AddReviewUseCase addReview = mock(AddReviewUseCase.class);
    private final UpdateReviewUseCase updateReview = mock(UpdateReviewUseCase.class);
    private final DeleteReviewUseCase deleteReview = mock(DeleteReviewUseCase.class);
    private final ValidateReviewPolicyUseCase validateReviewPolicy = mock(ValidateReviewPolicyUseCase.class);
    private final ReviewHistoryRecorder historyRecorder = mock(ReviewHistoryRecorder.class);
    private final EarnPointUseCase earnPoint = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deductPoint = mock(DeductPointUseCase.class);
    private final ReviewService service = new ReviewService(getReviews, getReviewCount, addReview, updateReview, deleteReview,
        validateReviewPolicy, historyRecorder, earnPoint, deductPoint);
    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
    private final ReviewContent content = new ReviewContent(new BigDecimal("4.5"), "본문", false);
    private final AddReviewCommand addCommand = new AddReviewCommand(42L, target, content);
    private final UpdateReviewCommand updateCommand = new UpdateReviewCommand(42L, 801L, content);
    private final RemoveReviewCommand removeCommand = new RemoveReviewCommand(42L, 801L);

    @Test
    void 리뷰_조회_요청의_대상과_페이지_조건을_조회_UseCase에_전달한다() {
        final var paging = new OffsetLimit(0, 20);
        final Page<ReviewItemResult> result = new Page<>(List.of(), false);
        when(getReviews.execute(target, paging)).thenReturn(result);
        assertThat(service.findReviews(new GetReviewsCommand(target, paging))).isSameAs(result);
        verifyNoInteractions(getReviewCount, addReview, updateReview, deleteReview, validateReviewPolicy, historyRecorder, earnPoint,
            deductPoint);
    }

    @Test
    void 리뷰_수_조회_Command에서_대상을_꺼내_UseCase에_전달한다() {
        when(getReviewCount.execute(target)).thenReturn(2L);

        assertThat(service.countReviews(new GetReviewCountCommand(target))).isEqualTo(2L);

        verify(getReviewCount).execute(target);
    }

    @Test
    void 회원이_리뷰를_작성하면_이력을_기록하고_포인트를_적립한_뒤_ID를_반환한다() {
        final ReviewKey reviewKey = new ReviewKey(42L, "ORDER_ITEM_42");
        when(validateReviewPolicy.validateNew(42L, target)).thenReturn(reviewKey);
        when(addReview.execute(target, reviewKey, content)).thenReturn(801L);

        assertThat(service.addReview(addCommand)).isEqualTo(801L);

        final var order = inOrder(validateReviewPolicy, addReview, historyRecorder, earnPoint);
        order.verify(validateReviewPolicy).validateNew(42L, target);
        order.verify(addReview).execute(target, reviewKey, content);
        order.verify(historyRecorder).record(801L, ReviewEventType.CREATE);
        order.verify(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(deductPoint);
    }

    @Test
    void 회원이_리뷰를_수정하면_이력을_기록하고_리뷰_ID를_반환한다() {
        when(updateReview.execute(42L, 801L, content)).thenReturn(801L);

        assertThat(service.updateReview(updateCommand)).isEqualTo(801L);

        final var order = inOrder(validateReviewPolicy, updateReview, historyRecorder);
        order.verify(validateReviewPolicy).validateUpdate(42L, 801L);
        order.verify(updateReview).execute(42L, 801L, content);
        order.verify(historyRecorder).record(801L, ReviewEventType.UPDATE);
        verifyNoInteractions(earnPoint, deductPoint);
    }

    @Test
    void 회원이_리뷰를_삭제하면_이력을_기록하고_포인트를_차감한_뒤_ID를_반환한다() {
        when(deleteReview.execute(42L, 801L)).thenReturn(801L);

        assertThat(service.removeReview(removeCommand)).isEqualTo(801L);

        final var order = inOrder(deleteReview, historyRecorder, deductPoint);
        order.verify(deleteReview).execute(42L, 801L);
        order.verify(historyRecorder).record(801L, ReviewEventType.DELETE);
        order.verify(deductPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(earnPoint);
    }

    @Test
    void 리뷰_작성_자격이_없으면_리뷰를_저장하거나_포인트를_적립하지_않는다() {
        when(validateReviewPolicy.validateNew(42L, target)).thenThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));

        assertThatThrownBy(() -> service.addReview(addCommand)).isInstanceOf(CoreException.class);

        verifyNoInteractions(addReview, historyRecorder);
        verifyNoInteractions(earnPoint, deductPoint);
    }

    @Test
    void 포인트_적립에_실패하면_오류를_호출자에게_전달한다() {
        final ReviewKey reviewKey = new ReviewKey(42L, "ORDER_ITEM_42");
        when(validateReviewPolicy.validateNew(42L, target)).thenReturn(reviewKey);
        when(addReview.execute(target, reviewKey, content)).thenReturn(801L);
        doThrow(new CoreException(ErrorCode.NOT_FOUND_DATA)).when(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> service.addReview(addCommand)).isInstanceOf(CoreException.class);

        final var order = inOrder(addReview, historyRecorder, earnPoint);
        order.verify(addReview).execute(target, reviewKey, content);
        order.verify(historyRecorder).record(801L, ReviewEventType.CREATE);
        order.verify(earnPoint).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
    }
}
