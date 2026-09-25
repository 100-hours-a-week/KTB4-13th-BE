package com.book.core.onboarding.api.converter;

import com.book.core.onboarding.api.response.OnboardingQuestionResponse;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import org.springframework.stereotype.Component;

@Component
public class OnboardingResultConverter {
    public OnboardingQuestionResponse toOnboardingQuestionResponse(final OnboardingQuestionResult result) {
        return OnboardingQuestionResponse.from(result);
    }
}
