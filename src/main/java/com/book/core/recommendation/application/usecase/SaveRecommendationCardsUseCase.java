package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.ResolvedRecommendationCard;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SaveRecommendationCardsUseCase {
    private final RecommendationCardRepositoryPort recommendationCardRepository;

    @Transactional
    public List<RecommendationCard> execute(final Long userId, final List<ResolvedRecommendationCard> cards) {
        final List<RecommendationCard> entities =
            cards.stream().map(card -> RecommendationCard.create(userId, card.bookId(), card.reasonLong())).toList();
        return recommendationCardRepository.saveAll(entities);
    }
}
