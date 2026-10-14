package com.book.core.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.domain.ReviewFormat;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    private final AddReviewUseCase create = mock(AddReviewUseCase.class);
    private final UpdateReviewUseCase update = mock(UpdateReviewUseCase.class);
    private final DeleteReviewUseCase delete = mock(DeleteReviewUseCase.class);
    private final GetReviewsUseCase get = mock(GetReviewsUseCase.class);
    private final ValidateReviewPolicyUseCase policy = mock(ValidateReviewPolicyUseCase.class);
    private final EarnPointUseCase earn = mock(EarnPointUseCase.class);
    private final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
    private final ReviewService service = new ReviewService(create, update, delete, get, policy, earn, deduct);
    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);

    @Test
    void 조회_Command에서_VO를_꺼내_UseCase에_전달한다() {
        final var paging = new OffsetLimit(0, 20);
        final Page<ReviewItemResult> result = new Page<>(List.of(), false);
        when(get.execute(target, paging)).thenReturn(result);

        assertThat(service.findReviews(new GetReviewsCommand(target, paging))).isSameAs(result);
        verify(get).execute(target, paging);
    }

    @Test
    void 작성_자격을_검증한_뒤_선택된_키와_대상을_저장_UseCase에_전달한다() {
        final var key = new ReviewKey(42L, "ORDER_ITEM_702");
        final var rate = new BigDecimal("4.5");
        when(policy.validateNew(42L, target)).thenReturn(key);
        when(create.execute(target, key, new ReviewContent(rate, "본문"), false, "127.0.0.1"))
            .thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));

        assertThat(service.addReview(new AddReviewCommand(42L, target, new ReviewContent(rate, "본문"), false, "127.0.0.1"))).isEqualTo(801L);

        final var order = inOrder(policy, create, earn);
        order.verify(policy).validateNew(42L, target);
        order.verify(create).execute(target, key, new ReviewContent(rate, "본문"), false, "127.0.0.1");
        order.verify(earn).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(deduct);
    }

    @Test
    void 작성_자격_검증이_실패하면_저장하지_않는다() {
        when(policy.validateNew(42L, target)).thenThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));

        assertThatThrownBy(
            () -> service.addReview(new AddReviewCommand(42L, target, new ReviewContent(new BigDecimal("4.5"), "본문"), false, "127.0.0.1")))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(create, earn, deduct);
    }

    @Test
    void 수정_Command를_필요한_인자로_풀어_UseCase에_전달한다() {
        final var rate = new BigDecimal("4.5");

        when(update.execute(42L, 801L, new ReviewContent(rate, "수정 본문"), true, "127.0.0.1")).thenReturn(801L);

        assertThat(service.updateReview(new UpdateReviewCommand(42L, 801L, new ReviewContent(rate, "수정 본문"), true, "127.0.0.1")))
            .isEqualTo(801L);

        final var order = inOrder(policy, update);
        order.verify(policy).validateUpdate(42L, 801L);
        order.verify(update).execute(42L, 801L, new ReviewContent(rate, "수정 본문"), true, "127.0.0.1");
        verifyNoInteractions(create, delete, get, earn, deduct);
    }

    @Test
    void 삭제_Command를_사용자와_리뷰_ID로_풀어_UseCase에_전달한다() {
        when(delete.execute(42L, 801L, "127.0.0.1")).thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));

        assertThat(service.removeReview(new RemoveReviewCommand(42L, 801L, "127.0.0.1"))).isEqualTo(801L);

        final var order = inOrder(delete, deduct);
        order.verify(delete).execute(42L, 801L, "127.0.0.1");
        order.verify(deduct).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(earn);
        verifyNoInteractions(create, update, get, policy);
    }


    @Test
    void 리뷰_저장이_실패하면_적립하지_않는다() {
        final var key = new ReviewKey(42L, "ORDER_ITEM_702");
        final var content = new ReviewContent(new BigDecimal("4.5"), "본문");
        when(policy.validateNew(42L, target)).thenReturn(key);
        when(create.execute(target, key, content, false, "127.0.0.1")).thenThrow(new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED));

        assertThatThrownBy(() -> service.addReview(new AddReviewCommand(42L, target, content, false, "127.0.0.1")))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(earn, deduct);
    }

    @Test
    void 리뷰_삭제가_실패하면_차감하지_않는다() {
        when(delete.execute(42L, 801L, "127.0.0.1")).thenThrow(new CoreException(ErrorCode.REVIEW_NOT_FOUND));

        assertThatThrownBy(() -> service.removeReview(new RemoveReviewCommand(42L, 801L, "127.0.0.1"))).isInstanceOf(CoreException.class);
        verifyNoInteractions(earn, deduct);
    }

    @Test
    void 리뷰_저장_후_적립_실패는_호출자에게_전파한다() {
        final var key = new ReviewKey(42L, "ORDER_ITEM_702");
        final var content = new ReviewContent(new BigDecimal("4.5"), "본문");
        when(policy.validateNew(42L, target)).thenReturn(key);
        when(create.execute(target, key, content, false, "127.0.0.1")).thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));
        final var failure = new CoreException(ErrorCode.NOT_FOUND_DATA);
        doThrow(failure).when(earn).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> service.addReview(new AddReviewCommand(42L, target, content, false, "127.0.0.1"))).isSameAs(failure);
        final var order = inOrder(create, earn);
        order.verify(create).execute(target, key, content, false, "127.0.0.1");
        order.verify(earn).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(delete, deduct);
    }

    @Test
    void 리뷰_삭제_후_차감_실패는_호출자에게_전파한다() {
        when(delete.execute(42L, 801L, "127.0.0.1")).thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));
        final var failure = new CoreException(ErrorCode.NOT_FOUND_DATA);
        doThrow(failure).when(deduct).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> service.removeReview(new RemoveReviewCommand(42L, 801L, "127.0.0.1"))).isSameAs(failure);
        final var order = inOrder(delete, deduct);
        order.verify(delete).execute(42L, 801L, "127.0.0.1");
        order.verify(deduct).execute(42L, PointType.REVIEW, 801L, BigDecimal.valueOf(1000));
        verifyNoInteractions(create, earn);
    }

    @Test
    void 수정_기간_검증이_실패하면_리뷰를_수정하지_않는다() {
        doThrow(new CoreException(ErrorCode.REVIEW_UPDATE_EXPIRED)).when(policy).validateUpdate(42L, 801L);

        assertThatThrownBy(() -> service
            .updateReview(new UpdateReviewCommand(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정 본문"), true, "127.0.0.1")))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(update, earn, deduct);
    }
}
