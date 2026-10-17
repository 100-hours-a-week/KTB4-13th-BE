package com.book.core.preference.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.usecase.GetUserMemoriesUseCase;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.preference.application.result.UserPreferencesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final GetUserMemoriesUseCase getUserMemoriesUseCase;

    public UserPreferencesResult getPreferences(final Long userId) {
        final OnboardingProgressResult onboarding = getOnboardingProgressUseCase.execute(new GetOnboardingProgressCommand(userId));
        if (onboarding.status() != OnboardingStatus.COMPLETED) {
            throw new CoreException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }
        return new UserPreferencesResult(onboarding.answers(), getUserMemoriesUseCase.execute(userId));
    }
}
