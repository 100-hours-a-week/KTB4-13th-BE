package com.book.core.recommendation.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.product.application.usecase.FindProductByBookIdUseCase;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetRecommendationCardUseCase {
    private final RecommendationCardRepositoryPort recommendationCardRepository;
    private final GetBooksUseCase getBooksUseCase;
    private final FindProductByBookIdUseCase findProductByBookIdUseCase;

    @Transactional(readOnly = true)
    public RecommendationCardDetailResult execute(final GetRecommendationCardCommand command) {
        final RecommendationCard card =
            recommendationCardRepository.findById(command.recommendationCardId()).filter(found -> found.isOwnedBy(command.userId()))
                .orElseThrow(() -> new CoreException(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));

        final Book book = getBooksUseCase.execute(List.of(card.bookId())).stream().findFirst()
            .orElseThrow(() -> new CoreException(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));

        final var product = findProductByBookIdUseCase.execute(card.bookId());
        return RecommendationCardDetailResult.of(card, book, product.orElse(null));
    }
}
