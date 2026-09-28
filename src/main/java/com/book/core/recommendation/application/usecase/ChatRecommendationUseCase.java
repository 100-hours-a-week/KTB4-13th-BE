package com.book.core.recommendation.application.usecase;

import com.book.core.book.application.usecase.FindBooksByIdsUseCase;
import com.book.core.book.domain.Book;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.result.ResolvedRecommendationCard;
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
    private final FindBooksByIdsUseCase findBooksByIdsUseCase;

    public ChatRecommendationAiOutcome execute(final ChatRecommendationCommand command) {
        final AiRecommendationChatRequest aiRequest = new AiRecommendationChatRequest(command.userId(), command.consented(), command.spec(),
            command.message(), command.recentTurns(), command.excludeBookIds());
        final AiRecommendationChatResult aiResult = aiRecommendationClient.chat(aiRequest);

        final List<Long> bookIds = aiResult.cards().stream().map(AiRecommendationCard::bookId).toList();
        final Map<Long, Book> booksById =
            findBooksByIdsUseCase.execute(bookIds).stream().collect(Collectors.toMap(Book::id, Function.identity()));

        final List<ResolvedRecommendationCard> cards = aiResult.cards().stream().filter(card -> booksById.containsKey(card.bookId()))
            .map(card -> toResolvedCard(card, booksById.get(card.bookId()))).limit(MAX_CARDS).toList();

        return new ChatRecommendationAiOutcome(aiResult.spec(), aiResult.reply(), cards, aiResult.followup(), aiResult.buttons(),
            aiResult.degraded());
    }

    private ResolvedRecommendationCard toResolvedCard(final AiRecommendationCard card, final Book book) {
        return new ResolvedRecommendationCard(book.id(), book.title(), book.author(), book.coverImageUrl(), card.reasonLong());
    }
}
