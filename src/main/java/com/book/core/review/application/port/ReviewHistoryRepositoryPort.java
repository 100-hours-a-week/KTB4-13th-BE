package com.book.core.review.application.port;

import com.book.core.review.domain.ReviewHistory;

public interface ReviewHistoryRepositoryPort {
    void append(final ReviewHistory history);
}
