package com.book.core.recommendation.api.response;

import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RecommendationFeedResponse {
    private final List<RecommendationFeedItemResponse> items;
    private final String nextCursor;
    private final boolean coldStart;

    public RecommendationFeedResponse(final List<RecommendationFeedItemResponse> items, final String nextCursor, final boolean coldStart) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.coldStart = coldStart;
    }

    public static RecommendationFeedResponse from(final RecommendationFeedResult result) {
        return new RecommendationFeedResponse(result.items().stream().map(RecommendationFeedItemResponse::from).toList(),
            result.nextCursor(), result.coldStart());
    }

    @JsonProperty
    public List<RecommendationFeedItemResponse> items() {
        return items;
    }

    @JsonProperty
    public String nextCursor() {
        return nextCursor;
    }

    @JsonProperty
    public boolean coldStart() {
        return coldStart;
    }
}
