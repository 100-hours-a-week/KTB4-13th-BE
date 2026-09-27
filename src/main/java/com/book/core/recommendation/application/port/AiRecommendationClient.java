package com.book.core.recommendation.application.port;

public interface AiRecommendationClient {
    AiRecommendationChatResult chat(final AiRecommendationChatRequest request);
}
