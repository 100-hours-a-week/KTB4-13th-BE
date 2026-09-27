package com.book.core.recommendation.application.command;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatRecommendationCommand {
    private final Long userId;
    private final boolean consented;
    private final Map<String, Object> spec;
    private final String message;
    private final List<RecommendationTurn> recentTurns;
    private final List<Long> excludeBookIds;

    public ChatRecommendationCommand(final Long userId, final boolean consented, final Map<String, Object> spec, final String message,
        final List<RecommendationTurn> recentTurns, final List<Long> excludeBookIds) {
        this.userId = userId;
        this.consented = consented;
        this.spec = spec;
        this.message = message;
        this.recentTurns = recentTurns;
        this.excludeBookIds = excludeBookIds;
    }

    public Long userId() {
        return userId;
    }

    public boolean consented() {
        return consented;
    }

    public Map<String, Object> spec() {
        return spec;
    }

    public String message() {
        return message;
    }

    public List<RecommendationTurn> recentTurns() {
        return recentTurns;
    }

    public List<Long> excludeBookIds() {
        return excludeBookIds;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final ChatRecommendationCommand that)) {
            return false;
        }
        return consented == that.consented && Objects.equals(userId, that.userId) && Objects.equals(spec, that.spec)
            && Objects.equals(message, that.message) && Objects.equals(recentTurns, that.recentTurns)
            && Objects.equals(excludeBookIds, that.excludeBookIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, consented, spec, message, recentTurns, excludeBookIds);
    }
}
