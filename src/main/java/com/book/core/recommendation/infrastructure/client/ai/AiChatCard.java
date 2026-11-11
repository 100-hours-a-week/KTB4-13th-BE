package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiChatCard(String isbn, @JsonProperty("match_score") Integer matchScore, @JsonProperty("reason_short") String reasonShort,
    @JsonProperty("reason_long") String reasonLong) {
}
