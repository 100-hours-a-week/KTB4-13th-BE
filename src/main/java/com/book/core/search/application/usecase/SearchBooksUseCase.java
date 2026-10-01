package com.book.core.search.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.usecase.FindProductIdsByBookIdsUseCase;
import com.book.core.search.application.command.SearchBooksCommand;
import com.book.core.search.application.port.BookSearchClient;
import com.book.core.search.application.port.BookSearchItem;
import com.book.core.search.application.port.BookSearchRequest;
import com.book.core.search.application.port.BookSearchResult;
import com.book.core.search.application.result.SearchBookItemResult;
import com.book.core.search.application.result.SearchBooksResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchBooksUseCase {
    private final BookSearchClient bookSearchClient;
    private final FindProductIdsByBookIdsUseCase findProductIdsByBookIdsUseCase;

    public SearchBooksResult execute(final SearchBooksCommand command) {
        final BookSearchResult searchResult =
            bookSearchClient.search(new BookSearchRequest(command.query(), command.category(), command.priceMin(), command.priceMax(),
                command.pubYearFrom(), command.pubYearTo(), command.sort(), command.cursor(), command.size()));

        // AI search returns only bookIds; the product detail link needs the active product's own ID.
        final Map<Long, Long> productIdsByBookId =
            findProductIdsByBookIdsUseCase.execute(searchResult.items().stream().map(BookSearchItem::bookId).toList());
        final List<SearchBookItemResult> items =
            searchResult.items().stream().map((final var item) -> toItemResult(item, productIdsByBookId.get(item.bookId()))).toList();
        return new SearchBooksResult(items, searchResult.nextCursor(), searchResult.fallbackMessage(), searchResult.degraded());
    }

    private static SearchBookItemResult toItemResult(final BookSearchItem item, final Long productId) {
        return new SearchBookItemResult(item.bookId(), productId, item.title(), item.author(), item.publisher(), item.price(),
            item.inStock(), item.coverUrl());
    }
}
