package com.book.core.onboarding.infrastructure.client.ai;

import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// @formatter:off
record AiProfileRequest(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("idempotency_key") String idempotencyKey,
        @JsonProperty("onboarding") AiProfileOnboarding onboarding,
        @JsonProperty("memories") List<Object> memories) {
    static AiProfileRequest from(final AiPersonalizationProfileRequest request, final String idempotencyKey) {
        final AiProfileOnboarding onboarding = new AiProfileOnboarding(request.readingTimes(), request.criteria(), request.categories(),
                request.tags(), request.likedBookIds());
        return new AiProfileRequest(request.userId(), idempotencyKey, onboarding, List.of());
    }
}
// @formatter:on
