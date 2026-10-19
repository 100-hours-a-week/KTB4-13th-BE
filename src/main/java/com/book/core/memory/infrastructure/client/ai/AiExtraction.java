package com.book.core.memory.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiExtraction(@JsonProperty("type") String type, @JsonProperty("value") String value, @JsonProperty("strength") Integer strength,
    @JsonProperty("applies_to") List<String> appliesTo, @JsonProperty("keyword") String keyword, @JsonProperty("vector") List<Float> vector,
    @JsonProperty("dim") Integer dim, @JsonProperty("source_conversation_id") String sourceConversationId) {
}
