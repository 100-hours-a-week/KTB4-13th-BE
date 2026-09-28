package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.port.RecommendationFeedClient;
import com.book.core.recommendation.application.port.RecommendationFeedRequest;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetRecommendationFeedUseCase {
    private final RecommendationFeedClient recommendationFeedClient;

    public RecommendationFeedResult execute(final GetRecommendationFeedCommand command) {
        return recommendationFeedClient.getFeed(new RecommendationFeedRequest(command.userId(), command.size(), command.cursor()));
    }
}
