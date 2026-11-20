package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import com.book.core.book.application.result.NewBookCandidate;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookCatalogClientTest {
    private static final String ISBN = "9788983921987";
    private final AtomicReference<String> query = new AtomicReference<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void 목록_응답의_전체_건수와_ISBN_출간일을_읽고_종이책_날짜_조건을_보낸다() {
        respond("/seoji/SearchApi.do", 200, """
            {"PAGE_NO":"1","TOTAL_COUNT":"3","docs":[
             {"EA_ISBN":"9788983921987","TITLE":"신간","REAL_PUBLISH_DATE":"","PUBLISH_PREDATE":"20261111"},
             {"EA_ISBN":"9788972756194","TITLE":"출간","REAL_PUBLISH_DATE":"20261020","PUBLISH_PREDATE":"20261101"},
             {"EA_ISBN":"9788950983529","TITLE":"날짜 없음","REAL_PUBLISH_DATE":"","PUBLISH_PREDATE":""}]}
            """);
        final var candidates = collector("test+key", "").collectCandidates(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 11));
        assertThat(candidates).containsExactly(new NewBookCandidate(ISBN, LocalDate.of(2026, 11, 11)),
            new NewBookCandidate("9788972756194", LocalDate.of(2026, 10, 20)));
        assertThat(query.get()).contains("cert_key=test%2Bkey", "start_publish_date=20261013", "end_publish_date=20261111", "ebook_yn=N",
            "page_no=1", "page_size=100", "form=%EC%A2%85%EC%9D%B4%EC%B1%85");
    }

    @Test
    void 정보나루_상세를_변환하고_연도만_있는_출판일과_선택값_누락을_허용한다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"detail":[{"book":{"isbn13":"9788983921987","bookname":"신간","authors":"저자","publisher":"출판사",
             "class_no":"813.7","publication_date":"2026","publication_year":"2026"}}]}}
            """);
        final var detail = collector("", "test+key").collectDetail(ISBN).orElseThrow();
        assertThat(detail.category()).isEqualTo("813.7");
        assertThat(detail.coverImageUrl()).isNull();
        assertThat(detail.description()).isNull();
        assertThat(query.get()).contains("authKey=test%2Bkey", "loaninfoYN=N", "format=json", "isbn13=" + ISBN);
    }

    @Test
    void 정보나루_분류번호가_비어_있으면_상세_없음으로_본다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"detail":[{"book":{"isbn13":"9788983921987","bookname":"신간","authors":"저자","publisher":"출판사",
             "class_no":"","publication_date":"2026"}}]}}
            """);
        assertThat(collector("", "key").collectDetail(ISBN)).isEmpty();
    }

    @Test
    void 호출_제한을_분류하고_응답이나_키를_예외에_담지_않는다() {
        respond("/api/srchDtlList", 429, "external response");
        final var collector = collector("", "sensitive-key");
        assertThatThrownBy(() -> collector.collectDetail(ISBN)).isInstanceOfSatisfying(BookCatalogException.class, (final var error) -> {
            assertThat(error.reason()).isEqualTo(BookCatalogFailureReason.RATE_LIMIT);
            assertThat(error.getMessage()).isEqualTo("RATE_LIMIT");
            assertThat(error.getCause()).isNull();
        });
    }

    @Test
    void 정보나루_오류_응답은_요청_거부로_분류한다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"errCode":"authErr","error":"인증정보가 일치하지 않습니다."}}
            """);
        final var collector = collector("", "key");
        assertThatThrownBy(() -> collector.collectDetail(ISBN)).isInstanceOfSatisfying(BookCatalogException.class, (final var error) -> {
            assertThat(error.reason()).isEqualTo(BookCatalogFailureReason.REJECTED);
            assertThat(error.externalCode()).isEqualTo("authErr");
        });
    }

    @Test
    void 정보나루에_없는_ISBN_응답은_상세_없음으로_본다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"errCode":"isbnMpngErr","error":"ISBN에 해당하는 도서가 없습니다."}}
            """);
        assertThat(collector("", "key").collectDetail(ISBN)).isEmpty();
    }

    @Test
    void 국중도_오류_응답은_요청_거부로_분류한다() {
        respond("/seoji/SearchApi.do", 200, """
            {"RESULT":"ERROR","ERR_CODE":"011","ERR_MESSAGE":"유효하지 않은 인증키 값입니다."}
            """);
        final var collector = collector("key", "");
        assertThatThrownBy(() -> collector.collectCandidates(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 11)))
            .isInstanceOfSatisfying(BookCatalogException.class, (final var error) -> {
                assertThat(error.reason()).isEqualTo(BookCatalogFailureReason.REJECTED);
                assertThat(error.externalCode()).isEqualTo("011");
            });
    }

    @Test
    void 응답_형식이_깨지면_일시_오류로_분류한다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"detail":"not-array"}}
            """);
        final var collector = collector("", "key");
        assertThatThrownBy(() -> collector.collectDetail(ISBN)).isInstanceOfSatisfying(BookCatalogException.class,
            (final var error) -> assertThat(error.reason()).isEqualTo(BookCatalogFailureReason.TEMPORARY));
    }

    private NewBookCollectorAdapter collector(final String nationalLibraryKey, final String jeongboNaruKey) {
        return new NewBookCollectorAdapter(
            BookCatalogClientConfiguration.createClient(NationalLibraryClient.class, baseUrl, "nationalLibraryKey", nationalLibraryKey),
            BookCatalogClientConfiguration.createClient(JeongboNaruClient.class, baseUrl, "jeongboNaruKey", jeongboNaruKey));
    }

    private void respond(final String path, final int status, final String body) {
        server.createContext(path, (final var exchange) -> {
            try (exchange) {
                query.set(exchange.getRequestURI().getRawQuery());
                final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
    }
}
