package com.book.core.recommendation.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

// @formatter:off
public record GetRecommendationFeedCommand(
        Long userId,
        RecommendationFeedSurface surface,
        int size,
        String cursor,
        RecommendationFeedSort sort,
        String category,
        Integer pubYearFrom,
        Integer pubYearTo,
        Integer matchScoreMin) {
    public GetRecommendationFeedCommand {
        final boolean hasRecommendMoreCondition =
                sort != null || category != null || pubYearFrom != null || pubYearTo != null || matchScoreMin != null;
        if (surface == RecommendationFeedSurface.HOME && hasRecommendMoreCondition) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (pubYearFrom != null && pubYearTo != null && pubYearFrom > pubYearTo) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (surface == RecommendationFeedSurface.RECOMMEND_MORE && sort == null) {
            sort = RecommendationFeedSort.MATCH;
        }
    }
}
// @formatter:on
