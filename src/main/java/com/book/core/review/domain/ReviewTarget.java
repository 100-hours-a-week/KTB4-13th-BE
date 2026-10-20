package com.book.core.review.domain;

import com.book.core.review.domain.enums.ReviewTargetType;

public record ReviewTarget(ReviewTargetType type, Long id) {
}
