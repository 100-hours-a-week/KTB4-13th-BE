package com.book.core.onboarding.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PutOnboardingAnswersRequest(@NotNull List<Long> optionIds) {}
