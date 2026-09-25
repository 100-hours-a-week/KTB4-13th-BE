package com.book.core.onboarding.api.converter;

import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import org.springframework.stereotype.Component;

@Component
public class OnboardingCommandConverter {
    public GetOnboardingQuestionCommand toGetOnboardingQuestionCommand(final Long userId, final Long questionId) {
        return new GetOnboardingQuestionCommand(userId, questionId);
    }
}
