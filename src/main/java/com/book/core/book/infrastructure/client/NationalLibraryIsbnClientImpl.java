package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import com.book.core.book.application.port.NationalLibraryIsbnClient;
import com.book.core.book.application.result.NewBookIsbnPage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class NationalLibraryIsbnClientImpl implements NationalLibraryIsbnClient {
    private static final int PAGE_SIZE = 100;
    private final RestClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    NationalLibraryIsbnClientImpl(
        final ObjectMapper mapper,
        @Value("${app.new-book.national-library-base-url:https://www.nl.go.kr}") final String baseUrl,
        @Value("${app.new-book.national-library-key:}") final String apiKey
    ) {
        this.http = BookCatalogHttp.connect(baseUrl);
        this.mapper = mapper;
        this.apiKey = apiKey;
    }

    @Override
    public NewBookIsbnPage findPaperBookIsbns(final LocalDate from, final LocalDate through, final int pageNumber) {
        final String body = BookCatalogHttp.get(http,
            (final var uri) -> uri.path("/seoji/SearchApi.do").queryParam("cert_key", "{key}").queryParam("result_style", "json")
                .queryParam("page_no", pageNumber).queryParam("page_size", PAGE_SIZE)
                .queryParam("start_publish_date", from.format(DateTimeFormatter.BASIC_ISO_DATE))
                .queryParam("end_publish_date", through.format(DateTimeFormatter.BASIC_ISO_DATE)).queryParam("ebook_yn", "N")
                .queryParam("form", "{form}").queryParam("sort", "INPUT_DATE").queryParam("order_by", "ASC").build(apiKey, "종이책"));
        try {
            final NationalLibraryIsbnResponse response = mapper.readValue(body, NationalLibraryIsbnResponse.class);
            if (response == null || response.totalCount() == null) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            final int totalCount = Integer.parseInt(response.totalCount());
            if (totalCount < 0
                || (totalCount > (long) (pageNumber - 1) * PAGE_SIZE && (response.docs() == null || response.docs().isEmpty()))) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            if (response.docs() == null) {
                return new NewBookIsbnPage(List.of(), false);
            }
            final List<String> isbns = response.docs().stream().filter(Objects::nonNull).map(NationalLibraryIsbnDocument::isbn13)
                .filter(Objects::nonNull).toList();
            return new NewBookIsbnPage(isbns, (long) pageNumber * PAGE_SIZE < totalCount);
        } catch (final JacksonException | IllegalArgumentException exception) {
            throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
        }
    }
}
