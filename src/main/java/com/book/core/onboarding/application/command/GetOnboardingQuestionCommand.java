package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GetOnboardingQuestionCommand {
    private final Long userId;
    private final Long questionId;
    private final List<Long> parentOptionIds;

    public GetOnboardingQuestionCommand(final Long userId, final Long questionId) {
        this(userId, questionId, null);
    }

    public GetOnboardingQuestionCommand(final Long userId, final Long questionId, final List<Long> parentOptionIds) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (questionId == null || questionId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (parentOptionIds != null && (parentOptionIds.isEmpty()
            || parentOptionIds.stream().anyMatch(parentOptionId -> parentOptionId == null || parentOptionId <= 0))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (parentOptionIds != null && parentOptionIds.size() != parentOptionIds.stream().distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.questionId = questionId;
        if (parentOptionIds == null) {
            this.parentOptionIds = null;
        } else {
            this.parentOptionIds = List.copyOf(parentOptionIds);
        }
    }

    public Long userId() {
        return userId;
    }

    public Long questionId() {
        return questionId;
    }

    public Optional<List<Long>> parentOptionIds() {
        return Optional.ofNullable(parentOptionIds);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final GetOnboardingQuestionCommand that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(questionId, that.questionId)
            && Objects.equals(parentOptionIds, that.parentOptionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, questionId, parentOptionIds);
    }
}
