package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.Objects;

public record PutOnboardingBooksCommand(Long userId, List<Long> bookIds) {
    public PutOnboardingBooksCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (bookIds == null || bookIds.stream().anyMatch(Objects::isNull)) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
        if (bookIds.size() != bookIds.stream().distinct().count()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
        bookIds = List.copyOf(bookIds);
    }
}
