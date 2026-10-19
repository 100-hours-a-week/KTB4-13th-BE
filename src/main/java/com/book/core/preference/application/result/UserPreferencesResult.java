package com.book.core.preference.application.result;

import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import java.util.List;

public record UserPreferencesResult(List<OnboardingAnswerGroupResult> onboarding, List<UserMemoryGroupResult> memories) {
}
