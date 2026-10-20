package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshClient;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshResult;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshRecommendationFeedUseCase {
    private final RecommendationFeedRefreshClient feedRefreshClient;

    public void execute() {
        RecommendationFeedRefreshResult refreshResult = feedRefreshClient.refreshFeed();
        while (refreshResult.remainingCount() > 0) {
            if (refreshResult.refreshedCount() == 0) {
                throw new CoreException(ErrorCode.AI_FEED_FAILURE);
            }
            refreshResult = feedRefreshClient.refreshFeed();
        }
    }
}
