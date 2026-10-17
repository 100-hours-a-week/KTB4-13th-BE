package com.book.core.memory.infrastructure.client.ai;

import com.book.core.memory.application.command.ConversationMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record AiExtractionRequest(@JsonProperty("user_id") Long userId, @JsonProperty("consented") boolean consented,
    @JsonProperty("conversation") List<ConversationMessage> conversation, @JsonProperty("conversation_id") String conversationId) {
}
