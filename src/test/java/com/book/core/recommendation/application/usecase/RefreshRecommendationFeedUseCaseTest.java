package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshClient;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshResult;
import org.junit.jupiter.api.Test;

class RefreshRecommendationFeedUseCaseTest {
    private final RecommendationFeedRefreshClient client = mock(RecommendationFeedRefreshClient.class);
    private final RefreshRecommendationFeedUseCase useCase = new RefreshRecommendationFeedUseCase(client);

    @Test
    void 남은_목록이_0이_될_때까지_다시_부른다() {
        when(client.refreshFeed()).thenReturn(result(12, 30), result(25, 5), result(5, 0));

        useCase.execute();

        verify(client, times(3)).refreshFeed();
    }

    @Test
    void 다시_뽑는_동안_남은_목록이_늘어도_다시_뽑은_목록이_있으면_계속_부른다() {
        when(client.refreshFeed()).thenReturn(result(2, 30), result(2, 31), result(31, 0));

        useCase.execute();

        verify(client, times(3)).refreshFeed();
    }

    @Test
    void 다시_뽑은_목록_없이_남은_목록만_있으면_반복을_멈추고_오류를_전달한다() {
        when(client.refreshFeed()).thenReturn(result(0, 30));

        assertThatThrownBy(useCase::execute).isInstanceOf(CoreException.class);

        verify(client, times(1)).refreshFeed();
    }

    @Test
    void 실패하면_더_부르지_않고_오류를_전달한다() {
        when(client.refreshFeed()).thenReturn(result(12, 30)).thenThrow(new CoreException(ErrorCode.AI_FEED_TIMEOUT));

        assertThatThrownBy(useCase::execute).isInstanceOf(CoreException.class);

        verify(client, times(2)).refreshFeed();
    }

    private static RecommendationFeedRefreshResult result(final int refreshedCount, final int remainingCount) {
        return new RecommendationFeedRefreshResult(refreshedCount, remainingCount);
    }
}
