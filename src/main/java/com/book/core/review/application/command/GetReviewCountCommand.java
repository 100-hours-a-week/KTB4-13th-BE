package com.book.core.review.application.command;

import com.book.core.review.domain.ReviewTarget;

public record GetReviewCountCommand(ReviewTarget target) {
}
