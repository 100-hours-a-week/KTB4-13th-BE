package com.book.core.review.application;

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
import com.book.core.review.domain.enums.ReviewOperation;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

class ReviewHistoryRecorderTest {
    private final ReviewHistoryRepositoryPort histories = mock(ReviewHistoryRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-14T12:00:00.123456789Z"), ZoneOffset.UTC);
    private final ReviewHistoryRecorder recorder = new ReviewHistoryRecorder(histories, clock);
    private final Review review =
        new Review(801L, 42L, "ORDER_ITEM_701", ReviewTargetType.PRODUCT, 10L, new BigDecimal("4.5"), "보존할 본문", false);

    @Test
    void 마이크로초_시각으로_리뷰_전체_내용을_기록한다() {
        recorder.record(review, ReviewOperation.UPDATE, "192.0.2.1");

        final var capture = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(histories).append(capture.capture());
        assertThat(capture.getValue().changedAt()).isEqualTo(LocalDateTime.parse("2026-10-14T12:00:00.123456"));
        assertThat(capture.getValue().content()).isEqualTo("보존할 본문");
        assertThat(capture.getValue().operation()).isEqualTo(ReviewOperation.UPDATE);
        assertThat(capture.getValue().requestIp()).isEqualTo("192.0.2.1");
    }

    @Test
    void 서버_시계가_이전_기록보다_뒤로_가도_이력_시각이_역행하지_않는다() {
        final var previous = LocalDateTime.parse("2026-10-14T12:01:00.000001");
        when(histories.findLatestChangedAt(801L)).thenReturn(Optional.of(previous));

        recorder.record(review, ReviewOperation.DELETE, "192.0.2.1");

        final var capture = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(histories).append(capture.capture());
        assertThat(capture.getValue().changedAt()).isEqualTo(previous);
    }

    @Test
    void 기술_저장_오류는_삼키거나_업무_오류로_변환하지_않는다() {
        final var failure = new DataAccessResourceFailureException("synthetic history failure");
        doThrow(failure).when(histories).append(any(ReviewHistory.class));

        assertThatThrownBy(() -> recorder.record(review, ReviewOperation.CREATE, "192.0.2.1")).isSameAs(failure);
    }
}
