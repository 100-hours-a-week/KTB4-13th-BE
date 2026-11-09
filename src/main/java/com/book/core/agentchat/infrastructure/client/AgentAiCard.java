package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AgentAiCard(String isbn, @JsonProperty("match_score") Integer matchScore, @JsonProperty("reason_short") String reasonShort,
    @JsonProperty("reason_long") String reasonLong) {
}
