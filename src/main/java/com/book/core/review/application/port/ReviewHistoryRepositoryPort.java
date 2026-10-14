package com.book.core.review.application.port;

import com.book.core.review.domain.ReviewHistory;
import java.time.LocalDateTime;
import java.util.Optional;

/** 리뷰 변경과 같은 트랜잭션에서 이력을 추가합니다. */
public interface ReviewHistoryRepositoryPort {
    Optional<LocalDateTime> findLatestChangedAt(final Long reviewId);

    void append(final ReviewHistory history);
}
