package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

class AiChatRequest {
    private final Long userId;
    private final boolean consented;
    private final Map<String, Object> spec;
    private final String message;
    private final List<AiChatTurn> recentTurns;
    private final List<Long> excludeBookIds;

    AiChatRequest(final Long userId, final boolean consented, final Map<String, Object> spec, final String message,
        final List<AiChatTurn> recentTurns, final List<Long> excludeBookIds) {
        this.userId = userId;
        this.consented = consented;
        this.spec = spec;
        this.message = message;
        this.recentTurns = recentTurns;
        this.excludeBookIds = excludeBookIds;
    }

    @JsonProperty("user_id")
    Long userId() {
        return userId;
    }

    @JsonProperty("consented")
    boolean consented() {
        return consented;
    }

    @JsonProperty("spec")
    Map<String, Object> spec() {
        return spec;
    }

    @JsonProperty("message")
    String message() {
        return message;
    }

    @JsonProperty("recent_turns")
    List<AiChatTurn> recentTurns() {
        return recentTurns;
    }

    @JsonProperty("exclude_book_ids")
    List<Long> excludeBookIds() {
        return excludeBookIds;
    }
}
