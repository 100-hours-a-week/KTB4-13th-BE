package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogException.Reason;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class BookCatalogClientTest {
    private static final String ISBN = "9788983921987";
    private final ObjectMapper mapper = new ObjectMapper();
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
    void 목록_응답의_전체_건수와_ISBN을_읽고_종이책_날짜_조건을_보낸다() {
        respond("/seoji/SearchApi.do", 200, """
            {"PAGE_NO":"1","TOTAL_COUNT":"101","docs":[{"EA_ISBN":"9788983921987","TITLE":"신간"}]}
            """);
        final var client = new NationalLibraryIsbnClientImpl(mapper, baseUrl, "test+key");
        final var page = client.findPaperBookIsbns(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 11), 1);
        assertThat(page.candidateIsbns()).containsExactly(ISBN);
        assertThat(page.hasNext()).isTrue();
        assertThat(query.get()).contains("cert_key=test%2Bkey", "start_publish_date=20261013", "end_publish_date=20261111", "ebook_yn=N",
            "page_no=1", "page_size=100", "form=%EC%A2%85%EC%9D%B4%EC%B1%85");
    }

    @Test
    void 정보나루_상세를_변환하고_선택값_누락을_허용한다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"detail":[{"book":{"isbn13":"9788983921987","bookname":"신간","authors":"저자","publisher":"출판사",
             "class_no":"813.7","publication_date":"2026-11-01"}}]}}
            """);
        final var client = new DataForLibraryBookDetailClientImpl(mapper, baseUrl, "test+key");
        final var detail = client.findBookDetail(ISBN).orElseThrow();
        assertThat(detail.publishedAt()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(detail.category()).isEqualTo("813.7");
        assertThat(detail.coverImageUrl()).isNull();
        assertThat(detail.description()).isNull();
        assertThat(query.get()).contains("authKey=test%2Bkey", "loaninfoYN=N", "format=json", "isbn13=" + ISBN);
    }

    @Test
    void 호출_제한을_분류하고_응답이나_키를_예외에_담지_않는다() {
        respond("/api/srchDtlList", 429, "external response");
        final var client = new DataForLibraryBookDetailClientImpl(mapper, baseUrl, "sensitive-key");
        assertThatThrownBy(() -> client.findBookDetail(ISBN)).isInstanceOfSatisfying(BookCatalogException.class, (final var error) -> {
            assertThat(error.reason()).isEqualTo(Reason.RATE_LIMIT);
            assertThat(error.getMessage()).isEqualTo("RATE_LIMIT");
            assertThat(error.getCause()).isNull();
        });
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
