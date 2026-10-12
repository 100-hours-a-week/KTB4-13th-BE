package com.book.core.review.application.command;

public record RemoveReviewCommand(Long userId, Long reviewId) {
}
