package com.book.core.review.domain;

import com.book.core.review.domain.enums.ReviewTargetType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReviewTest {
    @Test
    void 리뷰를_생성하면_별점을_소수_첫째_자리로_정규화한다() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.00"), "재미있게 읽었습니다.", false));

        assertThat(review.userId()).isEqualTo(42L);
        assertThat(review.targetId()).isEqualTo(701L);
        assertThat(review.rate()).isEqualByComparingTo("4.0");
        assertThat(review.isSpoiler()).isFalse();
    }

    @Test
    void 리뷰_별점은_0_5점부터_5점까지_0_5점_간격으로_허용한다() {
        assertThat(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("0.5"), "평점 0.5", false)).rate()).isEqualByComparingTo("0.5");
        assertThat(Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 702L), new ReviewTarget(ReviewTargetType.PRODUCT, 702L),
            new ReviewContent(new BigDecimal("5.0"), "평점 5", false)).rate()).isEqualByComparingTo("5.0");
    }

    @Test
    void 허용_범위를_벗어나거나_0_5점_단위가_아닌_별점을_거부한다() {
        assertInvalidReview(new BigDecimal("-0.1"), "내용");
        assertInvalidReview(new BigDecimal("10.1"), "내용");
        assertInvalidReview(new BigDecimal("4.05"), "내용");
    }

    @Test
    void 리뷰_본문이_255자를_초과하면_작성을_거부한다() {
        assertThatThrownBy(() -> Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(BigDecimal.ONE, "x".repeat(256), false))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 리뷰를_수정하면_별점과_본문을_함께_변경한다() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.0"), "기존 내용", false));

        review.updateContent(new ReviewContent(new BigDecimal("4.5"), "수정 내용", true));

        assertThat(review.rate()).isEqualByComparingTo("4.5");
        assertThat(review.content()).isEqualTo("수정 내용");
        assertThat(review.isSpoiler()).isTrue();
    }

    @Test
    void 유효하지_않은_본문으로_수정하면_별점과_스포일러_설정을_변경하지_않는다() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.0"), "기존 내용", false));

        assertThatThrownBy(() -> review.updateContent(new ReviewContent(new BigDecimal("4.5"), "x".repeat(256), true)))
            .isInstanceOf(CoreException.class);

        assertThat(review.rate()).isEqualByComparingTo("4.0");
        assertThat(review.content()).isEqualTo("기존 내용");
        assertThat(review.isSpoiler()).isFalse();
    }

    @Test
    void 구매_키를_주문_상품_ID로_해석하지_않고_그대로_저장한다() {
        final Review review = Review.create(new ReviewKey(42L, "PURCHASE_REFERENCE_ABC"), new ReviewTarget(ReviewTargetType.PRODUCT, 10L),
            new ReviewContent(new BigDecimal("4.5"), "본문", false));

        assertThat(review.userId()).isEqualTo(42L);
        assertThat(review.reviewKey()).isEqualTo("PURCHASE_REFERENCE_ABC");
        assertThat(review.targetId()).isEqualTo(10L);
    }

    private static void assertInvalidReview(final BigDecimal rating, final String content) {
        assertThatThrownBy(() -> Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(rating, content, false))).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 회원이_수정_요청에서_스포일러_설정을_생략하면_기존_설정을_유지한다() {
        final Review review = Review.create(new ReviewKey(42L, "ORDER_ITEM_703"), new ReviewTarget(ReviewTargetType.PRODUCT, 10L),
            new ReviewContent(new BigDecimal("4.5"), "기존 본문", true));
        review.updateContent(new ReviewContent(new BigDecimal("3.5"), "새 본문", null));
        assertThat(review.isSpoiler()).isTrue();
    }


}
