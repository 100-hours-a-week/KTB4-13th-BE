package com.book.core.review.application.command;

import com.book.core.review.domain.ReviewContent;

public record UpdateReviewCommand(Long userId, Long reviewId, ReviewContent content) {
}
