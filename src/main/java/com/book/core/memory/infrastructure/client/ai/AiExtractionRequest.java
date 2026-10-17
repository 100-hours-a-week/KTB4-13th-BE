package com.book.core.memory.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record AiExtractionRequest(@JsonProperty("user_id") Long userId, @JsonProperty("consented") boolean consented,
    @JsonProperty("conversation") List<AiExtractionMessage> conversation, @JsonProperty("conversation_id") String conversationId) {
}
