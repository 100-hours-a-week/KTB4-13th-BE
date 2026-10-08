package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.usecase.FindProductIdsByBookIdsUseCase;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.port.RecommendationFeedClient;
import com.book.core.recommendation.application.port.RecommendationFeedItem;
import com.book.core.recommendation.application.port.RecommendationFeedRequest;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.book.core.recommendation.application.result.GetRecommendationFeedResult;
import com.book.core.recommendation.application.result.RecommendationFeedItemResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetRecommendationFeedUseCase {
    private final RecommendationFeedClient recommendationFeedClient;
    private final FindProductIdsByBookIdsUseCase findProductIdsByBookIdsUseCase;

    public GetRecommendationFeedResult execute(final GetRecommendationFeedCommand command) {
        final RecommendationFeedResult feedResult =
            recommendationFeedClient.getFeed(new RecommendationFeedRequest(command.userId(), command.surface(), command.size(),
                command.cursor(), command.sort(), command.category(), command.pubYearFrom(), command.pubYearTo(), command.matchScoreMin()));

        // The AI feed returns only bookIds; the product detail link needs the active product's own ID.
        final Map<Long, Long> productIdsByBookId =
            findProductIdsByBookIdsUseCase.execute(feedResult.items().stream().map(RecommendationFeedItem::bookId).toList());
        final List<RecommendationFeedItemResult> items =
            feedResult.items().stream().map((final var item) -> toItemResult(item, productIdsByBookId.get(item.bookId()))).toList();
        return new GetRecommendationFeedResult(items, feedResult.nextCursor(), feedResult.coldStart(), feedResult.degraded());
    }

    private static RecommendationFeedItemResult toItemResult(final RecommendationFeedItem item, final Long productId) {
        return new RecommendationFeedItemResult(item.bookId(), productId, item.title(), item.author(), item.price(), item.coverUrl(),
            item.inStock(), item.matchScore());
    }
}
