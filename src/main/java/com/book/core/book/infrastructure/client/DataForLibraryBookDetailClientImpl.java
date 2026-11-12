package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogException.Reason;
import com.book.core.book.application.port.DataForLibraryBookDetailClient;
import com.book.core.book.domain.Isbn13;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class DataForLibraryBookDetailClientImpl implements DataForLibraryBookDetailClient {
    private final RestClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    DataForLibraryBookDetailClientImpl(final ObjectMapper mapper,
        @Value("${app.new-book.data-for-library-base-url:https://www.data4library.kr}") final String baseUrl,
        @Value("${app.new-book.data-for-library-key:}") final String apiKey) {
        this.http = BookCatalogHttp.connect(baseUrl);
        this.mapper = mapper;
        this.apiKey = apiKey;
    }

    @Override
    public Optional<BookDetail> findBookDetail(final String isbn13) {
        if (apiKey.isBlank()) {
            throw new BookCatalogException(Reason.REJECTED);
        }
        final String body = BookCatalogHttp.get(http, (final var uri) -> uri.path("/api/srchDtlList").queryParam("authKey", "{key}")
            .queryParam("isbn13", isbn13).queryParam("loaninfoYN", "N").queryParam("format", "json").build(apiKey));
        try {
            final Envelope envelope = mapper.readValue(body, Envelope.class);
            if (envelope == null || envelope.response() == null || envelope.response().error() != null) {
                throw new BookCatalogException(Reason.TEMPORARY);
            }
            final List<Entry> detail = envelope.response().detail();
            if (detail == null) {
                return Optional.empty();
            }
            return detail.stream().filter(Objects::nonNull).map(Entry::book).filter(Objects::nonNull)
                .filter((final var book) -> Isbn13.normalize(book.isbn13()).filter(isbn13::equals).isPresent())
                .map(DataForLibraryBookDetailExternalMapper::toBookDetail).flatMap(Optional::stream).findFirst();
        } catch (final JacksonException | IllegalArgumentException exception) {
            throw new BookCatalogException(Reason.TEMPORARY);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(Response response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(List<Entry> detail, tools.jackson.databind.JsonNode error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(ExternalBook book) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExternalBook(String isbn13, @JsonProperty("bookname") String title, @JsonProperty("authors") String author, String publisher,
        @JsonProperty("class_no") String category, @JsonProperty("publication_date") String publishedAt, String description,
        @JsonProperty("bookImageURL") String cover) {
    }
}
