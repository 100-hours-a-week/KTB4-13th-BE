package com.book.core.onboarding.application.result;

import java.util.List;

public record OnboardingAnswerGroupResult(Long questionId, List<Long> optionIds) {}
