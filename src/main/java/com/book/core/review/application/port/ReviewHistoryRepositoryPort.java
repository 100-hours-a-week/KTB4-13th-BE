package com.book.core.review.application.port;

import com.book.core.review.domain.ReviewHistory;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ReviewHistoryRepositoryPort {
    Optional<LocalDateTime> findLatestChangedAt(final Long reviewId);

    void append(final ReviewHistory history);
}
