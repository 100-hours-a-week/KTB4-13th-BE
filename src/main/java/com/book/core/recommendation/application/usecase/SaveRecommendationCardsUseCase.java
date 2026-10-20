package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.book.core.recommendation.application.result.ResolvedRecommendationCard;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SaveRecommendationCardsUseCase {
    private final RecommendationCardRepositoryPort recommendationCardRepository;

    @Transactional
    public List<RecommendationCardResult> execute(final Long userId, final List<ResolvedRecommendationCard> cards) {
        final List<RecommendationCard> entities =
            cards.stream().map(card -> RecommendationCard.create(userId, card.bookId(), card.reasonLong())).toList();
        final List<RecommendationCard> saved = recommendationCardRepository.saveAll(entities);
        return IntStream.range(0, saved.size()).mapToObj((final int index) -> {
            final ResolvedRecommendationCard card = cards.get(index);
            return new RecommendationCardResult(saved.get(index).id(), card.bookId(), card.productId(), card.title(), card.author(),
                card.coverImageUrl(), card.price(), card.matchScore(), card.reasonShort(), card.reasonLong());
        }).toList();
    }
}
