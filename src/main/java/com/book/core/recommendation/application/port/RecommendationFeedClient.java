package com.book.core.recommendation.application.port;

public interface RecommendationFeedClient {
    RecommendationFeedResult getFeed(final RecommendationFeedRequest request);
}
