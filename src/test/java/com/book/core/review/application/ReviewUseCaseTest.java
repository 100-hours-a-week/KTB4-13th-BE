package com.book.core.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

class ReviewUseCaseTest {
    private final ReviewRepositoryPort repository = mock(ReviewRepositoryPort.class);
    private final AddReviewUseCase create = new AddReviewUseCase(repository);
    private final UpdateReviewUseCase update = new UpdateReviewUseCase(repository);
    private final DeleteReviewUseCase delete = new DeleteReviewUseCase(repository);
    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
    private final ReviewKey key = new ReviewKey(42L, "ORDER_ITEM_701");

    @BeforeEach
    void 저장소가_전달받은_리뷰를_반환하도록_설정한다() {
        when(repository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 회원은_선택한_구매_키와_대상으로_리뷰를_작성할_수_있다() {
        when(repository.save(any(Review.class))).thenReturn(review());

        final var result = create.execute(target, key, new ReviewContent(new BigDecimal("4.5"), "본문", false));

        assertThat(result).isEqualTo(801L);

        final var capture = ArgumentCaptor.forClass(Review.class);
        verify(repository).save(capture.capture());
        final var saved = capture.getValue();
        assertThat(saved.userId()).isEqualTo(42L);
        assertThat(saved.reviewKey()).isEqualTo("ORDER_ITEM_701");
        assertThat(saved.targetType()).isEqualTo(ReviewTargetType.PRODUCT);
        assertThat(saved.targetId()).isEqualTo(10L);
    }

    @Test
    void 회원은_이미_사용한_구매_키로_리뷰를_작성할_수_없다() {
        when(repository.existsByUserIdAndReviewKey(42L, "ORDER_ITEM_701")).thenReturn(true);

        assertThatThrownBy(() -> create.execute(target, key, new ReviewContent(new BigDecimal("4.5"), "본문", false))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_REVIEWED));
        verify(repository, never()).save(any(Review.class));
    }

    @Test
    void 회원은_리뷰의_별점과_본문을_함께_수정할_수_있다() {
        final var review = review();
        when(repository.findActiveByIdWithLock(801L)).thenReturn(Optional.of(review));

        assertThat(update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정", true))).isEqualTo(801L);

        assertThat(review.content()).isEqualTo("수정");
        assertThat(review.rate()).isEqualByComparingTo("4.5");
        assertThat(review.isSpoiler()).isTrue();
        verify(repository).save(review);
    }

    @Test
    void 리뷰_작성자가_아니면_리뷰를_수정하거나_삭제할_수_없다() {
        final var review = review();
        when(repository.findActiveByIdWithLock(801L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> update.execute(999L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정", true)))
            .isInstanceOfSatisfying(CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> delete.execute(999L, 801L)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(review.isDeleted()).isFalse();
        assertThat(review.content()).isEqualTo("본문");
        verify(repository, never()).save(review);
    }

    @Test
    void 회원은_자신의_리뷰를_삭제할_수_있다() {
        final var review = review();
        when(repository.findActiveByIdWithLock(801L)).thenReturn(Optional.of(review));

        final var result = delete.execute(42L, 801L);

        assertThat(result).isEqualTo(801L);
        assertThat(review.isDeleted()).isTrue();
        verify(repository).save(review);
    }

    @Test
    void 회원은_존재하지_않거나_삭제된_리뷰를_수정하거나_삭제할_수_없다() {
        when(repository.findActiveByIdWithLock(801L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정", null))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        assertThatThrownBy(() -> delete.execute(42L, 801L)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    void 회원은_스포일러_설정만_변경할_수_있다() {
        final var review = review();
        when(repository.findActiveByIdWithLock(801L)).thenReturn(Optional.of(review));

        update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "본문", true));

        assertThat(review.isSpoiler()).isTrue();
    }

    private Review review() {
        return new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "본문", false);
    }
}
