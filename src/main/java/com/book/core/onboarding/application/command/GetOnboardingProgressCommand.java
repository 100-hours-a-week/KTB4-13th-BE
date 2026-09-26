package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetOnboardingProgressCommand(Long userId) {
    public GetOnboardingProgressCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
