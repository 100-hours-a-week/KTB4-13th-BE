package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class AiProfileRequest {
    private final Long userId;
    private final String idempotencyKey;
    private final AiProfileOnboarding onboarding;
    private final List<Object> memories;

    AiProfileRequest(final Long userId, final String idempotencyKey, final AiProfileOnboarding onboarding) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.onboarding = onboarding;
        this.memories = List.of();
    }

    @JsonProperty("user_id")
    Long userId() {
        return userId;
    }

    @JsonProperty("idempotency_key")
    String idempotencyKey() {
        return idempotencyKey;
    }

    @JsonProperty("onboarding")
    AiProfileOnboarding onboarding() {
        return onboarding;
    }

    @JsonProperty("memories")
    List<Object> memories() {
        return memories;
    }
}
