package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiFeedRefreshEnvelope(@JsonProperty("message") String message, @JsonProperty("data") Data data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(@JsonProperty("refreshed") Integer refreshed, @JsonProperty("remaining") Integer remaining) {
    }
}
