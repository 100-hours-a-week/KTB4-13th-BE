package com.book.core.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.review.domain.enums.ReviewEventType;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReviewHistoryTest {
    @Test
    void 원본을_수정하고_삭제해도_기록한_본문과_별점을_유지한다() {
        final var review = new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "최초 본문", false);
        final var history = new ReviewHistory(review, ReviewEventType.CREATE);

        review.updateContent(new ReviewContent(new BigDecimal("1.0"), "수정 본문", true));
        review.delete();

        assertThat(history.reviewId()).isEqualTo(801L);
        assertThat(history.userId()).isEqualTo(42L);
        assertThat(history.targetType()).isEqualTo(ReviewTargetType.PRODUCT);
        assertThat(history.targetId()).isEqualTo(10L);
        assertThat(history.eventType()).isEqualTo(ReviewEventType.CREATE);
        assertThat(history.content()).isEqualTo("최초 본문");
        assertThat(history.rate()).isEqualByComparingTo("4.5");
    }
}
