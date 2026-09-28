package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.Objects;

public class PutOnboardingAnswersCommand {
    private final Long userId;
    private final Long questionId;
    private final List<Long> optionIds;

    public PutOnboardingAnswersCommand(final Long userId, final Long questionId, final List<Long> optionIds) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (questionId == null || questionId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (optionIds == null || optionIds.stream().anyMatch(Objects::isNull)) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        if (optionIds.size() != optionIds.stream().distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        this.userId = userId;
        this.questionId = questionId;
        this.optionIds = List.copyOf(optionIds);
    }

    public Long userId() {
        return userId;
    }

    public Long questionId() {
        return questionId;
    }

    public List<Long> optionIds() {
        return optionIds;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final PutOnboardingAnswersCommand that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(questionId, that.questionId)
            && Objects.equals(optionIds, that.optionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, questionId, optionIds);
    }
}
