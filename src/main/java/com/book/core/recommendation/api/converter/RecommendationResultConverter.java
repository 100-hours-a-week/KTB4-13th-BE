package com.book.core.recommendation.api.converter;

import com.book.core.recommendation.api.response.ChatRecommendationResponse;
import com.book.core.recommendation.api.response.RecommendationCardDetailResponse;
import com.book.core.recommendation.api.response.RecommendationFeedResponse;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.GetRecommendationFeedResult;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import org.springframework.stereotype.Component;

@Component
public class RecommendationResultConverter {
    public ChatRecommendationResponse toChatRecommendationResponse(final ChatRecommendationResult result) {
        return ChatRecommendationResponse.from(result);
    }

    public RecommendationCardDetailResponse toRecommendationCardDetailResponse(final RecommendationCardDetailResult result) {
        return new RecommendationCardDetailResponse(result.recommendationCardId(), result.bookId(), result.reasonLong());
    }

    public RecommendationFeedResponse toRecommendationFeedResponse(final GetRecommendationFeedResult result) {
        return RecommendationFeedResponse.from(result);
    }
}
