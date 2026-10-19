package com.book.core.recommendation.application.usecase;

import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.product.application.result.RecommendationProductResult;
import com.book.core.product.application.usecase.GetRecommendationProductsUseCase;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.RecommendationCardCandidate;
import com.book.core.recommendation.application.command.ResolveRecommendationCommand;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.result.ResolvedRecommendationCard;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRecommendationUseCase {
    private static final int MAX_CARDS = 3;

    private final AiRecommendationClient aiRecommendationClient;
    private final GetBooksUseCase getBooksUseCase;
    private final GetRecommendationProductsUseCase getRecommendationProductsUseCase;

    public ChatRecommendationAiOutcome execute(final ChatRecommendationCommand command) {

        final AiRecommendationChatRequest aiRequest = new AiRecommendationChatRequest(command.userId(), command.consented(), command.spec(),
            command.message(), command.recentTurns(), command.excludeBookIds());
        final AiRecommendationChatResult aiResult = aiRecommendationClient.chat(aiRequest);

        return resolve(new ResolveRecommendationCommand(aiResult.spec(), aiResult.reply(),
            aiResult.cards().stream().map((final AiRecommendationCard card) -> new RecommendationCardCandidate(card.bookId(),
                card.matchScore(), card.reasonShort(), card.reasonLong())).toList(),
            aiResult.followup(), aiResult.buttons(), aiResult.degraded()));
    }

    /** AI 통신 없이 추천 후보를 기존 도서·상품 조회 규칙으로 보강합니다. */
    public ChatRecommendationAiOutcome resolve(final ResolveRecommendationCommand aiResult) {
        final List<Long> bookIds = aiResult.cards().stream().map(RecommendationCardCandidate::bookId).toList();
        final Map<Long, Book> booksById =
            getBooksUseCase.execute(bookIds).stream().filter(Book::isActive).collect(Collectors.toMap(Book::id, Function.identity()));

        final List<RecommendationCardCandidate> selected = aiResult.cards().stream()
            .filter((final RecommendationCardCandidate card) -> booksById.containsKey(card.bookId())).limit(MAX_CARDS).toList();
        final Map<Long, RecommendationProductResult> products =
            getRecommendationProductsUseCase.execute(selected.stream().map(RecommendationCardCandidate::bookId).toList());
        final List<ResolvedRecommendationCard> cards = selected.stream()
            .map(
                (final RecommendationCardCandidate card) -> toResolvedCard(card, booksById.get(card.bookId()), products.get(card.bookId())))
            .toList();

        return new ChatRecommendationAiOutcome(aiResult.spec(), aiResult.reply(), cards, aiResult.followup(), aiResult.buttons(),
            aiResult.degraded());
    }

    private ResolvedRecommendationCard toResolvedCard(final RecommendationCardCandidate card, final Book book,
        final RecommendationProductResult product) {
        Long productId = null;
        BigDecimal price = null;
        if (product != null) {
            productId = product.productId();
            price = product.price();
        }
        return new ResolvedRecommendationCard(book.id(), productId, book.title(), book.author(), book.coverImageUrl(), price,
            card.matchScore(), card.reasonShort(), card.reasonLong());
    }
}
