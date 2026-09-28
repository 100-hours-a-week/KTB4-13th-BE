package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Objects;

public class UpdatePersonalizedRecommendationConsentCommand {
    private final Long userId;
    private final boolean consented;

    public UpdatePersonalizedRecommendationConsentCommand(final Long userId, final boolean consented) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.consented = consented;
    }

    public Long userId() {
        return userId;
    }

    public boolean consented() {
        return consented;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final UpdatePersonalizedRecommendationConsentCommand that)) {
            return false;
        }
        return consented == that.consented && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, consented);
    }
}
