package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Objects;

public class GetPersonalizedRecommendationConsentCommand {
    private final Long userId;

    public GetPersonalizedRecommendationConsentCommand(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
    }

    public Long userId() {
        return userId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final GetPersonalizedRecommendationConsentCommand that)) {
            return false;
        }
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
