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
import com.book.core.review.domain.enums.ReviewOperation;
import static org.mockito.Mockito.verifyNoInteractions;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewFormat;
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
    private final com.book.core.review.application.ReviewHistoryRecorder recorder =
        mock(com.book.core.review.application.ReviewHistoryRecorder.class);
    private final ReviewRepositoryPort repository = mock(ReviewRepositoryPort.class);
    private final AddReviewUseCase create = new AddReviewUseCase(repository, recorder);
    private final UpdateReviewUseCase update = new UpdateReviewUseCase(repository, recorder);
    private final DeleteReviewUseCase delete = new DeleteReviewUseCase(repository, recorder);
    private final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
    private final ReviewKey key = new ReviewKey(42L, "ORDER_ITEM_701");

    @BeforeEach
    void returnSavedReview() {
        when(repository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 선택된_작성_키와_상품_대상으로_리뷰를_저장한다() {
        when(repository.save(any(Review.class))).thenReturn(review());

        final var result = create.execute(target, key, new ReviewContent(new BigDecimal("4.5"), "본문"), false, "127.0.0.1");

        assertThat(result.id()).isEqualTo(801L);
        assertThat(result.format()).isEqualTo(ReviewFormat.TEXT);
        assertThat(result.pointAmount()).isEqualByComparingTo("1000");

        final var capture = ArgumentCaptor.forClass(Review.class);
        verify(repository).save(capture.capture());
        final var saved = capture.getValue();
        assertThat(saved.userId()).isEqualTo(42L);
        assertThat(saved.reviewKey()).isEqualTo("ORDER_ITEM_701");
        assertThat(saved.targetType()).isEqualTo(ReviewTargetType.PRODUCT);
        assertThat(saved.targetId()).isEqualTo(10L);
    }

    @Test
    void 구매_키가_이미_사용됐으면_저장하지_않는다() {
        when(repository.existsByUserIdAndReviewKey(42L, "ORDER_ITEM_701")).thenReturn(true);

        assertThatThrownBy(() -> create.execute(target, key, new ReviewContent(new BigDecimal("4.5"), "본문"), false, "127.0.0.1"))
            .isInstanceOfSatisfying(CoreException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_REVIEWED));
        verify(repository, never()).save(any(Review.class));
    }

    @Test
    void 리뷰_수정은_별점과_본문_VO를_적용한다() {
        final var review = review();
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.of(review));

        assertThat(update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정"), true, "127.0.0.1")).isEqualTo(801L);

        assertThat(review.content()).isEqualTo("수정");
        assertThat(review.rate()).isEqualByComparingTo("4.5");
        assertThat(review.isSpoiler()).isTrue();
        verify(repository).save(review);
    }

    @Test
    void 소유자가_아니면_수정과_삭제를_거부한다() {
        final var review = review();
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> update.execute(999L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정"), true, "127.0.0.1"))
            .isInstanceOfSatisfying(CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> delete.execute(999L, 801L, "127.0.0.1")).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(review.isDeleted()).isFalse();
        assertThat(review.content()).isEqualTo("본문");
        verify(repository, never()).save(review);
    }

    @Test
    void 삭제는_활성_리뷰를_소프트_삭제한다() {
        final var review = review();
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.of(review));

        final var result = delete.execute(42L, 801L, "127.0.0.1");

        assertThat(result.id()).isEqualTo(801L);
        assertThat(result.format()).isEqualTo(ReviewFormat.TEXT);
        assertThat(result.pointAmount()).isEqualByComparingTo("1000");
        assertThat(review.isDeleted()).isTrue();
        verify(repository).save(review);
    }

    @Test
    void 존재하지_않거나_삭제된_리뷰를_수정하거나_삭제할_수_없다() {
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정"), null, "127.0.0.1"))
            .isInstanceOfSatisfying(CoreException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        assertThatThrownBy(() -> delete.execute(42L, 801L, "127.0.0.1")).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    void 같은_본문과_수치상_같은_별점은_이력을_추가하지_않는다() {
        final var review = review();
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.of(review));

        assertThat(update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.50"), "본문"), null, "127.0.0.1")).isEqualTo(801L);

        verify(repository, never()).save(any(Review.class));
        verifyNoInteractions(recorder);
    }

    @Test
    void 스포일러만_변경되어도_수정_이력을_남긴다() {
        final var review = review();
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.of(review));

        update.execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "본문"), true, "192.0.2.1");

        verify(recorder).record(review, ReviewOperation.UPDATE, "192.0.2.1");
        assertThat(review.isSpoiler()).isTrue();
    }

    @Test
    void 생성과_삭제_직후의_리뷰와_IP를_이력에_전달한다() {
        final var review = review();
        when(repository.save(any(Review.class))).thenReturn(review);
        when(repository.findActiveByIdForUpdate(801L)).thenReturn(Optional.of(review));

        create.execute(target, key, new ReviewContent(new BigDecimal("4.5"), "본문"), false, "192.0.2.1");
        verify(recorder).record(review, ReviewOperation.CREATE, "192.0.2.1");
        delete.execute(42L, 801L, "192.0.2.2");
        verify(recorder).record(review, ReviewOperation.DELETE, "192.0.2.2");
        assertThat(review.isDeleted()).isTrue();
        assertThat(review.content()).isEqualTo("본문");
    }

    private Review review() {
        return new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "본문", false);
    }
}
