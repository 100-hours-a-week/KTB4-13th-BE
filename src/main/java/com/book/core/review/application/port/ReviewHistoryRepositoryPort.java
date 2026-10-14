package com.book.core.review.application.port;

import com.book.core.review.domain.ReviewHistory;

/** 리뷰 변경과 같은 트랜잭션에서 이력을 추가합니다. */
public interface ReviewHistoryRepositoryPort {
    void append(final ReviewHistory history);
}
