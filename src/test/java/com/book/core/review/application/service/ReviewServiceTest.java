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
    private final ReviewService service = new ReviewService(create, update, delete, get, policy);
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
        when(create.execute(target, key, new ReviewContent(rate, "본문"), false))
            .thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));

        assertThat(service.addReview(new AddReviewCommand(42L, target, new ReviewContent(rate, "본문"), false))).isEqualTo(801L);

        final var order = inOrder(policy, create);
        order.verify(policy).validateNew(42L, target);
        order.verify(create).execute(target, key, new ReviewContent(rate, "본문"), false);

    }

    @Test
    void 작성_자격_검증이_실패하면_저장하지_않는다() {
        when(policy.validateNew(42L, target)).thenThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));

        assertThatThrownBy(
            () -> service.addReview(new AddReviewCommand(42L, target, new ReviewContent(new BigDecimal("4.5"), "본문"), false)))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(create);
    }

    @Test
    void 수정_Command를_필요한_인자로_풀어_UseCase에_전달한다() {
        final var rate = new BigDecimal("4.5");

        when(update.execute(42L, 801L, new ReviewContent(rate, "수정 본문"), true)).thenReturn(801L);

        assertThat(service.updateReview(new UpdateReviewCommand(42L, 801L, new ReviewContent(rate, "수정 본문"), true))).isEqualTo(801L);

        final var order = inOrder(policy, update);
        order.verify(policy).validateUpdate(42L, 801L);
        order.verify(update).execute(42L, 801L, new ReviewContent(rate, "수정 본문"), true);
        verifyNoInteractions(create, delete, get);
    }

    @Test
    void 삭제_Command를_사용자와_리뷰_ID로_풀어_UseCase에_전달한다() {
        when(delete.execute(42L, 801L)).thenReturn(new ReviewProcessResult(801L, ReviewFormat.TEXT));

        assertThat(service.removeReview(new RemoveReviewCommand(42L, 801L))).isEqualTo(801L);

        final var order = inOrder(delete);
        order.verify(delete).execute(42L, 801L);

        verifyNoInteractions(create, update, get, policy);
    }



    @Test
    void 수정_기간_검증이_실패하면_리뷰를_수정하지_않는다() {
        doThrow(new CoreException(ErrorCode.REVIEW_UPDATE_EXPIRED)).when(policy).validateUpdate(42L, 801L);

        assertThatThrownBy(
            () -> service.updateReview(new UpdateReviewCommand(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정 본문"), true)))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(update);
    }
}
