package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetOnboardingQuestionCommand(Long userId, Long questionId) {
    public GetOnboardingQuestionCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (questionId == null || questionId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
