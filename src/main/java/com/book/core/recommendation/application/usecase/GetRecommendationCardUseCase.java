package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.domain.RecommendationCard;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetRecommendationCardUseCase {
    private final RecommendationCardRepositoryPort recommendationCardRepository;

    @Transactional(readOnly = true)
    public RecommendationCardDetailResult execute(final GetRecommendationCardCommand command) {
        final RecommendationCard card =
            recommendationCardRepository.findById(command.recommendationCardId()).filter(found -> found.isOwnedBy(command.userId()))
                .orElseThrow(() -> new CoreException(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));

        return new RecommendationCardDetailResult(card.id(), card.bookId(), card.reasonLong());
    }
}
