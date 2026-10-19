package com.book.core.review.application.command;

import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.ReviewTarget;

public record AddReviewCommand(Long userId, ReviewTarget target, ReviewContent content, boolean isSpoiler) {
}
