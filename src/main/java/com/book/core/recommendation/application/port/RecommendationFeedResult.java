package com.book.core.recommendation.application.port;

import java.util.List;

public class RecommendationFeedResult {
    private final List<RecommendationFeedItem> items;
    private final String nextCursor;
    private final boolean coldStart;
    private final String degraded;

    public RecommendationFeedResult(final List<RecommendationFeedItem> items, final String nextCursor, final boolean coldStart,
        final String degraded) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.coldStart = coldStart;
        this.degraded = degraded;
    }

    public List<RecommendationFeedItem> items() {
        return items;
    }

    public String nextCursor() {
        return nextCursor;
    }

    public boolean coldStart() {
        return coldStart;
    }

    public String degraded() {
        return degraded;
    }
}
