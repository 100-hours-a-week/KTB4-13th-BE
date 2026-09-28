package com.book.core.recommendation.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Objects;

public class GetRecommendationCardCommand {
    private final Long userId;
    private final Long recommendationCardId;

    public GetRecommendationCardCommand(final Long userId, final Long recommendationCardId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (recommendationCardId == null || recommendationCardId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.recommendationCardId = recommendationCardId;
    }

    public Long userId() {
        return userId;
    }

    public Long recommendationCardId() {
        return recommendationCardId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final GetRecommendationCardCommand that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(recommendationCardId, that.recommendationCardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, recommendationCardId);
    }
}
