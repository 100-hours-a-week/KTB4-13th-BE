package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

record AiChatRequest(@JsonProperty("user_id") Long userId, boolean consented, Map<String, Object> spec, String message,
    @JsonProperty("recent_turns") List<AiChatTurn> recentTurns, @JsonProperty("exclude_isbns") List<String> excludeIsbns,
    @JsonProperty("purchased_isbns") List<String> purchasedIsbns, @JsonProperty("library_isbns") List<String> libraryIsbns) {
}
