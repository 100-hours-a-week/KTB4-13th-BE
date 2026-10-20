package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshClient;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshRecommendationFeedUseCase {
    private final RecommendationFeedRefreshClient feedRefreshClient;

    // AI 서버가 한 밤에 한 목록을 한 번만 다시 뽑아 remaining이 줄어들므로 0이 될 때까지 부릅니다.
    public void execute() {
        int remaining;
        do {
            remaining = feedRefreshClient.refreshFeed();
        } while (remaining > 0);
    }
}
