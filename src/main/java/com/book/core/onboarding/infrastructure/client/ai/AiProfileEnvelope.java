package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiProfileEnvelope {
    private final AiProfileResponse data;

    @JsonCreator
    AiProfileEnvelope(@JsonProperty("data") final AiProfileResponse data) {
        this.data = data;
    }

    AiProfileResponse data() {
        return data;
    }
}
