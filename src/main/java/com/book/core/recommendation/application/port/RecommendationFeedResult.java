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

    /** rule-only 축소 응답일 때만 값이 있고, 아니면 null. 정상 개인화/cold-start 응답은 저하가 아니다. */
    public String degraded() {
        return degraded;
    }
}
