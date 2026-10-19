package com.book.core.review.application;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewHistory;
import com.book.core.review.domain.enums.ReviewEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReviewHistoryRecorder {
    private final ReviewRepositoryPort reviews;
    private final ReviewHistoryRepositoryPort histories;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(final Long reviewId, final ReviewEventType eventType) {
        final Review review = reviews.findByIdIncludingDeleted(reviewId).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        histories.append(new ReviewHistory(review, eventType));
    }
}
