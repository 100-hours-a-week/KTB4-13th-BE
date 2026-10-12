package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.Objects;

// @formatter:off
public record PutOnboardingPreferencesCommand(Long userId, List<OnboardingPreferenceAnswerCommand> answers, List<Long> bookIds) {
    public PutOnboardingPreferencesCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (answers == null || answers.stream().anyMatch(Objects::isNull)) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        if (answers.size() != answers.stream().map(OnboardingPreferenceAnswerCommand::questionId).distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        if (bookIds == null || bookIds.stream().anyMatch(bookId -> bookId == null || bookId <= 0)) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
        if (bookIds.size() != bookIds.stream().distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
        answers = List.copyOf(answers);
        bookIds = List.copyOf(bookIds);
    }
}
// @formatter:on
