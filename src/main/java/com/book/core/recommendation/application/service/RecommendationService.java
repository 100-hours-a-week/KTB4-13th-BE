package com.book.core.recommendation.application.service;

import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.GetRecommendationFeedResult;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import com.book.core.recommendation.application.usecase.GetRecommendationCardUseCase;
import com.book.core.recommendation.application.usecase.GetRecommendationFeedUseCase;
import com.book.core.recommendation.application.usecase.SaveRecommendationCardsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final ChatRecommendationUseCase chatRecommendationUseCase;
    private final SaveRecommendationCardsUseCase saveRecommendationCardsUseCase;
    private final GetRecommendationCardUseCase getRecommendationCardUseCase;
    private final GetRecommendationFeedUseCase getRecommendationFeedUseCase;

    public ChatRecommendationResult chat(final ChatRecommendationCommand command) {
        final ChatRecommendationAiOutcome outcome = chatRecommendationUseCase.execute(command);
        final List<RecommendationCardResult> savedCards = saveRecommendationCardsUseCase.execute(command.userId(), outcome.cards());

        return ChatRecommendationResult.from(outcome, savedCards);
    }

    public RecommendationCardDetailResult getCard(final GetRecommendationCardCommand command) {
        return getRecommendationCardUseCase.execute(command);
    }

    public GetRecommendationFeedResult getFeed(final GetRecommendationFeedCommand command) {
        return getRecommendationFeedUseCase.execute(command);
    }

}
