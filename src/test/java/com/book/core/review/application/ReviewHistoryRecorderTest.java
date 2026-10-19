package com.book.core.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.enums.ReviewEventType;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

class ReviewHistoryRecorderTest {
    private final ReviewRepositoryPort reviews = mock(ReviewRepositoryPort.class);
    private final ReviewHistoryRepositoryPort histories = mock(ReviewHistoryRepositoryPort.class);
    private final ReviewHistoryRecorder recorder = new ReviewHistoryRecorder(reviews, histories);
    private final Review review =
        new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "보존할 본문", false);

    @BeforeEach
    void findReviewIncludingDeleted() {
        when(reviews.findByIdIncludingDeleted(801L)).thenReturn(Optional.of(review));
    }

    @Test
    void 리뷰_ID로_조회한_당시_상태를_이력으로_기록한다() {
        recorder.record(801L, ReviewEventType.UPDATE);

        final var capture = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(histories).append(capture.capture());
        verify(reviews).findByIdIncludingDeleted(801L);
        assertThat(capture.getValue().reviewId()).isEqualTo(801L);
        assertThat(capture.getValue().userId()).isEqualTo(42L);
        assertThat(capture.getValue().content()).isEqualTo("보존할 본문");
        assertThat(capture.getValue().eventType()).isEqualTo(ReviewEventType.UPDATE);
    }

    @Test
    void 기술_저장_오류는_삼키거나_업무_오류로_변환하지_않는다() {
        final var failure = new DataAccessResourceFailureException("synthetic history failure");
        doThrow(failure).when(histories).append(any(ReviewHistory.class));

        assertThatThrownBy(() -> recorder.record(801L, ReviewEventType.CREATE)).isSameAs(failure);
    }
}
