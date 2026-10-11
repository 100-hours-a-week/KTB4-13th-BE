package com.book.core.review.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.enums.ReviewTargetType;

// @formatter:off
public record GetReviewsCommand(ReviewTargetType targetType, Long targetId, OffsetLimit offsetLimit) {
    public GetReviewsCommand {
        if (targetType == null || targetId == null || targetId <= 0 || offsetLimit == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
// @formatter:on
