package com.book.core.review.application;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.enums.ReviewOperation;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 리뷰 쓰기 트랜잭션 안에서 스냅샷을 추가하고 기록 시각의 역행을 방지합니다. */
@Component
@RequiredArgsConstructor
public class ReviewHistoryRecorder {
    private final ReviewHistoryRepositoryPort histories;
    private final Clock clock;

    /** 수정과 삭제의 호출자는 해당 리뷰의 쓰기 잠금을 먼저 획득해야 합니다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(final Review review, final ReviewOperation operation, final String requestIp) {
        final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        final LocalDateTime previous = histories.findLatestChangedAt(review.id()).orElse(now);
        LocalDateTime changedAt = now;
        if (previous.isAfter(now)) {
            changedAt = previous;
        }
        histories.append(new ReviewHistory(review, operation, changedAt, requestIp));
    }
}
