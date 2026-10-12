package com.book.core.onboarding.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

// @formatter:off
public record OnboardingPreferenceAnswerRequest(
        @Schema(description = "온보딩 질문 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive Long questionId,
        @Schema(description = "선택한 옵션 ID 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull List<@NotNull @Positive Long> optionIds) {}
// @formatter:on
