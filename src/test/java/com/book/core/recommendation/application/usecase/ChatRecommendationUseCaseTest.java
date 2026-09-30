package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class ChatRecommendationUseCaseTest {
    private final FakeAiRecommendationClient aiRecommendationClient = new FakeAiRecommendationClient();
    private final FakeBookRepositoryPort bookRepository = new FakeBookRepositoryPort();
    private final GetBooksUseCase getBooksUseCase = new GetBooksUseCase(bookRepository);
    private final ChatRecommendationUseCase useCase = new ChatRecommendationUseCase(aiRecommendationClient, getBooksUseCase);

    @Test
    void AI_응답의_카드를_Book_정보로_보강한다() {
        bookRepository.books.put(1L, book(1L));
        aiRecommendationClient.result = new AiRecommendationChatResult(Map.of("intent", "semantic"), "reply",
            List.of(new AiRecommendationCard(1L, "이유")), "followup", List.of("btn"), false);

        final var outcome = useCase.execute(command(Map.of("intent", "semantic"), List.of()));

        assertThat(outcome.cards()).hasSize(1);
        assertThat(outcome.cards().getFirst().title()).isEqualTo("제목1");
        assertThat(outcome.cards().getFirst().reasonLong()).isEqualTo("이유");
        assertThat(outcome.reply()).isEqualTo("reply");
        assertThat(outcome.degraded()).isFalse();
    }

    @Test
    void 존재하지_않는_Book의_카드는_제외한다() {
        aiRecommendationClient.result =
            new AiRecommendationChatResult(Map.of(), "reply", List.of(new AiRecommendationCard(999L, "이유")), null, List.of(), false);

        final var outcome = useCase.execute(command(Map.of(), List.of()));

        assertThat(outcome.cards()).isEmpty();
    }

    @Test
    void 카드는_최대_3개까지만_반환한다() {
        bookRepository.books.put(1L, book(1L));
        bookRepository.books.put(2L, book(2L));
        bookRepository.books.put(3L, book(3L));
        bookRepository.books.put(4L, book(4L));
        aiRecommendationClient.result =
            new AiRecommendationChatResult(Map.of(), "reply", List.of(new AiRecommendationCard(1L, "a"), new AiRecommendationCard(2L, "b"),
                new AiRecommendationCard(3L, "c"), new AiRecommendationCard(4L, "d")), null, List.of(), false);

        final var outcome = useCase.execute(command(Map.of(), List.of()));

        assertThat(outcome.cards()).hasSize(3);
    }

    @Test
    void 쿼리_userId가_인증된_userId와_다르면_AI를_호출하지_않고_403을_응답한다() {
        final var command = new ChatRecommendationCommand(42L, 43L, true, Map.of(), "메시지", List.of(), List.of());

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThat(aiRecommendationClient.called).isFalse();
    }

    private ChatRecommendationCommand command(final Map<String, Object> spec, final List<Long> exclude) {
        return new ChatRecommendationCommand(42L, 42L, true, spec, "메시지", List.of(), exclude);
    }

    private Book book(final long id) {
        return new Book(id, null, null, "제목" + id, "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
    }

    private static class FakeAiRecommendationClient implements AiRecommendationClient {
        AiRecommendationChatResult result;
        boolean called;

        @Override
        public AiRecommendationChatResult chat(final AiRecommendationChatRequest request) {
            called = true;
            return result;
        }
    }

    private static class FakeBookRepositoryPort implements BookRepositoryPort {
        final Map<Long, Book> books = new HashMap<>();

        @Override
        public List<Book> findAllByIdIn(final List<Long> ids) {
            return ids.stream().map(books::get).filter(Objects::nonNull).toList();
        }
    }
}
