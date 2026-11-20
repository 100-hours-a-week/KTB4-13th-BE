package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.domain.Isbn13;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
class JeongboNaruClient {
    private static final String MISSING_ISBN_ERROR_CODE = "isbnMpngErr";
    private final BookCatalogHttpClient http;
    private final BookCatalogProperties properties;
    private final ObjectMapper mapper;

    Optional<BookDetail> findBookDetail(final String isbn13) {
        final String body = http.get(properties.dataForLibraryBaseUrl(),
            (final var uri) -> uri.path("/api/srchDtlList").queryParam("authKey", "{key}").queryParam("isbn13", isbn13)
                .queryParam("loaninfoYN", "N").queryParam("format", "json").build(properties.dataForLibraryKey()));
        try {
            final JsonNode response = mapper.readTree(body).path("response");
            if (!response.isObject()) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            if (!response.path("error").isMissingNode()) {
                final String errorCode = response.path("errCode").asString(null);
                // 정보나루에 없는 ISBN은 도서별 결과이므로 배치를 멈추지 않고 상세 없음으로 봅니다.
                if (MISSING_ISBN_ERROR_CODE.equals(errorCode)) {
                    return Optional.empty();
                }
                // 정보나루는 인증 오류 등을 HTTP 200의 errCode·error 필드로 응답합니다. 예: errCode=authErr
                throw new BookCatalogException(BookCatalogFailureReason.REJECTED, errorCode);
            }
            final JsonNode detail = response.path("detail");
            if (detail.isMissingNode() || detail.isNull()) {
                return Optional.empty();
            }
            if (!detail.isArray()) {
                throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
            }
            return detail.valueStream().map((final var entry) -> entry.path("book")).filter(JsonNode::isObject)
                .filter((final var book) -> Isbn13.normalize(JeongboNaruExternalMapper.text(book, "isbn13")).filter(isbn13::equals)
                    .isPresent())
                .map(JeongboNaruExternalMapper::toBookDetail).flatMap(Optional::stream).findFirst();
        } catch (final JacksonException | IllegalArgumentException exception) {
            throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
        }
    }
}
