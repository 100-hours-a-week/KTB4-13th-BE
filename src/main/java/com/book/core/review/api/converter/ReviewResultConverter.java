package com.book.core.review.api.converter;

import com.book.common.response.PageResponse;
import com.book.core.review.api.response.ReviewResponse;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import org.springframework.stereotype.Component;

@Component
public class ReviewResultConverter {
    public PageResponse<ReviewResponse> toResponse(final Page<ReviewItemResult> result) {
        final var content = result.content().stream()
            .map(item -> new ReviewResponse(item.id(), item.target().type(), item.target().id(), item.rate(), item.content())).toList();
        return new PageResponse<>(content, result.hasNext());
    }
}
