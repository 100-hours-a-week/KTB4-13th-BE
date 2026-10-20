package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshClient;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshRecommendationFeedUseCase {
    private final RecommendationFeedRefreshClient feedRefreshClient;

    public void execute() {
        int remainingFeedCount = feedRefreshClient.refreshFeed();
        while (remainingFeedCount > 0) {
            final int nextRemainingFeedCount = feedRefreshClient.refreshFeed();
            if (nextRemainingFeedCount >= remainingFeedCount) {
                throw new CoreException(ErrorCode.AI_FEED_FAILURE);
            }
            remainingFeedCount = nextRemainingFeedCount;
        }
    }
}
