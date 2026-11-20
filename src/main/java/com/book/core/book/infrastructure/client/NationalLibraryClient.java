package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import com.book.core.book.application.result.NewBookCandidate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
class NationalLibraryClient {
    private static final int PAGE_SIZE = 100;
    private final BookCatalogHttpClient http;
    private final BookCatalogProperties properties;
    private final ObjectMapper mapper;

    NationalLibraryPage searchPaperBooks(final LocalDate from, final LocalDate through, final int pageNumber) {
        final String body = http.get(properties.nationalLibraryBaseUrl(),
            (final var uri) -> uri.path("/seoji/SearchApi.do").queryParam("cert_key", "{key}").queryParam("result_style", "json")
                .queryParam("page_no", pageNumber).queryParam("page_size", PAGE_SIZE)
                .queryParam("start_publish_date", from.format(DateTimeFormatter.BASIC_ISO_DATE))
                .queryParam("end_publish_date", through.format(DateTimeFormatter.BASIC_ISO_DATE)).queryParam("ebook_yn", "N")
                .queryParam("form", "{form}").queryParam("sort", "INPUT_DATE").queryParam("order_by", "ASC")
                .build(properties.nationalLibraryKey(), "종이책"));
        try {
            final JsonNode response = mapper.readTree(body);
            if ("ERROR".equals(text(response, "RESULT"))) {
                // 국립중앙도서관은 인증 오류 등을 HTTP 200의 RESULT=ERROR, ERR_CODE로 응답합니다. 예: ERR_CODE=011
                throw new BookCatalogException(BookCatalogFailureReason.REJECTED, text(response, "ERR_CODE"));
            }
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
                return new NationalLibraryPage(List.of(), false);
            }
            final List<NewBookCandidate> candidates = docs.valueStream().map(NationalLibraryClient::toCandidate)
                .filter(Objects::nonNull).toList();
            return new NationalLibraryPage(candidates, (long) pageNumber * PAGE_SIZE < totalCount);
        } catch (final JacksonException | IllegalArgumentException exception) {
            throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
        }
    }

    private static NewBookCandidate toCandidate(final JsonNode document) {
        final String isbn13 = text(document, "EA_ISBN");
        // 정보나루 상세는 출판 연도만 주므로 국중도의 실제 출간일, 없으면 출간 예정일을 사용합니다.
        LocalDate publishedAt = date(text(document, "REAL_PUBLISH_DATE"));
        if (publishedAt == null) {
            publishedAt = date(text(document, "PUBLISH_PREDATE"));
        }
        if (isbn13 == null || publishedAt == null) {
            return null;
        }
        return new NewBookCandidate(isbn13, publishedAt);
    }

    private static LocalDate date(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (final DateTimeParseException exception) {
            return null;
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
