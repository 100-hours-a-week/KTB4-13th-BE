package com.book.core.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.review.domain.enums.ReviewOperation;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReviewHistoryTest {
    @Test
    void 원본을_수정하고_삭제해도_기록한_본문과_별점을_유지한다() {
        final var review = new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "최초 본문", false);
        final var changedAt = LocalDateTime.of(2026, 10, 14, 12, 0, 0, 123456000);
        final var history = new ReviewHistory(review, ReviewOperation.CREATE, changedAt, "192.0.2.1");

        review.updateContent(new ReviewContent(new BigDecimal("1.0"), "수정 본문"), true);
        review.delete();

        assertThat(history.reviewId()).isEqualTo(801L);
        assertThat(history.userId()).isEqualTo(42L);
        assertThat(history.targetType()).isEqualTo(ReviewTargetType.PRODUCT);
        assertThat(history.targetId()).isEqualTo(10L);
        assertThat(history.operation()).isEqualTo(ReviewOperation.CREATE);
        assertThat(history.content()).isEqualTo("최초 본문");
        assertThat(history.rate()).isEqualByComparingTo("4.5");
        assertThat(history.changedAt()).isEqualTo(changedAt);
        assertThat(history.requestIp()).isEqualTo("192.0.2.1");
    }
}
