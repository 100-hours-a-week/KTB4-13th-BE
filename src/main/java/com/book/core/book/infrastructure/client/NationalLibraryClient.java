package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import com.book.core.book.application.port.NewBookIsbnFinder;
import com.book.core.book.application.result.NewBookIsbnPage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
class NationalLibraryClient implements NewBookIsbnFinder {
    private static final int PAGE_SIZE = 100;
    private final BookCatalogHttpClient http;
    private final BookCatalogProperties properties;
    private final ObjectMapper mapper;

    @Override
    public NewBookIsbnPage findPaperBookIsbns(final LocalDate from, final LocalDate through, final int pageNumber) {
        final String body = http.get(properties.nationalLibraryBaseUrl(),
            (final var uri) -> uri.path("/seoji/SearchApi.do").queryParam("cert_key", "{key}").queryParam("result_style", "json")
                .queryParam("page_no", pageNumber).queryParam("page_size", PAGE_SIZE)
                .queryParam("start_publish_date", from.format(DateTimeFormatter.BASIC_ISO_DATE))
                .queryParam("end_publish_date", through.format(DateTimeFormatter.BASIC_ISO_DATE)).queryParam("ebook_yn", "N")
                .queryParam("form", "{form}").queryParam("sort", "INPUT_DATE").queryParam("order_by", "ASC")
                .build(properties.nationalLibraryKey(), "종이책"));
        try {
            final JsonNode response = mapper.readTree(body);
            final String total = text(response, "TOTAL_COUNT");
            if (total == null) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            final int totalCount = Integer.parseInt(total);
            final JsonNode docs = response.path("docs");
            final boolean noDocs = docs.isMissingNode() || docs.isNull();
            if (!noDocs && !docs.isArray()) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            if (totalCount < 0 || (totalCount > (long) (pageNumber - 1) * PAGE_SIZE && (noDocs || docs.isEmpty()))) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            if (noDocs) {
                return new NewBookIsbnPage(List.of(), false);
            }
            final List<String> isbns = docs.valueStream().map((final var document) -> text(document, "EA_ISBN"))
                .filter(Objects::nonNull).toList();
            return new NewBookIsbnPage(isbns, (long) pageNumber * PAGE_SIZE < totalCount);
        } catch (final JacksonException | IllegalArgumentException exception) {
            throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
        }
    }

    private static String text(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        if (!value.isValueNode() || value.isNull()) {
            return null;
        }
        return value.asString();
    }
}
