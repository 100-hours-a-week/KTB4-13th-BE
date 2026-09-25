package com.book.core.onboarding.application.service;

import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase;
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;

    public OnboardingQuestionResult getQuestion(final GetOnboardingQuestionCommand command) {
        return getOnboardingQuestionUseCase.execute(command);
    }

    public OnboardingProgressResult getProgress(final GetOnboardingProgressCommand command) {
        return getOnboardingProgressUseCase.execute(command);
    }
}
