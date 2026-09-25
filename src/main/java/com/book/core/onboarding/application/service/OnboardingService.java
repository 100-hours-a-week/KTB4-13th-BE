package com.book.core.onboarding.application.service;

import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase;

    public OnboardingQuestionResult getQuestion(final GetOnboardingQuestionCommand command) {
        return getOnboardingQuestionUseCase.execute(command);
    }
}
