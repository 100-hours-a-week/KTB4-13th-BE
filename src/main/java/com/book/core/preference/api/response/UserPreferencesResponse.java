package com.book.core.preference.api.response;

import java.util.List;

public record UserPreferencesResponse(List<OnboardingPreferenceResponse> onboarding, List<UserMemoryGroupResponse> memories) {
}
