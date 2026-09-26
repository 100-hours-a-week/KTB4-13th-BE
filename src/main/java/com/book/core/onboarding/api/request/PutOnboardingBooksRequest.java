package com.book.core.onboarding.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PutOnboardingBooksRequest(@NotNull List<Long> bookIds) {}
