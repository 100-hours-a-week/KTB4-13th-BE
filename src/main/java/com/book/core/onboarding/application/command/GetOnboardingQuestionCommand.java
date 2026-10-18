package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Objects;

public class GetOnboardingQuestionCommand {
    private final Long userId;
    private final Long questionId;

    public GetOnboardingQuestionCommand(final Long userId, final Long questionId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (questionId == null || questionId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.questionId = questionId;
    }

    public Long userId() {
        return userId;
    }

    public Long questionId() {
        return questionId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final GetOnboardingQuestionCommand that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(questionId, that.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, questionId);
    }
}
