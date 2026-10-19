package com.book.core.preference.api.converter;

import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.book.core.preference.api.response.OnboardingPreferenceOptionResponse;
import com.book.core.preference.api.response.OnboardingPreferenceResponse;
import com.book.core.preference.api.response.UserMemoryGroupResponse;
import com.book.core.preference.api.response.UserPreferencesResponse;
import com.book.core.preference.application.result.UserPreferencesResult;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UserPreferenceResultConverter {
    public UserPreferencesResponse toUserPreferencesResponse(final UserPreferencesResult result) {
        return new UserPreferencesResponse(result.onboarding().stream().map(this::toOnboardingPreferenceResponse).toList(),
            result.memories().stream().map(this::toUserMemoryGroupResponse).toList());
    }

    private OnboardingPreferenceResponse toOnboardingPreferenceResponse(final OnboardingAnswerGroupResult result) {
        return new OnboardingPreferenceResponse(result.questionId(),
            result.options().stream()
                .map((final OnboardingOptionResult option) -> new OnboardingPreferenceOptionResponse(option.optionId(), option.content()))
                .toList());
    }

    private UserMemoryGroupResponse toUserMemoryGroupResponse(final UserMemoryGroupResult result) {
        return new UserMemoryGroupResponse(result.memoryIds(), result.type().name().toLowerCase(Locale.ROOT), result.value(),
            result.categories(), result.conversationDates());
    }
}
