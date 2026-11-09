package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.result.RecommendationProductResult;
import com.book.core.product.application.usecase.GetRecommendationProductsUseCase;
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
    private final GetRecommendationProductsUseCase getRecommendationProductsUseCase;

    public GetRecommendationFeedResult execute(final GetRecommendationFeedCommand command) {
        final RecommendationFeedResult feedResult =
            recommendationFeedClient.getFeed(new RecommendationFeedRequest(command.userId(), command.surface(), command.size(),
                command.cursor(), command.sort(), command.category(), command.pubYearFrom(), command.pubYearTo(), command.matchScoreMin()));

        final Map<Long, RecommendationProductResult> products =
            getRecommendationProductsUseCase.execute(feedResult.items().stream().map(RecommendationFeedItem::bookId).toList());
        final List<RecommendationFeedItemResult> items = feedResult.items().stream()
            .map((final RecommendationFeedItem item) -> toItemResult(item, products.get(item.bookId()))).toList();
        return new GetRecommendationFeedResult(items, feedResult.nextCursor(), feedResult.coldStart(), feedResult.degraded());
    }

    private static RecommendationFeedItemResult toItemResult(final RecommendationFeedItem item, final RecommendationProductResult product) {
        Long productId = null;
        boolean inStock = false;
        if (product != null) {
            productId = product.productId();
            inStock = product.inStock();
        }
        return new RecommendationFeedItemResult(item.bookId(), productId, item.title(), item.author(), item.price(), item.coverUrl(),
            inStock, item.matchScore());
    }
}
