package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

// @formatter:off
public record OnboardingPreferenceAnswerCommand(Long questionId, List<Long> optionIds) {
    public OnboardingPreferenceAnswerCommand {
        if (questionId == null || questionId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (optionIds == null || optionIds.stream().anyMatch(optionId -> optionId == null || optionId <= 0)) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        if (optionIds.size() != optionIds.stream().distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        optionIds = List.copyOf(optionIds);
    }
}
// @formatter:on
