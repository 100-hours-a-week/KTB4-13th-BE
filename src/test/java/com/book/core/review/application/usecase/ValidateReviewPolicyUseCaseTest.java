package com.book.core.review.application.usecase;

import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewOrderItemRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValidateReviewPolicyUseCaseTest {
    private final ReviewOrderItemRepositoryPort orders = mock(ReviewOrderItemRepositoryPort.class);
    private final ReviewRepositoryPort reviews = mock(ReviewRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ValidateReviewPolicyUseCase useCase = new ValidateReviewPolicyUseCase(orders, reviews, clock);
    private final LocalDateTime now = LocalDateTime.now(clock);

    @Test
    void 결제_후_7일_이내_구매에서_사용하지_않은_첫_키를_선택한다() {
        when(orders.findRecentOrderItemsForProduct(42L, 10L, OrderStatus.PAID, now.minusDays(7))).thenReturn(List.of(701L, 702L));
        when(reviews.findByUserIdAndReviewKeyIn(42L, List.of("ORDER_ITEM_701", "ORDER_ITEM_702")))
            .thenReturn(List.of(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 10L),
                new ReviewContent(new BigDecimal("4.5"), "기존", false))));

        final var result = useCase.validateNew(42L, new ReviewTarget(ReviewTargetType.PRODUCT, 10L));

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.key()).isEqualTo("ORDER_ITEM_702");
        verify(orders).findRecentOrderItemsForProduct(42L, 10L, OrderStatus.PAID, now.minusDays(7));
    }

    @Test
    void 구매가_없으면_리뷰_키를_조회하지_않고_거부한다() {
        when(orders.findRecentOrderItemsForProduct(42L, 10L, OrderStatus.PAID, now.minusDays(7))).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.validateNew(42L, new ReviewTarget(ReviewTargetType.PRODUCT, 10L))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_HAS_NOT_ORDER));
        verifyNoInteractions(reviews);
    }

    @Test
    void 구매_키가_모두_사용되었으면_거부한다() {
        when(orders.findRecentOrderItemsForProduct(42L, 10L, OrderStatus.PAID, now.minusDays(7))).thenReturn(List.of(701L));
        when(reviews.findByUserIdAndReviewKeyIn(42L, List.of("ORDER_ITEM_701")))
            .thenReturn(List.of(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 10L),
                new ReviewContent(new BigDecimal("4.5"), "기존", false))));

        assertThatThrownBy(() -> useCase.validateNew(42L, new ReviewTarget(ReviewTargetType.PRODUCT, 10L))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_HAS_NOT_ORDER));
    }

    @Test
    void 작성_후_정확히_7일이면_수정할_수_있다() {
        stubReview(42L, now.minusDays(7));

        assertThatCode(() -> useCase.validateUpdate(42L, 801L)).doesNotThrowAnyException();
    }

    @Test
    void 작성_후_7일을_초과하면_수정을_거부한다() {
        stubReview(42L, now.minusDays(7).minusNanos(1));

        assertThatThrownBy(() -> useCase.validateUpdate(42L, 801L)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_UPDATE_EXPIRED));
    }

    @Test
    void 다른_사용자나_존재하지_않는_리뷰는_찾을_수_없다() {
        stubReview(99L, now);
        assertNotFound();
        when(reviews.findByIdAndUserId(801L, 42L)).thenReturn(Optional.empty());
        assertNotFound();
    }

    private void stubReview(final Long userId, final LocalDateTime createdAt) {
        final var review = mock(Review.class);
        when(review.userId()).thenReturn(userId);
        when(review.createdAt()).thenReturn(createdAt);
        when(reviews.findByIdAndUserId(801L, 42L)).thenReturn(Optional.of(review));
    }

    private void assertNotFound() {
        assertThatThrownBy(() -> useCase.validateUpdate(42L, 801L)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }
}
