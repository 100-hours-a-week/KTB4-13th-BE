package com.book.core.search.api.converter;

import com.book.core.search.application.command.BookSearchSort;
import com.book.core.search.application.command.SearchBooksCommand;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SearchCommandConverter {
    public SearchBooksCommand toSearchBooksCommand(final String query, final String category, final Integer priceMin,
        final Integer priceMax, final Integer pubYearFrom, final Integer pubYearTo, final String sort, final String cursor,
        final int size) {
        return new SearchBooksCommand(query, category, priceMin, priceMax, pubYearFrom, pubYearTo,
            BookSearchSort.valueOf(sort.toUpperCase(Locale.ROOT)), cursor, size);
    }
}
