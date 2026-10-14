package com.book.core.review.application;

import static java.time.temporal.ChronoUnit.MICROS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.enums.ReviewEventType;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

class ReviewHistoryRecorderTest {
    private final ReviewHistoryRepositoryPort histories = mock(ReviewHistoryRepositoryPort.class);
    private final ReviewHistoryRecorder recorder = new ReviewHistoryRecorder(histories);
    private final Review review =
        new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "보존할 본문", false);

    @Test
    void 마이크로초_시각으로_리뷰_전체_내용을_기록한다() {
        final var before = LocalDateTime.now().truncatedTo(MICROS);
        recorder.record(review, ReviewEventType.UPDATE);

        final var capture = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(histories).append(capture.capture());
        assertThat(capture.getValue().changedAt()).isBetween(before, LocalDateTime.now());
        assertThat(capture.getValue().changedAt().getNano() % 1000).isZero();
        assertThat(capture.getValue().content()).isEqualTo("보존할 본문");
        assertThat(capture.getValue().eventType()).isEqualTo(ReviewEventType.UPDATE);
    }

    @Test
    void 서버_시계가_이전_기록보다_뒤로_가도_이력_시각이_역행하지_않는다() {
        final var previous = LocalDateTime.now().plusDays(1).truncatedTo(MICROS);
        when(histories.findLatestChangedAt(801L)).thenReturn(Optional.of(previous));

        recorder.record(review, ReviewEventType.DELETE);

        final var capture = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(histories).append(capture.capture());
        assertThat(capture.getValue().changedAt()).isEqualTo(previous);
    }

    @Test
    void 기술_저장_오류는_삼키거나_업무_오류로_변환하지_않는다() {
        final var failure = new DataAccessResourceFailureException("synthetic history failure");
        doThrow(failure).when(histories).append(any(ReviewHistory.class));

        assertThatThrownBy(() -> recorder.record(review, ReviewEventType.CREATE)).isSameAs(failure);
    }
}
