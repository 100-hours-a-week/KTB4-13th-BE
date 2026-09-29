package com.book.core.recommendation.application.service;

import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import com.book.core.recommendation.application.usecase.GetRecommendationCardUseCase;
import com.book.core.recommendation.application.usecase.GetRecommendationFeedUseCase;
import com.book.core.recommendation.application.usecase.SaveRecommendationCardsUseCase;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.List;
import java.util.stream.IntStream;
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
        final List<RecommendationCard> savedCards = saveRecommendationCardsUseCase.execute(command.userId(), outcome.cards());

        final List<RecommendationCardResult> cardResults = combine(savedCards, outcome);
        return new ChatRecommendationResult(outcome.spec(), outcome.reply(), cardResults, outcome.followup(), outcome.buttons(),
            outcome.degraded());
    }

    public RecommendationCardDetailResult getCard(final GetRecommendationCardCommand command) {
        return getRecommendationCardUseCase.execute(command);
    }

    public RecommendationFeedResult getFeed(final GetRecommendationFeedCommand command) {
        return getRecommendationFeedUseCase.execute(command);
    }

    private List<RecommendationCardResult> combine(final List<RecommendationCard> savedCards, final ChatRecommendationAiOutcome outcome) {
        return IntStream.range(0, savedCards.size()).mapToObj(index -> {
            final RecommendationCard saved = savedCards.get(index);
            final var resolved = outcome.cards().get(index);
            return new RecommendationCardResult(saved.id(), resolved.bookId(), resolved.title(), resolved.author(),
                resolved.coverImageUrl(), resolved.reasonLong());
        }).toList();
    }
}
