package com.book.core.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ReviewServiceTest {
    private final AddReviewUseCase create = mock(AddReviewUseCase.class);
    private final UpdateReviewUseCase update = mock(UpdateReviewUseCase.class);
    private final DeleteReviewUseCase delete = mock(DeleteReviewUseCase.class);
    private final GetReviewsUseCase get = mock(GetReviewsUseCase.class);
    private final ValidateReviewPolicyUseCase policy = mock(ValidateReviewPolicyUseCase.class);
    private final EarnPointUseCase earn = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
    private final ReviewHistoryRecorder history = mock(ReviewHistoryRecorder.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
    private final ReviewService service =
        new ReviewService(create, update, delete, get, policy, earn, deduct, history, new TransactionTemplate(transactionManager));
    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
    private final ReviewKey key = new ReviewKey(42L, "ORDER_ITEM_702");
    private final ReviewContent content = new ReviewContent(new BigDecimal("4.5"), "본문", false);
    private final Review review =
        new Review(801L, 42L, "ORDER_ITEM_702", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "본문", false);

    @BeforeEach
    void prepareTransaction() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(policy.validateNew(42L, target)).thenReturn(key);
    }

    @Test
    void 조회_Command의_VO를_조회_UseCase에_전달한다() {
        final var paging = new OffsetLimit(0, 20);
        final Page<ReviewItemResult> result = new Page<>(List.of(), false);
        when(get.execute(target, paging)).thenReturn(result);
        assertThat(service.findReviews(new GetReviewsCommand(target, paging))).isSameAs(result);
        verifyNoInteractions(history, transactionManager);
    }

    @Test
    void 작성과_이력을_커밋한_뒤_포인트를_적립한다() {
        when(create.execute(target, key, content)).thenReturn(review);
        assertThat(service.addReview(new AddReviewCommand(42L, target, content))).isEqualTo(801L);
        final var order = inOrder(policy, create, history, transactionManager, earn);
        order.verify(policy).validateNew(42L, target);
        order.verify(create).execute(target, key, content);
        order.verify(history).record(review, ReviewEventType.CREATE);
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(earn).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(deduct);
    }

    @Test
    void 실제_수정_결과에만_수정_이력을_기록한다() {
        when(update.execute(42L, 801L, content)).thenReturn(Optional.of(review));
        assertThat(service.updateReview(new UpdateReviewCommand(42L, 801L, content))).isEqualTo(801L);
        final var order = inOrder(policy, update, history, transactionManager);
        order.verify(policy).validateUpdate(42L, 801L);
        order.verify(update).execute(42L, 801L, content);
        order.verify(history).record(review, ReviewEventType.UPDATE);
        order.verify(transactionManager).commit(transactionStatus);
        verifyNoInteractions(earn, deduct);
    }

    @Test
    void 변경_없는_수정은_이력을_기록하지_않고_리뷰_ID를_반환한다() {
        when(update.execute(42L, 801L, content)).thenReturn(Optional.empty());
        assertThat(service.updateReview(new UpdateReviewCommand(42L, 801L, content))).isEqualTo(801L);
        verifyNoInteractions(history, earn, deduct);
    }

    @Test
    void 삭제와_이력을_커밋한_뒤_포인트를_차감한다() {
        when(delete.execute(42L, 801L)).thenReturn(review);
        assertThat(service.removeReview(new RemoveReviewCommand(42L, 801L))).isEqualTo(801L);
        final var order = inOrder(delete, history, transactionManager, deduct);
        order.verify(delete).execute(42L, 801L);
        order.verify(history).record(review, ReviewEventType.DELETE);
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(deduct).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
    }

    @Test
    void 작성_정책_검증이_실패하면_트랜잭션과_후속_작업을_시작하지_않는다() {
        when(policy.validateNew(42L, target)).thenThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));
        assertThatThrownBy(() -> service.addReview(new AddReviewCommand(42L, target, content))).isInstanceOf(CoreException.class);
        verifyNoInteractions(create, history, transactionManager, earn, deduct);
    }

    @Test
    void 수정_기간_검증이_실패하면_후속_작업을_시작하지_않는다() {
        doThrow(new CoreException(ErrorCode.REVIEW_UPDATE_EXPIRED)).when(policy).validateUpdate(42L, 801L);
        assertThatThrownBy(() -> service.updateReview(new UpdateReviewCommand(42L, 801L, content))).isInstanceOf(CoreException.class);
        verifyNoInteractions(update, history, transactionManager);
    }

    @Test
    void 이력_기록이_실패하면_롤백하고_포인트를_적립하지_않는다() {
        when(create.execute(target, key, content)).thenReturn(review);
        final var failure = new IllegalStateException("history failure");
        doThrow(failure).when(history).record(review, ReviewEventType.CREATE);
        assertThatThrownBy(() -> service.addReview(new AddReviewCommand(42L, target, content))).isSameAs(failure);
        verify(transactionManager).rollback(transactionStatus);
        verifyNoInteractions(earn, deduct);
    }

    @Test
    void 포인트_적립_실패는_리뷰와_이력_커밋_후_전파된다() {
        when(create.execute(target, key, content)).thenReturn(review);
        final var failure = new CoreException(ErrorCode.NOT_FOUND_DATA);
        doThrow(failure).when(earn).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        assertThatThrownBy(() -> service.addReview(new AddReviewCommand(42L, target, content))).isSameAs(failure);
        verify(transactionManager).commit(transactionStatus);
    }
}
