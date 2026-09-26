package com.book.core.onboarding.api.converter;

import com.book.core.onboarding.api.request.PutOnboardingAnswersRequest;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import org.springframework.stereotype.Component;

@Component
public class OnboardingCommandConverter {
    public GetOnboardingQuestionCommand toGetOnboardingQuestionCommand(final Long userId, final Long questionId) {
        return new GetOnboardingQuestionCommand(userId, questionId);
    }

    public GetOnboardingProgressCommand toGetOnboardingProgressCommand(final Long userId) {
        return new GetOnboardingProgressCommand(userId);
    }

    public PutOnboardingAnswersCommand toPutOnboardingAnswersCommand(
            final Long userId, final Long questionId, final PutOnboardingAnswersRequest request) {
        return new PutOnboardingAnswersCommand(userId, questionId, request.optionIds());
    }
}
