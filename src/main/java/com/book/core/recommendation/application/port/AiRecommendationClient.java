package com.book.core.recommendation.application.port;

import java.util.function.Consumer;

public interface AiRecommendationClient {
    AiRecommendationChatResult chat(final AiRecommendationChatRequest request);

    AiRecommendationChatResult chat(AiRecommendationChatRequest request, Consumer<String> onDelta);
}
