package com.book.core.review.api.converter;

import com.book.common.response.PageResponse;
import com.book.core.review.api.response.ReviewResponse;
import com.book.core.review.application.result.GetReviewsResult;
import org.springframework.stereotype.Component;

@Component
public class ReviewResultConverter {
    public PageResponse<ReviewResponse> toResponse(final GetReviewsResult result) {
        final var content = result.content().stream()
            .map(item -> new ReviewResponse(item.id(), item.targetType(), item.targetId(), item.rate(), item.content())).toList();
        return new PageResponse<>(content, result.hasNext());
    }
}
