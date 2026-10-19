package com.book.core.preference.api.response;

import java.util.List;

public record OnboardingPreferenceResponse(Long questionId, List<OnboardingPreferenceOptionResponse> options) {
}
