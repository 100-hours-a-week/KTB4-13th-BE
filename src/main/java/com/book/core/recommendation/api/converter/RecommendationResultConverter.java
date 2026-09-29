package com.book.core.recommendation.api.converter;

import com.book.core.recommendation.api.response.ChatRecommendationResponse;
import com.book.core.recommendation.api.response.RecommendationCardDetailResponse;
import com.book.core.recommendation.api.response.RecommendationFeedResponse;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import org.springframework.stereotype.Component;

@Component
public class RecommendationResultConverter {
    public ChatRecommendationResponse toChatRecommendationResponse(final ChatRecommendationResult result) {
        return ChatRecommendationResponse.from(result);
    }

    public RecommendationCardDetailResponse toRecommendationCardDetailResponse(final RecommendationCardDetailResult result) {
        return RecommendationCardDetailResponse.from(result);
    }

    public RecommendationFeedResponse toRecommendationFeedResponse(final RecommendationFeedResult result) {
        return RecommendationFeedResponse.from(result);
    }
}
