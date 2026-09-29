package com.book.core.search.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.search.application.command.BookSearchSort;
import com.book.core.search.application.command.SearchBooksCommand;
import com.book.core.search.application.port.BookSearchClient;
import com.book.core.search.application.port.BookSearchRequest;
import com.book.core.search.application.port.BookSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchBooksUseCaseTest {
    private final FakeBookSearchClient bookSearchClient = new FakeBookSearchClient();
    private final SearchBooksUseCase useCase = new SearchBooksUseCase(bookSearchClient);

    @Test
    void 검색_조건을_그대로_AI_검색_요청으로_전달하고_결과를_반환한다() {
        final var result =
            useCase.execute(new SearchBooksCommand("투자 입문", "경제경영", 10000, 20000, 2020, 2024, BookSearchSort.NEWEST, "abc", 20));

        assertThat(bookSearchClient.request)
            .isEqualTo(new BookSearchRequest("투자 입문", "경제경영", 10000, 20000, 2020, 2024, BookSearchSort.NEWEST, "abc", 20));
        assertThat(result).isSameAs(bookSearchClient.result);
    }

    private static final class FakeBookSearchClient implements BookSearchClient {
        private final BookSearchResult result = new BookSearchResult(List.of(), null, null, null);
        private BookSearchRequest request;

        @Override
        public BookSearchResult search(final BookSearchRequest request) {
            this.request = request;
            return result;
        }
    }
}
