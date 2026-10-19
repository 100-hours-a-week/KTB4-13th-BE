package com.book.core.agentchat.infrastructure.client;

import com.book.core.recommendation.application.command.RecommendationTurn;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

record AgentAiRequest(@JsonProperty("user_id") Long userId, boolean consented, Map<String, Object> spec, String message,
    @JsonProperty("recent_turns") List<RecommendationTurn> recentTurns, @JsonProperty("exclude_book_ids") List<Long> excludeBookIds,
    @JsonProperty("idempotency_key") String idempotencyKey, @JsonProperty("conversation_id") String conversationId,
    @JsonProperty("user_context") Map<String, Boolean> userContext, @JsonProperty("context_cards") List<AgentAiContextCard> contextCards,
    @JsonProperty("focused_isbn") String focusedIsbn, List<AgentAiCart> cart, List<AgentAiOffer> offers) {
}
