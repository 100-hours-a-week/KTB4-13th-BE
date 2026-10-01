package com.book.core.onboarding.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class GetOnboardingBookCandidatesCommand {
    private final List<String> subcategoryCodes;

    public GetOnboardingBookCandidatesCommand(final List<String> subcategoryCodes) {
        if (subcategoryCodes == null || subcategoryCodes.stream().anyMatch((final var code) -> code == null || code.isBlank())) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_SUBCATEGORY_SELECTION);
        }
        final List<String> selectedCodes = subcategoryCodes.stream().distinct().toList();
        if (selectedCodes.isEmpty() || selectedCodes.size() > 9) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_SUBCATEGORY_SELECTION);
        }
        this.subcategoryCodes = selectedCodes;
    }
}
