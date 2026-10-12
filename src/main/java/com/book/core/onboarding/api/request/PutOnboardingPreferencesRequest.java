package com.book.core.onboarding.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

// @formatter:off
public record PutOnboardingPreferencesRequest(
        @Schema(description = "Q1~Q4 질문별 답변 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull List<@NotNull @Valid OnboardingPreferenceAnswerRequest> answers,
        @Schema(description = "Q5 선택 도서 ID 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull List<@NotNull @Positive Long> bookIds) {}
// @formatter:on
