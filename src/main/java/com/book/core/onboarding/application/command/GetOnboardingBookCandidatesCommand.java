package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

/** Selected subcategory codes for onboarding book candidates. */
// @formatter:off
public record GetOnboardingBookCandidatesCommand(List<String> subcategoryCodes) {
    public GetOnboardingBookCandidatesCommand {
        if (subcategoryCodes == null || subcategoryCodes.stream().anyMatch((final var code) -> code == null || code.isBlank())) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_SUBCATEGORY_SELECTION);
        }
        subcategoryCodes = subcategoryCodes.stream().distinct().toList();
        if (subcategoryCodes.isEmpty() || subcategoryCodes.size() > 9) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_SUBCATEGORY_SELECTION);
        }
    }
}
// @formatter:on
