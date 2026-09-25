package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.Objects;

public class PutOnboardingBooksCommand {
    private final Long userId;
    private final List<Long> bookIds;

    public PutOnboardingBooksCommand(final Long userId, final List<Long> bookIds) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (bookIds == null || bookIds.stream().anyMatch(Objects::isNull)) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
        if (bookIds.size() != bookIds.stream().distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
        this.userId = userId;
        this.bookIds = List.copyOf(bookIds);
    }

    public Long userId() {
        return userId;
    }

    public List<Long> bookIds() {
        return bookIds;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final PutOnboardingBooksCommand that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(bookIds, that.bookIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, bookIds);
    }
}
