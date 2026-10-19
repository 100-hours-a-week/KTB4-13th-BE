package com.book.core.review.application;

import static java.time.temporal.ChronoUnit.MICROS;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.enums.ReviewEventType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReviewHistoryRecorder {
    private final ReviewHistoryRepositoryPort histories;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(final Review review, final ReviewEventType eventType) {
        final LocalDateTime now = LocalDateTime.now().truncatedTo(MICROS);
        final LocalDateTime latestChangedAt = histories.findLatestChangedAt(review.id()).orElse(now);
        LocalDateTime changedAt = now;
        if (latestChangedAt.isAfter(now)) {
            changedAt = latestChangedAt;
        }
        histories.append(new ReviewHistory(review, eventType, changedAt));
    }
}
