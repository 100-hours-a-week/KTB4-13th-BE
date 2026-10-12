package com.book.core.review.application.command;

import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.ReviewTarget;

public record GetReviewsCommand(ReviewTarget target, OffsetLimit offsetLimit) {
}
