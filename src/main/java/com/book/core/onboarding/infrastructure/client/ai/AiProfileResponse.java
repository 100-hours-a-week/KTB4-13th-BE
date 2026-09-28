package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiProfileResponse {
    private final boolean coldStart;
    private final int profileVersion;

    @JsonCreator
    AiProfileResponse(@JsonProperty("cold_start") final boolean coldStart, @JsonProperty("profile_version") final int profileVersion) {
        this.coldStart = coldStart;
        this.profileVersion = profileVersion;
    }

    boolean coldStart() {
        return coldStart;
    }

    int profileVersion() {
        return profileVersion;
    }
}
