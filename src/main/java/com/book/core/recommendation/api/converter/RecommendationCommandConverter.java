package com.book.core.recommendation.api.converter;

import com.book.core.recommendation.api.request.ChatRecommendationRequest;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.command.RecommendationTurn;
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
}
