package com.book.core.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.ValidateReviewableOrderItemUseCase;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.usecase.CreateReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.domain.Review;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReviewUseCaseTest {
    private final ReviewRepositoryPort reviewRepository = mock(ReviewRepositoryPort.class);
    private final ValidateReviewableOrderItemUseCase validateReviewableOrderItem = mock(ValidateReviewableOrderItemUseCase.class);
    private final CreateReviewUseCase createReview = new CreateReviewUseCase(reviewRepository, validateReviewableOrderItem);
    private final UpdateReviewUseCase updateReview = new UpdateReviewUseCase(reviewRepository);
    private final DeleteReviewUseCase deleteReview = new DeleteReviewUseCase(reviewRepository);

    @Test
    void 결제_완료한_주문상품에_중복되지_않은_리뷰를_저장한다() {
        final CreateReviewCommand command = new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false);
        when(reviewRepository.existsActiveByUserIdAndOrderItemId(42L, 701L)).thenReturn(false);

        createReview.execute(command);

        verify(validateReviewableOrderItem).execute(42L, 701L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void 작성_자격을_통과하지_못하면_리뷰_저장을_시도하지_않는다() {
        final CreateReviewCommand command = new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false);
        doThrow(new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER)).when(validateReviewableOrderItem).execute(42L, 701L);

        assertThatThrownBy(() -> createReview.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_HAS_NOT_ORDER));

        verify(reviewRepository, never()).existsActiveByUserIdAndOrderItemId(42L, 701L);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void 활성_리뷰가_이미_있으면_E7001로_거부한다() {
        final CreateReviewCommand command = new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false);
        when(reviewRepository.existsActiveByUserIdAndOrderItemId(42L, 701L)).thenReturn(true);

        assertThatThrownBy(() -> createReview.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_REVIEWED));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void 리뷰_수정은_입력된_필드만_적용한다() {
        final Review review = Review.create(42L, 701L, new BigDecimal("4.5"), "기존", false);
        when(reviewRepository.findActiveById(801L)).thenReturn(Optional.of(review));

        updateReview.execute(new UpdateReviewCommand(801L, null, "수정", true));

        assertThat(review.content()).isEqualTo("수정");
        assertThat(review.rating()).isEqualByComparingTo("4.5");
        assertThat(review.isSpoiler()).isTrue();
        verify(reviewRepository).save(review);
    }

    @Test
    void 삭제는_활성_리뷰를_소프트_삭제한다() {
        final Review review = Review.create(42L, 701L, new BigDecimal("4.5"), "본문", false);
        when(reviewRepository.findActiveById(801L)).thenReturn(Optional.of(review));

        deleteReview.execute(new DeleteReviewCommand(801L));

        assertThat(review.isDeleted()).isTrue();
        verify(reviewRepository).save(review);
    }

    @Test
    void 존재하지_않거나_삭제된_리뷰를_수정하거나_삭제할_수_없다() {
        when(reviewRepository.findActiveById(801L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateReview.execute(new UpdateReviewCommand(801L, null, "수정", null))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        assertThatThrownBy(() -> deleteReview.execute(new DeleteReviewCommand(801L))).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }
}
