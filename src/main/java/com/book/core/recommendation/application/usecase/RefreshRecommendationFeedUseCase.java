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

    // 한 밤에 한 목록은 한 번만 다시 뽑아 remaining이 줄어야 하므로, 줄지 않으면 야간 배치 잠금을 붙잡지 않도록 멈춥니다.
    public void execute() {
        int remaining = feedRefreshClient.refreshFeed();
        while (remaining > 0) {
            final int next = feedRefreshClient.refreshFeed();
            if (next >= remaining) {
                throw new CoreException(ErrorCode.AI_FEED_FAILURE);
            }
            remaining = next;
        }
    }
}
