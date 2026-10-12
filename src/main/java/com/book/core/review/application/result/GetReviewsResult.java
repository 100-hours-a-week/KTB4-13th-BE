package com.book.core.review.application.result;

import java.util.List;

public record GetReviewsResult(List<ReviewItemResult> content, boolean hasNext) {
}
