package com.book.core.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.review.domain.enums.ReviewTargetType;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReviewTest {
    @Test
    void creates_review_with_rating_normalized_to_one_decimal() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.00"), "재미있게 읽었습니다."), false);

        assertThat(review.userId()).isEqualTo(42L);
        assertThat(review.targetId()).isEqualTo(701L);
        assertThat(review.rate()).isEqualByComparingTo("4.0");
        assertThat(review.isSpoiler()).isFalse();
    }

    @Test
    void accepts_rating_bounds_in_tenth_steps() {
        assertThat(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("0.0"), "평점 0"), false).rate()).isEqualByComparingTo("0.0");
        assertThat(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 702L), new ReviewTarget(ReviewTargetType.PRODUCT, 702L),
            new ReviewContent(new BigDecimal("10.0"), "평점 10"), false).rate()).isEqualByComparingTo("10.0");
    }

    @Test
    void rejects_rating_outside_bounds_or_between_tenth_steps() {
        assertInvalidReview(new BigDecimal("-0.1"), "내용");
        assertInvalidReview(new BigDecimal("10.1"), "내용");
        assertInvalidReview(new BigDecimal("4.05"), "내용");
    }

    @Test
    void rejects_content_over_255_characters() {

        assertThatThrownBy(() -> Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(BigDecimal.ONE, "x".repeat(256)), false)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void updates_only_fields_that_were_provided() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.0"), "기존 내용"), false);

        review.update(new ReviewContent(new BigDecimal("8.7"), review.content()), true);

        assertThat(review.rate()).isEqualByComparingTo("8.7");
        assertThat(review.content()).isEqualTo("기존 내용");
        assertThat(review.isSpoiler()).isTrue();
    }

    @Test
    void rejects_invalid_partial_update_without_mutating_other_fields() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.0"), "기존 내용"), false);

        assertThatThrownBy(() -> review.update(new ReviewContent(new BigDecimal("4.5"), "x".repeat(256)), true))
            .isInstanceOf(CoreException.class);

        assertThat(review.rate()).isEqualByComparingTo("4.0");
        assertThat(review.content()).isEqualTo("기존 내용");
        assertThat(review.isSpoiler()).isFalse();
    }

    private static void assertInvalidReview(final BigDecimal rating, final String content) {
        assertThatThrownBy(() -> Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(rating, content), false)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
