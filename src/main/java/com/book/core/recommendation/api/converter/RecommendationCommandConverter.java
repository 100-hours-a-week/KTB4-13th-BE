package com.book.core.recommendation.api.converter;

import com.book.core.recommendation.api.request.ChatRecommendationRequest;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.command.RecommendationFeedSort;
import com.book.core.recommendation.application.command.RecommendationFeedSurface;
import com.book.core.recommendation.application.command.RecommendationTurn;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RecommendationCommandConverter {
    public ChatRecommendationCommand toChatRecommendationCommand(final Long userId, final ChatRecommendationRequest request) {
        final var turns = request.recentTurns().stream().map(turn -> new RecommendationTurn(turn.role(), turn.text())).toList();
        return new ChatRecommendationCommand(userId, Boolean.TRUE.equals(request.consented()), request.spec(), request.message(), turns,
            request.excludeBookIdsOrEmpty());
    }

    public GetRecommendationCardCommand toGetRecommendationCardCommand(final Long userId, final Long recommendationCardId) {
        return new GetRecommendationCardCommand(userId, recommendationCardId);
    }

    public GetRecommendationFeedCommand toGetRecommendationFeedCommand(final Long userId, final String surface, final int size,
        final String cursor, final String sort, final String category, final Integer pubYearFrom, final Integer pubYearTo,
        final Integer matchScoreMin) {
        final RecommendationFeedSort feedSort = sort == null ? null : RecommendationFeedSort.valueOf(sort.toUpperCase(Locale.ROOT));
        return new GetRecommendationFeedCommand(userId, RecommendationFeedSurface.valueOf(surface.toUpperCase(Locale.ROOT)), size, cursor,
            feedSort, category, pubYearFrom, pubYearTo, matchScoreMin);
    }
}
